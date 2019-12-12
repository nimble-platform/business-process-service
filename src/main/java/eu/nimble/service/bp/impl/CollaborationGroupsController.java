package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.model.hyperjaxb.*;
import eu.nimble.service.bp.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.swagger.model.FederatedCollaborationGroupMetadata;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import eu.nimble.service.bp.util.HttpResponseUtil;
import eu.nimble.service.bp.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.swagger.api.CollaborationGroupsApi;
import eu.nimble.service.bp.swagger.model.CollaborationGroup;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.TrustPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

/**
 * {@link CollaborationGroupDAO}s are entities that keep track collaboration activities with several companies related to
 * an individual product. Collaborations with distinct companies are kept in {@link ProcessInstanceGroupDAO}s.
 * In this sense, considering a simple example, {@link CollaborationGroupDAO} of a seller might contain two {@link ProcessInstanceGroupDAO}s
 * such that the first one contains seller-buyer activities and the second one seller-transport service provider activities.
 */
@Controller
public class CollaborationGroupsController implements CollaborationGroupsApi{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private JPARepositoryFactory repoFactory;
    @Autowired
    private IValidationUtil validationUtil;

    @Override
    @ApiOperation(value = "", notes = "Archives the collaboration group by setting the archive field of the specified CollaborationGroup and the included ProcessInstanceGroups.")
    public ResponseEntity archiveCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be archived (collaborationGroup.hjid)", required = true) @PathVariable("id") String id,
                                                                        @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) {
        logger.debug("Archiving CollaborationGroup: {}", id);
        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            // check whether the collaboration group exists or not
            CollaborationGroupDAO collaborationGroupDAO = CollaborationGroupDAOUtility.getCollaborationGroupDAO(Long.parseLong(id));
            if(collaborationGroupDAO == null){
                String msg = String.format("CollaborationGroup with id %s does not exist", id);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }

            // check whether the group is archiable or not
            boolean isArchivable = CollaborationGroupDAOUtility.isCollaborationGroupArchivable(collaborationGroupDAO.getHjid());
            if (!isArchivable) {
                String msg = String.format("CollaborationGroup with id %s is not archivable", id);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(msg);
            }

            collaborationGroupDAO = CollaborationGroupDAOUtility.archiveCollaborationGroup(collaborationGroupDAO);

            CollaborationGroup collaborationGroup = HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroupDAO);
            ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(collaborationGroup);
            logger.debug("Archived CollaborationGroup: {}", id);
            return response;

        } catch (Exception e) {
            String msg = String.format("Unexpected error while archiving CollaborationGroup with id: %s", id);
            logger.error(msg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);        }
    }

    @Override
    @ApiOperation(value = "", notes = "Deletes the specified CollaborationGroup permanently.")
    public ResponseEntity<Void> deleteCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be deleted (collaborationGroup.hjid)", required = true) @PathVariable("id") String id,
                                                         @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) {
        logger.debug("Deleting CollaborationGroup ID: {}", id);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }


        CollaborationGroupDAOUtility.deleteCollaborationGroupDAOsByID(Collections.singletonList(Long.parseLong(id)));

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Deleted CollaborationGroup ID: {}", id);
        return response;
    }

    @Override
    @ApiOperation(value = "", notes = "Retrieves the specified CollaborationGroup.")
    public ResponseEntity getCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be received (collaborationGroup.hjid)", required = true) @PathVariable("id") String id,
                                                @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) {
        logger.debug("Getting CollaborationGroup: {}", id);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        CollaborationGroupDAO collaborationGroupDAO = repoFactory.forBpRepository(true).getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(id));
        if (collaborationGroupDAO == null) {
            logger.error("There does not exist a collaboration group with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        for (ProcessInstanceGroupDAO associatedProcessInstanceGroup : collaborationGroupDAO.getAssociatedProcessInstanceGroups()) {
            ProcessInstanceGroupDAO processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(associatedProcessInstanceGroup.getID());
            associatedProcessInstanceGroup.setFirstActivityTime(processInstanceGroupDAO.getFirstActivityTime());
            associatedProcessInstanceGroup.setLastActivityTime(processInstanceGroupDAO.getLastActivityTime());
        }

        if(collaborationGroupDAO.getFederatedCollaborationGroupMetadatas() != null){
            for (FederatedCollaborationGroupMetadataDAO federatedCollaborationGroupMetadata : collaborationGroupDAO.getFederatedCollaborationGroupMetadatas()) {
                try {
                    HttpResponse<JsonNode> response = Unirest.get(SpringBridge.getInstance().getGenericConfig().getDelegateServiceUrl()+"/collaboration-groups/"+federatedCollaborationGroupMetadata.getID())
                            .header("Authorization", bearerToken)
                            .queryString("delegateId",federatedCollaborationGroupMetadata.getFederationID())
                            .asJson();
                    ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
                    CollaborationGroup collaborationGroup = objectMapper.readValue(response.getBody().toString(),CollaborationGroup.class);
                    for (ProcessInstanceGroup associatedProcessInstanceGroup : collaborationGroup.getAssociatedProcessInstanceGroups()) {
                        collaborationGroupDAO.getAssociatedProcessInstanceGroups().add(HibernateSwaggerObjectMapper.createProcessInstanceGroup_DAO(associatedProcessInstanceGroup));
                    }

                } catch (Exception e) {
                    logger.error("Failed to get collaboration group",e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
            }
        }

        CollaborationGroup collaborationGroup = HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(collaborationGroup);
        logger.debug("Retrieved CollaborationGroup: {}", id);
        return response;
    }

    @Override
    @ApiOperation(value = "", notes = "Retrieves CollaborationGroups based on the provided filter parameters. Some of the filter parameters (e.g. product names, trading partner ids) are sought in the inner ProcessInstanceGroups.")
    public ResponseEntity getCollaborationGroups(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                 @ApiParam(value = "Identifier of the party as specified by the identity service (party.id)") @RequestParam(value = "partyId", required = false) String partyId,
                                                 @ApiParam(value = "Names of the products for which the collaboration activities are performed. If multiple product names to be provided, they should be comma-separated.") @RequestParam(value = "relatedProducts", required = false) List<String> relatedProducts,
                                                 @ApiParam(value = "Categories of the products.<br>For example: MDF raw,Split air conditioner") @RequestParam(value = "relatedProductCategories", required = false) List<String> relatedProductCategories,
                                                 @ApiParam(value = "Identifier (party id) of the corresponding trading partners") @RequestParam(value = "tradingPartnerIDs", required = false) List<String> tradingPartnerIDs,
                                                 @ApiParam(value = "Offset of the first result among the complete result set satisfying the given criteria", defaultValue = "0") @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
                                                 @ApiParam(value = "Number of results to be included in the result set", defaultValue = "10") @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                                                 @ApiParam(value = "Whether the collaboration group is archived or not", defaultValue = "false") @RequestParam(value = "archived", required = false, defaultValue = "false") Boolean archived,
                                                 @ApiParam(value = "Status of the process instance included in the group.<br>Possible values:<ul><li>STARTED</li><li>WAITING</li><li>CANCELLED</li><li>COMPLETED</li></ul>") @RequestParam(value = "status", required = false) List<String> status,
                                                 @ApiParam(value = "Role of the party in the collaboration.<br>Possible values:<ul><li>SELLER</li><li>BUYER</li></ul>") @RequestParam(value = "collaborationRole", required = false) String collaborationRole,
                                                 @ApiParam(value = "Identify Project Or Not", defaultValue = "false") @RequestParam(value = "isProject", required = false, defaultValue = "false") Boolean isProject,
                                                 @ApiParam(value = ""  ) @RequestHeader(value="federationId", required=false) String federationId) {
        logger.debug("Getting collaboration groups for party: {}", partyId);
        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            if(partyId != null && federationId == null){
                String msg = "Both party and federation id should be provided";
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
            }

            List<CollaborationGroupDAO> results = CollaborationGroupDAOUtility.getCollaborationGroupDAOs(partyId,federationId, collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories, status, null, null, limit, offset,isProject);
            int totalSize = CollaborationGroupDAOUtility.getCollaborationGroupSize(partyId, federationId,collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories, status, null, null,isProject);
            logger.debug(" There are {} collaboration groups in total", results.size());
            List<CollaborationGroup> collaborationGroups = new ArrayList<>();
            List<Long> groupHjids = new ArrayList<>();
            for (CollaborationGroupDAO result : results) {
                collaborationGroups.add(HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(result));
                groupHjids.add(result.getHjid());
            }

            for (CollaborationGroup collaborationGroup : collaborationGroups) {
                if(collaborationGroup.getFederatedCollaborationGroupMetadatas() != null){
                    for (FederatedCollaborationGroupMetadata federatedCollaborationGroupMetadata : collaborationGroup.getFederatedCollaborationGroupMetadatas()) {
                        try {
                            HttpResponse<JsonNode> response = Unirest.get(SpringBridge.getInstance().getGenericConfig().getDelegateServiceUrl()+"/collaboration-groups/"+federatedCollaborationGroupMetadata.getID())
                                    .header("Authorization", bearerToken)
                                    .queryString("delegateId",federatedCollaborationGroupMetadata.getFederationID())
                                    .asJson();
                            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
                            CollaborationGroup cp = objectMapper.readValue(response.getBody().toString(),CollaborationGroup.class);
                            for (ProcessInstanceGroup associatedProcessInstanceGroup : cp.getAssociatedProcessInstanceGroups()) {
                                collaborationGroup.getAssociatedProcessInstanceGroups().add(associatedProcessInstanceGroup);
                            }

                        } catch (Exception e) {
                            logger.error("Failed to get collaboration group",e);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                        }
                    }
                }
            }

            CollaborationGroupResponse groupResponse = new CollaborationGroupResponse();
            groupResponse.setCollaborationGroups(collaborationGroups);
            groupResponse.setSize(totalSize);

            // get archiveable statuses
            Map<Long, Set<ProcessInstanceStatus>> archiveableStatuses = CollaborationGroupDAOUtility.getCollaborationProcessInstanceStatusesForCollaborationGroups(groupHjids);
            for(Map.Entry<Long, Set<ProcessInstanceStatus>> e : archiveableStatuses.entrySet()) {
                for(CollaborationGroup collaborationGroup : groupResponse.getCollaborationGroups()) {
                    if(e.getKey() == Long.parseLong(collaborationGroup.getID())) {
                        collaborationGroup.setIsArchiveable(CollaborationGroupDAOUtility.doesProcessInstanceStatusesMeetArchivingConditions(new ArrayList<>(e.getValue())));
                    }
                }
            }

            ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(groupResponse);
            logger.debug("Retrieved collaboration groups for party: {}", partyId);
            return response;

        } catch (Exception e) {
            String msg = String.format("Unexpected error while getting CollaborationGroups for party: %s", partyId);
            logger.error(msg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }
    }

    @Override
    @ApiOperation(value = "", notes = "Restores the specified, archived CollaborationGroup.")
    public ResponseEntity<CollaborationGroup> restoreCollaborationGroup(@ApiParam(value = "Identifier of the collaboration group to be restored (collaborationGroup.hjid)", required = true) @PathVariable("id") String id,
                                                                        @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) {
        logger.debug("Restoring CollaborationGroup: {}", id);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        // check the existence of collaboration group
        CollaborationGroupDAO collaborationGroupDAO = CollaborationGroupDAOUtility.getCollaborationGroupDAO(Long.parseLong(id));
        if(collaborationGroupDAO == null){
            String msg = String.format("CollaborationGroup with id %s does not exist", id);
            logger.error(msg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        collaborationGroupDAO = CollaborationGroupDAOUtility.restoreCollaborationGroup(collaborationGroupDAO);

        CollaborationGroup collaborationGroup = HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(collaborationGroup);
        logger.debug("Restored CollaborationGroup: {}", id);
        return response;
    }

    @Override
    @ApiOperation(value = "", notes = "Updates the name of the specified CollaborationGroup")
    public ResponseEntity<Void> updateCollaborationGroupName(@ApiParam(value = "Identifier of the collaboration group", required = true) @PathVariable("id") String id,
                                                             @ApiParam(value = "Value to be set as name of the collaboration group", required = true) @RequestParam(value = "groupName", required = true) String groupName,
                                                             @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) {
        logger.debug("Updating name of the collaboration group :" + id);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        GenericJPARepository repo = repoFactory.forBpRepository();
        CollaborationGroupDAO collaborationGroupDAO = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(id));
        if (collaborationGroupDAO == null) {
            String msg = String.format("There does not exist a collaboration group with id: %s", id);
            logger.error(msg);
            ResponseEntity response = ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            return response;
        }
        collaborationGroupDAO.setName(groupName);
        repo.updateEntity(collaborationGroupDAO);
        logger.debug("Updated name of the collaboration group :" + id);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        return response;
    }



	@Override
    @ApiOperation(value = "", notes = "Merge list of CollaborationGroups")
    public ResponseEntity<CollaborationGroup> mergeCollaborationGroups(
            @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
            @ApiParam(value = "Identifier of the base collaboration group ", required = true) @RequestParam("bcid") String bcid,
            @ApiParam(value = "", required = true) @RequestBody String cgidsAsString
    ) {
        logger.debug("Merging the collaboration groups {} to the base collaboration group {}",cgidsAsString,bcid);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        GenericJPARepository repo = repoFactory.forBpRepository(true);
        CollaborationGroupDAO collaborationGroupDAO = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(bcid));
        if (collaborationGroupDAO == null) {
            String msg = String.format("There does not exist a collaboration group with id: %s", bcid);
            logger.error(msg);
            ResponseEntity response = ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            return response;
        }
        ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
        List<FederatedCollaborationGroupMetadata> cgids;
        try {
            cgids = objectMapper.readValue(cgidsAsString,new TypeReference<List<FederatedCollaborationGroupMetadata>>(){});
        } catch (IOException e) {
            String msg = String.format("Failed to read Federated Collaboration Group Metadata: %s",cgidsAsString);
            logger.error(msg,e);
            ResponseEntity response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
            return response;
        }

        // get collaboration groups' ids which will be merged to the base one
        List<String> collaborationGroupsBelongingToThisInstance = new ArrayList<>();
        List<FederatedCollaborationGroupMetadataDAO> collaborationGroupsBelongingToDifferentInstance = new ArrayList<>();

        for(FederatedCollaborationGroupMetadata federatedCollaborationGroupMetadata: cgids){
            if (federatedCollaborationGroupMetadata.getFederationID().contentEquals(SpringBridge.getInstance().getGenericConfig().getFederationId())) {
                collaborationGroupsBelongingToThisInstance.add(federatedCollaborationGroupMetadata.getID());
            }
            else{
                collaborationGroupsBelongingToDifferentInstance.add(HibernateSwaggerObjectMapper.createFederatedCollaborationGroupMetadata(federatedCollaborationGroupMetadata));
            }
        }

        // merging the groups belonging to the current instance
        if(collaborationGroupsBelongingToThisInstance.size() > 0){
            List<ProcessInstanceGroupDAO> allProcessInstanceGroups = collaborationGroupDAO.getAssociatedProcessInstanceGroups();

            for(String cgid : collaborationGroupsBelongingToThisInstance){
                CollaborationGroupDAO mergeCollaborationGroupDAO = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(cgid));
                if(mergeCollaborationGroupDAO != null) {
                    allProcessInstanceGroups.addAll(mergeCollaborationGroupDAO.getAssociatedProcessInstanceGroups());
                }
            }

            collaborationGroupDAO.setAssociatedProcessInstanceGroups(allProcessInstanceGroups);

            collaborationGroupDAO.setIsProject(true);
            collaborationGroupDAO = repo.updateEntity(collaborationGroupDAO);

            for(String cgid : collaborationGroupsBelongingToThisInstance){
                CollaborationGroupDAOUtility.deleteCollaborationGroupDAOsByID(Collections.singletonList(Long.parseLong(cgid)));
            }
        }
        // merging the groups belonging to a different instance
        if(collaborationGroupsBelongingToDifferentInstance.size() > 0){
            List<FederatedCollaborationGroupMetadataDAO> newFeds = new ArrayList<>(collaborationGroupDAO.getFederatedCollaborationGroupMetadatas());

            for (FederatedCollaborationGroupMetadataDAO federatedCollaborationGroupMetadataDAO : collaborationGroupsBelongingToDifferentInstance) {
                newFeds.add(federatedCollaborationGroupMetadataDAO);
                HttpResponse<JsonNode> response = null;
                try {
                    response = Unirest.get(SpringBridge.getInstance().getGenericConfig().getDelegateServiceUrl()+"/collaboration-groups/"+federatedCollaborationGroupMetadataDAO.getID())
                            .header("Authorization", bearerToken)
                            .queryString("delegateId",federatedCollaborationGroupMetadataDAO.getFederationID())
                            .asJson();
                    CollaborationGroup collaborationGroup = objectMapper.readValue(response.getBody().toString(),CollaborationGroup.class);
                    for (FederatedCollaborationGroupMetadata federatedCollaborationGroupMetadata : collaborationGroup.getFederatedCollaborationGroupMetadatas()) {
                        newFeds.add(HibernateSwaggerObjectMapper.createFederatedCollaborationGroupMetadata(federatedCollaborationGroupMetadata));
                    }
                } catch (Exception e) {
                    logger.error("failed to get collaboration group",e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
            }

            collaborationGroupDAO.setFederatedCollaborationGroupMetadatas(newFeds);
            collaborationGroupDAO.setIsProject(true);
            collaborationGroupDAO = repo.updateEntity(collaborationGroupDAO);

            // unmerge collaboration groups for federated ones
            for (FederatedCollaborationGroupMetadata cgid : cgids) {
                try {
                    HttpResponse<JsonNode> response = Unirest.get(SpringBridge.getInstance().getGenericConfig().getDelegateServiceUrl()+"/collaboration-groups/unmerge")
                            .header("Authorization", bearerToken)
                            .queryString("id",cgid.getID())
                            .queryString("delegateId",cgid.getFederationID())
                            .asJson();
                } catch (Exception e) {
                    logger.error("failed to unmerge group",e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
            }
        }

        CollaborationGroup collaborationGroup = HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(collaborationGroup);
        logger.debug("Merged the collaboration groups {} to the base collaboration group {}",cgids,bcid);
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Checks whether the collaborations of the given party are finished/completed or not. The service considers only the collaborations " +
            "where the party has the given role.")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token")
    })
    public ResponseEntity checkAllCollaborationsFinished(@ApiParam(value = "The identifier of party", required = true) @RequestParam(value = "partyId",required = true) String partyId,
                                                         @ApiParam(value = ""  ) @RequestHeader(value="federationId", required=true) String federationId,
                                                         @ApiParam(value = "Role of the party in the collaboration.<br>Possible values: <ul><li>SELLER</li><li>BUYER</li></ul>") @RequestParam(value = "collaborationRole", required = true,defaultValue = "SELLER") String collaborationRole,
                                                         @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.info("Checking whether all collaborations are finished for party {} and role {}",partyId,collaborationRole);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        // whether all collaborations are finished or not
        Boolean allCollaborationsFinished = true;

        // get collaboration groups
        List<CollaborationGroupDAO> collaborationGroups = CollaborationGroupDAOUtility.getCollaborationGroupDAOs(partyId,federationId,collaborationRole);
        // check whether there is a CompletedTaskType for each process instance group,that is, collaboration is finished
        for(CollaborationGroupDAO collaborationGroup:collaborationGroups){
            Boolean isCollaborationFinished = isCollaborationFinished(collaborationGroup);
            if(!isCollaborationFinished){
                allCollaborationsFinished = false;
                break;
            }
        }

        logger.info("All collaborations are finished {} for party {} and role {}",allCollaborationsFinished,partyId,collaborationRole);
        return ResponseEntity.ok(allCollaborationsFinished.toString());
    }

    @ApiOperation(value = "",notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token")
    })
    @RequestMapping(value = "/collaboration-groups/federated",
            method = RequestMethod.GET)
    public ResponseEntity getFederatedCollaborationGroup(@ApiParam(value = "The identifier of party", required = true) @RequestParam(value = "id",required = true) List<String> groupId,
                                                         @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestParam(value="federationId", required=true) List<String> federationId,
                                                         @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.info("Retrieving federated collaboration group");

        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        int size = groupId.size();
        List<CollaborationGroup> collaborationGroups = new ArrayList<>();
        for(int i = 0; i < size ; i++){
            CollaborationGroupDAO collaborationGroupDAO = CollaborationGroupDAOUtility.getFederatedCollaborationGroup(groupId.get(i),federationId.get(i));

            if(collaborationGroupDAO != null){

                for (ProcessInstanceGroupDAO associatedProcessInstanceGroup : collaborationGroupDAO.getAssociatedProcessInstanceGroups()) {
                    ProcessInstanceGroupDAO processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(associatedProcessInstanceGroup.getID());
                    associatedProcessInstanceGroup.setFirstActivityTime(processInstanceGroupDAO.getFirstActivityTime());
                    associatedProcessInstanceGroup.setLastActivityTime(processInstanceGroupDAO.getLastActivityTime());
                }

                if(collaborationGroupDAO.getFederatedCollaborationGroupMetadatas() != null){
                    for (FederatedCollaborationGroupMetadataDAO federatedCollaborationGroupMetadata : collaborationGroupDAO.getFederatedCollaborationGroupMetadatas()) {
                        try {
                            HttpResponse<JsonNode> response = Unirest.get(SpringBridge.getInstance().getGenericConfig().getDelegateServiceUrl()+"/collaboration-groups/"+federatedCollaborationGroupMetadata.getID())
                                    .header("Authorization", bearerToken)
                                    .queryString("delegateId",federatedCollaborationGroupMetadata.getFederationID())
                                    .asJson();
                            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
                            CollaborationGroup collaborationGroup = objectMapper.readValue(response.getBody().toString(),CollaborationGroup.class);
                            for (ProcessInstanceGroup associatedProcessInstanceGroup : collaborationGroup.getAssociatedProcessInstanceGroups()) {
                                collaborationGroupDAO.getAssociatedProcessInstanceGroups().add(HibernateSwaggerObjectMapper.createProcessInstanceGroup_DAO(associatedProcessInstanceGroup));
                            }

                        } catch (Exception e) {
                            logger.error("Failed to get collaboration group",e);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                        }
                    }
                }
                CollaborationGroup collaborationGroup = HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroupDAO);
                collaborationGroups.add(collaborationGroup);
            }
        }

        logger.info("Retrieved federated collaboration group");
        return ResponseEntity.ok(collaborationGroups);
    }


    @ApiOperation(value = "",notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token")
    })
    @RequestMapping(value = "/collaboration-groups/document/{documentId}",
            method = RequestMethod.POST)
    public ResponseEntity addFederatedMetadataToCollaborationGroup(@ApiParam(value = "The identifier of party", required = true) @PathVariable(value = "documentId") String documentId,
                                                                  @RequestBody String body,
                                                                  @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestParam(value="partyId", required=true) String partyId,
                                                                   @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="federationId", required=true) String federationId,
                                                                  @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.info("Adding federated metadata to collaboration group for document {}",documentId);

        FederatedCollaborationGroupMetadata federatedCollaborationGroupMetadata;
        try {
            federatedCollaborationGroupMetadata = JsonSerializationUtility.getObjectMapper().readValue(body,FederatedCollaborationGroupMetadata.class);
        } catch (Exception e) {
            logger.error("Failed to deserialize federatedCollaborationGroupMetadata",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        String processInstanceId = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId).getProcessInstanceID();
        CollaborationGroupDAO collaborationGroupDAO = CollaborationGroupDAOUtility.getCollaborationGroupDAO(partyId,federationId,processInstanceId);
        if(collaborationGroupDAO.getFederatedCollaborationGroupMetadatas() == null){
            List<FederatedCollaborationGroupMetadataDAO> federatedCollaborationGroupMetadataDAOS = new ArrayList<>();
            federatedCollaborationGroupMetadataDAOS.add(HibernateSwaggerObjectMapper.createFederatedCollaborationGroupMetadata(federatedCollaborationGroupMetadata));
            collaborationGroupDAO.setFederatedCollaborationGroupMetadatas(federatedCollaborationGroupMetadataDAOS);
        }
        else {
            collaborationGroupDAO.getFederatedCollaborationGroupMetadatas().add(HibernateSwaggerObjectMapper.createFederatedCollaborationGroupMetadata(federatedCollaborationGroupMetadata));
        }
        new JPARepositoryFactory().forBpRepository().updateEntity(collaborationGroupDAO);
        logger.info("Added federated metadata to collaboration group for document {}",documentId);
        return ResponseEntity.ok(null);
    }


    @ApiOperation(value = "",notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token")
    })
    @RequestMapping(value = "/collaboration-groups/unmerge",
            method = RequestMethod.GET)
    public ResponseEntity unMergeCollaborationGroup(@ApiParam(value = "The identifier of party", required = true) @RequestParam(value = "id",required = true) String groupId,
                                                         @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.info("Unmerging collaboration group");

        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        CollaborationGroupDAO collaborationGroupDAO = CollaborationGroupDAOUtility.getCollaborationGroupDAO(Long.parseLong(groupId));

        if (collaborationGroupDAO == null) {
            logger.error("There does not exist a collaboration group for id: {}", groupId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        if(collaborationGroupDAO.getFederatedCollaborationGroupMetadatas() != null){
            collaborationGroupDAO.getFederatedCollaborationGroupMetadatas().clear();
        }
        new JPARepositoryFactory().forBpRepository().updateEntity(collaborationGroupDAO);

        logger.info("Unmerged collaboration group");
        return ResponseEntity.ok(null);
    }
    private boolean isCollaborationFinished(CollaborationGroupDAO collaborationGroup){
        boolean collaborationFinished = true;
        // check whether there is a CompletedTaskType for each process instance group,that is, collaboration is finished
        for(ProcessInstanceGroupDAO processInstanceGroup: collaborationGroup.getAssociatedProcessInstanceGroups()){
            if(!TrustPersistenceUtility.completedTaskExist(processInstanceGroup.getProcessInstanceIDs())){
                collaborationFinished = false;
                break;
            }
        }
        return collaborationFinished;
    }
}
