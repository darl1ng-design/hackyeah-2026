package pl.hubmalopolski.hub.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.repo.InnovationRepository;
import java.util.*;

/**
 * Matchmaking spoleczny (modul I): hybrid — pgvector (semantyka) + BM25 pg_textsearch
 * (dokladne slowa), polaczone reciprocal-rank fusion, nastepnie rerank LLM z
 * uzasadnieniem "dlaczego to pasuje". Bez modelu LLM fallbackuje do kolejnosci RRF
 * — demo nigdy nie pada.
 */
@Service
public class MatchmakingService {

    static final String META_INNOVATION_ID = "innovationId";
    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final InnovationRepository innovations;

    @Value("${hub.match.top-k:8}")
    private int vectorTopK;

    @Value("${hub.match.bm25-top-k:8}")
    private int bm25TopK;

    public MatchmakingService(VectorStore vectorStore, ChatClient chatClient, InnovationRepository innovations) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.innovations = innovations;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Indeksuje innowacje w vector_store (wyzwalane przy seedzie i przy tworzeniu przez admina).
     */
    public void index(Innovation in) {
        String oldVectorId = in.getVectorId();
        String text = String.join("\n",
                "Tytul: " + in.getTitle(),
                "Opis: " + nullSafe(in.getSummary()) + " " + nullSafe(in.getDescription()),
                "Grupa docelowa: " + nullSafe(in.getTargetGroup()),
                "Region: " + (in.getRegion() == null ? "" : in.getRegion().getLabel()),
                "Obszar: " + (in.getArea() != null ? in.getArea().getName() : ""));
        Document doc = Document.builder()
                .text(text)
                .metadata(META_INNOVATION_ID, in.getId())
                .metadata("area", in.getArea() != null ? in.getArea().getName() : "")
                .build();
        vectorStore.add(List.of(doc));
        in.setVectorId(doc.getId());
        if (oldVectorId != null && !oldVectorId.equals(doc.getId())) {
            try { vectorStore.delete(List.of(oldVectorId)); }
            catch (Exception e) { log.warn("Nie można usunąć poprzedniego wektora {}", oldVectorId, e); }
        }
    }

    public void removeIndex(Innovation in) {
        if (in.getVectorId() == null) return;
        vectorStore.delete(List.of(in.getVectorId()));
        in.setVectorId(null);
    }

    public List<MatchResult> match(String problemText) {
        // 1) wektory
        Map<Long, Double> byVector = new LinkedHashMap<>();
        try {
            List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(problemText).topK(vectorTopK).similarityThresholdAll().build());
            for (Document d : docs) {
                Object id = d.getMetadata().get(META_INNOVATION_ID);
                if (id instanceof Number n) {
                    byVector.put(n.longValue(), d.getScore() != null ? d.getScore() : 0.0);
                }
            }
        } catch (Exception e) {
            log.warn("Wyszukiwanie wektorowe niedostepne: {}", e.getMessage());
        }

        // 2) BM25 (pg_textsearch) — dokladne dopasowanie slow
        Map<Long, Double> byBm25 = new LinkedHashMap<>();
        try {
            for (Object[] row : innovations.searchBm25(problemText, bm25TopK)) {
                byBm25.put(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue());
            }
        } catch (Exception e) {
            log.warn("BM25 niedostepne: {}", e.getMessage());
        }
        List<Long> bm25Ranked = new ArrayList<>(byBm25.keySet());

        // 3) reciprocal-rank fusion wektor + BM25
        Map<Long, Double> scores = new HashMap<>();
        int rank = 0;
        for (Long id : byVector.keySet()) scores.merge(id, 1.0 / (60 + ++rank), Double::sum);
        rank = 0;
        for (Long id : bm25Ranked) scores.merge(id, 1.0 / (60 + ++rank), Double::sum);

        if (scores.isEmpty()) return matchKeywords(problemText);

        // tylko top-5 fuzji trafia do reranku LLM — kazdy kandydat to ~50 tokenow
        // promptu i ~30 dekodowania; na CPU (4 vCPU, ~12 tok/s) liczy sie kazda linijka.
        List<Long> fusedIds = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(5).map(Map.Entry::getKey).toList();

