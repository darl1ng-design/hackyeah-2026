package pl.hubmalopolski.hub.match;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.domain.InnovationStatus;
import pl.hubmalopolski.hub.domain.Region;
import pl.hubmalopolski.hub.repo.InnovationRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchmakingServiceTests {
    @Test
    void keywordResultIsReturnedWhenVectorAndBm25SearchAreUnavailable() {
        VectorStore vectors = mock(VectorStore.class);
        ChatClient chat = mock(ChatClient.class);
        InnovationRepository innovations = mock(InnovationRepository.class);
        Innovation innovation = new Innovation("Telefon dla seniora", "Wsparcie seniorów",
                "Wolontariusze dzwonią do osób samotnych", "Seniorzy",
                InnovationStatus.WDROZONA, Region.MALOPOLSKA, null);
        ReflectionTestUtils.setField(innovation, "id", 17L);
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(innovations.searchBm25(anyString(), anyInt())).thenThrow(new IllegalStateException("BM25 offline"));
        when(innovations.searchKeyword(eq("senior"), any(Pageable.class)))
                .thenReturn(List.of(innovation));

        MatchmakingService service = new MatchmakingService(vectors, chat, innovations);
        ReflectionTestUtils.setField(service, "vectorTopK", 8);
        ReflectionTestUtils.setField(service, "bm25TopK", 8);

        List<MatchResult> results = service.match("Senior potrzebuje kontaktu");

        assertEquals(1, results.size());
        assertEquals("Telefon dla seniora", results.get(0).innovation().getTitle());
        assertFalse(results.get(0).why().isBlank());
    }
}
