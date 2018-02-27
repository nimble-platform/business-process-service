package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.HibernateUtilityRef;
import eu.nimble.service.bp.swagger.api.GroupApi;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 06-Feb-18.
 */
@Controller
public class ProcessInstanceGroupController implements GroupApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ResponseEntity<ProcessInstanceGroup> addProcessInstanceToGroup(@ApiParam(value = "Identifier of the process instance group to which a new process instance id is added", required = true) @PathVariable("ID") String ID, @ApiParam(value = "Identifier of the process instance to be added", required = true) @RequestParam(value = "processInstanceID", required = true) String processInstanceID) {
        logger.debug("Adding process instance: {} to ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = DAOUtility.getProcessInstanceGroupDAOByID(ID);
        processInstanceGroupDAO.getProcessInstanceIDs().add(processInstanceID);
        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Added process instance: {} to ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<ProcessInstanceGroup> deleteProcessInstanceFromGroup(@ApiParam(value = "Identifier of the process instance group from which the process instance id is deleted", required = true) @PathVariable("ID") String ID, @ApiParam(value = "Identifier of the process instance to be deleted", required = true) @RequestParam(value = "processInstanceID", required = true) String processInstanceID) {
        logger.debug("Deleting process instance: {} from ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = DAOUtility.getProcessInstanceGroupDAOByID(ID);
        processInstanceGroupDAO.getProcessInstanceIDs().remove(processInstanceID);
        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Deleted process instance: {} from ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<Void> deleteProcessInstanceGroup(@ApiParam(value = "", required = true) @PathVariable("ID") String ID) {
        logger.debug("Deleting ProcessInstanceGroup ID: {}", ID);

        DAOUtility.deleteProcessInstanceGroupDAOByID(ID);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("ProcessInstanceGroup deleted successfully");
        logger.debug("Deleted ProcessInstanceGroups: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<ProcessInstanceGroup> getProcessInstanceGroup(@ApiParam(value = "", required = true) @PathVariable("ID") String ID) {
        logger.debug("Getting ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = DAOUtility.getProcessInstanceGroupDAOByID(ID);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Retrieved ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<List<ProcessInstanceGroup>> getProcessInstanceGroups(@ApiParam(value = "Identifier of the party") @RequestParam(value = "partyID", required = false) String partyID, @ApiParam(value = "Offset of the first result among the complete result set satisfying the given criteria", defaultValue = "0") @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset, @ApiParam(value = "Number of results to be included in the result set", defaultValue = "10") @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit, @ApiParam(value = "", defaultValue = "false") @RequestParam(value = "archived", required = false, defaultValue = "false") Boolean archived) {
        logger.debug("Getting ProcessInstanceGroups for party: {}", partyID);

        List<ProcessInstanceGroupDAO> processInstanceGroupDAOs = DAOUtility.getProcessInstanceGroupDAOs(partyID, offset, limit, archived);

        List<ProcessInstanceGroup> processInstanceGroups = new ArrayList<>();
        for(ProcessInstanceGroupDAO processInstanceGroupDAO : processInstanceGroupDAOs) {
            processInstanceGroups.add(HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO));
        }

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroups);
        logger.debug("Retrieved ProcessInstanceGroups for party: {}", partyID);
        return response;
    }

    @Override
    public ResponseEntity<ProcessInstanceGroup> archiveGroup(@ApiParam(value = "Identifier of the process instance group to be archived", required = true) @PathVariable("ID") String ID) {
        logger.debug("Archiving ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = DAOUtility.getProcessInstanceGroupDAOByID(ID);
        processInstanceGroupDAO.setArchived(true);

        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Archived ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<ProcessInstanceGroup> restoreGroup(@ApiParam(value = "Identifier of the process instance group to be restored", required = true) @PathVariable("ID") String ID) {
        logger.debug("Restoring ProcessInstanceGroup: {}", ID);

        ProcessInstanceGroupDAO processInstanceGroupDAO = DAOUtility.getProcessInstanceGroupDAOByID(ID);
        processInstanceGroupDAO.setArchived(false);

        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO);

        ProcessInstanceGroup processInstanceGroup = HibernateSwaggerObjectMapper.createProcessInstanceGroup(processInstanceGroupDAO);
        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body(processInstanceGroup);
        logger.debug("Restored ProcessInstanceGroup: {}", ID);
        return response;
    }

    @Override
    public ResponseEntity<Void> saveProcessInstanceGroup(@ApiParam(value = "The content of the process instance group to be saved", required = true) @RequestBody ProcessInstanceGroup processInstanceGroup) {
        logger.debug("Saving ProcessInstanceGroup {}", processInstanceGroup.toString());
        ProcessInstanceGroupDAO processInstanceGroupDAO = HibernateSwaggerObjectMapper.createProcessInstanceGroup_DAO(processInstanceGroup);
        HibernateUtilityRef.getInstance("bp-data-model").persist(processInstanceGroupDAO);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("Successfully saved ProcessInstanceGroup");
        logger.debug("Saved ProcessInstanceGroup {}", processInstanceGroup.toString());
        return response;
    }

    @Override
    public ResponseEntity<Void> archiveAllGroups(@ApiParam(value = "Identifier of the party of which groups will be archived", required = true) @RequestParam(value = "partyID", required = true) String partyID) {
        logger.debug("Archiving ProcessInstanceGroups for party {}", partyID);

        DAOUtility.archiveAllGroupsForParty(partyID);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("Successfully archived ProcessInstanceGroup");
        logger.debug("Archived ProcessInstanceGroup for party {}", partyID);
        return response;
    }

    @Override
    public ResponseEntity<Void> deleteAllArchivedGroups(@ApiParam(value = "Identifier of the party of which groups will be deleted", required = true) @RequestParam(value = "partyID", required = true) String partyID) {
        logger.debug("Deleting archived ProcessInstanceGroups for party {}", partyID);

        DAOUtility.deleteArchivedGroupsForParty(partyID);

        ResponseEntity response = ResponseEntity.status(HttpStatus.OK).body("Successfully archived ProcessInstanceGroup");
        logger.debug("Deleted archived ProcessInstanceGroups for party {}", partyID);
        return response;
    }
}
