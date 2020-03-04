package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
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
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Arrays;

@ApiIgnore
@Controller
public class ObjectController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @ApiOperation(value = "",notes = "Deletes object with the given hjid and class name")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the object successfully"),
            @ApiResponse(code = 404, message = "There does not exist a class for the given class name"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token")
    })
    @RequestMapping(value = "/object/{hjid}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteObject(@ApiParam(value = "The identifier of the object (hjid) to be deleted", required = true) @PathVariable(value = "hjid") String hjid,
                                                 @ApiParam(value = "Class name of the object to be deleted. Some examples are eu.nimble.service.model.ubl.quotation.QuotationType and eu.nimble.service.model.ubl.order.OrderType",required = true) @RequestParam(value = "className",required = true) String className,
                                                 @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) {
        // set request log of ExecutionContext
        String requestLog = String.format("Deleting the object with hjid: %s, className: %s",hjid,className);
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);

        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        Class objectClass = null;
        try {
            objectClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CLASS.toString(), Arrays.asList(className),e);
        }

        // delete the object
        new JPARepositoryFactory().forCatalogueRepository().deleteEntityByHjid(objectClass,Long.parseLong(hjid));

        logger.info("Deleted the object with hjid: {}, className: {}",hjid,className);
        return ResponseEntity.ok(null);
    }

}
