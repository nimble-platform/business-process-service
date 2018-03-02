package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.HibernateUtilityRef;
import eu.nimble.service.bp.swagger.api.GroupApi;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import eu.nimble.utility.DateUtility;
import io.swagger.annotations.ApiParam;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 06-Feb-18.
 */
@Controller
public class ProcessInstanceGroupController implements GroupApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ResponseEntity<ProcessInstanceGroup> addProcessInstanceToGroup(
            @ApiParam(value = "Identifier of the process instance group to which a new process instance id is added", required = true) @PathVariable("ID") String ID,
            @ApiParam(value = "Identifier of the process instance to be added", required = true) @RequestParam(value = "processInstanceID", required = true) String processInstanceID) {
        logger.debug("Adding process instance: {} to ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = DAOUtility.getProcessInstanceGroupDAOByID(ID);
        processInstanceGroupDAO.getProcessInstanceIDs().add(processInstanceID);
        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Added process instance: {} to ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<ProcessInstanceGroup> deleteProcessInstanceFromGroup(@ApiParam(value = "Identifier of the process instance group from which the process instance id is deleted", required = true) @PathVariable("ID") String ID, @ApiParam(value = "Identifier of the process instance to be deleted", required = true) @RequestParam(value = "processInstanceID", required = true) String processInstanceID) {
        logger.debug("Deleting process instance: {} from ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = DAOUtility.getProcessInstanceGroupDAOByID(ID);
        processInstanceGroupDAO.getProcessInstanceIDs().remove(processInstanceID);
        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Deleted process instance: {} from ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<Void> deleteProcessInstanceGroup(@ApiParam(value = "", required = true) @PathVariable("ID") String ID) {
        logger.debug("Deleting ProcessInstanceGroup ID: {}", ID);

        DAOUtility.deleteProcessInstanceGroupDAOByID(ID);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Deleted ProcessInstanceGroups: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<ProcessInstanceGroup> getProcessInstanceGroup(@ApiParam(value = "", required = true) @PathVariable("ID") String ID) {
        logger.debug("Getting ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = DAOUtility.getProcessInstanceGroupDAOByID(ID);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Retrieved ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<List<ProcessInstanceGroup>> getProcessInstanceGroups(@ApiParam(value = "Identifier of the party") @RequestParam(value = "partyID", required = false) String partyID
            , @ApiParam(value = "Related products") @RequestParam(value = "relatedProducts", required = false) List<String> relatedProducts
            , @ApiParam(value = "Identifier of the corresponsing trading partner ID") @RequestParam(value = "tradingPartnerIDs", required = false) List<String> tradingPartnerIDs
            , @ApiParam(value = "Initiation date range") @RequestParam(value = "initiationDateRange", required = false) String initiationDateRange
            , @ApiParam(value = "Last activity date range") @RequestParam(value = "lastActivityDateRange", required = false) String lastActivityDateRange
            , @ApiParam(value = "Offset of the first result among the complete result set satisfying the given criteria", defaultValue = "0") @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset
            , @ApiParam(value = "Number of results to be included in the result set", defaultValue = "10") @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit
            , @ApiParam(value = "", defaultValue = "false") @RequestParam(value = "archived", required = false, defaultValue = "false") Boolean archived) {
        logger.debug("Getting ProcessInstanceGroups for party: {}", partyID);

        List<ProcessInstanceGroupDAO> processInstanceGroupDAOs = DAOUtility.getProcessInstanceGroupDAOs(partyID.trim(), offset, limit, archived);
        logger.debug(" There are {} process instance groups as a whole..", processInstanceGroupDAOs.size());
        List<ProcessInstanceGroup> processInstanceGroups = new ArrayList<>();
        for (ProcessInstanceGroupDAO processInstanceGroupDAO : processInstanceGroupDAOs) {

            // In order to apply filter first decompose the elements...
            if(partyID != null) {
                logger.debug("Party ID is not null: {}", partyID);
                String firstProcessInstanceID = processInstanceGroupDAO.getProcessInstanceIDs().get(0);
                String lastProcessInstanceID = processInstanceGroupDAO.getProcessInstanceIDs().get(processInstanceGroupDAO.getProcessInstanceIDs().size() - 1);

                ProcessInstanceDAO firstProcessInstance = DAOUtility.getProcessIntanceDAOByID(firstProcessInstanceID);
                String creationDate = firstProcessInstance.getCreationDate();
                DateTime creationDateTime = DateUtility.convert(creationDate);

                ProcessInstanceDAO lastProcessInstance = DAOUtility.getProcessIntanceDAOByID(lastProcessInstanceID);
                String lastActivityDate = lastProcessInstance.getCreationDate();
                DateTime lastActivityDateTime = DateUtility.convert(lastActivityDate);

                logger.debug(" First instance {}, creation date {}, last instance {}, activity date {}...", firstProcessInstanceID, creationDate, lastProcessInstanceID, lastActivityDate);

                boolean creationDateFilterOK = false;
                if (initiationDateRange != null) {
                    String[] parts = initiationDateRange.split("_");
                    DateTime start = DateUtility.convert(parts[0]);
                    DateTime finish = DateUtility.convert(parts[1]);

                    creationDateFilterOK = creationDateTime.isAfter(start) && creationDateTime.isBefore(finish);
                } else
                    creationDateFilterOK = true;

                boolean lastActivityDateFilterOK = false;
                if (lastActivityDateRange != null) {
                    String[] parts = lastActivityDateRange.split("_");
                    DateTime start = DateUtility.convert(parts[0]);
                    DateTime finish = DateUtility.convert(parts[1]);

                    lastActivityDateFilterOK = lastActivityDateTime.isAfter(start) && lastActivityDateTime.isBefore(finish);
                } else
                    lastActivityDateFilterOK = true;

                boolean productsFilterOK = false;
                boolean tradingPartnersFilterOK = false;
                // get process instance documents, to reach the related products in the document...
                List<ProcessDocumentMetadataDAO> processInstanceDocuments = DAOUtility.getProcessDocumentMetadataByProcessInstanceID(firstProcessInstanceID);
                for (ProcessDocumentMetadataDAO processInstanceDocument : processInstanceDocuments) {
                    String initiatorID = processInstanceDocument.getInitiatorID();
                    String responderID = processInstanceDocument.getResponderID();
                    logger.debug(" Document initiator {} and document responder {}...", initiatorID, responderID);
                    if (!tradingPartnerIDs.isEmpty()) {
                        for (String tradingPartner : tradingPartnerIDs) {
                            if (tradingPartner.equals(initiatorID) || tradingPartner.equals(responderID)) {
                                tradingPartnersFilterOK = true;
                                break;
                            }
                        }
                    } else
                        tradingPartnersFilterOK = true;


                    List<String> products = processInstanceDocument.getRelatedProducts();
                    logger.debug(" Document related products {}...", products);
                    if (!relatedProducts.isEmpty()) {
                        for (String product : products) {
                            if (relatedProducts.contains(product)) {
                                productsFilterOK = true;
                                break;
                            }
                        }
                    } else
                        productsFilterOK = true;
                }

                if (creationDateFilterOK && lastActivityDateFilterOK && tradingPartnersFilterOK && productsFilterOK)
                    processInstanceGroups.add(HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO));
            } else {
                processInstanceGroups.add(HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO));
            }
        }

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroups);
        logger.debug("Retrieved ProcessInstanceGroups for party: {}", partyID);
        return response;
    }

    @Override
    public ResponseEntity<ProcessInstanceGroup> archiveGroup(@ApiParam(value = "Identifier of the process instance group to be archived", required = true) @PathVariable("ID") String ID) {
        logger.debug("Archiving ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = DAOUtility.getProcessInstanceGroupDAOByID(ID);
        processInstanceGroupDAO.setArchived(true);

        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Archived ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<ProcessInstanceGroup> restoreGroup(@ApiParam(value = "Identifier of the process instance group to be restored", required = true) @PathVariable("ID") String ID) {
        logger.debug("Restoring ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = DAOUtility.getProcessInstanceGroupDAOByID(ID);
        processInstanceGroupDAO.setArchived(false);

        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Restored ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<Void> saveProcessInstanceGroup(@ApiParam(value = "The content of the process instance group to be saved", required = true) @RequestBody ProcessInstanceGroup processInstanceGroup) {
        logger.debug("Saving ProcessInstanceGroup {}", processInstanceGroup.toString());
        ProcessInstanceGroupDAO processInstanceGroupDAO = HibernateSwaggerObjectMapper.createProcessInstanceGroup_DAO(processInstanceGroup);
        HibernateUtilityRef.getInstance("bp-data-model").persist(processInstanceGroupDAO);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Saved ProcessInstanceGroup {}", processInstanceGroup.toString());
        return response;
    }

    @Override
    public ResponseEntity<Void> archiveAllGroups(@ApiParam(value = "Identifier of the party of which groups will be archived", required = true) @RequestParam(value = "partyID", required = true) String partyID) {
        logger.debug("Archiving ProcessInstanceGroups for party {}", partyID);

        DAOUtility.archiveAllGroupsForParty(partyID);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Archived ProcessInstanceGroup for party {}", partyID);
        return response;
    }

    @Override
    public ResponseEntity<Void> deleteAllArchivedGroups(@ApiParam(value = "Identifier of the party of which groups will be deleted", required = true) @RequestParam(value = "partyID", required = true) String partyID) {
        logger.debug("Deleting archived ProcessInstanceGroups for party {}", partyID);

        DAOUtility.deleteArchivedGroupsForParty(partyID);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Deleted archived ProcessInstanceGroups for party {}", partyID);
        return response;
    }
}
