package eu.nimble.service.bp.util.email;

import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentStatus;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;

import java.util.Date;

public interface IEmailSenderUtil {
    void sendTrustScoreUpdateEmail(String partyID, String federationID, String bearerToken, String language);

    void sendCollaborationStatusEmail(String bearerToken, String originalBearerToken, String clientFederationId,ProcessInstanceGroupDAO groupDAO,String language);

    void sendNewDeliveryDateEmail(String bearerToken, Date newDeliveryDate, String buyerPartyId, String buyerPartyFederationId, String sellerFederationId, String processInstanceId);

    void sendBusinessProcessStatusEmail(String bearerToken, String originalBearerToken, String clientFederationId, String documentId, String language);

    void notifyPartyOnNewDeliveryDate(String toEmail,String productName, String respondingPartyName, String expectedDeliveryDate, String url);

    void notifyPartyOnBusinessProcess(String[] toEmail, String initiatingPersonName, String productName, String initiatingPartyName,
                                 String url, String subject, String respondingPartyName, String language, ProcessDocumentStatus processDocumentStatus);

}
