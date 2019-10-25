package eu.nimble.service.bp.model.statistics;

import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;

import java.math.BigDecimal;

public class FulfilmentStatistics {

    private ItemType item;
    private BigDecimal dispatchedQuantity;
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

    public ItemType getItem() {
        return item;
    }

    public void setItem(ItemType item) {
        this.item = item;
    }
}
