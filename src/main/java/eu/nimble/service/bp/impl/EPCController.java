package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.nimble.service.bp.config.GenericConfig;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.model.tt.TTInfo;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.CataloguePersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.JsonSerializationUtility;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by suat on 06-Jun-18.
 */
@Controller
public class EPCController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    static class EpcCodes {
        private String orderId;
        private Set<String> codes = new HashSet<>();

        String getOrderId() {
            return orderId;
        }

        void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        Set<String> getCodes() {
            return codes;
        }

        void setCodes(Set<String> codes) {
            this.codes = codes;
        }
    }

    @ApiOperation(value = "",notes = "Gets track & trace-related details for the specified EPC code. First, the corresponding order" +
            " is fetched for the specified code from the data channel service. Then, the CatalogueLine is retrieved for the product" +
            " included in the order and the identifier of corresponding process instance is retrieved from the document metadata"+
            " of the process.", response = TTInfo.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the T&T details successfully", response = TTInfo.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "The given epc code is not used in any orders"),
            @ApiResponse(code = 500, message = "Unexpected error while getting the T&T details for the given epc")
    })
    @RequestMapping(value = "/t-t/epc-details",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getTTDetails(@ApiParam(value = "The electronic product code for which the track & tracing details are requested", required = true) @RequestParam(value = "epc", required = true) String epc,
                                       @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.info("Getting track & tracing details for epc: {}", epc);

        try {
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            GenericConfig config = SpringBridge.getInstance().getGenericConfig();
            String dataChannelServiceUrlStr = config.getDataChannelServiceUrl()+"/epc/code/"+epc;

            HttpResponse<com.mashape.unirest.http.JsonNode> response = Unirest.get(dataChannelServiceUrlStr)
                    .header("Authorization", bearerToken).asJson();

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            List<EpcCodes> epcCodesList = objectMapper.readValue(response.getBody().toString(),new TypeReference<List<EpcCodes>>(){});

            if(epcCodesList.size() <= 0){
                String msg = "The epc: %s is not used in any orders.";
                logger.error(String.format(msg, epc));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(msg, epc));
            }

            // get order id
            String orderId = epcCodesList.get(0).getOrderId();
            // get corresponding process instance id
            String processInstanceId = ProcessDocumentMetadataDAOUtility.findByDocumentID(orderId).getProcessInstanceID();
            // get order to retrieve catalogue uuid and catalogue line id
            OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId, DocumentType.ORDER);
            Object[] lineHjidAndCatalogUuid = CataloguePersistenceUtility.getCatalogueIdAndLineId(order);

            TTInfo ttInfo = new TTInfo();
            ttInfo.setProcessInstanceId(processInstanceId);
            ttInfo.setCatalogueLineHjid(lineHjidAndCatalogUuid[0].toString());
            ttInfo.setCatalogueUuid(lineHjidAndCatalogUuid[1].toString());

            logger.info("Received track & tracing details for epc: {}", epc);
            return ResponseEntity.ok(ttInfo);

        } catch (Exception e) {
            String msg = "Unexpected error while getting the T&T details for the epc: %s";
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
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            Object[] partyIdAndManufacturerItemId = CataloguePersistenceUtility.getCatalogueLinePartyIdAndManufacturersItemIdentification(publishedProductID);
            if(partyIdAndManufacturerItemId == null){
                String msg = "There is no catalogue line for hjid : %d";
                logger.error(String.format(msg,publishedProductID));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(msg,publishedProductID));
            }

            List<String> orderIds = DocumentPersistenceUtility.getOrderIds(partyIdAndManufacturerItemId[0].toString(), partyIdAndManufacturerItemId[1].toString());

            String params = "";
            int size = orderIds.size();
            for(int i = 0; i < size;i++){
                if(i != size-1){
                    params = params + orderIds.get(i) + ",";
                }
                else {
                    params = params + orderIds.get(i);
                }
            }

            GenericConfig config = SpringBridge.getInstance().getGenericConfig();
            String dataChannelServiceUrlStr = config.getDataChannelServiceUrl()+"/epc/list?orders="+params;

            HttpResponse<String> response = Unirest.get(dataChannelServiceUrlStr)
                    .header("Authorization", bearerToken).asString();

            List<String> epcCodes = new ArrayList<>();
            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();;
            List<EpcCodes> epcCodesList = objectMapper.readValue(response.getBody().toString(),new TypeReference<List<EpcCodes>>(){});

            for(EpcCodes epcCodes1 : epcCodesList){
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