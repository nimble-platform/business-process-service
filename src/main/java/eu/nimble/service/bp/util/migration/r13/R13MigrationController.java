package eu.nimble.service.bp.util.migration.r13;

import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
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

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

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
        // set request log of ExecutionContext
        String requestLog = "Incoming request to delete duplicate documents";
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);

        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
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