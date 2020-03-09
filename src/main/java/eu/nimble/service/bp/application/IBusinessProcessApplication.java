package eu.nimble.service.bp.application;

import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;

import java.util.List;

/**
 * Created by yildiray on 6/5/2017.
 */
public interface IBusinessProcessApplication {
    public Object createDocument(String initiatorID, String responderID, String content,
                                 ProcessDocumentMetadata.TypeEnum documentType);

    public void saveDocument(String businessContextId,String processInstanceId, String initiatorID, String responderID,String creatorUserID, Object documentObject, List<String> relatedProducts, List<String> relatedProductCategories,String initiatorFederationId,String responderFederationId);

    public void sendDocument(String businessContextId,String processInstanceId, String initiatorID, String responderID, Object documentObject);
}
