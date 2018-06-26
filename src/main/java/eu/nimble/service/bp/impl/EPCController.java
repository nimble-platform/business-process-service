package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.nimble.service.bp.config.GenericConfig;
import eu.nimble.service.bp.impl.model.tt.TTInfo;
import eu.nimble.service.bp.impl.util.persistence.HibernateUtilityRef;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.utility.Configuration;
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

import java.util.*;

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

    @ApiOperation(value = "Gets product-related and company-related details for the given epc code", response = TTInfo.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the T&T details successfully", response = TTInfo.class)})
    @RequestMapping(value = "/t-t/epc-details",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getTTDetails(@ApiParam(value = "The electronic product code for which the track & tracing details are requested") @RequestParam(value = "epc", required = true) String epc) {
        logger.info("Getting track & tracing details for epc: {}", epc);

        try {
            TTInfo ttInfo = new TTInfo();
            ttInfo.setMasterUrl("https://falcon-dev.ikap.biba.uni-bremen.de/masterData");
            ttInfo.setEventUrl("https://falcon-dev.ikap.biba.uni-bremen.de/simpleTracking");
            ttInfo.setProductionProcessTemplate("https://falcon-dev.ikap.biba.uni-bremen.de/productionProcessTemplate/lindbacks_test");
            ttInfo.setRelatedProductId("temp-product-id");

            ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(ttInfo);
            logger.debug("Retrieved T6T details for epc: {}", epc);
            return response;

        } catch (Exception e) {
            String msg = "Unexpected error while getting the T&T details for the epc: %s";
            logger.error(String.format(msg, epc));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }
    }

    @ApiOperation(value = "Gets EPC codes associated with the given product id", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the T&T details successfully", response = List.class)})
    @RequestMapping(value = "/t-t/epc-codes",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getEPCCodes(@ApiParam(value = "The product ID for which the related EPC codes will be retrieved") @RequestParam(value = "productId", required = true) String productId) {
        logger.info("Getting epc codes for productId: {}", productId);

        try {
            List<String> epcCodes = new ArrayList<>();
            epcCodes.add("urn:epc:id:sgtin:0614141.lindback.2017");
            epcCodes.add("urn:epc:id:sgtin:0614141.lindback.201702");
            epcCodes.add("urn:epc:id:sgtin:0614141.lindback.201703");

            ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(epcCodes);
            logger.debug("Retrieved epc codes for productId: {}", productId);
            return response;

        } catch (Exception e) {
            String msg = "Unexpected error while getting the epc codes for the productId: %s";
            logger.error(String.format(msg, productId));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }
    }

    @ApiOperation(value = "Gets EPC codes that belongs to the same published product ID", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved epc codes successfully", response = List.class)})
    @RequestMapping(value = "/t-t/products/epc-codes",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getEPCCodesBelongsToProduct(@ApiParam(value = "The published product ID") @RequestParam(value = "publishedProductID", required = true) String publishedProductID,
                                                      @ApiParam(value = "The manufacturer party ID") @RequestParam(value = "manufacturerPartyID", required = true) String manufacturerPartyID,
                                                      @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Getting epc codes for manufacturerPartyID: {} and productId: {}", manufacturerPartyID,publishedProductID);
        try {
            String query = "select order_.ID from OrderType order_ join order_.orderLine line where line.lineItem.item.manufacturerParty.ID = '"+manufacturerPartyID+"' AND line.lineItem.item.manufacturersItemIdentification.ID = '"+publishedProductID+"'";
            List<String> orderIds = (List<String>) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);

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

            HttpResponse<com.mashape.unirest.http.JsonNode> response = Unirest.get(dataChannelServiceUrlStr)
                    .header("Authorization", bearerToken).asJson();

            List<String> epcCodes = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();
            List<EpcCodes> epcCodesList = objectMapper.readValue(response.getBody().toString(),new TypeReference<List<EpcCodes>>(){});

            for(EpcCodes epcCodes1 : epcCodesList){
                epcCodes.addAll(epcCodes1.getCodes());
            }
            logger.info("Retrieved epc codes for manufacturerPartyID: {} and productId: {}", manufacturerPartyID,publishedProductID);
            return ResponseEntity.status(HttpStatus.OK).body(epcCodes);
        }
        catch (Exception e){
            String msg = "Unexpected error while getting the epc codes for manufacturerPartyID: %s and productId: %s";
            logger.error(String.format(msg, manufacturerPartyID,publishedProductID));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
