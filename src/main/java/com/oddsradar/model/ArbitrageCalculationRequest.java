package com.oddsradar.model;

import java.util.List;

public class ArbitrageCalculationRequest {

    private List<Double> odds;
    private List<String> bookmakers;
    private List<String> outcomeNames;
    private double totalStake;

    public ArbitrageCalculationRequest() {}

    public List<Double> getOdds() {
        return odds;
    }

    public void setOdds(List<Double> odds) {
        this.odds = odds;
    }

    public List<String> getBookmakers() {
        return bookmakers;
    }

    public void setBookmakers(List<String> bookmakers) {
        this.bookmakers = bookmakers;
    }

    public List<String> getOutcomeNames() {
        return outcomeNames;
    }

    public void setOutcomeNames(List<String> outcomeNames) {
        this.outcomeNames = outcomeNames;
    }

    public double getTotalStake() {
        return totalStake;
    }

    public void setTotalStake(double totalStake) {
        this.totalStake = totalStake;
    }
}