        List<Innovation> candidates = fusedIds.stream()
                .map(innovations::findById)
                .filter(Optional::isPresent).map(Optional::get)
                .filter(Innovation::isPublished)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        return finish(problemText, candidates, byVector, byBm25);
    }

    private List<MatchResult> matchKeywords(String problemText) {
        List<String> terms = Arrays.stream(problemText.toLowerCase(Locale.ROOT)
                        .split("[^\\p{L}\\p{N}]+"))
                .filter(term -> term.length() >= 4)
                .distinct().limit(8).toList();
        if (terms.isEmpty()) return List.of();

        Map<Long, Innovation> found = new HashMap<>();
        Map<Long, Set<String>> matchingTerms = new HashMap<>();
        for (String term : terms) {
            try {
                for (Innovation innovation : innovations.searchKeyword(term, PageRequest.of(0, 20))) {
                    if (innovation.getId() == null) continue;
                    found.put(innovation.getId(), innovation);
                    matchingTerms.computeIfAbsent(innovation.getId(), id -> new LinkedHashSet<>()).add(term);
                }
            } catch (Exception e) {
                log.warn("Wyszukiwanie slow kluczowych niedostepne: {}", e.getMessage());
                return List.of();
            }
        }
        return found.values().stream()
                .sorted(Comparator.<Innovation>comparingInt(
                        innovation -> matchingTerms.get(innovation.getId()).size()).reversed()
                        .thenComparing(Innovation::getId))
                .limit(5)
                .map(innovation -> {
                    Set<String> hits = matchingTerms.get(innovation.getId());
                    return new MatchResult(innovation,
                            "Pasujące słowa z opisu problemu: " + String.join(", ", hits) + ".",
                            (double) hits.size() / terms.size());
                })
                .toList();
    }

    private List<MatchResult> finish(String problemText, List<Innovation> candidates,
                                     Map<Long, Double> byVector, Map<Long, Double> byBm25) {
        if (candidates.isEmpty()) return List.of();
        // uczciwy wynik: cosine z wektorow albo znormalizowany BM25 (skalowany do 0.9),
        // NIGDY score/max (to dawalo top wynikowi falszywe 100%).
        double maxBm25 = byBm25.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        java.util.function.Function<Long, Double> displayScore = id -> {
            double v = byVector.getOrDefault(id, 0.0);
            if (maxBm25 > 0) v = Math.max(v, byBm25.getOrDefault(id, 0.0) / maxBm25 * 0.9);
            return v;
        };
        try {
            StringBuilder sb = new StringBuilder();
            for (Innovation in : candidates) {
                String summary = nullSafe(in.getSummary());
                if (summary.length() > 80) summary = summary.substring(0, 80);
                sb.append("- id=").append(in.getId()).append(" | ").append(in.getTitle())
                        .append(" | ").append(summary).append("\n");
            }
            MatchExplanations out = chatClient.prompt()
                    .user(u -> u.text("""
                            Problem: {problem}

                            Innowacje:
                            {candidates}

                            Wybierz max 5 pasujacych. Zwracaj JSON: matches z polami
                            innovationId (liczba) i why (POJEDYNCZE zdanie po polsku, max 12 slow).
                            """).param("problem", problemText).param("candidates", sb.toString()))
                    .options(OpenAiChatOptions.builder().maxTokens(400).temperature(0.0).extraBody(java.util.Map.of("enable_thinking", false)))
                    .call()
                    .entity(MatchExplanations.class);
            if (out != null && out.matches() != null && !out.matches().isEmpty()) {
                Map<Long, Innovation> byId = new HashMap<>();
                candidates.forEach(i -> byId.put(i.getId(), i));
                List<MatchResult> results = new ArrayList<>();
                for (MatchExplanations.MatchExplanation m : out.matches()) {
                    Innovation in = byId.get(m.innovationId());
                    if (in != null) {
                        String why = m.why() == null || m.why().isBlank()
                                ? "Podobny problem lub odbiorca — dopasowanie hybrydowe (wektor + BM25)."
                                : m.why().trim();
                        results.add(new MatchResult(in, why, displayScore.apply(in.getId())));
                    }
                }
                // LLM decyduje KTORE trafiaja na liste (i daje why); wyswietlane % to
                // similarity — wiec sortujemy po niej, inaczej top wyniku moze byc
                // ponizej slabzego (sprzecznosc widoczna dla uzytkownika).
                results.sort(Comparator.comparingDouble(MatchResult::score).reversed());
                if (!results.isEmpty()) return results;
            }
        } catch (Exception e) {
            log.warn("Rerank LLM niedostepny ({}), uzywam kolejnosci wektorowej", e.getMessage());
        }
        // fallback: kolejnosc RRF (wektor + BM25), generyczne uzasadnienie
        Map<Long, Double> rrf = new HashMap<>();
        int r = 0;
        for (Long id : byVector.keySet()) rrf.merge(id, 1.0 / (60 + ++r), Double::sum);
        r = 0;
        for (Long id : byBm25.keySet()) rrf.merge(id, 1.0 / (60 + ++r), Double::sum);
        return candidates.stream()
                .sorted(Comparator.comparingDouble((Innovation i) -> rrf.getOrDefault(i.getId(), 0.0)).reversed())
                .limit(5)
                .map(i -> new MatchResult(i, "Podobny problem lub odbiorca — dopasowanie hybrydowe (wektor + BM25).",
                        displayScore.apply(i.getId())))
                .toList();
    }
}
