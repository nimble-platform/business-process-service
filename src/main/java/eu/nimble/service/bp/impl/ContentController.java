package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.impl.util.CamundaEngine;
import eu.nimble.service.bp.impl.util.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.swagger.api.ContentApi;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class ContentController implements ContentApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ResponseEntity<ModelApiResponse> addProcessDefinition(@RequestBody Process body) {
        logger.info(" $$$ Adding business process definition: ");
        logger.debug(" $$$ {}", body.toString());

        CamundaEngine.addProcessDefinition(body);

        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteProcessDefinition(@PathVariable("processID") String processID) {
        logger.info(" $$$ Deleting business process definition for ... {}", processID);

        CamundaEngine.deleteProcessDefinition(processID);

        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<Process> getProcessDefinition(@PathVariable("processID") String processID) {
        logger.info(" $$$ Getting business process definition for ... {}", processID);

        Process process = CamundaEngine.getProcessDefinition(processID);

        logger.debug(" $$$ Returning process definition {}", process.toString());
        return new ResponseEntity<>(process, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Process>> getProcessDefinitions() {
        logger.info(" $$$ Getting business process definitions");

        List<Process> processes = CamundaEngine.getProcessDefinitions();

        return new ResponseEntity<>(processes, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateProcessDefinition(@RequestBody Process body) {
        logger.info(" $$$ Updating business process definition: ");
        logger.debug(" $$$ {}", body.toString());

        CamundaEngine.updateProcessDefinition(body);

        return HibernateSwaggerObjectMapper.getApiResponse();
    }
}
