package eu.nimble.service.bp.impl.util.email;

import eu.nimble.common.rest.identity.IdentityClientTyped;
import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.impl.persistence.util.DAOUtility;
import eu.nimble.service.bp.impl.persistence.util.DocumentMetadataDAOUtility;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.List;

/**
 * Created by suat on 16-Oct-18.
 */
@Component
public class EmailSenderUtil {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EmailService emailService;

    @Autowired
    private IdentityClientTyped identityClient;

    // email sender thread
    public void sendCancellationEmail(String bearerToken, ProcessInstanceGroupDAO groupDAO) {
        new Thread(() -> {
            // Collect the trading partner name
            String cancellingPartyId = groupDAO.getPartyID();
            PartyType tradingPartner;
            String tradingPartnerId = DocumentMetadataDAOUtility.getTradingPartnerId(groupDAO.getProcessInstanceIDs().get(0), cancellingPartyId);
            try {
                tradingPartner = identityClient.getParty(bearerToken, tradingPartnerId);

            } catch (IOException e) {
                logger.error("Failed to send email for cancellation of group: {}", groupDAO.getID());
                logger.error("Failed to get party with id: {} from identity service", tradingPartnerId, e);
                return;
            }

            // collect product name
            List<String> productNameList = DAOUtility.getProcessDocumentMetadataByProcessInstanceID(groupDAO.getProcessInstanceIDs().get(0)).get(0).getRelatedProducts();
            StringBuilder productNames = new StringBuilder("");
            for (int i = 0; i < productNameList.size() - 1; i++) {
                productNames.append(productNameList.get(i)).append(", ");
            }
            productNames.append(productNameList.get(productNameList.size() - 1));


            // Try to identify the recipient email
            //
            // If there are more than one instances in the group, simply select the first process instance and
            // use the creatorID of the document created by the trading party. Otherwise, use the email of the first
            // person associated with the trading partner
            String toEmail;
            if (groupDAO.getProcessInstanceIDs().size() > 1) {
                ProcessDocumentMetadataDAO documentMetadataDAO = DocumentMetadataDAOUtility.getDocumentOfTheOtherParty(groupDAO.getProcessInstanceIDs().get(0), cancellingPartyId);
                // get person via the identity client
                String personId = documentMetadataDAO.getCreatorUserID();
                PersonType person;
                try {
                    person = identityClient.getPerson(bearerToken, personId);
                    toEmail = person.getContact().getElectronicMail();
                } catch (IOException e) {
                    logger.error("Failed to send email for cancellation of group: {}", groupDAO.getID());
                    logger.error("Failed to get person with id: {} from identity service", personId, e);
                    return;
                }

            } else {
                toEmail = tradingPartner.getPerson().get(0).getContact().getElectronicMail();
            }

            // collect name of the person cancelling the collaboration
            PersonType cancellingPerson;
            String cancellingPersonName;
            try {
                cancellingPerson = identityClient.getPerson(bearerToken);

            } catch (IOException e) {
                logger.error("Failed to send email for cancellation of group: {}", groupDAO.getID());
                logger.error("Failed to get person with token: {} from identity service", bearerToken, e);
                return;
            }
            cancellingPersonName = new StringBuilder("").append(cancellingPerson.getFirstName()).append(" ").append(cancellingPerson.getFamilyName()).toString();


            notifyPartyWithCancelledCollaboration(toEmail, cancellingPersonName, productNames.toString(), tradingPartner.getName().getValue());
            logger.info("Collaboration cancellation mail sent to: {} for cancellation of group: {}", toEmail, groupDAO.getID());
        }).start();
    }

    public void notifyPartyWithCancelledCollaboration(String toEmail, String cancellingPersonName, String productName, String tradingPartnerName) {
        Context context = new Context();

        context.setVariable("tradingPartnerPerson", cancellingPersonName);
        context.setVariable("tradingPartner", tradingPartnerName);
        context.setVariable("product", productName);

        String subject = "NIMBLE: Business process cancelled";

        emailService.send(new String[]{toEmail}, subject, "cancelled_collaboration", context);
    }
}
