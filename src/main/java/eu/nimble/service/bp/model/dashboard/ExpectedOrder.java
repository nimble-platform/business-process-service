package eu.nimble.service.bp.model.dashboard;

import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;

import java.math.BigDecimal;
import java.util.List;

public class ExpectedOrder {

    private List<String> unShippedOrderIds;
    private BigDecimal lineHjid;
    private String state;
    private String processType;
    private String processInstanceId;
    private ProcessDocumentMetadata responseMetadata;

    public List<String> getUnShippedOrderIds() {
        return unShippedOrderIds;
    }

    public void setUnShippedOrderIds(List<String> unShippedOrderIds) {
        this.unShippedOrderIds = unShippedOrderIds;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getProcessType() {
        return processType;
    }

    public void setProcessType(String processType) {
        this.processType = processType;
    }

    public ProcessDocumentMetadata getResponseMetadata() {
        return responseMetadata;
    }

    public void setResponseMetadata(ProcessDocumentMetadata responseMetadata) {
        this.responseMetadata = responseMetadata;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public BigDecimal getLineHjid() {
        return lineHjid;
    }

    public void setLineHjid(BigDecimal lineHjid) {
        this.lineHjid = lineHjid;
    }
}
