package eu.nimble.service.bp.util.migration.r15;

import eu.nimble.service.bp.model.hyperjaxb.FederatedCollaborationGroupMetadataDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import java.math.BigInteger;
import java.util.List;

@ApiIgnore
@Controller
public class R15MigrationController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String QUERY_GET_PRECEDING_GROUP_ID = "SELECT group.ID FROM ProcessInstanceGroupDAO group WHERE group.hjid = :hjid";
    // native query
    private final String QUERY_GET_PRECEDING_GROUP_HJID = "select precedingprocessinstancegroup_hjid from process_instance_group_dao where hjid = :hjid";

    @ApiOperation(value = "", notes = "Federate business-process data models")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Federated business-process data models successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while federating business-process data models")
    })
    @RequestMapping(value = "/r15/migration/federate-bp-models",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity federateBpDataModels(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken
    ) {
        logger.info("Incoming request to federate bp data models");

        // check token
        ResponseEntity tokenCheck = eu.nimble.service.bp.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        // federation id
        String federationId = SpringBridge.getInstance().getFederationId();
        if(federationId == null){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("This instance does not have a federation id");
        }

        GenericJPARepository bpRepository = new JPARepositoryFactory().forBpRepositoryMultiTransaction(true);
        try{

            // set federation ids of ProcessDocumentMetadataDAO
            List<ProcessDocumentMetadataDAO> processDocumentMetadataDAOS = bpRepository.getEntities(ProcessDocumentMetadataDAO.class);
            logger.info("There are {} processDocumentMetadataDAOS",processDocumentMetadataDAOS.size());
            for (ProcessDocumentMetadataDAO processDocumentMetadataDAO : processDocumentMetadataDAOS) {
                processDocumentMetadataDAO.setInitiatorFederationID(federationId);
                processDocumentMetadataDAO.setResponderFederationID(federationId);
                // update entity
                bpRepository.updateEntity(processDocumentMetadataDAO);
            }
            // set federation id and preceding process metadata of ProcessInstanceGroupDAO
            List<ProcessInstanceGroupDAO> processInstanceGroupDAOS = bpRepository.getEntities(ProcessInstanceGroupDAO.class);
            logger.info("There are {} processInstanceGroupDAOS",processInstanceGroupDAOS.size());
            for (ProcessInstanceGroupDAO processInstanceGroupDAO : processInstanceGroupDAOS) {
                // set federation id
                processInstanceGroupDAO.setFederationID(federationId);

                // get preceding group hjid
                List<BigInteger> precedingGroupHjids = bpRepository.getEntities(QUERY_GET_PRECEDING_GROUP_HJID, new String[]{"hjid"}, new Object[]{processInstanceGroupDAO.getHjid()},null,null,true);
                if(precedingGroupHjids != null && precedingGroupHjids.size() > 0 && precedingGroupHjids.get(0) != null){
                    logger.info("Group with id {} has a preceding group with hjid {}",processInstanceGroupDAO.getID(),precedingGroupHjids.get(0));
                    String precedingGroupId = bpRepository.getSingleEntity(QUERY_GET_PRECEDING_GROUP_ID, new String[]{"hjid"}, new Object[]{precedingGroupHjids.get(0).longValue()});
                    // create FederatedCollaborationGroupMetadataDAO
                    FederatedCollaborationGroupMetadataDAO federatedCollaborationGroupMetadataDAO = new FederatedCollaborationGroupMetadataDAO();
                    federatedCollaborationGroupMetadataDAO.setID(precedingGroupId);
                    federatedCollaborationGroupMetadataDAO.setFederationID(federationId);
                    // set preceding process metadata for group
                    processInstanceGroupDAO.setPrecedingProcessInstanceGroupMetadata(federatedCollaborationGroupMetadataDAO);
                }

                // update entity
                bpRepository.updateEntity(processInstanceGroupDAO);
            }

            bpRepository.commit();
        }
        catch (Exception e){
            bpRepository.rollback();
            String msg = "Unexpected error while federating bp data models";
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }

        logger.info("Completed request to federate bp data models");
        return ResponseEntity.ok(null);
    }
}
