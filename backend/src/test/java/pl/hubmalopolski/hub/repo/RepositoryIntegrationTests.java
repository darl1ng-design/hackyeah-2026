package pl.hubmalopolski.hub.repo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import pl.hubmalopolski.hub.catalog.InnovationFilters;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;
import pl.hubmalopolski.hub.domain.Idea;
import pl.hubmalopolski.hub.domain.IdeaModerationStatus;
import pl.hubmalopolski.hub.domain.IdeaStage;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.domain.InnovationStatus;
import pl.hubmalopolski.hub.domain.ProblemReport;
import pl.hubmalopolski.hub.domain.Region;
import pl.hubmalopolski.hub.domain.ReportMatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class RepositoryIntegrationTests {
    @Autowired InnovationRepository innovations;
    @Autowired IdeaRepository ideas;
    @Autowired AppUserRepository users;
    @Autowired ProblemReportRepository reports;
    @Autowired ReportMatchRepository matches;

    @Test
    void regionAndTextFiltersAreAppliedBeforePagination() {
        innovations.save(new Innovation("Pomoc seniorom", null, null, null,
                InnovationStatus.ROZWOJ, Region.MALOPOLSKA, null));
        innovations.save(new Innovation("Druga pomoc seniorom", null, null, null,
                InnovationStatus.ROZWOJ, Region.MALOPOLSKA, null));
        innovations.save(new Innovation("Pomoc seniorom", null, null, null,
                InnovationStatus.ROZWOJ, Region.POLSKA, null));

        Page<Innovation> page = innovations.findAll(
                InnovationFilters.matching(null, "seniorom", Region.MALOPOLSKA),
                PageRequest.of(0, 1, Sort.by("title")));

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void percentSignInSearchIsLiteral() {
        innovations.save(new Innovation("Oferta 100%", null, null, null,
                InnovationStatus.ROZWOJ, Region.MALOPOLSKA, null));
        innovations.save(new Innovation("Oferta 100 razy", null, null, null,
                InnovationStatus.ROZWOJ, Region.MALOPOLSKA, null));

        Page<Innovation> page = innovations.findAll(InnovationFilters.matching(null, "100%", null),
                PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals("Oferta 100%", page.getContent().get(0).getTitle());
    }

    @Test
    void ownerAndReportMatchesCanBeReadAfterSaving() {
        AppUser owner = users.save(new AppUser("anna@example.org", "hash", "Anna", AppUserRole.MEMBER));
        Idea idea = ideas.save(new Idea("Pomysł", null, null, IdeaStage.MYSL, null,
                "Anna", owner));
        Innovation innovation = innovations.save(new Innovation("Pomoc", null, null, null,
                InnovationStatus.ROZWOJ, Region.MALOPOLSKA, null));
        ProblemReport report = reports.save(new ProblemReport("Opis problemu", Region.MALOPOLSKA, "Anna"));
        matches.save(new ReportMatch(report, innovation, 0, "Pasuje do problemu", 0.8));

        assertEquals(IdeaModerationStatus.PENDING, idea.getModerationStatus());
        assertEquals(1, ideas.findByOwnerEmailOrderByCreatedAtDesc("anna@example.org").size());
        assertTrue(ideas.findByModerationStatusOrderByCreatedAtDesc(IdeaModerationStatus.APPROVED).isEmpty());
        assertEquals("Pasuje do problemu",
                matches.findByReportIdOrderByRankOrderAsc(report.getId()).get(0).getWhy());
    }
}
