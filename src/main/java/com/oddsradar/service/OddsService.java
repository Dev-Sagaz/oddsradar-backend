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

            // NOTA: fórmula corrigida para bater com a calculadora de referência (Surebet).
            // (1 - sumImplied) * 100 é uma aproximação que subestima o profit real
            // quando sumImplied é bem menor que 1. A fórmula correta é (1/sumImplied - 1) * 100.
            // Ex.: sumImplied = 0.80 -> aproximação dá 20.00%, fórmula correta dá 25.00%.
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

    /**
     * Calcula a distribuição ideal de stakes para um conjunto de odds (uma por desfecho),
     * dado um valor total a ser apostado. Replica a lógica de "distribuir" da calculadora
     * de surebets de referência: o stake em cada desfecho é proporcional ao inverso da odd,
     * de forma que o lucro seja igual (ou o mais próximo possível, após arredondamento)
     * independentemente de qual desfecho ocorra.
     *
     * @param odds         lista de odds, uma por desfecho/casa (ex: [2.10, 2.05] para 1x2 de 2 vias)
     * @param bookmakers   nomes das casas correspondentes (mesma ordem de odds)
     * @param outcomeNames nomes dos desfechos correspondentes (mesma ordem de odds)
     * @param totalStake   valor total que o usuário quer apostar
     * @return ArbitrageCalculation com profitPercent e o stake/payout/profit de cada desfecho
     */
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

        // 1. Soma das probabilidades implícitas: Σ(1/odd_i)
        double sumImplied = odds.stream()
                .mapToDouble(odd -> 1.0 / odd)
                .sum();

        boolean isArbitrage = sumImplied < 1.0;

        // 2. Profit % real = (1/sumImplied - 1) * 100
        double profitPercent = ((1.0 / sumImplied) - 1.0) * 100.0;

        // 3. Stake ideal por desfecho: stake_i = (totalStake / odd_i) / sumImplied
        //    Isso garante que stake_i * odd_i seja igual para todo i (mesmo payout em qualquer desfecho).
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
