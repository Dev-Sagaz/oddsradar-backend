package com.oddsradar.service;

import com.oddsradar.client.OddsApiClient;
import com.oddsradar.client.OddsPapiClient;
import com.oddsradar.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OddsService {

    private final OddsApiClient oddsApiClient;
    private final OddsPapiClient oddsPapiClient;

    private static final Set<String> BOOKMAKERS_BR = Set.of(
        "Betano",
        "Novibet",
        "Sportingbet",
        "Esportes da Sorte",
        "Esportiva Bet",
        "Bet365",
        "Pinnacle",
        "KTO",
        "Estrela Bet",
        "BetBra",
        "Superbet"
    );

    private static final Map<String, Double> EXCHANGE_COMMISSION = Map.of(
        "Betfair",       6.5,
        "Betfair_ex_eu", 6.5,
        "Matchbook",     2.0
    );

    private double applyExchangeCommission(double odd, double commissionPct) {
        return 1.0 + (odd - 1.0) * (1.0 - commissionPct / 100.0);
    }

    @Cacheable("best-odds")
    public List<BestOddsResult> getBestOdds(String sport) {

        // 1. Busca das duas APIs e combina os jogos
        List<OddsResponse> gamesFromOddsApi = new ArrayList<>();
        List<OddsResponse> gamesFromOddsPapi = new ArrayList<>();

        try { gamesFromOddsApi  = oddsApiClient.getOdds(sport);  } catch (Exception ignored) {}
        try { gamesFromOddsPapi = oddsPapiClient.getOdds(sport); } catch (Exception ignored) {}

        // 2. Indexa jogos da OddsAPI por homeTeam+awayTeam
        Map<String, OddsResponse> gameMap = new LinkedHashMap<>();
        for (OddsResponse g : gamesFromOddsApi) {
            gameMap.put(gameKey(g), g);
        }

        // 3. Mescla jogos do OddsPapi — se já existe, adiciona bookmakers; senão cria novo
        for (OddsResponse g : gamesFromOddsPapi) {
            String key = gameKey(g);
            if (gameMap.containsKey(key)) {
                // Adiciona bookmakers do OddsPapi ao jogo existente
                gameMap.get(key).getBookmakers().addAll(g.getBookmakers());
            } else {
                gameMap.put(key, g);
            }
        }

        List<BestOddsResult> results = new ArrayList<>();

        for (OddsResponse game : gameMap.values()) {
            // [effectivePrice, rawPrice, commission]
            Map<String, double[]> best = new HashMap<>();
            Map<String, String> bestBook = new HashMap<>();

            for (Bookmaker bm : game.getBookmakers()) {
                if (!BOOKMAKERS_BR.contains(bm.getTitle())) continue;

                double commission = EXCHANGE_COMMISSION.getOrDefault(bm.getTitle(), 0.0);
                boolean isExchange = commission > 0;

                for (Market market : bm.getMarkets()) {
                    if (!"h2h".equals(market.getKey())) continue;
                    for (Outcome outcome : market.getOutcomes()) {
                        String key = outcome.getName();
                        double rawPrice = outcome.getPrice();
                        double effectivePrice = isExchange
                            ? applyExchangeCommission(rawPrice, commission)
                            : rawPrice;

                        if (!best.containsKey(key) ||
                            effectivePrice > best.get(key)[0]) {
                            best.put(key, new double[]{effectivePrice, rawPrice, commission});
                            bestBook.put(key, bm.getTitle());
                        }
                    }
                }
            }

            if (best.isEmpty()) continue;

            List<BestOddsResult.BestOutcome> outcomes = new ArrayList<>();
            best.forEach((name, prices) -> {
                String bmTitle = bestBook.get(name);
                boolean isExchange = EXCHANGE_COMMISSION.containsKey(bmTitle);
                outcomes.add(new BestOddsResult.BestOutcome(
                    name,
                    prices[0],   // effectivePrice
                    bmTitle,
                    prices[1],   // rawPrice
                    prices[2],   // commission
                    isExchange
                ));
            });

            double sumImplied = outcomes.stream()
                .mapToDouble(o -> 1.0 / o.getBestPrice())
                .sum();

            boolean isArbitrage = sumImplied < 1.0;
            double profitPercent = isArbitrage
                ? Math.round(((1.0 / sumImplied) - 1.0) * 10000.0) / 100.0
                : 0.0;

            results.add(new BestOddsResult(
                game.getId(),
                game.getHome_team(),
                game.getAway_team(),
                game.getCommence_time(),
                outcomes,
                isArbitrage,
                profitPercent
            ));
        }

        return results;
    }

    // Chave única por jogo para mesclar as duas APIs
    private String gameKey(OddsResponse g) {
        return (g.getHome_team() + "|" + g.getAway_team()).toLowerCase();
    }

    public List<String> getAvailableBookmakers(String sport) {
        Set<String> titles = new TreeSet<>();
        try {
            for (OddsResponse g : oddsApiClient.getOdds(sport))
                for (Bookmaker bm : g.getBookmakers())
                    titles.add(bm.getTitle());
        } catch (Exception ignored) {}
        try {
            for (OddsResponse g : oddsPapiClient.getOdds(sport))
                for (Bookmaker bm : g.getBookmakers())
                    titles.add(bm.getTitle());
        } catch (Exception ignored) {}
        return new ArrayList<>(titles);
    }

    public ArbitrageCalculation calculateArbitrage(
            List<Double> odds,
            List<String> bookmakers,
            List<String> outcomeNames,
            double totalStake) {

        if (odds == null || odds.isEmpty())
            throw new IllegalArgumentException("A lista de odds não pode ser vazia");
        if (bookmakers == null || bookmakers.size() != odds.size())
            throw new IllegalArgumentException("bookmakers deve ter o mesmo tamanho que odds");
        if (outcomeNames == null || outcomeNames.size() != odds.size())
            throw new IllegalArgumentException("outcomeNames deve ter o mesmo tamanho que odds");
        if (totalStake <= 0)
            throw new IllegalArgumentException("totalStake deve ser maior que zero");

        double sumImplied = odds.stream()
            .mapToDouble(odd -> 1.0 / odd)
            .sum();

        boolean isArbitrage = sumImplied < 1.0;
        double profitPercent = ((1.0 / sumImplied) - 1.0) * 100.0;

        List<ArbitrageCalculation.OutcomeStake> outcomeStakes = new ArrayList<>();
        for (int i = 0; i < odds.size(); i++) {
            double odd = odds.get(i);
            double stake = (totalStake / odd) / sumImplied;
            double payout = stake * odd;
            double profit = payout - totalStake;

            outcomeStakes.add(new ArbitrageCalculation.OutcomeStake(
                outcomeNames.get(i),
                bookmakers.get(i),
                odd,
                round2(stake),
                round2(payout),
                round2(profit)
            ));
        }

        return new ArbitrageCalculation(
            isArbitrage,
            round2(profitPercent),
            totalStake,
            outcomeStakes
        );
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
