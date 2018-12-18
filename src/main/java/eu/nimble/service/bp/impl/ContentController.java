package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDAO;
import eu.nimble.service.bp.impl.persistence.bp.BusinessProcessRepository;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.jssequence.JSSequenceDiagramParser;
import eu.nimble.service.bp.impl.persistence.util.DAOUtility;
import eu.nimble.service.bp.impl.persistence.util.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.swagger.api.ContentApi;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.Process;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private BusinessProcessRepository businessProcessRepository;

    @Override
    @ApiOperation(value = "",notes = "Add a new business process")
    public ResponseEntity<ModelApiResponse> addProcessDefinition(@RequestBody Process body) {
        logger.info(" $$$ Adding business process definition: ");
        logger.debug(" $$$ {}", body.toString());

        String bpmnContent = body.getBpmnContent();
        if (bpmnContent == null || bpmnContent.trim().equals("")) {
            logger.info(" $$$ BPMN Content is empty. Hence BPMN is created from the text content...");
            JSSequenceDiagramParser parser = new JSSequenceDiagramParser(body);
            bpmnContent = parser.getBPMNContent();
            logger.debug(" $$$ Generated BPMN Content: \n {}", bpmnContent);
            body.setTransactions(parser.getTransactions());
        }

        CamundaEngine.addProcessDefinition(body.getProcessID(), bpmnContent);

        ProcessDAO processDAO = HibernateSwaggerObjectMapper.createProcess_DAO(body);
//        HibernateUtilityRef.getInstance("bp-data-model").persist(processDAO);
        businessProcessRepository.persistEntity(processDAO);

        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    @ApiOperation(value = "",notes = "Delete a business process definition")
    public ResponseEntity<ModelApiResponse> deleteProcessDefinition(@PathVariable("processID") String processID) {
        logger.info(" $$$ Deleting business process definition for ... {}", processID);

        CamundaEngine.deleteProcessDefinition(processID);

        ProcessDAO processDAO = DAOUtility.getProcessDAOByID(processID);
        if(processDAO != null)
//            HibernateUtilityRef.getInstance("bp-data-model").delete(ProcessDAO.class, processDAO.getHjid());
            businessProcessRepository.deleteEntityByHjid(ProcessDAO.class, processDAO.getHjid());

        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    @ApiOperation(value = "",notes = "Get the business process definitions")
    public ResponseEntity<Process> getProcessDefinition(@PathVariable("processID") String processID) {
        logger.info(" $$$ Getting business process definition for ... {}", processID);

        ProcessDAO processDAO = DAOUtility.getProcessDAOByID(processID);
        // The process definition is not in the database...
        Process process = null;
        if (processDAO != null)
            process = HibernateSwaggerObjectMapper.createProcess(processDAO);
        else
            process = CamundaEngine.getProcessDefinition(processID);

        logger.debug(" $$$ Returning process definition {}", process.toString());
        return new ResponseEntity<>(process, HttpStatus.OK);
    }

    @Override
    @ApiOperation(value = "",notes = "Get the business process definitions")
    public ResponseEntity<List<Process>> getProcessDefinitions() {
        logger.info(" $$$ Getting business process definitions");

        // first get the ones in the database
        List<ProcessDAO> processDAOs = DAOUtility.getProcessDAOs();
        List<Process> processes = new ArrayList<>();
        for (ProcessDAO processDAO : processDAOs) {
            Process process = HibernateSwaggerObjectMapper.createProcess(processDAO);
            processes.add(process);
        }
        // then get the ones initially loaded
        List<Process> defaultProcesses = CamundaEngine.getProcessDefinitions();
        // merge them
        for(Process defaultProcess: defaultProcesses) {
            boolean found = false;
            for(Process process: processes) {
                if(process.getProcessID().equals(defaultProcess.getProcessID())) {
                    found = true;
                    break;
                }
            }
            if(!found)
                processes.add(defaultProcess);
        }

        return new ResponseEntity<>(processes, HttpStatus.OK);
    }

    @Override
    @ApiOperation(value = "",notes = "Update a business process")
    public ResponseEntity<ModelApiResponse> updateProcessDefinition(@RequestBody Process body) {
        logger.info(" $$$ Updating business process definition: ");
        logger.debug(" $$$ {}", body.toString());

        String bpmnContent = body.getBpmnContent();
        if (bpmnContent == null || bpmnContent.trim().equals("")) {
            logger.info(" $$$ BPMN Content is empty. Hence BPMN is created from the text content...");
            JSSequenceDiagramParser parser = new JSSequenceDiagramParser(body);
            bpmnContent = parser.getBPMNContent();
            body.setTransactions(parser.getTransactions());
        }

        CamundaEngine.updateProcessDefinition(body.getProcessID(), bpmnContent);

        ProcessDAO processDAO = DAOUtility.getProcessDAOByID(body.getProcessID());
        ProcessDAO processDAONew = HibernateSwaggerObjectMapper.createProcess_DAO(body);

        processDAONew.setHjid(processDAO.getHjid());

//        HibernateUtilityRef.getInstance("bp-data-model").update(processDAONew);
        businessProcessRepository.updateEntity(processDAONew);

        return HibernateSwaggerObjectMapper.getApiResponse();
    }
}
