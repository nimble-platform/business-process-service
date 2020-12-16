package eu.nimble.service.bp.model.statistics;

public class ProcessDocumentMetadataSummary {

    private String submissionDate;
    private String initiatorPartyID;
    private String initiatorPartyName;
    private String responderPartyID;
    private String responderPartyName;
    private String type;
    private String status;

    public ProcessDocumentMetadataSummary() {
    }

    public String getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(String submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getInitiatorPartyID() {
        return initiatorPartyID;
    }

    public void setInitiatorPartyID(String initiatorPartyID) {
        this.initiatorPartyID = initiatorPartyID;
    }

    public String getInitiatorPartyName() {
        return initiatorPartyName;
    }

    public void setInitiatorPartyName(String initiatorPartyName) {
        this.initiatorPartyName = initiatorPartyName;
    }

    public String getResponderPartyID() {
        return responderPartyID;
    }

    public void setResponderPartyID(String responderPartyID) {
        this.responderPartyID = responderPartyID;
    }

    public String getResponderPartyName() {
        return responderPartyName;
    }

    public void setResponderPartyName(String responderPartyName) {
        this.responderPartyName = responderPartyName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
