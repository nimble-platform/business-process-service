package eu.nimble.service.bp.impl;

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
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
public class ObjectController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
        logger.info("Deleting the object with hjid: {}, className: {}",hjid,className);

        // check token
        ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }
        Class objectClass = null;
        try {
            objectClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            String msg = "There does not exist a class for the given class name: %s";
            logger.error(String.format(msg, className));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(msg, className));
        }

        // delete the object
        new JPARepositoryFactory().forCatalogueRepository().deleteEntityByHjid(objectClass,Long.parseLong(hjid));

        logger.info("Deleted the object with hjid: {}, className: {}",hjid,className);
        return ResponseEntity.ok(null);
    }

}
