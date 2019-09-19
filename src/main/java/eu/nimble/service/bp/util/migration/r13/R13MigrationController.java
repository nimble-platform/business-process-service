package eu.nimble.service.bp.util.migration.r13;

import eu.nimble.service.bp.model.hyperjaxb.GroupStatus;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CompletedTaskType;
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

import java.util.ArrayList;
import java.util.List;

@ApiIgnore
@Controller
public class R13MigrationController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String QUERY_GET_DUPLICATE_IDS = "SELECT ID FROM %s GROUP BY ID HAVING count(ID) > 1";
    private final String QUERY_GET_DOCUMENTS_BY_ID = "FROM %s WHERE ID = :id";

    private final int LIMIT = 100;
    private final int OFFSET = 1;

    @ApiOperation(value = "", notes = "This script makes sure that each document has a unique identifier. It deletes the duplicate documents.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted duplicate documents successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token")
    })
    @RequestMapping(value = "/r13/migration/remove-duplicates",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteDuplicateDocuments(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken
    ) {
        logger.info("Incoming request to delete duplicate documents");

        // check token
        ResponseEntity tokenCheck = eu.nimble.service.bp.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        GenericJPARepository repo = new JPARepositoryFactory().forCatalogueRepository(true);

        List<String> tableNames = getTableNames();
        for (String tableName : tableNames) {
            // get duplicate ids
            List<String> duplicateIds = repo.getEntities(String.format(QUERY_GET_DUPLICATE_IDS, tableName));
            if (duplicateIds.size() > 0) {
                logger.info("In table {}, duplicates exist for the documents with following ids: {}", tableName, duplicateIds);
                for (String id : duplicateIds) {
                    // remove duplicate documents
                    List<Object> documents = repo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, tableName), new String[]{"id"}, new Object[]{id}, LIMIT, OFFSET);
                    for (Object document : documents) {
                        repo.deleteEntity(document);
                    }
                }
            }
        }

        logger.info("Completed request to delete duplicate documents");
        return ResponseEntity.ok(null);
    }

    @ApiOperation(value = "", notes = "It sets the status of ProcessInstanceGroups to COMPLETED if the collaboration is completed for that group.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated process instance group status successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while updating the status of Process Instance Groups")
    })
    @RequestMapping(value = "/r13/migration/update-group-status",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity updateProcessInstanceGroupStatus(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken
    ) {
        logger.info("Incoming request to update process instance group status");

        // check token
        ResponseEntity tokenCheck = eu.nimble.service.bp.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        try{
            GenericJPARepository catalogueRepo = new JPARepositoryFactory().forCatalogueRepository(true);
            GenericJPARepository bpRepo = new JPARepositoryFactory().forBpRepository(true);

            // get CompletedTasks
            List<CompletedTaskType> completedTasks = catalogueRepo.getEntities(CompletedTaskType.class);
            for(CompletedTaskType completedTask : completedTasks){
                // only consider the completed tasks whose status is Completed
                if(completedTask.getDescription().get(0).getValue().contentEquals("Completed")){
                    String processInstanceId = completedTask.getAssociatedProcessInstanceID();
                    // get process instance groups
                    List<ProcessInstanceGroupDAO> processInstanceGroupDAOS = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAOs(processInstanceId, bpRepo);
                    logger.info("For process instance id: {}, there are {} process instance groups",processInstanceId, processInstanceGroupDAOS.size());
                    // update the status of process instance group
                    for(ProcessInstanceGroupDAO processInstanceGroupDAO: processInstanceGroupDAOS){
                        logger.debug("Updating the status of ProcessInstanceGroup with id {}",processInstanceGroupDAO.getID());
                        processInstanceGroupDAO.setStatus(GroupStatus.COMPLETED);
                        bpRepo.updateEntity(processInstanceGroupDAO);
                        logger.debug("Updated the status of ProcessInstanceGroup with id {}",processInstanceGroupDAO.getID());
                    }
                }
            }
        }
        catch (Exception e){
            String msg = "Unexpected error while updating the status of Process Instance Groups";
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }

        logger.info("Completed request to update process instance group status");
        return ResponseEntity.ok(null);
    }

    private List<String> getTableNames() {
        List<String> tableNames = new ArrayList<>();

        tableNames.add("ItemInformationRequestType");
        tableNames.add("ItemInformationResponseType");
        tableNames.add("RequestForQuotationType");
        tableNames.add("QuotationType");
        tableNames.add("PpapRequestType");
        tableNames.add("PpapResponseType");
        tableNames.add("OrderType");
        tableNames.add("OrderResponseSimpleType");
        tableNames.add("DespatchAdviceType");
        tableNames.add("ReceiptAdviceType");
        tableNames.add("TransportExecutionPlanRequestType");
        tableNames.add("TransportExecutionPlanType");

        return tableNames;
    }
}