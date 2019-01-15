package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.impl.contract.ContractGenerator;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.zip.ZipOutputStream;

@Controller
public class ContractGeneratorController {
    private final Logger logger = LoggerFactory.getLogger(ContractGeneratorController.class);

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "",notes = "Generate the contract for the given order id")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Failed to generate contract for the order with the given id")
    })
    @RequestMapping(value = "/contracts/create-bundle",
            method = RequestMethod.GET,
            produces = {"application/zip"})
    public void generateContract(@ApiParam(value = "Identifier of the order for which the contract will be generated") @RequestParam(value = "orderId", required = true) String orderId,
                                 @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,HttpServletResponse response){
        try{
            logger.info("Generating contract for the order with id : {}",orderId);
            // check token
            boolean isValid = SpringBridge.getInstance().getIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                response.setStatus(HttpStatus.NOT_FOUND.value());
                try{
                    response.getOutputStream().write(msg.getBytes());
                }
                catch (Exception e1){
                    logger.error("Failed to write the error message to the output stream",e1);
                }
            }

            ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());

            ContractGenerator contractGenerator = new ContractGenerator();
            contractGenerator.generateContract(orderId,zos);

            response.flushBuffer();

            zos.close();

            logger.info("Generated contract for the order with id : {}",orderId);

        }
        catch (Exception e){
            logger.error("Failed to generate contract for the order with id : {}",orderId,e);

            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try{
                response.getOutputStream().write("Failed to generate contract".getBytes());
            }
            catch (Exception e1){
                logger.error("Failed to write the error message to the output stream",e1);
            }
        }

    }

    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Generated the text version of Order Terms and Conditions for the given order id"),
            @ApiResponse(code = 500, message = "Failed to generate the text version of Order Terms and Conditions for the given order id")
    })
    @RequestMapping(value = "/contracts/create-terms",
            produces = {MediaType.TEXT_PLAIN_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity generateOrderTermsAndConditionsAsText(@ApiParam(value = "Identifier of the order for which terms and conditions are generated") @RequestParam(value = "orderId", required = true) String orderId,
                                                                @ApiParam(value = "Identifier of the seller party") @RequestParam(value = "sellerPartyId", required = false) String sellerPartyId,
                                                                @ApiParam(value = "Identifier of the buyer party") @RequestParam(value = "buyerPartyId", required = false) String buyerPartyId,
                                                                @ApiParam(value = "The selected incoterms while negotiating.\nExample:DDP (Delivery Duty Paid)") @RequestParam(value = "incoterms", required = false) String incoterms,
                                                                @ApiParam(value = "The list of selected trading terms while negotiating.\nExample:[{\"id\":\"Cash_on_delivery\",\"description\":\"Cash on delivery\",\"tradingTermFormat\":\"COD\",\"value\":[\"true\"]}]") @RequestParam(value = "tradingTerms", required = false) String tradingTerms,
                                                                @ApiParam(value = "The Bearer token provided by the identity service") @RequestHeader(value = "Authorization", required = true) String bearerToken){
        logger.info("Generating Order Terms and Conditions as text for the order with id : {}",orderId);

        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }

            ContractGenerator contractGenerator = new ContractGenerator();

            String text = contractGenerator.generateOrderTermsAndConditionsAsText(orderId,sellerPartyId,buyerPartyId,incoterms,tradingTerms,bearerToken);

            logger.info("Generated Order Terms and Conditions as text for the order with id : {}",orderId);
            return ResponseEntity.ok(text);
        }
        catch (Exception e){
            logger.error("Failed to generate Order Terms and Conditions",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate Order Terms and Conditions");
        }
    }

}