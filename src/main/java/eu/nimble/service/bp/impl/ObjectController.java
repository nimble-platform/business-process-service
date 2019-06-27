package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.impl.util.bp.DocumentEnumClassMapper;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ObjectController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ApiOperation(value = "",notes = "Deletes object with the given hjid and document type")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the object successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token")
    })
    @RequestMapping(value = "/object/{hjid}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity getDocumentJsonContent(@ApiParam(value = "The identifier of the object (hjid) to be deleted", required = true) @PathVariable(value = "hjid") String hjid,
                                                 @ApiParam(value = "Type of the object to be deleted",required = true) @RequestParam(value = "documentType",required = true) DocumentType documentType,
                                                 @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) {
        logger.info("Deleting the object with hjid: {}, documentType: {}",hjid,documentType.toString());

        // check token
        ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }
        // get document class
        Class documentClass = DocumentEnumClassMapper.getDocumentClass(DocumentType.valueOf(documentType.toString()));
        // delete the object
        new JPARepositoryFactory().forCatalogueRepository().deleteEntityByHjid(documentClass,Long.parseLong(hjid));

        logger.info("Deleted the object with hjid: {}, documentType: {}",hjid,documentType.toString());
        return ResponseEntity.ok(null);
    }

}
