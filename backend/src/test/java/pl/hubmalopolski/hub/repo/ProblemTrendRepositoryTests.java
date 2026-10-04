package pl.hubmalopolski.hub.repo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import pl.hubmalopolski.hub.domain.ChallengeArea;
import pl.hubmalopolski.hub.domain.ProblemReport;
import pl.hubmalopolski.hub.domain.Region;
import pl.hubmalopolski.hub.domain.ReportStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class ProblemTrendRepositoryTests {
    @Autowired ProblemReportRepository reports;
    @Autowired ChallengeAreaRepository areas;

    @Test
    void countsNeedsByAreaRegionStatusAndMonthWithinRange() {
        ChallengeArea area = areas.save(new ChallengeArea("Samotność", "Opis"));
        ProblemReport first = new ProblemReport("Potrzeba wsparcia", Region.MALOPOLSKA, null);
        first.setArea(area);
        reports.save(first);
        ProblemReport second = new ProblemReport("Potrzeba kontaktu", Region.MALOPOLSKA, null);
        second.setStatus(ReportStatus.W_REALIZACJI);
        reports.save(second);

        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2027-01-01T00:00:00Z");
        assertEquals(2, reports.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to));
        assertEquals(2L, reports.countByRegionInPeriod(from, to).get(0)[1]);
        assertEquals(2, reports.countByAreaInPeriod(from, to).size());
        assertEquals(2, reports.countByStatusInPeriod(from, to).size());
        assertEquals(2L, reports.countByMonthInPeriod(from, to).get(0)[2]);
        assertEquals(0, reports.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                Instant.parse("2020-01-01T00:00:00Z"), Instant.parse("2021-01-01T00:00:00Z")));
    }
}
