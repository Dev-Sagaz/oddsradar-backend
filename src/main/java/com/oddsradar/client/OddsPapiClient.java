package com.oddsradar.client;

import com.oddsradar.model.OddsResponse;
import com.oddsradar.model.Bookmaker;
import com.oddsradar.model.Market;
import com.oddsradar.model.Outcome;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
public class OddsPapiClient {

    private final RestTemplate restTemplate;

    @Value("${oddspapi.key}")
    private String apiKey;

    private static final String BASE_URL = "https://api.oddspapi.io/v4";

    private static final List<String> BOOKMAKERS_BR_SLUGS = List.of(
        "betano.bet.br",
        "superbet.bet.br",
        "sportingbet.bet.br",
        "bet365",
        "pinnacle",
        "novibet",
        "novibet.bet.br"
    );

    private static final String BOOKMAKERS_PARAM =
        String.join(",", BOOKMAKERS_BR_SLUGS);

    // Mapa de sport key → tournamentSlug do OddsPapi
    private static final Map<String, String> SPORT_TOURNAMENT_MAP = Map.of(
        "soccer_fifa_world_cup",    "world-cup",
        "soccer_brazil_campeonato", "brasileiro-serie-a",
        "soccer_brazil_serie_b",    "brasileiro-serie-b",
        "soccer_epl",               "premier-league",
        "basketball_nba",           "nba"
    );

    public List<OddsResponse> getOdds(String sport) {
        String tournamentSlug = SPORT_TOURNAMENT_MAP.getOrDefault(sport, "world-cup");

        String from = LocalDate.now().toString();
        String to   = LocalDate.now().plusDays(7).toString();

        String fixturesUrl = BASE_URL + "/fixtures"
            + "?apiKey=" + apiKey
            + "&sportId=10"
            + "&hasOdds=true"
            + "&from=" + from
            + "&to=" + to;

        OddsPapiFixture[] allFixtures;
        try {
            allFixtures = restTemplate.getForObject(fixturesUrl, OddsPapiFixture[].class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        if (allFixtures == null || allFixtures.length == 0) return Collections.emptyList();

        // Filtra pelo torneio correto
        List<OddsPapiFixture> fixtures = Arrays.stream(allFixtures)
            .filter(f -> tournamentSlug.equals(f.getTournamentSlug()))
            .toList();

        if (fixtures.isEmpty()) return Collections.emptyList();

        List<OddsResponse> results = new ArrayList<>();

        for (OddsPapiFixture fixture : fixtures) {
    try { Thread.sleep(500); } catch (InterruptedException ignored) {} // 500ms entre requests

    String oddsUrl = BASE_URL + "/odds"
        + "?apiKey=" + apiKey
        + "&fixtureId=" + fixture.getFixtureId()
        + "&bookmakers=" + BOOKMAKERS_PARAM;

    OddsPapiOddsResponse oddsResp;
    try {
        oddsResp = restTemplate.getForObject(oddsUrl, OddsPapiOddsResponse.class);
    } catch (Exception e) {
        continue;
    }
    if (oddsResp == null || oddsResp.getBookmakerOdds() == null) continue;

    OddsResponse game = convertToOddsResponse(fixture, oddsResp);
    if (game != null) results.add(game);
}

        return results;
    }

    private OddsResponse convertToOddsResponse(
            OddsPapiFixture fixture,
            OddsPapiOddsResponse oddsResp) {

        List<Bookmaker> bookmakers = new ArrayList<>();

        for (Map.Entry<String, OddsPapiBookmakerOdds> entry :
                oddsResp.getBookmakerOdds().entrySet()) {

            String slug = entry.getKey();
            OddsPapiBookmakerOdds bmOdds = entry.getValue();

            if (bmOdds.getMarkets() == null) continue;

            OddsPapiMarket market101 = bmOdds.getMarkets().get("101");
            if (market101 == null || market101.getOutcomes() == null) continue;

            List<Outcome> outcomes = new ArrayList<>();
            extractOutcome(market101, "101", fixture.getParticipant1Name(), outcomes);
            extractOutcome(market101, "102", "Draw", outcomes);
            extractOutcome(market101, "103", fixture.getParticipant2Name(), outcomes);

            if (outcomes.isEmpty()) continue;

            Market market = new Market();
            market.setKey("h2h");
            market.setOutcomes(outcomes);

            Bookmaker bm = new Bookmaker();
            bm.setTitle(slugToTitle(slug));
            bm.setMarkets(List.of(market));

            bookmakers.add(bm);
        }

        if (bookmakers.isEmpty()) return null;

        OddsResponse game = new OddsResponse();
        game.setId(fixture.getFixtureId());
        game.setHome_team(fixture.getParticipant1Name());
        game.setAway_team(fixture.getParticipant2Name());
        game.setCommence_time(fixture.getStartTime()); // corrigido: startTime
        game.setBookmakers(bookmakers);

        return game;
    }

    private void extractOutcome(
            OddsPapiMarket market,
            String outcomeId,
            String name,
            List<Outcome> outcomes) {
        try {
            double price = market.getOutcomes()
                .get(outcomeId)
                .getPlayers()
                .get("0")
                .getPrice();
            if (price > 1.0) {
                Outcome o = new Outcome();
                o.setName(name);
                o.setPrice(price);
                outcomes.add(o);
            }
        } catch (Exception ignored) {}
    }

    private String slugToTitle(String slug) {
        return switch (slug) {
            case "betano.bet.br"      -> "Betano";
            case "superbet.bet.br"    -> "Superbet";
            case "sportingbet.bet.br" -> "Sportingbet";
            case "bet365"             -> "Bet365";
            case "pinnacle"           -> "Pinnacle";
            case "novibet"            -> "Novibet";
            case "novibet.bet.br"     -> "Novibet";
            default                   -> slug;
        };
    }

    @lombok.Data
    public static class OddsPapiFixture {
        private String fixtureId;
        private String participant1Name;
        private String participant2Name;
        private String startTime;        // corrigido: era startDate
        private String tournamentSlug;   // novo: para filtrar por torneio
    }

    @lombok.Data
    public static class OddsPapiOddsResponse {
        private Map<String, OddsPapiBookmakerOdds> bookmakerOdds;
    }

    @lombok.Data
    public static class OddsPapiBookmakerOdds {
        private Map<String, OddsPapiMarket> markets;
    }

    @lombok.Data
    public static class OddsPapiMarket {
        private Map<String, OddsPapiOutcome> outcomes;
    }

    @lombok.Data
    public static class OddsPapiOutcome {
        private Map<String, OddsPapiPlayer> players;
    }

    @lombok.Data
    public static class OddsPapiPlayer {
        private double price;
    }
}
