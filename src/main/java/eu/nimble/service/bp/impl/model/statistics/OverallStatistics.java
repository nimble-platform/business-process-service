package eu.nimble.service.bp.impl.model.statistics;

public class OverallStatistics {

    private double averageNegotiationTime;
    private double averageResponseTime;
    private double tradingVolume;
    private int numberOfTransactions;

    public double getAverageNegotiationTime() {
        return averageNegotiationTime;
    }

    public void setAverageNegotiationTime(double averageNegotiationTime) {
        this.averageNegotiationTime = averageNegotiationTime;
    }

    public double getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public double getTradingVolume() {
        return tradingVolume;
    }

    public void setTradingVolume(double tradingVolume) {
        this.tradingVolume = tradingVolume;
    }

    public int getNumberOfTransactions() {
        return numberOfTransactions;
    }

    public void setNumberOfTransactions(int numberOfTransactions) {
        this.numberOfTransactions = numberOfTransactions;
    }
}
