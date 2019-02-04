package eu.nimble.service.bp.impl.model.statistics;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by suat on 21-Jun-18.
 */
public class BusinessProcessCount {
    // companies --> business processes --> status --> count
    @ApiModelProperty(value = "",example =
            "{" +
                    "'750': {" +
                    "'companyName': 'ABCCompany'," +
                    "'companyCounts':{" +
                    "'ORDER':{" +
                    "'WAITINGRESPONSE': 1" +
                    "}" +
                    "}" +
                    "}")
    private Map<String, CompanyProcessCount> counts = new HashMap<>();

    public Map getCounts() {
        return counts;
    }

    public void addCount(String companyId,String processType, String status, Long count,String companyName) {
        // check company map
        CompanyProcessCount companyProcessCount = counts.get(companyId);
        if(companyProcessCount == null) {
            companyProcessCount = new CompanyProcessCount();
            companyProcessCount.setCompanyName(companyName);
            counts.put(companyId, companyProcessCount);
        }

        // check process type map
        Map<String, Long> processTypeMap = companyProcessCount.getCompanyCounts().get(processType);
        if(processTypeMap == null) {
            processTypeMap = new HashMap<>();
            companyProcessCount.getCompanyCounts().put(processType,processTypeMap);
        }

        // check status map
        processTypeMap.put(status, count);
    }

    private static class CompanyProcessCount {
        private String companyName;
        private Map<String, Map<String, Long>> companyCounts = new HashMap<>();
        // process type,status,count

        public Map<String, Map<String, Long>> getCompanyCounts() {
            return companyCounts;
        }

        public void setCompanyCounts(Map<String, Map<String, Long>> companyCounts) {
            this.companyCounts = companyCounts;
        }

        public void setCompanyName(String companyName){
            this.companyName = companyName;
        }

        public String getCompanyName() {
            return companyName;
        }
    }

}
