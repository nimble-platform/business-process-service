package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.impl.model.tt.TTInfo;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 06-Jun-18.
 */
@Controller
public class EPCController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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

    @ApiOperation(value = "Gets productId codes associated with the given product id", response = List.class)
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
}
