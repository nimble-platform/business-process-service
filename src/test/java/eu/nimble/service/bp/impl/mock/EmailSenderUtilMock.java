package eu.nimble.service.bp.impl.mock;

import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.util.email.IEmailSenderUtil;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Date;

@Profile("test")
@Component
public class EmailSenderUtilMock implements IEmailSenderUtil {
    @Override
    public void notifyTrustScoreUpdate(String partyID, String federationID, String bearerToken) {

    }

    @Override
    public void sendCollaborationStatusEmail(String bearerToken, ProcessInstanceGroupDAO groupDAO) {

    }

    @Override
    public void sendNewDeliveryDateEmail(String bearerToken, Date newDeliveryDate, String buyerPartyId, String buyerPartyFederationId, String sellerFederationId, String processInstanceId) {

    }

    @Override
    public void sendActionPendingEmail(String bearerToken, String documentId) {

    }

    @Override
    public void notifyPartyOnPendingCollaboration(String[] toEmail, String initiatingPersonName, String productName, String initiatingPartyName, String url, String subject, String respondingPartyName) {

    }

    @Override
    public void notifyPartyOnCollaboration(String[] toEmail, String initiatingPersonName, String productName, String initiatingPartyName, String url, String subject, String respondingPartyName) {

    }

    @Override
    public void notifyPartyOnNewDeliveryDate(String toEmail, String productName, String respondingPartyName, String expectedDeliveryDate, String url) {

    }
}
