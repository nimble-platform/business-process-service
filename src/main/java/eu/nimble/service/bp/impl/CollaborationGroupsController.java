package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.CollaborationGroupDAO;
import eu.nimble.service.bp.impl.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.swagger.api.CollaborationGroupsApi;
import eu.nimble.service.bp.swagger.model.CollaborationGroup;
import eu.nimble.service.bp.swagger.model.CollaborationGroupResponse;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
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
    private JPARepositoryFactory repoFactory;

    @Override
    public ResponseEntity<CollaborationGroup> archiveCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be archived", required = true) @PathVariable("id") String id) {
        logger.debug("Archiving CollaborationGroup: {}", id);

        CollaborationGroupDAO collaborationGroupDAO = CollaborationGroupDAOUtility.archiveCollaborationGroup(id);

        CollaborationGroup collaborationGroup = HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(collaborationGroup);
        logger.debug("Archived CollaborationGroup: {}", id);
        return response;
    }

    @Override
    public ResponseEntity<Void> deleteCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be deleted", required = true) @PathVariable("id") String id) {
        logger.debug("Deleting CollaborationGroup ID: {}", id);

        CollaborationGroupDAOUtility.deleteCollaborationGroupDAOByID(Long.parseLong(id));

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Deleted CollaborationGroup ID: {}", id);
        return response;
    }

    @Override
    public ResponseEntity<CollaborationGroup> getCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be received", required = true) @PathVariable("id") String id) {
        logger.debug("Getting CollaborationGroup: {}", id);

        CollaborationGroupDAO collaborationGroupDAO = repoFactory.forBpRepository().getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(id));
        if (collaborationGroupDAO == null) {
            logger.error("There does not exist a collaboration group with id: {}", id);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        CollaborationGroup collaborationGroup = HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(collaborationGroup);
        logger.debug("Retrieved CollaborationGroup: {}", id);
        return response;
    }

    @Override
    @ApiOperation(value = "", notes = "Retrieve process instance groups for the specified party. If no partyID is specified, then all groups are returned")
    public ResponseEntity<CollaborationGroupResponse> getCollaborationGroups(@ApiParam(value = "Identifier of the party as specified by the identity service") @RequestParam(value = "partyId", required = false) String partyId,
                                                                             @ApiParam(value = "Names of the products for which the collaboration activities are performed") @RequestParam(value = "relatedProducts", required = false) List<String> relatedProducts,
                                                                             @ApiParam(value = "Categories of the products.\nFor example:MDF raw,Split air conditioner") @RequestParam(value = "relatedProductCategories", required = false) List<String> relatedProductCategories,
                                                                             @ApiParam(value = "Identifier (party id) of the corresponding trading partners") @RequestParam(value = "tradingPartnerIDs", required = false) List<String> tradingPartnerIDs,
                                                                             @ApiParam(value = "Offset of the first result among the complete result set satisfying the given criteria", defaultValue = "0") @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
                                                                             @ApiParam(value = "Number of results to be included in the result set", defaultValue = "10") @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                                                                             @ApiParam(value = "Whether the collaboration group is archived or not", defaultValue = "false") @RequestParam(value = "archived", required = false, defaultValue = "false") Boolean archived,
                                                                             @ApiParam(value = "Status of the process instance included in the group\nPossible values:STARTED,WAITING,CANCELLED,COMPLETED") @RequestParam(value = "status", required = false) List<String> status,
                                                                             @ApiParam(value = "Role of the party in the collaboration.\nPossible values:SELLER,BUYER") @RequestParam(value = "collaborationRole", required = false) String collaborationRole) {
        logger.debug("Getting collaboration groups for party: {}", partyId);

        List<CollaborationGroupDAO> results = CollaborationGroupDAOUtility.getCollaborationGroupDAOs(partyId, collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories, status, null, null, limit, offset);
        int totalSize = CollaborationGroupDAOUtility.getCollaborationGroupSize(partyId, collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories, status, null, null);
        logger.debug(" There are {} collaboration groups in total", results.size());
        List<CollaborationGroup> collaborationGroups = new ArrayList<>();
        for (CollaborationGroupDAO result : results) {
            collaborationGroups.add(HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(result));
        }

        CollaborationGroupResponse groupResponse = new CollaborationGroupResponse();
        groupResponse.setCollaborationGroups(collaborationGroups);
        groupResponse.setSize(totalSize);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(groupResponse);
        logger.debug("Retrieved collaboration groups for party: {}", partyId);
        return response;
    }

    @Override
    public ResponseEntity<CollaborationGroup> restoreCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be restored", required = true) @PathVariable("id") String id) {
        logger.debug("Restoring CollaborationGroup: {}", id);

        CollaborationGroupDAO collaborationGroupDAO = CollaborationGroupDAOUtility.restoreCollaborationGroup(id);

        CollaborationGroup collaborationGroup = HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(collaborationGroup);
        logger.debug("Restored CollaborationGroup: {}", id);
        return response;
    }

    @Override
    public ResponseEntity<Void> updateCollaborationGroupName(@ApiParam(value = "Identifier of the collaboration group", required = true) @PathVariable("id") String id,
                                                             @ApiParam(value = "Value to be set as name of the collaboration group", required = true) @RequestParam(value = "groupName", required = true) String groupName) {
        logger.debug("Updating name of the collaboration group :" + id);
        GenericJPARepository repo = repoFactory.forBpRepository();
        CollaborationGroupDAO collaborationGroupDAO = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(id));
        if (collaborationGroupDAO == null) {
            String msg = String.format("There does not exist a collaboration group with id: %s", id);
            logger.error(msg);
            ResponseEntity response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
            return response;
        }
        collaborationGroupDAO.setName(groupName);
        repo.updateEntity(collaborationGroupDAO);
        logger.debug("Updated name of the collaboration group :" + id);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        return response;
    }
}
