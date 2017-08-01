package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDAO;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.jssequence.JSSequenceDiagramParser;
import eu.nimble.service.bp.swagger.api.ContentApi;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.Process;
import eu.nimble.utility.HibernateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
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

        String bpmnContent = body.getBpmnContent();
        if(bpmnContent == null || bpmnContent.trim().equals("")) {
            JSSequenceDiagramParser parser = new JSSequenceDiagramParser(body);
            bpmnContent = parser.getBPMNContent();
            body.setTransactions(parser.getTransactions());
        }

        CamundaEngine.addProcessDefinition(body.getProcessID(), bpmnContent);

        ProcessDAO processDAO = HibernateSwaggerObjectMapper.createProcess_DAO(body);
        HibernateUtility.getInstance("bp-data-model").persist(processDAO);

        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteProcessDefinition(@PathVariable("processID") String processID) {
        logger.info(" $$$ Deleting business process definition for ... {}", processID);

        CamundaEngine.deleteProcessDefinition(processID);

        ProcessDAO processDAO = DAOUtility.getProcessDAOByID(processID);
        HibernateUtility.getInstance("bp-data-model").delete(ProcessDAO.class, processDAO.getHjid());

        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<Process> getProcessDefinition(@PathVariable("processID") String processID) {
        logger.info(" $$$ Getting business process definition for ... {}", processID);

        Process processCamunda = CamundaEngine.getProcessDefinition(processID);  // This will be returned in case of BPMN is requested

        ProcessDAO processDAO = DAOUtility.getProcessDAOByID(processID);

        Process process = HibernateSwaggerObjectMapper.createProcess(processDAO);
        process.setBpmnContent(processCamunda.getBpmnContent());

        logger.debug(" $$$ Returning process definition {}", process.toString());
        return new ResponseEntity<>(process, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Process>> getProcessDefinitions() {
        logger.info(" $$$ Getting business process definitions");

        // The following can be used if all bpmn contents are necessary
        //List<Process> processes = CamundaEngine.getProcessDefinitions();
        List<ProcessDAO> processDAOs = DAOUtility.getProcessDAOs();
        List<Process> processes = new ArrayList<>();
        for(ProcessDAO processDAO: processDAOs) {
            Process process = HibernateSwaggerObjectMapper.createProcess(processDAO);
            processes.add(process);
        }

        return new ResponseEntity<>(processes, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateProcessDefinition(@RequestBody Process body) {
        logger.info(" $$$ Updating business process definition: ");
        logger.debug(" $$$ {}", body.toString());

        String bpmnContent = body.getBpmnContent();
        if(bpmnContent == null || bpmnContent.trim().equals("")) {
            JSSequenceDiagramParser parser = new JSSequenceDiagramParser(body);
            bpmnContent = parser.getBPMNContent();
            body.setTransactions(parser.getTransactions());
        }

        CamundaEngine.updateProcessDefinition(body.getProcessID(), bpmnContent);

        ProcessDAO processDAO = DAOUtility.getProcessDAOByID(body.getProcessID());
        ProcessDAO processDAONew = HibernateSwaggerObjectMapper.createProcess_DAO(body);

        processDAONew.setHjid(processDAO.getHjid());

        HibernateUtility.getInstance("bp-data-model").update(processDAONew);

        return HibernateSwaggerObjectMapper.getApiResponse();
    }
}
