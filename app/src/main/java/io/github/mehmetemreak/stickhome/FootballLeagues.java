package io.github.mehmetemreak.stickhome;

import java.util.Arrays;
import java.util.List;

/** Curated list of TheSportsDB league IDs the user can toggle on/off. */
public class FootballLeagues {

    public static class League {
        public final String id;
        public final String name;
        public final String sport;
        public final boolean enabledByDefault;

        public League(String id, String name, String sport, boolean enabledByDefault) {
            this.id = id;
            this.name = name;
            this.sport = sport;
            this.enabledByDefault = enabledByDefault;
        }
    }

    public static final List<League> ALL = Arrays.asList(
            new League("4339", "Süper Lig", "Futbol", true),
            new League("4480", "Şampiyonlar Ligi", "Futbol", true),
            new League("4328", "Premier League", "Futbol", true),
            new League("4335", "La Liga", "Futbol", true),
            new League("4481", "Avrupa Ligi", "Futbol", false),
            new League("5071", "Konferans Ligi", "Futbol", false),
            new League("4332", "Serie A", "Futbol", false),
            new League("4331", "Bundesliga", "Futbol", false),
            new League("4334", "Ligue 1", "Futbol", false),
            new League("4387", "NBA", "Basketbol", false)
    );
}
