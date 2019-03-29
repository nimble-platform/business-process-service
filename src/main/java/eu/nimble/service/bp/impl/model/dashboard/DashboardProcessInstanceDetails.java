package eu.nimble.service.bp.impl.model.dashboard;

import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;

import java.util.List;

public class DashboardProcessInstanceDetails {

    private Object requestDocument;
    private Object responseDocument;
    private ProcessDocumentMetadata requestMetadata;
    private ProcessDocumentMetadata responseMetadata;
    private List<HistoricVariableInstance> variableInstance;
    private HistoricActivityInstance lastActivityInstance;
    private HistoricProcessInstance processInstance;
    private PersonType requestCreatorUser;
    private PersonType responseCreatorUser;


    public DashboardProcessInstanceDetails() {
    }

    public Object getRequestDocument() {
        return requestDocument;
    }

    public void setRequestDocument(Object requestDocument) {
        this.requestDocument = requestDocument;
    }

    public Object getResponseDocument() {
        return responseDocument;
    }

    public void setResponseDocument(Object responseDocument) {
        this.responseDocument = responseDocument;
    }

    public ProcessDocumentMetadata getRequestMetadata() {
        return requestMetadata;
    }

    public void setRequestMetadata(ProcessDocumentMetadata requestMetadata) {
        this.requestMetadata = requestMetadata;
    }

    public ProcessDocumentMetadata getResponseMetadata() {
        return responseMetadata;
    }

    public void setResponseMetadata(ProcessDocumentMetadata responseMetadata) {
        this.responseMetadata = responseMetadata;
    }

    public List<HistoricVariableInstance> getVariableInstance() {
        return variableInstance;
    }

    public void setVariableInstance(List<HistoricVariableInstance> variableInstance) {
        this.variableInstance = variableInstance;
    }

    public HistoricActivityInstance getLastActivityInstance() {
        return lastActivityInstance;
    }

    public void setLastActivityInstance(HistoricActivityInstance lastActivityInstance) {
        this.lastActivityInstance = lastActivityInstance;
    }

    public HistoricProcessInstance getProcessInstance() {
        return processInstance;
    }

    public void setProcessInstance(HistoricProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    public PersonType getRequestCreatorUser() {
        return requestCreatorUser;
    }

    public void setRequestCreatorUser(PersonType requestCreatorUserInfo) {
        this.requestCreatorUser = requestCreatorUserInfo;
    }

    public PersonType getResponseCreatorUser() {
        return responseCreatorUser;
    }

    public void setResponseCreatorUser(PersonType responseCreatorUserInfo) {
        this.responseCreatorUser = responseCreatorUserInfo;
    }

}
