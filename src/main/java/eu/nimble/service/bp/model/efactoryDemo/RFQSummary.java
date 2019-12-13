package eu.nimble.service.bp.model.efactoryDemo;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class RFQSummary {
    @NotNull
    private String productID;
    @NotNull
    private BigDecimal numberOfProductsRequested;
    @NotNull
    private String endpointOfTheBuyer;
    @NotNull
    private String buyerPartyId;
    @NotNull
    private String buyerPartyName;
    @NotNull
    private String processInstanceId;
    @NotNull
    private String messageName;
    private String previousDocumentId;

    public RFQSummary() {
    }

    public RFQSummary(String productID, BigDecimal numberOfProductsRequested, String endpointOfTheBuyer, String buyerPartyId, String buyerPartyName, String processInstanceId, String messageName, String previousDocumentId) {
        this.productID = productID;
        this.numberOfProductsRequested = numberOfProductsRequested;
        this.endpointOfTheBuyer = endpointOfTheBuyer;
        this.buyerPartyId = buyerPartyId;
        this.buyerPartyName = buyerPartyName;
        this.processInstanceId = processInstanceId;
        this.messageName = messageName;
        this.previousDocumentId = previousDocumentId;
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

    public String getPreviousDocumentId() {
        return previousDocumentId;
    }

    public void setPreviousDocumentId(String previousDocumentId) {
        this.previousDocumentId = previousDocumentId;
    }
}
