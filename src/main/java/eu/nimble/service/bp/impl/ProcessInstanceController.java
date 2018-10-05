package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.persistence.*;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.utility.HibernateUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Created by dogukan on 09.08.2018.
 */

@Controller
public class ProcessInstanceController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ApiOperation(value = "",notes = "Cancel the process instance with the given id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Cancelled the process instance successfully"),
            @ApiResponse(code = 400, message = "There does not exist a process instance with the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while cancelling the process instance with the given id")
    })
    @RequestMapping(value = "/processInstance",
            method = RequestMethod.DELETE)
    public ResponseEntity cancelProcessInstance(@RequestParam(value = "processInstanceId", required = true) String processInstanceId,
                                                @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.debug("Cancelling process instance with id: {}",processInstanceId);

        try {
            ProcessInstanceDAO instanceDAO = DAOUtility.getProcessInstanceDAOByID(processInstanceId);
            // check whether the process instance with the given id exists or not
            if(instanceDAO == null){
                logger.error("There does not exist a process instance with id:{}",processInstanceId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("There does not exist a process instance with the given id");
            }
            // cancel the process
            CamundaEngine.cancelProcessInstance(processInstanceId);
            // change status of the process
            instanceDAO.setStatus(ProcessInstanceStatus.CANCELLED);
            HibernateUtilityRef.getInstance("bp-data-model").update(instanceDAO);
        }
        catch (Exception e) {
            logger.error("Failed to cancel the process instance with id:{}",processInstanceId,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel the process instance with the given id");
        }

        logger.debug("Cancelled process instance with id: {}",processInstanceId);
        return ResponseEntity.ok(null);
    }

    @ApiOperation(value = "",notes = "Update the process instance with the given id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated the process instance successfully"),
            @ApiResponse(code = 400, message = "There does not exist a process instance with the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while updating the process instance with the given id")
    })
    @RequestMapping(value = "/processInstance",
            method = RequestMethod.PUT)
    public ResponseEntity updateProcessInstance(@RequestBody String content,
                                                @ApiParam(value = "Type of the process instance document to be updated") @RequestParam(value = "processID") DocumentType processID,
                                                @RequestParam(value = "processInstanceID") String processInstanceID,
                                                @ApiParam(value = "Id of the user who updated the process instance") @RequestParam(value = "creatorUserID") String creatorUserID) {

        logger.debug("Updating process instance with id: {}",processInstanceID);

        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);

        try {
            ProcessInstanceDAO instanceDAO = DAOUtility.getProcessInstanceDAOByID(processInstanceID);
            // check whether the process instance with the given id exists or not
            if(instanceDAO == null){
                logger.error("There does not exist a process instance with id:{}",processInstanceID);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("There does not exist a process instance with the given id");
            }
            // update creator user id of metadata
            ProcessDocumentMetadata processDocumentMetadata = DocumentDAOUtility.getRequestMetadata(processInstanceID);
            processDocumentMetadata.setCreatorUserID(creatorUserID);
            DocumentDAOUtility.updateDocumentMetadata(businessProcessContext.getId(),processDocumentMetadata);
            // update the corresponding document
            DocumentDAOUtility.updateDocument(businessProcessContext.getId(),content,processID);
        }
        catch (Exception e) {
            logger.error("Failed to update the process instance with id:{}",processInstanceID,e);
            businessProcessContext.handleExceptions();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update the process instance with the given id");
        }
        finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }

        logger.debug("Updated process instance with id: {}",processInstanceID);
        return ResponseEntity.ok(null);
    }
}
