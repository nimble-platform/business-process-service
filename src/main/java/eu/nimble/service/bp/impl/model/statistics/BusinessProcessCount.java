package eu.nimble.service.bp.impl.model.statistics;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by suat on 21-Jun-18.
 */
public class BusinessProcessCount {
    // companies --> business processes --> status --> count
    private Map<String, Map<String, Map<String, Long>>> counts = new HashMap<>();

    public Map getCounts() {
        return counts;
    }

    public void addCount(String companyId, String processType, String status, Long count) {
        // check company map
        Map<String, Map<String, Long>> companyMap = counts.get(companyId);
        if(companyMap == null) {
            companyMap = new HashMap<>();
            counts.put(companyId, companyMap);
        }

        // check process type map
        Map<String, Long> processTypeMap = companyMap.get(processType);
        if(processTypeMap == null) {
            processTypeMap = new HashMap<>();
            companyMap.put(processType, processTypeMap);
        }

        // check status map
        processTypeMap.put(status, count);
    }
}
