package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ApplicationExecutionType;
import eu.nimble.service.bp.hyperjaxb.model.ApplicationType;
import eu.nimble.service.bp.hyperjaxb.model.ExecutionConfigurationDAO;
import eu.nimble.service.bp.hyperjaxb.model.TransactionConfigurationDAO;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import org.camunda.bpm.engine.ProcessEngines;

/**
 * Created by suat on 04-Jan-19.
 */
public class ExecutionConfigurationDAOUtility {
    public static ExecutionConfiguration getExecutionConfiguration(String partnerID, String processID, ProcessConfiguration.RoleTypeEnum roleType, String transactionID, ExecutionConfiguration.ApplicationTypeEnum applicationType) {
        String processKey = ProcessEngines.getDefaultProcessEngine().getRepositoryService().getProcessDefinition(processID).getKey();

        TransactionConfigurationDAO transactionConfigurationDAO = null;
        ExecutionConfigurationDAO executionConfigurationDAO = null;
        // Get buyer order application preference for data adapter
        /*ProcessConfigurationDAO processConfiguration = DAOUtility.getProcessConfiguration(partnerID, processKey, roleType);

        if (processConfiguration != null) { // it is configured
            List<TransactionConfigurationDAO> configurations = processConfiguration.getTransactionConfigurations();
            for (TransactionConfigurationDAO configuration : configurations) {
                if(configuration.getTransactionID().equals(transactionID)) {
                    transactionConfigurationDAO = configuration;
                    for (ExecutionConfigurationDAO executionConfiguration : configuration.getExecutionConfigurations()) {
                        if (executionConfiguration.getApplicationType().value().equals(applicationType.toString())) {
                            executionConfigurationDAO = executionConfiguration;
                            break;
                        }
                    }
                }
            }
        } else { // it is not configured by the partner
            */
        transactionConfigurationDAO = new TransactionConfigurationDAO();
        // TODO: Retrieve it from the identity service or context
        transactionConfigurationDAO.setTransactionID(transactionID);

        executionConfigurationDAO = new ExecutionConfigurationDAO();
        executionConfigurationDAO.setExecutionUri("eu.nimble.service.bp.application.ubl.UBLDataAdapterApplication");
        executionConfigurationDAO.setExecutionType(ApplicationExecutionType.JAVA);
        executionConfigurationDAO.setApplicationType(ApplicationType.fromValue(applicationType.toString()));
        transactionConfigurationDAO.getExecutionConfigurations().add(executionConfigurationDAO);
        //}

        ExecutionConfiguration executionConfiguration = HibernateSwaggerObjectMapper.createExecutionConfiguration(executionConfigurationDAO);
        return executionConfiguration;
    }
}
