package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.model.hyperjaxb.*;
import eu.nimble.service.bp.swagger.api.ProcessInstanceGroupsApi;
import eu.nimble.service.bp.swagger.model.*;
import eu.nimble.service.bp.util.email.IEmailSenderUtil;
import eu.nimble.service.bp.util.persistence.bp.*;
import eu.nimble.service.bp.util.persistence.catalogue.TrustPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.validation.IValidationUtil;
import feign.Response;
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

import java.util.*;

/**
 * Created by suat on 06-Feb-18.
 */
@Controller
public class ProcessInstanceGroupController implements ProcessInstanceGroupsApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ProcessInstanceController processInstanceController;
    @Autowired
    private DocumentController documentController;
    @Autowired
    private IEmailSenderUtil emailSenderUtil;
    @Autowired
    private JPARepositoryFactory repoFactory;
    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @Override
    public ResponseEntity<ProcessInstanceGroupResponse> getCollaborationGroups(
            @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String authorization,
            @ApiParam(value = "Identifier of the party as specified by the identity service (party.id)") @RequestParam(value = "partyId", required = false) String partyId,
            @ApiParam(value = "Names of the products for which the collaboration activities are performed. If multiple product names to be provided, they should be comma-separated.") @RequestParam(value = "relatedProducts", required = false) List<String> relatedProducts,
            @ApiParam(value = "Categories of the products.<br>For example: MDF raw,Split air conditioner") @RequestParam(value = "relatedProductCategories", required = false) List<String> relatedProductCategories,
            @ApiParam(value = "Identifier (party id) of the corresponding trading partners") @RequestParam(value = "tradingPartnerIDs", required = false) List<String> tradingPartnerIDs,
            @ApiParam(value = "Offset of the first result among the complete result set satisfying the given criteria", defaultValue = "0") @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            @ApiParam(value = "Number of results to be included in the result set", defaultValue = "10") @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @ApiParam(value = "Whether the process instance group is archived or not", defaultValue = "false") @RequestParam(value = "archived", required = false, defaultValue = "false") Boolean archived,
            @ApiParam(value = "Status of the process instance included in the group.<br>Possible values:<ul><li>INPROGRESS</li><li>WAITING</li><li>CANCELLED</li><li>COMPLETED</li></ul>") @RequestParam(value = "status", required = false) List<String> status,
            @ApiParam(value = "Role of the party in the collaboration.<br>Possible values:<ul><li>SELLER</li><li>BUYER</li></ul>") @RequestParam(value = "collaborationRole", required = false) String collaborationRole,
            @ApiParam(value = "Indicator whether the process instance groups are belong to a project type of collaborat'on group", defaultValue = "false") @RequestParam(value = "isProject", required = false, defaultValue = "false") Boolean isProject,
            @ApiParam(value = ""  ) @RequestHeader(value="federationId", required=false) String federationId
            ) {

        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to get process instance groups for party: %s", partyId);
        executionContext.setRequestLog(requestLog);

        logger.debug(requestLog);
        try {
            // validate role
            if(!validationUtil.validateRole(authorization, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            if(partyId != null && federationId == null){
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_MISSING_PARTY_PARAMETERS.toString());
            }

            ProcessInstanceGroupResponse result = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupResponses(partyId,federationId, collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories, status, limit, offset, isProject);
            int totalCount = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupCount(partyId, federationId,collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories, status, isProject);
            result.setSize(totalCount);

            ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(result);
            logger.debug("Completed request to get process instance groups for party: {}", partyId);
            return response;

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_PROCESS_INSTANCE_GROUPS.toString(),Arrays.asList(partyId),e);
        }
    }

    @Override
    @ApiOperation(value = "", notes = "Deletes the process instance group")
    public ResponseEntity<Void> deleteProcessInstanceGroup(@ApiParam(value = "Identifier of the ProcessInstanceGroup to be deleted (processInstanceGroup.id)", required = true) @PathVariable("id") String id,
                                                           @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) throws NimbleException {
        // set request log of ExecutionContext
        String requestLog = String.format("Deleting ProcessInstanceGroup ID: %s", id);
        executionContext.setRequestLog(requestLog);

        logger.debug(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        ProcessInstanceGroupDAO processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(id);
        if(processInstanceGroupDAO == null){
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_INSTANCE_GROUP.toString(), Arrays.asList(id));
        }

        ProcessInstanceGroupDAOUtility.deleteProcessInstanceGroupDAOByID(id);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Deleted ProcessInstanceGroups: {}", id);
        return response;
    }

    @Override
    @ApiOperation(value = "", notes = "Retrieves the process instance group specified with the ID")
    public ResponseEntity<ProcessInstanceGroup> getProcessInstanceGroup(@ApiParam(value = "Identifier of the ProcessInstanceGroup to be received (processInstanceGroup.id)", required = true) @PathVariable("id") String id,
                                                                        @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Getting ProcessInstanceGroup: %s", id);
        executionContext.setRequestLog(requestLog);

        logger.debug(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        ProcessInstanceGroupDAO processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(id);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.convertProcessInstanceGroupDAO(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Retrieved ProcessInstanceGroup: {}", id);
        return response;
    }

    @Override
    @ApiOperation(value = "", notes = "Generates detailed filtering criteria for the current query parameters in place." +
            " The filter can be used to restrict the set of groups for easy discovery.")
    public ResponseEntity<ProcessInstanceGroupFilter> getProcessInstanceGroupFilters(
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
            @ApiParam(value = "Identifier of the party as specified by the identity service", required = true) @RequestParam(value = "partyId", required = true) String partyId,
            @ApiParam(value = "Names of the products for which the collaboration activities are performed") @RequestParam(value = "relatedProducts", required = false) List<String> relatedProducts,
            @ApiParam(value = "Categories of the products.<br>For example:MDF raw,Split air conditioner") @RequestParam(value = "relatedProductCategories", required = false) List<String> relatedProductCategories,
            @ApiParam(value = "Identifier (party id) of the corresponding trading partners") @RequestParam(value = "tradingPartnerIDs", required = false) List<String> tradingPartnerIDs,
            @ApiParam(value = "Whether the collaboration group is archived or not", defaultValue = "false") @RequestParam(value = "archived", required = false, defaultValue = "false") Boolean archived,
            @ApiParam(value = "Role of the party in the collaboration.<br>Possible values: <ul><li>SELLER</li><li>BUYER</li></ul>") @RequestParam(value = "collaborationRole", required = false) String collaborationRole,
            @ApiParam(value = "Status of the process instance included in the group.<br>Possible values: <ul><li>STARTED</li><li>WAITING</li><li>CANCELLED</li><li>COMPLETED</li></ul>") @RequestParam(value = "status", required = false) List<String> status,
            @ApiParam(value = "", required = true) @RequestHeader(value = "federationId", required = true) String federationId,
            @ApiParam(value = "Identify Project Or Not", defaultValue = "false") @RequestParam(value = "isProject", required = false, defaultValue = "false") Boolean isProject) {

        // set request log of ExecutionContext
        String requestLog = String.format("Retrieving filters for partyId: %s, archived: %s, products: %s, categories: %s, parties: %s", partyId, archived,
                relatedProducts != null ? relatedProducts.toString() : "[]",
                relatedProductCategories != null ? relatedProductCategories.toString() : "[]",
                tradingPartnerIDs != null ? tradingPartnerIDs.toString() : "[]");
        executionContext.setRequestLog(requestLog);

        // validate role
        if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        logger.debug(requestLog);
        ProcessInstanceGroupFilter filters = null;
        try{
            filters = ProcessInstanceGroupDAOUtility.getFilterDetails(partyId, federationId,collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories, status, null, null, isProject, bearerToken);
        }
        catch (Exception e){
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_PROCESS_INSTANCE_GROUP_FILTERS.toString(),e);
        }
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(filters);
        logger.debug("Filters retrieved for partyId: {}, archived: {}, products: {}, categories: {}, parties: {}", partyId, archived,
                relatedProducts != null ? relatedProducts.toString() : "[]",
                relatedProductCategories != null ? relatedProductCategories.toString() : "[]",
                tradingPartnerIDs != null ? tradingPartnerIDs.toString() : "[]");
        return response;
    }

    @Override
    @ApiOperation(value = "", notes = "Gets the order document included in a business process group. If order response id is provided, it simply returns the corresponding order")
    public ResponseEntity<Void> getOrderDocument(@ApiParam(value = "Identifier of a process instance included in the group", required = false) @RequestParam(value = "processInstanceId", required = false) String processInstanceId,
                                                 @ApiParam(value = "Identifier of the order response", required = false) @RequestParam(value = "orderResponseId", required = false) String orderResponseId,
                                                @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to get order document for process instance id: %s",processInstanceId);
            executionContext.setRequestLog(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            String orderJson = null;

            if(orderResponseId != null) {
                ProcessDocumentMetadataDAO orderMetadata = ProcessDocumentMetadataDAOUtility.getMetadataForCorrespondingDocument(orderResponseId);
                if(orderMetadata == null){
                    throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_METADATA_FOR_ORDER_RESPONSE.toString(),Arrays.asList(orderResponseId));
                }
                orderJson = (String) documentController.getDocumentJsonContent(orderMetadata.getDocumentID(),bearerToken).getBody();
            }
            else{
                // check whether the process instance id exists
                ProcessInstanceDAO pi = ProcessInstanceDAOUtility.getById(processInstanceId);
                if (pi == null) {
                    throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_ID.toString(),Arrays.asList(processInstanceId));
                }

                String sourceOrderResponseId = ProcessInstanceGroupDAOUtility.getSourceOrderResponseIdForTransportRelatedProcess(processInstanceId);
                // get the preceding process instance group
                FederatedCollaborationGroupMetadataDAO federatedCollaborationGroupMetadataDAO = ProcessInstanceGroupDAOUtility.getPrecedingProcessInstanceGroup(processInstanceId);
                if (federatedCollaborationGroupMetadataDAO != null) {
                    // preceding group is in this instance
                    if(federatedCollaborationGroupMetadataDAO.getFederationID().contentEquals(SpringBridge.getInstance().getFederationId())){
                        ResponseEntity responseEntity = getOrderDocument(processInstanceId,sourceOrderResponseId,bearerToken);
                        if(responseEntity.getStatusCodeValue() == 200){
                            orderJson = (String) responseEntity.getBody();
                        }
                    }
                    // preceding group is in a different instance
                    else{
                        Response response = SpringBridge.getInstance().getDelegateClient().getOrderDocument(bearerToken,processInstanceId,sourceOrderResponseId,federatedCollaborationGroupMetadataDAO.getFederationID());
                        if(response.status() == 200){
                            orderJson = HttpResponseUtil.extractBodyFromFeignClientResponse(response);
                        }
                    }
                }
            }

            // get the order content
            ResponseEntity response;
            if (orderJson != null) {
                response = ResponseEntity.status(HttpStatus.OK).body(orderJson);

            } else {
                response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return response;

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_ORDER_DOCUMENT.toString(),Arrays.asList(processInstanceId),e);
        }
    }

    @ApiOperation(value = "", notes = "Finishes the collaboration (negotiation) which is represented by the specified ProcessInstanceGroup")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Finished the collaboration for the given group id successfully."),
            @ApiResponse(code = 400, message = "The specified group is already finished."),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist a process instance group with the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while finishing the collaboration")
    })
    @RequestMapping(value = "/process-instance-groups/{id}/finish",
            method = RequestMethod.POST)
    public ResponseEntity finishCollaboration(@ApiParam(value = "Identifier of the process instance group to be finished", required = true) @PathVariable(value = "id", required = true) String id,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Finishing the collaboration for the group id: %s", id);
            executionContext.setRequestLog(requestLog);

            logger.debug(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            ProcessInstanceGroupDAO groupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(id);
            if (groupDAO == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_INSTANCE_GROUP.toString(),Arrays.asList(id));
            }

            // if there's a completed task for these processes, we could not finish that group
            List<String> processInstanceIDs = groupDAO.getProcessInstanceIDs();
            if(TrustPersistenceUtility.completedTaskExist(processInstanceIDs)){
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_ALREADY_FINISHED.toString(),Arrays.asList(id));
            }

            // create completed tasks for both parties
            String processInstanceID = groupDAO.getProcessInstanceIDs().get(groupDAO.getProcessInstanceIDs().size() - 1);
            TrustPersistenceUtility.createCompletedTasksForBothParties(processInstanceID, bearerToken, "Completed",null);

            // update ProcessInstanceGroup status
            GenericJPARepository bpRepo = new JPARepositoryFactory().forBpRepository(true);
            List<ProcessInstanceGroupDAO> processInstanceGroupDAOS = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAOs(processInstanceID, bpRepo);
            for(ProcessInstanceGroupDAO processInstanceGroupDAO: processInstanceGroupDAOS){
                processInstanceGroupDAO.setStatus(GroupStatus.COMPLETED);
                bpRepo.updateEntity(processInstanceGroupDAO);
            }

            // send email to the trading partner
            emailSenderUtil.sendCollaborationStatusEmail(bearerToken, groupDAO);

            logger.debug("Finished the collaboration for the group id: {} successfully", id);
            return ResponseEntity.ok(null);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FINISH_COLLABORATION.toString(),Arrays.asList(id),e);
        }
    }

    @ApiOperation(value = "", notes = "Cancels the collaboration (negotiation) which is represented by the specified ProcessInstanceGroup")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Cancelled the collaboration for the given group id successfully."),
            @ApiResponse(code = 400, message = "The specified group cannot be cancelled"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist a process instance group with the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while cancelling the collaboration")
    })
    @RequestMapping(value = "/process-instance-groups/{id}/cancel",
            method = RequestMethod.POST)
    public ResponseEntity cancelCollaboration(@ApiParam(value = "Identifier of the process instance group to be cancelled", required = true) @PathVariable(value = "id", required = true) String id,
                                              @ApiParam(value = "The cancellation reason", required = false, defaultValue = "") @RequestBody(required = false) String cancellationReason,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws NimbleException {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Cancelling the collaboration for the group id: %s", id);
            executionContext.setRequestLog(requestLog);

            logger.debug(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            ProcessInstanceGroupDAO groupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(id);
            if (groupDAO == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_INSTANCE_GROUP.toString(),Arrays.asList(id));
            }

            // check whether the group is already cancelled or not
            if(groupDAO.getStatus().equals(GroupStatus.CANCELLED)){
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_ALREADY_CANCELLED.toString(),Arrays.asList(id));
            }

            if (groupDAO.getStatus().equals(GroupStatus.COMPLETED)) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_ALREADY_COMPLETED.toString(),Arrays.asList(id));
            }

            // update the group of the party initiating the cancel request
            GenericJPARepository repo = repoFactory.forBpRepository(true);
            groupDAO.setStatus(GroupStatus.CANCELLED);
            groupDAO = repo.updateEntity(groupDAO);

            // cancel processes in the group
            for (String processID : groupDAO.getProcessInstanceIDs()) {
                ProcessInstanceDAO instanceDAO = ProcessInstanceDAOUtility.getById(processID);
                // if process is completed or already cancelled, continue
                if (instanceDAO.getStatus() == ProcessInstanceStatus.COMPLETED || instanceDAO.getStatus() == ProcessInstanceStatus.CANCELLED) {
                    instanceDAO.setStatus(ProcessInstanceStatus.CANCELLED);
                    repo.updateEntity(instanceDAO);
                    continue;
                }
                // otherwise, cancel the process
                processInstanceController.cancelProcessInstance(processID, bearerToken);
            }

            // update the groups associated with the first group
            List<ProcessInstanceGroupDAO> associatedProcessInstanceGroups = ProcessInstanceGroupDAOUtility.getAssociatedProcessInstanceGroupDAOs(groupDAO.getPartyID(),groupDAO.getFederationID(),groupDAO.getProcessInstanceIDs());
            for (ProcessInstanceGroupDAO group : associatedProcessInstanceGroups) {
                // check whether the associated group can be cancelled or not
                boolean isCancellableGroup = group.getStatus().equals(GroupStatus.INPROGRESS);
                // if it is ok, change status of the associated group
                if (isCancellableGroup) {
                    group.setStatus(GroupStatus.CANCELLED);
                    repo.updateEntity(group);
                }
            }

            // create completed tasks for both parties
            String processInstanceID = groupDAO.getProcessInstanceIDs().get(groupDAO.getProcessInstanceIDs().size() - 1);
            TrustPersistenceUtility.createCompletedTasksForBothParties(processInstanceID, bearerToken, "Cancelled",cancellationReason);

            // send email to the trading partner
            emailSenderUtil.sendCollaborationStatusEmail(bearerToken, groupDAO);

            logger.debug("Cancelled the collaboration for the group id: {} successfully", id);
            return ResponseEntity.ok(null);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CANCEL_COLLABORATION.toString(),Arrays.asList(id),e);
        }
    }

    @ApiOperation(value = "",notes = "Checks whether collaboration which is represented by the specified ProcessInstanceGroup is finished/completed or not.")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist a process instance group with the given id")
    })
    @RequestMapping(value = "/process-instance-groups/{id}/finished",
            method = RequestMethod.GET)
    public ResponseEntity<String> checkCollaborationFinished(@ApiParam(value = "The identifier of the process instance group to be checked",required = true) @PathVariable("id") String id,
                                                             @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) throws NimbleException {
        // set request log of ExecutionContext
        String requestLog = String.format("Checking whether the collaboration represented by process instance group %s is finished",id);
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);

        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        // get the collaboration group
        ProcessInstanceGroupDAO groupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(id);
        if (groupDAO == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_INSTANCE_GROUP.toString(),Arrays.asList(id));
        }

        // whether the collaboration is finished or not
        Boolean collaborationFinished = TrustPersistenceUtility.completedTaskExist(groupDAO.getProcessInstanceIDs());

        logger.info("The collaboration represented by process instance group {} finished {}",id,collaborationFinished);
        return ResponseEntity.ok(collaborationFinished.toString());
    }

    @ApiOperation(value = "", notes = "Retrieves the process instances belonging to the specified ProcessInstanceGroup")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the process instances for the given group id successfully.", response = ProcessInstance.class,responseContainer = "List"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist a process instance group with the given id")
    })
    @RequestMapping(value = "/process-instance-groups/{id}/process-instances",
            method = RequestMethod.GET)
    public ResponseEntity getProcessInstancesIncludedInTheGroup(@ApiParam(value = "Identifier of the process instance group to be checked", required = true) @PathVariable(value = "id", required = true) String id,
                                                                @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws NimbleException {
        // set request log of ExecutionContext
        String requestLog = String.format("Retrieving process instances for the group id: %s", id);
        executionContext.setRequestLog(requestLog);

        logger.debug(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        ProcessInstanceGroupDAO groupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(id);
        if (groupDAO == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_INSTANCE_GROUP.toString(),Arrays.asList(id));
        }

        // get ProcessInstanceDAOs
        List<ProcessInstanceDAO> processInstanceDAOS = ProcessInstanceDAOUtility.getProcessInstancesIncludedInTheGroup(id);
        // convert each ProcessInstanceDAO to ProcessInstance
        List<ProcessInstance> processInstances = HibernateSwaggerObjectMapper.createProcessInstances(processInstanceDAOS);
        logger.debug("Retrieving process instances for the group id: {} successfully", id);
        return ResponseEntity.ok(processInstances);
    }
}
