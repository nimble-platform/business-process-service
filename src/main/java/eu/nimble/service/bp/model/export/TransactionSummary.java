package eu.nimble.service.bp.model.export;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import eu.nimble.service.model.ubl.document.IDocument;

import java.util.List;

/**
 * Created by suat on 12-Jun-19.
 */
public class TransactionSummary {
    private String businessProcessInstanceId;
    private String transactionTime;
    private String transactionDirection;
    private String companyUserId;
    private String companyUserName;
    private String correspondingCompanyId;
    private String correspondingCompanyName;
    private String exchangedDocumentId;
    @JsonIgnore
    private IDocument exchangedDocument;
    private List<String> auxiliaryFileIds;
    @JsonIgnore
    private List<DocumentReferenceType> auxiliaryFiles;

    public String getBusinessProcessInstanceId() {
        return businessProcessInstanceId;
    }

    public void setBusinessProcessInstanceId(String businessProcessInstanceId) {
        this.businessProcessInstanceId = businessProcessInstanceId;
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getTransactionDirection() {
        return transactionDirection;
    }

    public void setTransactionDirection(String transactionDirection) {
        this.transactionDirection = transactionDirection;
    }

    public String getCompanyUserId() {
        return companyUserId;
    }

    public void setCompanyUserId(String companyUserId) {
        this.companyUserId = companyUserId;
    }

    public String getCompanyUserName() {
        return companyUserName;
    }

    public void setCompanyUserName(String companyUserName) {
        this.companyUserName = companyUserName;
    }

    public String getCorrespondingCompanyId() {
        return correspondingCompanyId;
    }

    public void setCorrespondingCompanyId(String correspondingCompanyId) {
        this.correspondingCompanyId = correspondingCompanyId;
    }

    public String getCorrespondingCompanyName() {
        return correspondingCompanyName;
    }

    public void setCorrespondingCompanyName(String correspondingCompanyName) {
        this.correspondingCompanyName = correspondingCompanyName;
    }

    public String getExchangedDocumentId() {
        return exchangedDocumentId;
    }

    public void setExchangedDocumentId(String exchangedDocumentId) {
        this.exchangedDocumentId = exchangedDocumentId;
    }

    public IDocument getExchangedDocument() {
        return exchangedDocument;
    }

    public void setExchangedDocument(IDocument exchangedDocument) {
        this.exchangedDocument = exchangedDocument;
    }

    public List<String> getAuxiliaryFileIds() {
        return auxiliaryFileIds;
    }

    public void setAuxiliaryFileIds(List<String> auxiliaryFileIds) {
        this.auxiliaryFileIds = auxiliaryFileIds;
    }

    public List<DocumentReferenceType> getAuxiliaryFiles() {
        return auxiliaryFiles;
    }

    public void setAuxiliaryFiles(List<DocumentReferenceType> auxiliaryFiles) {
        this.auxiliaryFiles = auxiliaryFiles;
    }
}
