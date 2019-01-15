package eu.nimble.service.bp.impl.model.statistics;

public class OverallStatistics {

    private double averageCollaborationTime;
    private double averageResponseTime;
    private double tradingVolume;
    private int numberOfTransactions;

    public double getAverageCollaborationTime() {
        return averageCollaborationTime;
    }

    public void setAverageCollaborationTime(double averageCollaborationTime) {
        this.averageCollaborationTime = averageCollaborationTime;
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
