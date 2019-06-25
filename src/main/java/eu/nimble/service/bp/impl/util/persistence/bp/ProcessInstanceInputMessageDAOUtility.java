package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessVariablesDAO;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

public class ProcessInstanceInputMessageDAOUtility {

    private static final String QUERY_GET_BY_PARTY_ID = "SELECT inputMessageDAO.hjid FROM ProcessInstanceInputMessageDAO inputMessageDAO join inputMessageDAO.variables variables WHERE variables.hjid in :hjids";
    private static final String QUERY_GET_PROCESS_VARIABLES_BY_PARTY_ID = "SELECT variables.hjid FROM ProcessVariablesDAO variables WHERE variables.initiatorID = :partyId OR variables.responderID = :partyId";

    private static final String QUERY_GET_PROCESS_VARIABLES = "SELECT variables FROM ProcessVariablesDAO variables WHERE variables.hjid in :hjids";
    private static final String QUERY_DELETE_INPUT_MESSAGE_BY_HJIDS = "DELETE FROM ProcessInstanceInputMessageDAO inputMessageDAO WHERE inputMessageDAO.hjid in :hjids";

    public static void deleteProcessInstanceInputMessageDAOAndProcessVariablesByPartyId(String partyId) {
        GenericJPARepository genericJPARepository = new JPARepositoryFactory().forBpRepository();
        List<Long> processVariableHjids = genericJPARepository.getEntities(QUERY_GET_PROCESS_VARIABLES_BY_PARTY_ID, new String[]{"partyId"}, new Object[]{partyId});
        if (processVariableHjids.size() > 0) {
            List<Long> inputMessageHjids = genericJPARepository.getEntities(QUERY_GET_BY_PARTY_ID, new String[]{"hjids"}, new Object[]{processVariableHjids});
            if (inputMessageHjids.size() > 0) {
                genericJPARepository.executeUpdate(QUERY_DELETE_INPUT_MESSAGE_BY_HJIDS, new String[]{"hjids"}, new Object[]{inputMessageHjids});
            }
            List<ProcessVariablesDAO> processVariablesDAOS = genericJPARepository.getEntities(QUERY_GET_PROCESS_VARIABLES, new String[]{"hjids"}, new Object[]{processVariableHjids});
            for (ProcessVariablesDAO processVariablesDAO : processVariablesDAOS) {
                genericJPARepository.deleteEntity(processVariablesDAO);
            }
        }
    }

}
