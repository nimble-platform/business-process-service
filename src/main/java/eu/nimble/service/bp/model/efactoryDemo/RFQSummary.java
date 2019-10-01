package eu.nimble.service.bp.model.efactoryDemo;

import java.math.BigDecimal;

public class RFQSummary {

    private String productID;
    private BigDecimal numberOfProductsRequested;
    private String endpointOfTheBuyer;
    private String buyerPartyId;
    private String buyerPartyName;
    private String processInstanceId;
    private String messageName;

    public RFQSummary() {
    }

    public RFQSummary(String productID, BigDecimal numberOfProductsRequested, String endpointOfTheBuyer, String buyerPartyId, String buyerPartyName, String processInstanceId, String messageName) {
        this.productID = productID;
        this.numberOfProductsRequested = numberOfProductsRequested;
        this.endpointOfTheBuyer = endpointOfTheBuyer;
        this.buyerPartyId = buyerPartyId;
        this.buyerPartyName = buyerPartyName;
        this.processInstanceId = processInstanceId;
        this.messageName = messageName;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public BigDecimal getNumberOfProductsRequested() {
        return numberOfProductsRequested;
    }

    public void setNumberOfProductsRequested(BigDecimal numberOfProductsRequested) {
        this.numberOfProductsRequested = numberOfProductsRequested;
    }

    public String getEndpointOfTheBuyer() {
        return endpointOfTheBuyer;
    }

    public void setEndpointOfTheBuyer(String endpointOfTheBuyer) {
        this.endpointOfTheBuyer = endpointOfTheBuyer;
    }

    public String getBuyerPartyId() {
        return buyerPartyId;
    }

    public void setBuyerPartyId(String buyerPartyId) {
        this.buyerPartyId = buyerPartyId;
    }

    public String getBuyerPartyName() {
        return buyerPartyName;
    }

    public void setBuyerPartyName(String buyerPartyName) {
        this.buyerPartyName = buyerPartyName;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getMessageName() {
        return messageName;
    }

    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }
}
