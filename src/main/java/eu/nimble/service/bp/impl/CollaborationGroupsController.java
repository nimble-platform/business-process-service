package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.CollaborationGroupDAO;
import eu.nimble.service.bp.impl.persistence.bp.CollaborationGroupDAORepository;
import eu.nimble.service.bp.impl.persistence.util.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.persistence.util.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.bp.swagger.api.CollaborationGroupsApi;
import eu.nimble.service.bp.swagger.model.CollaborationGroup;
import eu.nimble.service.bp.swagger.model.CollaborationGroupResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
@Controller
public class CollaborationGroupsController implements CollaborationGroupsApi{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CollaborationGroupDAORepository collaborationGroupDAORepository;

    @Override
    public ResponseEntity<CollaborationGroup> archiveCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be archived", required = true) @PathVariable("ID") String ID) {
        logger.debug("Archiving CollaborationGroup: {}", ID);

        CollaborationGroupDAO collaborationGroupDAO = ProcessInstanceGroupDAOUtility.archiveCollaborationGroup(ID);

        CollaborationGroup collaborationGroup = HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(collaborationGroup);
        logger.debug("Archived CollaborationGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<Void> deleteCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be deleted", required = true) @PathVariable("ID") String ID) {
        logger.debug("Deleting CollaborationGroup ID: {}", ID);

        ProcessInstanceGroupDAOUtility.deleteCollaborationGroupDAOByID(Long.parseLong(ID));

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Deleted CollaborationGroup ID: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<CollaborationGroup> getCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be received", required = true) @PathVariable("ID") String ID) {
        logger.debug("Getting CollaborationGroup: {}", ID);

        CollaborationGroupDAO collaborationGroupDAO = collaborationGroupDAORepository.getOne(Long.parseLong(ID));
        if (collaborationGroupDAO == null) {
            logger.error("There does not exist a collaboration group with id: {}", ID);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        CollaborationGroup collaborationGroup = HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(collaborationGroup);
        logger.debug("Retrieved CollaborationGroup: {}", ID);
        return response;
    }

    @Override
    @ApiOperation(value = "", notes = "Retrieve process instance groups for the specified party. If no partyID is specified, then all groups are returned")
    public ResponseEntity<CollaborationGroupResponse> getCollaborationGroups(@ApiParam(value = "Identifier of the party as specified by the identity service") @RequestParam(value = "partyID", required = false) String partyID,
                                                                             @ApiParam(value = "Related products") @RequestParam(value = "relatedProducts", required = false) List<String> relatedProducts,
                                                                             @ApiParam(value = "Related product categories") @RequestParam(value = "relatedProductCategories", required = false) List<String> relatedProductCategories,
                                                                             @ApiParam(value = "Identifier of the corresponsing trading partner ID") @RequestParam(value = "tradingPartnerIDs", required = false) List<String> tradingPartnerIDs,
                                                                             @ApiParam(value = "Offset of the first result among the complete result set satisfying the given criteria", defaultValue = "0") @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
                                                                             @ApiParam(value = "Number of results to be included in the result set", defaultValue = "10") @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                                                                             @ApiParam(value = "", defaultValue = "false") @RequestParam(value = "archived", required = false, defaultValue = "false") Boolean archived,
                                                                             @ApiParam(value = "Status of the process instance included in the group\nPossible values:STARTED,WAITING,CANCELLED,COMPLETED") @RequestParam(value = "status", required = false) List<String> status,
                                                                             @ApiParam(value = "Role of the party in the collaboration.\nPossible values:SELLER,BUYER") @RequestParam(value = "collaborationRole", required = false) String collaborationRole) {
        logger.debug("Getting collaboration groups for party: {}", partyID);

        List<CollaborationGroupDAO> results = ProcessInstanceGroupDAOUtility.getCollaborationGroupDAOs(partyID, collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories, status, null, null, limit, offset);
        int totalSize = ProcessInstanceGroupDAOUtility.getCollaborationGroupSize(partyID, collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories, status, null, null);
        logger.debug(" There are {} collaboration groups in total", results.size());
        List<CollaborationGroup> collaborationGroups = new ArrayList<>();
        for (CollaborationGroupDAO result : results) {
            collaborationGroups.add(HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(result));
        }

        CollaborationGroupResponse groupResponse = new CollaborationGroupResponse();
        groupResponse.setCollaborationGroups(collaborationGroups);
        groupResponse.setSize(totalSize);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(groupResponse);
        logger.debug("Retrieved collaboration groups for party: {}", partyID);
        return response;
    }

    @Override
    public ResponseEntity<CollaborationGroup> restoreCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be restored", required = true) @PathVariable("ID") String ID) {
        logger.debug("Restoring CollaborationGroup: {}", ID);

        CollaborationGroupDAO collaborationGroupDAO = ProcessInstanceGroupDAOUtility.restoreCollaborationGroup(ID);

        CollaborationGroup collaborationGroup = HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(collaborationGroup);
        logger.debug("Restored CollaborationGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<Void> updateCollaborationGroupName(@ApiParam(value = "Identifier of the collaboration group", required = true) @PathVariable("ID") String ID,
                                                             @ApiParam(value = "Value to be set as name of the collaboration group", required = true) @RequestParam(value = "groupName", required = true) String groupName) {
        logger.debug("Updating name of the collaboration group :" + ID);
        CollaborationGroupDAO collaborationGroupDAO = collaborationGroupDAORepository.getOne(Long.parseLong(ID));
        if (collaborationGroupDAO == null) {
            String msg = String.format("There does not exist a collaboration group with id: %s", ID);
            logger.error(msg);
            ResponseEntity response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
            return response;
        }
        collaborationGroupDAO.setName(groupName);
        collaborationGroupDAORepository.save(collaborationGroupDAO);
        logger.debug("Updated name of the collaboration group :" + ID);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        return response;
    }
}
