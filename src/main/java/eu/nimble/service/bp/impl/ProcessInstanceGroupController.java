package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.persistence.bp.ProcessInstanceGroupDAORepository;
import eu.nimble.service.bp.impl.persistence.util.DAOUtility;
import eu.nimble.service.bp.impl.persistence.util.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.persistence.util.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.bp.impl.persistence.util.TrustUtility;
import eu.nimble.service.bp.impl.util.controller.HttpResponseUtil;
import eu.nimble.service.bp.impl.util.email.EmailSenderUtil;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.bp.swagger.api.GroupApi;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupFilter;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupResponse;
import eu.nimble.service.model.ubl.order.OrderType;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 06-Feb-18.
 */
@Controller
public class ProcessInstanceGroupController implements GroupApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ProcessInstanceGroupDAOUtility groupDaoUtility;
    @Autowired
    private ProcessInstanceController processInstanceController;
    @Autowired
    private DocumentController documentController;
    @Autowired
    private EmailSenderUtil emailSenderUtil;
    @Autowired
    private ProcessInstanceGroupDAORepository processInstanceGroupDAORepository;

    @Override
    @ApiOperation(value = "",notes = "Add a new process instance to the specified")
    public ResponseEntity<ProcessInstanceGroup> addProcessInstanceToGroup(
            @ApiParam(value = "Identifier of the process instance group to which a new process instance id is added", required = true) @PathVariable("ID") String ID,
            @ApiParam(value = "Identifier of the process instance to be added", required = true) @RequestParam(value = "processInstanceID", required = true) String processInstanceID) {
        logger.debug("Adding process instance: {} to ProcessInstanceGroup: {}", ID);
        ProcessInstanceGroupDAO processInstanceGroupDAO = null;
        try {
            processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(ID);
            processInstanceGroupDAO.getProcessInstanceIDs().add(processInstanceID);
        }
        catch (Exception e){
            logger.error("Failed to get process instance group with the given id : {}",ID,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

//        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);
        processInstanceGroupDAORepository.save(processInstanceGroupDAO);

        // retrieve the group DAO again to populate the first/last activity times
        processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(ID);
        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.convertProcessInstanceGroupDAO(processInstanceGroupDAO);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Added process instance: {} to ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Delete the process instance from the process instance group")
    public ResponseEntity<ProcessInstanceGroup> deleteProcessInstanceFromGroup(@ApiParam(value = "Identifier of the process instance group from which the process instance id is deleted", required = true) @PathVariable("ID") String ID, @ApiParam(value = "Identifier of the process instance to be deleted", required = true) @RequestParam(value = "processInstanceID", required = true) String processInstanceID) {
        logger.debug("Deleting process instance: {} from ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(ID);
        processInstanceGroupDAO.getProcessInstanceIDs().remove(processInstanceID);
//        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);
        processInstanceGroupDAORepository.save(processInstanceGroupDAO);

        processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(ID);
        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.convertProcessInstanceGroupDAO(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Deleted process instance: {} from ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Retrieve the process instances for the given process group")
    public ResponseEntity<List<ProcessInstance>> getProcessInstancesOfGroup(@ApiParam(value = "Identifier of the process instance group for which the associated process instances will be retrieved", required = true) @PathVariable("ID") String ID) {
        logger.debug("Getting ProcessInstances for group: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(ID);
        List<ProcessInstance> processInstances = HibernateSwaggerObjectMapper.createProcessInstances(ProcessInstanceGroupDAOUtility.getProcessInstances(processInstanceGroupDAO.getProcessInstanceIDs()));
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstances);
        logger.debug("Retrieved ProcessInstances for group: {}", ID);
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Delete the process instance group along with the included process instances")
    public ResponseEntity<Void> deleteProcessInstanceGroup(@ApiParam(value = "", required = true) @PathVariable("ID") String ID) {
        logger.debug("Deleting ProcessInstanceGroup ID: {}", ID);

        ProcessInstanceGroupDAOUtility.deleteProcessInstanceGroupDAOByID(ID);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Deleted ProcessInstanceGroups: {}", ID);
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Retrieve the process instance group specified with the ID")
    public ResponseEntity<ProcessInstanceGroup> getProcessInstanceGroup(@ApiParam(value = "", required = true) @PathVariable("ID") String ID) {
        logger.debug("Getting ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(ID);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.convertProcessInstanceGroupDAO(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Retrieved ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Retrieve process instance groups for the specified party. If no partyID is specified, then all groups are returned")
    public ResponseEntity<ProcessInstanceGroupResponse> getProcessInstanceGroups(@ApiParam(value = "Identifier of the party") @RequestParam(value = "partyID", required = false) String partyID,
                                                                                 @ApiParam(value = "Related products") @RequestParam(value = "relatedProducts", required = false) List<String> relatedProducts,
                                                                                 @ApiParam(value = "Related product categories") @RequestParam(value = "relatedProductCategories", required = false) List<String> relatedProductCategories,
                                                                                 @ApiParam(value = "Identifier of the corresponsing trading partner ID") @RequestParam(value = "tradingPartnerIDs", required = false) List<String> tradingPartnerIDs,
                                                                                 @ApiParam(value = "Initiation date range for the first process instance in the group") @RequestParam(value = "initiationDateRange", required = false) String initiationDateRange,
                                                                                 @ApiParam(value = "Last activity date range. It is the latest submission date of the document to last process instance in the group") @RequestParam(value = "lastActivityDateRange", required = false) String lastActivityDateRange,
                                                                                 @ApiParam(value = "Offset of the first result among the complete result set satisfying the given criteria", defaultValue = "0") @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
                                                                                 @ApiParam(value = "Number of results to be included in the result set", defaultValue = "10") @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
                                                                                 @ApiParam(value = "", defaultValue = "false") @RequestParam(value = "archived", required = false, defaultValue = "false") Boolean archived,
                                                                                 @ApiParam(value = "status") @RequestParam(value = "status",required = false) List<String> status,
                                                                                 @ApiParam(value = "") @RequestParam(value = "collaborationRole", required = false) String collaborationRole) {
        logger.debug("Getting ProcessInstanceGroups for party: {}", partyID);

        List<ProcessInstanceGroupDAO> results = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAOs(partyID, collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories, status,null, null, limit, offset);
        int totalSize = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupSize(partyID, collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories, status,null, null);
        logger.debug(" There are {} process instance groups in total", results.size());
        List<ProcessInstanceGroup> processInstanceGroups = new ArrayList<>();
        for (ProcessInstanceGroupDAO result : results) {
            processInstanceGroups.add(HibernateSwaggerObjectMapper.convertProcessInstanceGroupDAO(result));
        }

        ProcessInstanceGroupResponse groupResponse = new ProcessInstanceGroupResponse();
        groupResponse.setProcessInstanceGroups(processInstanceGroups);
        groupResponse.setSize(totalSize);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(groupResponse);
        logger.debug("Retrieved ProcessInstanceGroups for party: {}", partyID);
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Generate detailed filtering criteria for the current query parameters in place")
    public ResponseEntity<ProcessInstanceGroupFilter> getProcessInstanceGroupFilters(
            @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
            @ApiParam(value = "Identifier of the party") @RequestParam(value = "partyID", required = false) String partyID,
            @ApiParam(value = "Related products") @RequestParam(value = "relatedProducts", required = false) List<String> relatedProducts,
            @ApiParam(value = "Related product categories") @RequestParam(value = "relatedProductCategories", required = false) List<String> relatedProductCategories,
            @ApiParam(value = "Identifier of the corresponding trading partner ID") @RequestParam(value = "tradingPartnerIDs", required = false) List<String> tradingPartnerIDs,
            @ApiParam(value = "Initiation date range for the first process instance in the group") @RequestParam(value = "initiationDateRange", required = false) String initiationDateRange,
            @ApiParam(value = "Last activity date range. It is the latest submission date of the document to last process instance in the group") @RequestParam(value = "lastActivityDateRange", required = false) String lastActivityDateRange,
            @ApiParam(value = "", defaultValue = "false") @RequestParam(value = "archived", required = false, defaultValue = "false") Boolean archived,
            @ApiParam(value = "") @RequestParam(value = "collaborationRole", required = false) String collaborationRole,
            @ApiParam(value = "Status") @RequestParam(value = "status",required = false) List<String> status) {

        ProcessInstanceGroupFilter filters = groupDaoUtility.getFilterDetails(partyID, collaborationRole, archived, tradingPartnerIDs, relatedProducts, relatedProductCategories,status,null, null, bearerToken);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(filters);
        logger.debug("Filters retrieved for partyId: {}, archived: {}, products: {}, categories: {}, parties: {}", partyID, archived,
                relatedProducts != null ? relatedProducts.toString() : "[]",
                relatedProductCategories != null ? relatedProductCategories.toString() : "[]",
                tradingPartnerIDs != null ? tradingPartnerIDs.toString() : "[]");
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Archive the process instance group specified with ID")
    public ResponseEntity<ProcessInstanceGroup> archiveGroup(@ApiParam(value = "Identifier of the process instance group to be archived", required = true) @PathVariable("ID") String ID) {
        logger.debug("Archiving ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(ID);
        processInstanceGroupDAO.setArchived(true);

//        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);
        processInstanceGroupDAORepository.save(processInstanceGroupDAO);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.convertProcessInstanceGroupDAO(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Archived ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Restore the archived process instance group specified with ID")
    public ResponseEntity<ProcessInstanceGroup> restoreGroup(@ApiParam(value = "Identifier of the process instance group to be restored", required = true) @PathVariable("ID") String ID) {
        logger.debug("Restoring ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(ID);
        processInstanceGroupDAO.setArchived(false);

//        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);
        processInstanceGroupDAORepository.save(processInstanceGroupDAO);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.convertProcessInstanceGroupDAO(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Restored ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Save a process group along with the initial process instance")
    public ResponseEntity<Void> saveProcessInstanceGroup(@ApiParam(value = "The content of the process instance group to be saved", required = true) @RequestBody ProcessInstanceGroup processInstanceGroup) {
        logger.debug("Saving ProcessInstanceGroup {}", processInstanceGroup.toString());
        ProcessInstanceGroupDAO processInstanceGroupDAO = HibernateSwaggerObjectMapper.createProcessInstanceGroup_DAO(processInstanceGroup);
//        HibernateUtilityRef.getInstance("bp-data-model").persist(processInstanceGroupDAO);
        processInstanceGroupDAORepository.save(processInstanceGroupDAO);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Saved ProcessInstanceGroup {}", processInstanceGroup.toString());
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Archive all the groups of the given party")
    public ResponseEntity<Void> archiveAllGroups(@ApiParam(value = "Identifier of the party of which groups will be archived", required = true) @RequestParam(value = "partyID", required = true) String partyID) {
        logger.debug("Archiving ProcessInstanceGroups for party {}", partyID);

        ProcessInstanceGroupDAOUtility.archiveAllGroupsForParty(partyID);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Archived ProcessInstanceGroup for party {}", partyID);
        return response;
    }

    @Override
    @ApiOperation(value = "",notes = "Delete all the archived groups of the given party")
    public ResponseEntity<Void> deleteAllArchivedGroups(@ApiParam(value = "Identifier of the party of which groups will be deleted", required = true) @RequestParam(value = "partyID", required = true) String partyID) {
        logger.debug("Deleting archived ProcessInstanceGroups for party {}", partyID);

        ProcessInstanceGroupDAOUtility.deleteArchivedGroupsForParty(partyID);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("true");
        logger.debug("Deleted archived ProcessInstanceGroups for party {}", partyID);
        return response;
    }

    @Override
    @ApiOperation(value = "", notes = "Get the order content in a business process group given a process id included in the group")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the order content", response = OrderType.class),
            @ApiResponse(code = 404, message = "No order exists")
    })
    public ResponseEntity<Void> getOrderProcess(@ApiParam(value = "Identifier of a process instance included in the group", required = true) @RequestParam(value = "processInstanceId", required = true) String processInstanceId,
                                                @ApiParam(value = "", required = true) @RequestHeader(value = "Authorization", required = true) String authorization) {
        try {
            // check whether the process instance id exists
            ProcessInstanceDAO pi = DAOUtility.getProcessInstanceDAOByID(processInstanceId);
            if (pi == null) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("No process ID exists for the process id: %s", processInstanceId), null, HttpStatus.NOT_FOUND, LogLevel.INFO);
            }
            // get the order
            String orderId = ProcessInstanceGroupDAOUtility.getOrderIdInGroup(processInstanceId);

            // get the order content
            ResponseEntity response;
            if (orderId != null) {
                String orderJson = (String) documentController.getDocumentJsonContent(orderId).getBody();
                response = ResponseEntity.status(HttpStatus.OK).body(orderJson);

            } else {
                response = ResponseEntity.status(HttpStatus.OK).body(null);
            }
            return response;

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the order content for process id: %s", processInstanceId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "",notes = "Cancel the collaboration (negotiation) for the given group id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Cancelled the collaboration for the given group id successfully "),
            @ApiResponse(code = 400, message = "There does not exist a process instance group with the given id"),
            @ApiResponse(code = 500, message = "Failed to cancel collaboration")
    })
    @RequestMapping(value = "/group/{ID}/cancel",
            method = RequestMethod.POST)
    public ResponseEntity cancelCollaboration(@ApiParam(value="Identifier of the process instance group to be cancelled") @PathVariable(value = "ID", required = true) String ID,
                                              @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        try {
            logger.debug("Cancelling the collaboration for the group id: {}", ID);
            ProcessInstanceGroupDAO groupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(ID);
            if (groupDAO == null) {
                String msg = String.format("There does not exist a process instance group with the id: %s", ID);
                logger.warn(msg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
            }

            // check whether the group consists of an approved order or an accepted transport execution plan
            List<String> processInstanceIDs = groupDAO.getProcessInstanceIDs();
            boolean isCancellableGroup = cancellableGroup(processInstanceIDs);
            if(!isCancellableGroup){
                logger.error("Process instance group with id:{} can not be cancelled",ID);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            // update the group of the party initiating the cancel request
            groupDAO.setStatus(GroupStatus.CANCELLED);
//            GenericJPARepositoryImpl.getInstance("bp-data-model").update(groupDAO);
            groupDAO = SpringBridge.getInstance().getProcessInstanceGroupDAORepository().save(groupDAO);

            // cancel processes in the group
            for(String processID : groupDAO.getProcessInstanceIDs()){
                ProcessInstanceDAO instanceDAO = DAOUtility.getProcessInstanceDAOByID(processID);
                // if process is completed or already cancelled, continue
                if(instanceDAO.getStatus() == ProcessInstanceStatus.COMPLETED || instanceDAO.getStatus() == ProcessInstanceStatus.CANCELLED){
                    instanceDAO.setStatus(ProcessInstanceStatus.CANCELLED);
//                    GenericJPARepositoryImpl.getInstance("bp-data-model").update(instanceDAO);
                    SpringBridge.getInstance().getProcessInstanceDAORepository().save(instanceDAO);
                    continue;
                }
                // otherwise, cancel the process
                processInstanceController.cancelProcessInstance(processID,bearerToken);
            }

            // update the groups associated with the first group
            for (String id : groupDAO.getAssociatedGroups()) {
                ProcessInstanceGroupDAO group = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(id);
                // check whether the associated group can be cancelled or not
                isCancellableGroup = cancellableGroup(group.getProcessInstanceIDs());
                // if it is ok, change status of the associated group
                if(isCancellableGroup){
                    group.setStatus(GroupStatus.CANCELLED);
                    //GenericJPARepositoryImpl.getInstance("bp-data-model").update(group);
                    group = SpringBridge.getInstance().getProcessInstanceGroupDAORepository().save(group);
                }
            }

            // create completed tasks for both parties
            String processInstanceID = groupDAO.getProcessInstanceIDs().get(groupDAO.getProcessInstanceIDs().size() - 1);
            TrustUtility.createCompletedTasksForBothParties(processInstanceID, bearerToken, "Cancelled");

            // send email to the trading partner
            emailSenderUtil.sendCancellationEmail(bearerToken, groupDAO);

            logger.debug("Cancelled the collaboration for the group id: {} successfully", ID);
            return ResponseEntity.ok(null);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while cancelling the group: %s", ID), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean cancellableGroup(List<String> processInstanceIDs){
        for(String instanceID: processInstanceIDs){
            List<ProcessDocumentMetadataDAO> metadataDAOS = DAOUtility.getProcessDocumentMetadataByProcessInstanceID(instanceID);
            for (ProcessDocumentMetadataDAO metadataDAO: metadataDAOS){
                if(metadataDAO.getType() == DocumentType.ORDERRESPONSESIMPLE && metadataDAO.getStatus() == ProcessDocumentStatus.APPROVED || metadataDAO.getType() == DocumentType.TRANSPORTEXECUTIONPLAN && metadataDAO.getStatus() == ProcessDocumentStatus.APPROVED){
                    return false;
                }
            }
        }
        return true;
    }
}
