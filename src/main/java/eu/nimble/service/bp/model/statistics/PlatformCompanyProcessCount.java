package eu.nimble.service.bp.model.statistics;

import java.math.BigInteger;
import java.util.List;

public class PlatformCompanyProcessCount {

    private BigInteger totalCompanyCount;
    private List<CompanyProcessCount> companyProcessCounts;

    public PlatformCompanyProcessCount() {
    }

    public PlatformCompanyProcessCount(BigInteger totalCompanyCount, List<CompanyProcessCount> companyProcessCounts) {
        this.totalCompanyCount = totalCompanyCount;
        this.companyProcessCounts = companyProcessCounts;
    }

    public BigInteger getTotalCompanyCount() {
        return totalCompanyCount;
    }

    public void setTotalCompanyCount(BigInteger totalCompanyCount) {
        this.totalCompanyCount = totalCompanyCount;
    }

    public List<CompanyProcessCount> getCompanyProcessCounts() {
        return companyProcessCounts;
    }

    public void setCompanyProcessCounts(List<CompanyProcessCount> companyProcessCounts) {
        this.companyProcessCounts = companyProcessCounts;
    }
}
