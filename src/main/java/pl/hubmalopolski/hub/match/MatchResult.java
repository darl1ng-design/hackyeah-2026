package pl.hubmalopolski.hub.match;

import pl.hubmalopolski.hub.domain.Innovation;

/** Wynik matchmakeingu: innowacja + powod dopasowania + wynik podobienstwa. */
public record MatchResult(Innovation innovation, String why, double score) {}
