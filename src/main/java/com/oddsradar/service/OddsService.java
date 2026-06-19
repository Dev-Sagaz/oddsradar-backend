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

    private final OddsApiClient oddsApiClient;

    @Cacheable("best-odds")
    public List<BestOddsResult> getBestOdds(String sport) {
        List<OddsResponse> games = oddsApiClient.getOdds(sport);
        List<BestOddsResult> results = new ArrayList<>();

        for (OddsResponse game : games) {
            Map<String, double[]> best = new HashMap<>();
            Map<String, String> bestBook = new HashMap<>();

            for (Bookmaker bm : game.getBookmakers()) {
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
                ? Math.round((1.0 - sumImplied) * 10000.0) / 100.0
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
}
