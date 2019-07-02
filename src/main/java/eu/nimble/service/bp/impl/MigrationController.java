package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.serialization.BinaryObjectSerializerGetBinaryObjects;
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

import java.util.List;

@ApiIgnore
@Controller
public class MigrationController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String CATALOG_BINARY_CONTENT_URI = "CatalogBinaryContentUri:";
    private final String BUSINESS_PROCESS_BINARY_CONTENT_URI = "BusinessProcessBinaryContentUri:";

    @ApiOperation(value = "",notes = "Updates the uri of binary objects. Firstly, it retrieves binary objects belonging to the catalogues and updates their uris to CatalogBinaryContentUri:UUID." +
            " For the rest of binary objects, it updates their uris to BusinessProcessBinaryContentUri:UUID.After running this migration script, it is important to change BINARY_CONTENT_URL" +
            " environment variable of business-process service to 'BusinessProcessBinaryContentUri:' and that of catalogue service to 'CatalogBinaryContentUri:'.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated binary content uris successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while updating binary content uris")
    })
    @RequestMapping(value = "/migration/binarycontents",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity updateBinaryContents(@ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) {
        logger.info("Incoming request to update binary content uris");

        // check token
        ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        GenericJPARepository repo = new JPARepositoryFactory().forCatalogueRepository(true);
        // get catalogues
        String GET_CATALOGUES = "FROM CatalogueType";
        try{
            List<CatalogueType> catalogues = repo.getEntities(GET_CATALOGUES);
            logger.info("Retrieved catalogues");
            // get catalogue binary content uris
            List<BinaryObjectType> catalogBinaryObjects = getCatalogBinaryObjects(catalogues);
            logger.info("Retrieved catalogue binary objects");
            // update catalog binary contents
            SpringBridge.getInstance().getBinaryContentService().updateBinaryContentUris(catalogBinaryObjects, CATALOG_BINARY_CONTENT_URI);
            logger.info("Updated uris of catalogue binary objects");
            // get binary objects
            List<BinaryObjectType> bpBinaryObjects = SpringBridge.getInstance().getBinaryContentService().getBinaryObjects();
            logger.info("Retrieved business process binary objects");
            // update bp binary contents
            SpringBridge.getInstance().getBinaryContentService().updateBinaryContentUris(bpBinaryObjects, BUSINESS_PROCESS_BINARY_CONTENT_URI);
            logger.info("Updated uris of business process binary objects");
            // delete old binary contents
            SpringBridge.getInstance().getBinaryContentService().deleteBinaryContents();
            logger.info("Deleted binary contents with old uris");
        }
        catch (Exception e){
            logger.error("Unexpected error while updating binary content uris",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error while updating binary content uris");
        }
        logger.info("Completed request to update binary content uris");
        return ResponseEntity.ok(null);
    }

    private List<BinaryObjectType> getCatalogBinaryObjects(List<CatalogueType> catalogues){
        ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
        BinaryObjectSerializerGetBinaryObjects serializer = new BinaryObjectSerializerGetBinaryObjects();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(BinaryObjectType.class, serializer);
        objectMapper.registerModule(simpleModule);

        try {
            objectMapper.writeValueAsString(catalogues);

        } catch (JsonProcessingException e) {
            String msg = String.format("Failed to serialize object: %s", catalogues.getClass().getName());
            logger.error(msg);
            throw new RuntimeException(msg, e);
        }
        return serializer.getObjects();
    }
}