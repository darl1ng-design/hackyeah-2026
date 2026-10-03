package pl.hubmalopolski.hub.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.repo.InnovationRepository;

import java.util.*;

/**
 * Matchmaking spoleczny (modul I): hybrid wektor + slowa kluczowe, nastepnie
 * rerank LLM z uzasadnieniem "dlaczego to pasuje". Bez klucza API / bledu LLM
 * fallbackuje do kolejnosci wektorowej — demo nigdy nie pada.
 */
@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);
    static final String META_INNOVATION_ID = "innovationId";

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final InnovationRepository innovations;

    @Value("${hub.match.top-k:8}")
    private int vectorTopK;

    public MatchmakingService(VectorStore vectorStore, ChatClient chatClient, InnovationRepository innovations) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.innovations = innovations;
    }

    /** Indeksuje innowacje w vector_store (wyzwalane przy seedzie i przy tworzeniu przez admina). */
    public void index(Innovation in) {
        String text = String.join("\n",
                "Tytul: " + in.getTitle(),
                "Opis: " + nullSafe(in.getSummary()) + " " + nullSafe(in.getDescription()),
                "Grupa docelowa: " + nullSafe(in.getTargetGroup()),
                "Region: " + nullSafe(in.getRegion()),
                "Obszar: " + (in.getArea() != null ? in.getArea().getName() : ""));
        Document doc = Document.builder()
                .text(text)
                .metadata(META_INNOVATION_ID, in.getId())
                .metadata("area", in.getArea() != null ? in.getArea().getName() : "")
                .build();
        vectorStore.add(List.of(doc));
        in.setVectorId(doc.getId());
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

        // 2) slowa kluczowe (LIKE) — wypelnienie i fallback
        List<Innovation> candidates = new ArrayList<>();
        Map<Long, Double> scores = new HashMap<>(byVector);
        for (Innovation in : innovations.findAll()) {
            if (!byVector.containsKey(in.getId())) {
                String hay = (nullSafe(in.getTitle()) + " " + nullSafe(in.getSummary()) + " "
                        + nullSafe(in.getDescription()) + " " + nullSafe(in.getTargetGroup())).toLowerCase();
                for (String w : problemText.toLowerCase().split("\\W+")) {
                    if (w.length() >= 5 && hay.contains(w)) {
                        candidates.add(in);
                        scores.putIfAbsent(in.getId(), 0.3);
                        break;
                    }
                }
            }
        }
        byVector.keySet().stream()
                .map(innovations::findById)
                .filter(Optional::isPresent).map(Optional::get)
                .forEach(candidates::add);
        return finish(problemText, candidates, scores);
    }

    private List<MatchResult> finish(String problemText, List<Innovation> candidates, Map<Long, Double> scores) {
        if (candidates.isEmpty()) return List.of();
        try {
            StringBuilder sb = new StringBuilder();
            for (Innovation in : candidates) {
                sb.append("- id=").append(in.getId()).append(" | ").append(in.getTitle())
                  .append(" | ").append(nullSafe(in.getSummary()))
                  .append(" | grupa: ").append(nullSafe(in.getTargetGroup())).append("\n");
            }
            MatchExplanations out = chatClient.prompt()
                    .user(u -> u.text("""
                        Problem zgloszony przez mieszkanca:
                        {problem}

                        Kandydujace innowacje spoleczne:
                        {candidates}

                        Wybierz maksymalnie 5 najlepiej dopasowanych innowacji i dla kazanej
                        napisz jedno zdanie po polsku: dlaczego pasuje do tego problemu.
                        """).param("problem", problemText).param("candidates", sb.toString()))
                    .call()
                    .entity(MatchExplanations.class);
            if (out != null && out.matches() != null && !out.matches().isEmpty()) {
                Map<Long, Innovation> byId = new HashMap<>();
                candidates.forEach(i -> byId.put(i.getId(), i));
                List<MatchResult> results = new ArrayList<>();
                for (MatchExplanations.MatchExplanation m : out.matches()) {
                    Innovation in = byId.get(m.innovationId());
                    if (in != null) results.add(new MatchResult(in, m.why(), scores.getOrDefault(in.getId(), 0.0)));
                }
                if (!results.isEmpty()) return results;
            }
        } catch (Exception e) {
            log.warn("Rerank LLM niedostepny ({}), uzywam kolejnosci wektorowej", e.getMessage());
        }
        // fallback: kolejnosc wektorowa, generyczne uzasadnienie
        return candidates.stream()
                .sorted(Comparator.comparingDouble((Innovation i) -> scores.getOrDefault(i.getId(), 0.0)).reversed())
                .limit(5)
                .map(i -> new MatchResult(i, "Podobny problem w opisie innowacji — dopasowanie kluczowe.",
                        scores.getOrDefault(i.getId(), 0.0)))
                .toList();
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}
