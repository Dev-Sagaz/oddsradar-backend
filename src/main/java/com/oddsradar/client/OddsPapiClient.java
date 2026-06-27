package com.oddsradar.client;

import com.oddsradar.model.OddsResponse;
import com.oddsradar.model.Bookmaker;
import com.oddsradar.model.Market;
import com.oddsradar.model.Outcome;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@RequiredArgsConstructor
public class OddsPapiClient {

    private final RestTemplate restTemplate;

    @Value("${oddspapi.key}")
    private String apiKey;

    private static final String BASE_URL = "https://api.oddspapi.io/v4";

    // Slugs das casas BR no OddsPapi
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

    // sportId no OddsPapi: 10 = Soccer, 2 = Basketball, 5 = Tennis
    private static final Map<String, Integer> SPORT_ID_MAP = Map.of(
        "soccer_fifa_world_cup",       10,
        "soccer_brazil_campeonato",    10,
        "soccer_brazil_serie_b",       10,
        "soccer_epl",                  10,
        "basketball_nba",              2
    );

    public List<OddsResponse> getOdds(String sport) {
        int sportId = SPORT_ID_MAP.getOrDefault(sport, 10);

        // 1. Busca fixtures do esporte
        String fixturesUrl = BASE_URL + "/fixtures"
            + "?apiKey=" + apiKey
            + "&sportId=" + sportId
            + "&hasOdds=true";

        OddsPapiFixture[] fixtures;
        try {
            fixtures = restTemplate.getForObject(fixturesUrl, OddsPapiFixture[].class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        if (fixtures == null || fixtures.length == 0) return Collections.emptyList();

        List<OddsResponse> results = new ArrayList<>();

        // 2. Para cada fixture, busca odds das casas BR
        for (OddsPapiFixture fixture : fixtures) {
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

            // 3. Converte para o modelo interno OddsResponse
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

            // Market 101 = 1X2 (Full Time Result)
            OddsPapiMarket market101 = bmOdds.getMarkets().get("101");
            if (market101 == null || market101.getOutcomes() == null) continue;

            List<Outcome> outcomes = new ArrayList<>();

            // 101 = Home, 102 = Draw, 103 = Away
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
        game.setCommence_time(fixture.getStartDate());
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

    // Converte slug para nome legível
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

    // ---- Classes internas de deserialização ----

    @lombok.Data
    public static class OddsPapiFixture {
        private String fixtureId;
        private String participant1Name;
        private String participant2Name;
        private String startDate;
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
