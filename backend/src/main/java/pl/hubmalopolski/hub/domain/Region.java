package pl.hubmalopolski.hub.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum Region {
    DOLNOSLASKIE("Dolnośląskie"),
    KUJAWSKO_POMORSKIE("Kujawsko-pomorskie"),
    LUBELSKIE("Lubelskie"),
    LUBUSKIE("Lubuskie"),
    LODZKIE("Łódzkie"),
    MALOPOLSKA("Małopolska"),
    MAZOWIECKIE("Mazowieckie"),
    OPOLSKIE("Opolskie"),
    PODKARPACKIE("Podkarpackie"),
    PODLASKIE("Podlaskie"),
    POMORSKIE("Pomorskie"),
    SLASKIE("Śląskie"),
    SWIETOKRZYSKIE("Świętokrzyskie"),
    WARMINSKO_MAZURSKIE("Warmińsko-mazurskie"),
    WIELKOPOLSKIE("Wielkopolskie"),
    ZACHODNIOPOMORSKIE("Zachodniopomorskie"),
    POLSKA("Cała Polska"),
    INNE("Inny region");

    private final String label;

    Region(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static Region fromCode(String value) {
        if (value == null) return null;
        String code = value.trim().toUpperCase(Locale.ROOT);
        if (code.equals("MAŁOPOLSKA") || code.equals("MALOPOLSKIE")
                || code.equals("MAŁOPOLSKIE")) return MALOPOLSKA;
        return Region.valueOf(code);
    }
}
