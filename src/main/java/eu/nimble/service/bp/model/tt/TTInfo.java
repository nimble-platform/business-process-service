package eu.nimble.service.bp.model.tt;

/**
 * Created by suat on 06-Jun-18.
 */
public class TTInfo {
    private String processInstanceId;
    private String catalogueUuid;
    private String catalogueLineHjid;

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getCatalogueUuid() {
        return catalogueUuid;
    }

    public void setCatalogueUuid(String catalogueUuid) {
        this.catalogueUuid = catalogueUuid;
    }

    public String getCatalogueLineHjid() {
        return catalogueLineHjid;
    }

    public void setCatalogueLineHjid(String catalogueLineHjid) {
        this.catalogueLineHjid = catalogueLineHjid;
    }
}
