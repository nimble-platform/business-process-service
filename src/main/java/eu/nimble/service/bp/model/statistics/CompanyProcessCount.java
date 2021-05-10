package eu.nimble.service.bp.model.statistics;

import java.math.BigInteger;

public class CompanyProcessCount {

    private String partyId;
    private String federationId;
    private BigInteger purchasesCount;
    private BigInteger salesCount;

    public CompanyProcessCount() {
    }

    public CompanyProcessCount(String partyId, String federationId, BigInteger purchasesCount, BigInteger salesCount) {
        this.partyId = partyId;
        this.federationId = federationId;
        this.purchasesCount = purchasesCount;
        this.salesCount = salesCount;
    }

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public String getFederationId() {
        return federationId;
    }

    public void setFederationId(String federationId) {
        this.federationId = federationId;
    }

    public BigInteger getPurchasesCount() {
        return purchasesCount;
    }

    public void setPurchasesCount(BigInteger purchasesCount) {
        this.purchasesCount = purchasesCount;
    }

    public BigInteger getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(BigInteger salesCount) {
        this.salesCount = salesCount;
    }
}
