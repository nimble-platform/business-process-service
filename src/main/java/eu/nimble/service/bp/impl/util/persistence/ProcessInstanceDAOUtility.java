package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CompletedTaskType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;

import java.util.List;

/**
 * Created by suat on 18-Oct-18.
 */
public class ProcessInstanceDAOUtility {
    private static final String GET_COMPLETED_TASK_QUERY = "" +
            "SELECT completedTask FROM QualifyingPartyType qParty join qParty.completedTask completedTask WHERE qParty.party.ID = ? AND completedTask.associatedProcessInstanceID = ?";

    public static CompletedTaskType getCompletedTask(String partyId, String processInstanceId) {
        CompletedTaskType completedTask = HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).load(GET_COMPLETED_TASK_QUERY, partyId, processInstanceId);
        return completedTask;
    }
}
