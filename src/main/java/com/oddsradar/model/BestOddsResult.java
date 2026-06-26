package com.oddsradar.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class BestOddsResult {
    private String gameId;
    private String homeTeam;
    private String awayTeam;
    private String commenceTime;
    private List<BestOutcome> bestOutcomes;
    private boolean arbitrage;
    private double profitPercent;

    @Data
    @AllArgsConstructor
    public static class BestOutcome {
        private String outcomeName;
        private Double bestPrice;       // odd efetiva (já com desconto da comissão)
        private String bookmaker;
        private Double rawPrice;        // odd bruta exibida no frontend
        private Double commission;      // % de comissão (0.0 se não for exchange)
        private boolean exchange;       // true se for Betfair, Matchbook etc.
    }
}
