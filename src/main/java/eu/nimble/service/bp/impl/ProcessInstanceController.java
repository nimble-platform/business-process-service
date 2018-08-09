package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceStatus;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateUtilityRef;
import org.camunda.bpm.engine.ProcessEngines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by dogukan on 09.08.2018.
 */

@Controller
public class ProcessInstanceController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/processInstance",
            method = RequestMethod.DELETE)
    public ResponseEntity cancelProcessInstance(@RequestParam(value = "processInstanceId", required = true) String processInstanceId) {
        logger.debug("Cancelling process instance with id: {}",processInstanceId);

        try {
            ProcessInstanceDAO instanceDAO = DAOUtility.getProcessIntanceDAOByID(processInstanceId);
            // check whether the process instance with the given id exists or not
            if(instanceDAO == null){
                logger.error("There does not exist a process instance with id:{}",processInstanceId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("There does not exist a process instance with the given id");
            }
            // cancel the process
            ProcessEngines.getDefaultProcessEngine().getRuntimeService().deleteProcessInstance(processInstanceId,"",true,true);
            // change status of the process
            instanceDAO.setStatus(ProcessInstanceStatus.ABORTED);
            HibernateUtilityRef.getInstance("bp-data-model").update(instanceDAO);
        }
        catch (Exception e) {
            logger.error("Failed to cancel the process instance with id:{}",processInstanceId,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel the process instance with the given id");
        }

        logger.debug("Cancelled process instance with id: {}",processInstanceId);
        return ResponseEntity.ok(null);
    }

}
