package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.model.tt.OrderEPC;
import eu.nimble.service.bp.util.HttpResponseUtil;
import eu.nimble.service.bp.util.persistence.catalogue.CataloguePersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.JsonSerializationUtility;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 06-Jun-18.
 */
@Controller
public class EPCController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IValidationUtil validationUtil;

    @ApiOperation(value = "",notes = "Gets product information as CatalogueLine for the specified EPC code. First, the corresponding order" +
            " is fetched for the specified code from the data channel service and then, the CatalogueLine is retrieved for the product" +
            " included in the order.", response = CatalogueLineType.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the CatalogueLine details successfully", response = CatalogueLineType.class),
            @ApiResponse(code = 400, message = "The given epc code is not used in any orders"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while getting the T&T details for the given epc")
    })
    @RequestMapping(value = "/t-t/catalogueline",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getCatalogueLineForEPCCode(@ApiParam(value = "The electronic product code for which the track & tracing details are requested", required = true) @RequestParam(value = "epc", required = true) String epc,
                                                     @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        logger.info("Getting track & tracing details for epc: {}", epc);

        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            Response response = SpringBridge.getInstance().getDataChannelClient().getEPCCodesForOrder(bearerToken, epc);
            String responseBody;
            try {
                responseBody = HttpResponseUtil.extractBodyFromFeignClientResponse(response);
            } catch (IOException e) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to retrieve epc codes for code: %s", epc), e, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            List<OrderEPC> epcCodesList = objectMapper.readValue(responseBody.toString(),new TypeReference<List<OrderEPC>>(){});

            if(epcCodesList.size() <= 0){
                String msg = "The epc: %s is not used in any orders.";
                logger.error(String.format(msg, epc));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format(msg, epc));
            }

            // get order id
            String orderId = epcCodesList.get(0).getOrderId();
            // get order to retrieve catalogue uuid and catalogue line id
            OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId, DocumentType.ORDER);
            String catalogueUuid = order.getOrderLine().get(0).getLineItem().getItem().getCatalogueDocumentReference().getID();
            String lineId = order.getOrderLine().get(0).getLineItem().getItem().getManufacturersItemIdentification().getID();
            CatalogueLineType line = CataloguePersistenceUtility.getCatalogueLine(catalogueUuid, lineId);

            logger.info("Retrieved CatalogueLine for the specified epc: {}", epc);
            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(line));

        } catch (Exception e) {
            String msg = "Unexpected error while getting CatalogueLine for the epc: %s";
            logger.error(String.format(msg, epc),e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(String.format(msg, epc));
        }
    }

    @ApiOperation(value = "",notes = "Gets EPC codes that belongs to the same published product ID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved epc codes successfully", response = String.class,responseContainer = "List"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There is no catalogue line for the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while getting the epc codes for the given id")
    })
    @RequestMapping(value = "/t-t/epc-codes",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getEPCCodesBelongsToProduct(@ApiParam(value = "The identifier of the published product (catalogueLine.hjid)", required = true) @RequestParam(value = "productId", required = true) Long publishedProductID,
                                                      @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Getting epc codes for productId: {}", publishedProductID);
        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            Object[] partyIdAndManufacturerItemId = CataloguePersistenceUtility.getCatalogueLinePartyIdAndManufacturersItemIdentification(publishedProductID);
            if(partyIdAndManufacturerItemId == null){
                String msg = "There is no catalogue line for hjid : %d";
                logger.error(String.format(msg,publishedProductID));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(msg,publishedProductID));
            }

            List<String> epcCodes = new ArrayList<>();
            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

            List<String> orderIds = DocumentPersistenceUtility.getOrderIds(partyIdAndManufacturerItemId[0].toString(), partyIdAndManufacturerItemId[1].toString());
            Response response = SpringBridge.getInstance().getDataChannelClient().getEPCCodesForOrders(bearerToken, orderIds);
            String responseBody = HttpResponseUtil.extractBodyFromFeignClientResponse(response);
            List<OrderEPC> epcCodesList = objectMapper.readValue(responseBody.toString(),new TypeReference<List<OrderEPC>>(){});

            for(OrderEPC epcCodes1 : epcCodesList){
                epcCodes.addAll(epcCodes1.getCodes());
            }

            logger.info("Retrieved epc codes for productId: {}",publishedProductID);
            return ResponseEntity.status(HttpStatus.OK).body(epcCodes);
        }
        catch (Exception e){
            String msg = "Unexpected error while getting the epc codes for productId: %d";
            logger.error(String.format(msg,publishedProductID),e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(String.format(msg,publishedProductID));
        }
    }
}