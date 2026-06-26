package com.oddsradar.service;

import com.oddsradar.client.OddsApiClient;
import com.oddsradar.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OddsService {

private static final Set<String> BOOKMAKERS_BR = Set.of(
    "Bet365",
    "Betfair",
    "Betfair_ex_eu",
    "1xBet",
    "Betano",
    "Novibet",
    "Superbet",
    "KTO",
    "Sportingbet",
    "BetMGM",
    "Betnacional",
    "Aposta Ganha",
    "Esportes da Sorte",
    "King Panda",
    "Pinnacle",
    "Blaze",
    "Betboom",
    "Vbet",
    "Brazino777",
    "BetBra",
    "Matchbook"
);

    private final OddsApiClient oddsApiClient;

    @Cacheable("best-odds")
    public List<BestOddsResult> getBestOdds(String sport) {
        List<OddsResponse> games = oddsApiClient.getOdds(sport);
        List<BestOddsResult> results = new ArrayList<>();

        for (OddsResponse game : games) {
            Map<String, double[]> best = new HashMap<>();
            Map<String, String> bestBook = new HashMap<>();

            for (Bookmaker bm : game.getBookmakers()) {
                // 🔒 Filtra apenas casas autorizadas no Brasil
                if (!BOOKMAKERS_BR.contains(bm.getTitle())) continue;

                for (Market market : bm.getMarkets()) {
                    if (!"h2h".equals(market.getKey())) continue;
                    for (Outcome outcome : market.getOutcomes()) {
                        String key = outcome.getName();
                        double price = outcome.getPrice();
                        if (!best.containsKey(key) ||
                            price > best.get(key)[0]) {
                            best.put(key, new double[]{price});
                            bestBook.put(key, bm.getTitle());
                        }
                    }
                }
            }

            List<BestOddsResult.BestOutcome> outcomes = new ArrayList<>();
            best.forEach((name, price) ->
                outcomes.add(new BestOddsResult.BestOutcome(
                    name, price[0], bestBook.get(name)))
            );

            // Cálculo de arbitragem
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

    // Endpoint de debug — lista todos os títulos de bookmakers que a API está retornando
    public List<String> getAvailableBookmakers(String sport) {
        List<OddsResponse> games = oddsApiClient.getOdds(sport);
        Set<String> titles = new TreeSet<>();
        for (OddsResponse game : games) {
            for (Bookmaker bm : game.getBookmakers()) {
                titles.add(bm.getTitle());
            }
        }
        return new ArrayList<>(titles);
    }

    public ArbitrageCalculation calculateArbitrage(
            List<Double> odds,
            List<String> bookmakers,
            List<String> outcomeNames,
            double totalStake) {

        if (odds == null || odds.isEmpty()) {
            throw new IllegalArgumentException("A lista de odds não pode ser vazia");
        }
        if (bookmakers == null || bookmakers.size() != odds.size()) {
            throw new IllegalArgumentException("bookmakers deve ter o mesmo tamanho que odds");
        }
        if (outcomeNames == null || outcomeNames.size() != odds.size()) {
            throw new IllegalArgumentException("outcomeNames deve ter o mesmo tamanho que odds");
        }
        if (totalStake <= 0) {
            throw new IllegalArgumentException("totalStake deve ser maior que zero");
        }

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
