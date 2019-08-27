package eu.nimble.service.bp.util;

import eu.nimble.service.bp.model.hyperjaxb.CollaborationGroupDAO;
import eu.nimble.service.bp.model.hyperjaxb.CollaborationStatus;
import eu.nimble.service.bp.model.hyperjaxb.GroupStatus;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.utility.HibernateUtility;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.*;

public class MigrationUtil {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MigrationUtil.class);

    public static void main(String[] args) {
        MigrationUtil script = new MigrationUtil();
        try {
            HibernateUtility hu = HibernateUtility.getInstance("bp-data-model", script.getConfigs());

            createCollaborationGroups(hu);

            System.exit(0);

        } catch (Exception e) {
            logger.error("Failure", e);
        }
    }

    private static void createCollaborationGroups(HibernateUtility hibernateUtility){
        String query = "from ProcessInstanceGroupDAO pig";
        List<ProcessInstanceGroupDAO> groupDAOS = (List<ProcessInstanceGroupDAO>) hibernateUtility.loadAll(query);
        logger.info("Process instance group daos obtained");

        // for each group,create a collaboration group
        for (ProcessInstanceGroupDAO groupDAO: groupDAOS){
            CollaborationGroupDAO collaborationGroupDAO = new CollaborationGroupDAO();
            collaborationGroupDAO.setArchived(groupDAO.isArchived());
            collaborationGroupDAO.setStatus(CollaborationStatus.INPROGRESS);
            collaborationGroupDAO.getAssociatedProcessInstanceGroups().add(groupDAO);

            hibernateUtility.update(collaborationGroupDAO);
            logger.info("Collaboration group is created for process instance group: {}",groupDAO.getID());
        }
        logger.info("Collaboration groups are created for all process instance groups.");

//        // create groups and association between groups
//        for (ProcessInstanceGroupDAO groupDAO: groupDAOS){
//            // get list of process instance ids as string
//            String listOfProcessInstanceId = "(";
//            int size = groupDAO.getProcessInstanceIDs().size();
//            for (int i = 0; i < size;i++){
//                if(i == size-1){
//                    listOfProcessInstanceId += "'"+groupDAO.getProcessInstanceIDs().get(i)+"')";
//                }
//                else {
//                    listOfProcessInstanceId += "'"+groupDAO.getProcessInstanceIDs().get(i)+"',";
//                }
//            }
//            // get process instance daos
//            query = "select instanceDAO from ProcessInstanceDAO instanceDAO where instanceDAO.processInstanceID in " + listOfProcessInstanceId;
//            List<ProcessInstanceDAO> processInstanceDAOS = (List<ProcessInstanceDAO>) hibernateUtility.loadAll(query);
//
//            // get number of groups needed
//            int numOfGroupsNeeded = 0;
//            for(ProcessInstanceDAO processInstanceDAO: processInstanceDAOS){
//                if(processInstanceDAO.getPrecedingProcess() == null){
//                    numOfGroupsNeeded++;
//                }
//            }
//            // we will use the existing group as the first one
//            numOfGroupsNeeded--;
//            for(int i = 0; i < numOfGroupsNeeded ; i++){
//
//                // get process instance ids which the group will have
//                List<String> processInstanceIDs = new ArrayList<>();
//                boolean firstOne = true;
//                for(ProcessInstanceDAO processInstanceDAO: processInstanceDAOS){
//                    if(processInstanceDAO.getPrecedingProcess() == null){
//                        // the original group will have the first process instance
//                        if(firstOne){
//                            firstOne = false;
//                            continue;
//                        }
//                        else {
//                            processInstanceIDs.add(processInstanceDAO.getProcessInstanceID());
//                        }
//                    }
//                    else if(processInstanceIDs.contains(processInstanceDAO.getProcessInstanceID())){
//                        processInstanceIDs.add(processInstanceDAO.getProcessInstanceID());
//                    }
//                }
//
//                // remove these process instance ids from the original
//                groupDAO.getProcessInstanceIDs().removeAll(processInstanceIDs);
//
//                // create process instance dao
//                ProcessInstanceGroupDAO processInstanceGroupDAO = createProcessInstanceDao(hibernateUtility,groupDAO.isArchived(),groupDAO.getName(), groupDAO.getPartyID(),
//                        groupDAO.getCollaborationRole(),processInstanceIDs,groupDAO.getAssociatedGroups().get(1),groupDAO.getStatus());
//
//
//                // TODO: set preceding process
//                //processInstanceGroupDAO.setPrecedingProcess();
//                processInstanceGroupDAO.setPrecedingProcessInstanceGroup(groupDAO);
//
//                // remove association group from the original one
//                groupDAO.getAssociatedGroups().remove(1);
//
//                // using the process instances, set association between groups
//                for(String processInstanceId: processInstanceIDs){
//                    query = "select pig from ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems idItems join pig.associatedGroupsItems agItems " +
//                            "where '"+processInstanceId+"' = idItems.item and '"+groupDAO.getID()+"' = agItems.item";
//                    List<ProcessInstanceGroupDAO> groupDAOList = (List<ProcessInstanceGroupDAO>) hibernateUtility.loadAll(query);
//                    // delete the reference to the original group and instead, add a reference to the new group
//                    for (ProcessInstanceGroupDAO instanceGroupDAO:groupDAOList){
//                        instanceGroupDAO.getAssociatedGroups().remove(groupDAO.getID());
//                        instanceGroupDAO.getAssociatedGroups().add(processInstanceGroupDAO.getID());
//
//                        hibernateUtility.update(instanceGroupDAO);
//                    }
//                }
//
//                hibernateUtility.update(groupDAO);
//                hibernateUtility.update(processInstanceGroupDAO);
//                // add the new group to collaboration group
//                query = "select cg from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups apig where apig.ID = '"+groupDAO.getID()+"'";
//                CollaborationGroupDAO collaborationGroupDAO = (CollaborationGroupDAO) hibernateUtility.loadIndividualItem(query);
//                collaborationGroupDAO.getAssociatedCollaborationGroups().add(processInstanceGroupDAO.getHjid());
//                hibernateUtility.update(collaborationGroupDAO);
//            }
//        }
    }

    private static ProcessInstanceGroupDAO createProcessInstanceDao(HibernateUtility hibernateUtility,boolean archived,String relatedProducts, String partyId, String collaborationRole,
                                                                    List<String> processInstanceIds,String associatedGroup,GroupStatus groupStatus){
        String uuid = UUID.randomUUID().toString();
        ProcessInstanceGroupDAO group = new ProcessInstanceGroupDAO();
        group.setArchived(archived);
        group.setID(uuid);
        group.setName(relatedProducts);
        group.setPartyID(partyId);
        group.setStatus(groupStatus);
        group.setCollaborationRole(collaborationRole);
        group.setProcessInstanceIDs(processInstanceIds);
        hibernateUtility.persist(group);
        return group;
    }

    private Map getConfigs(){
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        try {
            PropertySource<?> applicationYamlPropertySource = loader.load(
                    "properties", new ClassPathResource("releases.r6/r6migration.yml"), null);

            Map map = ((MapPropertySource) applicationYamlPropertySource).getSource();

            String url = (String) map.get("hibernate.connection.url");
            url = url.replace("${DB_HOST}",System.getenv("DB_HOST")).replace("${DB_PORT}",System.getenv("DB_PORT"));

            map.put("hibernate.connection.url",url);
            map.put("hibernate.connection.username",System.getenv("DB_USERNAME"));
            map.put("hibernate.connection.password",System.getenv("DB_PASSWORD"));

            return map;

        } catch (IOException e) {
            logger.error("", e);
            throw new RuntimeException();
        }
    }
}