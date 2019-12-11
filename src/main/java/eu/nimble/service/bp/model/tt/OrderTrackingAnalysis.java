package eu.nimble.service.bp.model.tt;

public class OrderTrackingAnalysis {

    private String epc;
    private String message;
    private String productionEndTime;

    public OrderTrackingAnalysis() {
    }

    public OrderTrackingAnalysis(String epc, String message, String productionEndTime) {
        this.epc = epc;
        this.message = message;
        this.productionEndTime = productionEndTime;
    }

    public String getEpc() {
        return epc;
    }

    public void setEpc(String epc) {
        this.epc = epc;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getProductionEndTime() {
        return productionEndTime;
    }

    public void setProductionEndTime(String productionEndTime) {
        this.productionEndTime = productionEndTime;
    }
}
