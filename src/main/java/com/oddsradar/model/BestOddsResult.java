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

    @Data
    @AllArgsConstructor
    public static class BestOutcome {
        private String outcomeName;
        private Double bestPrice;
        private String bookmaker;
    }
}
