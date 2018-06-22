package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.impl.contract.ContractGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

}