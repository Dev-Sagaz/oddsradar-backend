package com.oddsradar.model;
 
import java.util.List;
 
public class ArbitrageCalculation {
 
    private boolean arbitrage;
    private double profitPercent;
    private double totalStake;
    private List<OutcomeStake> outcomes;
 
    public ArbitrageCalculation() {}
 
    public ArbitrageCalculation(boolean arbitrage, double profitPercent, double totalStake, List<OutcomeStake> outcomes) {
        this.arbitrage = arbitrage;
        this.profitPercent = profitPercent;
        this.totalStake = totalStake;
        this.outcomes = outcomes;
    }
 
    public boolean isArbitrage() {
        return arbitrage;
    }
 
    public void setArbitrage(boolean arbitrage) {
        this.arbitrage = arbitrage;
    }
 
    public double getProfitPercent() {
        return profitPercent;
    }
 
    public void setProfitPercent(double profitPercent) {
        this.profitPercent = profitPercent;
    }
 
    public double getTotalStake() {
        return totalStake;
    }
 
    public void setTotalStake(double totalStake) {
        this.totalStake = totalStake;
    }
 
    public List<OutcomeStake> getOutcomes() {
        return outcomes;
    }
 
    public void setOutcomes(List<OutcomeStake> outcomes) {
        this.outcomes = outcomes;
    }
 
    public static class OutcomeStake {
        private String outcomeName;
        private String bookmaker;
        private double odd;
        private double stake;
        private double payout;
        private double profit;
 
        public OutcomeStake() {}
 
        public OutcomeStake(String outcomeName, String bookmaker, double odd, double stake, double payout, double profit) {
            this.outcomeName = outcomeName;
            this.bookmaker = bookmaker;
            this.odd = odd;
            this.stake = stake;
            this.payout = payout;
            this.profit = profit;
        }
 
        public String getOutcomeName() {
            return outcomeName;
        }
 
        public void setOutcomeName(String outcomeName) {
            this.outcomeName = outcomeName;
        }
 
        public String getBookmaker() {
            return bookmaker;
        }
 
        public void setBookmaker(String bookmaker) {
            this.bookmaker = bookmaker;
        }
 
        public double getOdd() {
            return odd;
        }
 
        public void setOdd(double odd) {
            this.odd = odd;
        }
 
        public double getStake() {
            return stake;
        }
 
        public void setStake(double stake) {
            this.stake = stake;
        }
 
        public double getPayout() {
            return payout;
        }
 
        public void setPayout(double payout) {
            this.payout = payout;
        }
 
        public double getProfit() {
            return profit;
        }
 
        public void setProfit(double profit) {
            this.profit = profit;
        }
    }
}
