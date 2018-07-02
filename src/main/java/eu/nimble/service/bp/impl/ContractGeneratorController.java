package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.impl.contract.ContractGenerator;
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
    @RequestMapping(value = "/contracts/create-bundle",
            method = RequestMethod.GET,
            produces = {"application/zip"})
    public void generateContract(@RequestParam(value = "orderId", required = true) String orderId, HttpServletResponse response){
        try{
            logger.info("Generating contract for the order with id : {}",orderId);

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

    @RequestMapping(value = "/contracts/create-terms",
            produces = {MediaType.TEXT_PLAIN_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity generateOrderTermsAndConditionsAsText(@RequestParam(value = "orderId", required = true) String orderId,
                                                                @RequestParam(value = "sellerParty", required = true) String sellerParty,
                                                                @RequestParam(value = "buyerParty", required = true) String buyerParty,
                                                                @RequestParam(value = "incoterms", required = true) String incoterms,
                                                                @RequestParam(value = "tradingTerms", required = true) String tradingTerms){
        logger.info("Generating Order Terms and Conditions as text for the order with id : {}",orderId);

        try {
            ContractGenerator contractGenerator = new ContractGenerator();

            String text = contractGenerator.generateOrderTermsAndConditionsAsText(orderId,sellerParty,buyerParty,incoterms,tradingTerms);

            logger.info("Generated Order Terms and Conditions as text for the order with id : {}",orderId);
            return ResponseEntity.ok(text);
        }
        catch (Exception e){
            logger.error("Failed to generate Order Terms and Conditions",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate Order Terms and Conditions");
        }
    }

}