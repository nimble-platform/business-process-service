package eu.nimble.service.bp.impl.model.tt;

/**
 * Created by suat on 06-Jun-18.
 */
public class TTInfo {
    private String eventUrl;
    private String masterUrl;
    private String productionProcessTemplate;
    private String relatedProductId;

    public String getEventUrl() {
        return eventUrl;
    }

    public void setEventUrl(String eventUrl) {
        this.eventUrl = eventUrl;
    }

    public String getMasterUrl() {
        return masterUrl;
    }

    public void setMasterUrl(String masterUrl) {
        this.masterUrl = masterUrl;
    }

    public String getProductionProcessTemplate() {
        return productionProcessTemplate;
    }

    public void setProductionProcessTemplate(String productionProcessTemplate) {
        this.productionProcessTemplate = productionProcessTemplate;
    }

    public String getRelatedProductId() {
        return relatedProductId;
    }

    public void setRelatedProductId(String relatedProductId) {
        this.relatedProductId = relatedProductId;
    }
}
