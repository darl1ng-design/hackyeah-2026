package pl.hubmalopolski.hub.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import pl.hubmalopolski.hub.domain.ChallengeArea;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.match.MatchmakingService;
import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;

import java.util.List;

/** Zaladuje przykladowe dane ROPS (dozwolone przez regulamin) i indeksuje je wektorowo. */
@Component
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final ChallengeAreaRepository areas;
    private final InnovationRepository innovations;
    private final MatchmakingService matchmaking;

    @Value("${hub.seed-vectors:true}")
    private boolean seedVectors;

    public SeedRunner(ChallengeAreaRepository areas, InnovationRepository innovations, MatchmakingService matchmaking) {
        this.areas = areas; this.innovations = innovations; this.matchmaking = matchmaking;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (areas.count() > 0) return;

        ChallengeArea aging = areas.save(new ChallengeArea("Starzenie sie spoleczenstwa",
                "Wsparcie osob starszych, samotnosc, opieke i aktywnosc."));
        ChallengeArea mental = areas.save(new ChallengeArea("Zdrowie psychiczne",
                "Kryzysy, depresja, wsparcie mlodziezy i doroslych."));
        ChallengeArea digital = areas.save(new ChallengeArea("Wykluczenie cyfrowe",
                "Nierowny dostep do kompetencji i uslug cyfrowych."));
        ChallengeArea services = areas.save(new ChallengeArea("Dostep do uslug spolecznych",
                "Koordynacja i dostepnosc uslug, takze dla osob z szczególnymi potrzebami."));

        Innovation sadzcy = new Innovation("Sascy seniorzy", "Siec sasiedzka wolontariuszy odwiedzajacych samotnych seniorow.",
                "Lokalni wolontariusze regularnie odwiedzaja samotne osoby starsze, wspolnie robia zakupy i spedzaja czas. Sprawdzona metoda ograniczania samotnosci w malych gminach.",
                "Seniorzy 70+, mieszkancy wsi", "WDROZONA", "powiat myslenicki", null);
        sadzcy.setArea(aging);
        Innovation telefon = new Innovation("Telefon zaufania dla mlodziezy 116 111", "Bezpłatna, anonimowa linia wsparcia emocjonalnego.",
                "Mlodziez moze anonimowo porozmawiac z przeszkolonym konsultantem o problemach, kryzysach i samotnosci. Dziala cala dobe.",
                "Mlodziez 12-19 lat", "WDROZONA", "malopolska", null);
        telefon.setArea(mental);
        Innovation cyfrowy = new Innovation("Cyfrowy dziadek", "Uczniowie ucza seniorow obslugi smartfonow i e-uslug.",
                "Intergeneracyjny program mentoringowy: mlodziez prowadzi indywidualne lekcje technologii dla osob starszych — recepty online, przelewy, wideorozmowy z rodzina.",
                "Seniorzy 65+", "TESTOWANA", "Krakow", null);
        cyfrowy.setArea(digital);
        Innovation asystent = new Innovation("Asystent osob z niepelnsprawnoscia", "Trener pracy i asystent wspierajacy samodzielne zycie i zatrudnienie.",
                "Asystenci towarzysza osobom z niepelnsprawnoscia w codziennych czynnosciach i poszukiwaniu pracy, dzialaja przy Centrach Uslug Spolecznych.",
                "Osoby z niepelnsprawnoscia", "WDROZONA", "Tarnow", null);
        asystent.setArea(services);
        Innovation klub = new Innovation("Klub samopomocy dla osob w kryzysie", "Regularne spotkania grupy wsparcia prowadzonej przez psychologa.",
                "Mieszkancy doswiadczeni kryzysem psychicznym lub zaloba spotykaja sie raz w tygodniu; grupa prowadzi wspolne projekty i wzajemna asyste.",
                "Dorozy po kryzysach psychicznych", "ROZWOJ", "powiat wadowicki", null);
        klub.setArea(mental);
        Innovation mobilny = new Innovation("Mobilny punkt uslug spolecznych", "Bus docierajacy do solectw bez ośrodka pomocy.",
                "Cyklicznie objezdzajacy gmine bus z pracownikami socjalnymi, prawnikiem i asystentem — uslugi docieraja tam, gdzie nie ma placowki.",
                "Mieszkancy peryferyjnych solectw", "TESTOWANA", "powiat limanowski", null);
        mobilny.setArea(services);

        innovations.saveAll(List.of(sadzcy, telefon, cyfrowy, asystent, klub, mobilny));

        if (seedVectors) {
            for (Innovation in : innovations.findAll()) {
                try { matchmaking.index(in); }
                catch (Exception e) {
                    log.warn("Indeksowanie wektorowe wylaczone ({}). Ustaw OPENAI_API_KEY i uruchom ponownie.", e.getMessage());
                    break;
                }
            }
        }
        log.info("Seed: {} obszarow, {} innowacji", areas.count(), innovations.count());
    }
}
