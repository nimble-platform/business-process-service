package eu.nimble.service.bp.application;

import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;

/**
 * Created by yildiray on 6/5/2017.
 */
public interface IBusinessProcessApplication {
    public Object createDocument(String initiatorID, String responderID, String content,
                                 ProcessDocumentMetadata.TypeEnum documentType);

    public void saveDocument(String processInstanceId, String initiatorID, String responderID, Object documentObject);

    public void sendDocument(String processInstanceId, String initiatorID, String responderID, Object documentObject);
}
