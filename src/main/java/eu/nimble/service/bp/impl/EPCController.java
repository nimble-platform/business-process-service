package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.nimble.service.bp.config.GenericConfig;
import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.impl.model.tt.TTInfo;
import eu.nimble.service.bp.impl.util.persistence.CatalogueDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.DocumentDAOUtility;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.Configuration;
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
    public ResponseEntity getTTDetails(@ApiParam(value = "The electronic product code for which the track & tracing details are requested") @RequestParam(value = "epc", required = true) String epc,
                                       @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.info("Getting track & tracing details for epc: {}", epc);

        try {
            /*
            TTInfo ttInfo = new TTInfo();
            ttInfo.setMasterUrl("https://falcon-dev.ikap.biba.uni-bremen.de/masterData");
            ttInfo.setEventUrl("https://falcon-dev.ikap.biba.uni-bremen.de/simpleTracking");
            ttInfo.setProductionProcessTemplate("https://falcon-dev.ikap.biba.uni-bremen.de/productionProcessTemplate/lindbacks_test");
            ttInfo.setRelatedProductId("temp-product-id");

            ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(ttInfo);
            logger.debug("Retrieved T6T details for epc: {}", epc);
            return response;*/
            GenericConfig config = SpringBridge.getInstance().getGenericConfig();
            String dataChannelServiceUrlStr = config.getDataChannelServiceUrl()+"/epc/code/"+epc;

            HttpResponse<com.mashape.unirest.http.JsonNode> response = Unirest.get(dataChannelServiceUrlStr)
                    .header("Authorization", bearerToken).asJson();

            ObjectMapper objectMapper = new ObjectMapper();
            List<EpcCodes> epcCodesList = objectMapper.readValue(response.getBody().toString(),new TypeReference<List<EpcCodes>>(){});

            if(epcCodesList.size() <= 0){
                String msg = "The epc: %s is not used in any orders.";
                logger.error(String.format(msg, epc));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(msg, epc));
            }

            String orderId = epcCodesList.get(0).getOrderId();

            OrderType order = (OrderType) DocumentDAOUtility.getUBLDocument(orderId, DocumentType.ORDER);

            CatalogueLineType catalogueLine = CatalogueDAOUtility.getCatalogueLine(order);


            TTInfo ttInfo = new TTInfo();
            ttInfo.setEventUrl(catalogueLine.getGoodsItem().getItem().getTrackAndTraceDetails().getEventURL());
            ttInfo.setMasterUrl(catalogueLine.getGoodsItem().getItem().getTrackAndTraceDetails().getMasterURL());
            ttInfo.setProductionProcessTemplate(catalogueLine.getGoodsItem().getItem().getTrackAndTraceDetails().getProductionProcessTemplate());
            ttInfo.setRelatedProductId(catalogueLine.getHjid().toString());

            logger.info("Received track & tracing details for epc: {}", epc);
            return ResponseEntity.ok(ttInfo);

        } catch (Exception e) {
            String msg = "Unexpected error while getting the T&T details for the epc: %s";
            logger.error(String.format(msg, epc),e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(String.format(msg, epc));
        }
    }

//    @ApiOperation(value = "Gets EPC codes associated with the given product id", response = List.class)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Retrieved the T&T details successfully", response = List.class)})
//    @RequestMapping(value = "/t-t/epc-codes",
//            produces = {"application/json"},
//            method = RequestMethod.GET)
//    public ResponseEntity getEPCCodes(@ApiParam(value = "The product ID for which the related EPC codes will be retrieved") @RequestParam(value = "productId", required = true) String productId) {
//        logger.info("Getting epc codes for productId: {}", productId);
//
//        try {
//            List<String> epcCodes = new ArrayList<>();
//            epcCodes.add("urn:epc:id:sgtin:0614141.lindback.2017");
//            epcCodes.add("urn:epc:id:sgtin:0614141.lindback.201702");
//            epcCodes.add("urn:epc:id:sgtin:0614141.lindback.201703");
//
//            ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(epcCodes);
//            logger.debug("Retrieved epc codes for productId: {}", productId);
//            return response;
//
//        } catch (Exception e) {
//            String msg = "Unexpected error while getting the epc codes for the productId: %s";
//            logger.error(String.format(msg, productId));
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
//        }
//    }

    @ApiOperation(value = "Gets EPC codes that belongs to the same published product ID", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved epc codes successfully", response = List.class)})
    @RequestMapping(value = "/t-t/epc-codes",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getEPCCodesBelongsToProduct(@ApiParam(value = "The published product ID") @RequestParam(value = "productId", required = true) Long publishedProductID,
                                                      @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Getting epc codes for productId: {}", publishedProductID);
        try {
            CatalogueLineType catalogueLine = (CatalogueLineType) HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).load(CatalogueLineType.class, publishedProductID);

            if(catalogueLine == null){
                String msg = "There is no catalogue line for hjid : %d";
                logger.error(String.format(msg,publishedProductID));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(msg,publishedProductID));
            }

            List<String> orderIds = CatalogueDAOUtility.getOrderIds(catalogueLine);

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