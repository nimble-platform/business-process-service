package eu.nimble.service.bp.model.statistics;

import java.math.BigDecimal;

public class FulfilmentStatistics {

    private Long lineItemHjid;
    private BigDecimal dispatchedQuantity;
    private BigDecimal acceptedQuantity;
    private BigDecimal rejectedQuantity;
    private BigDecimal requestedQuantity;

    public BigDecimal getDispatchedQuantity() {
        return dispatchedQuantity;
    }

    public void setDispatchedQuantity(BigDecimal dispatchedQuantity) {
        this.dispatchedQuantity = dispatchedQuantity;
    }

    public BigDecimal getRejectedQuantity() {
        return rejectedQuantity;
    }

    public void setRejectedQuantity(BigDecimal rejectedQuantity) {
        this.rejectedQuantity = rejectedQuantity;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(BigDecimal requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Long getLineItemHjid() {
        return lineItemHjid;
    }

    public void setLineItemHjid(Long lineItemHjid) {
        this.lineItemHjid = lineItemHjid;
    }

    public BigDecimal getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public void setAcceptedQuantity(BigDecimal acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
    }
}
