package eu.nimble.service.bp.impl.util;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.utility.HibernateUtility;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MigrationUtil {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MigrationUtil.class);
    private static final String environment = "local";

    // connection parameters for 'local'
    private static String url;
    private static String username;
    private static String password;

    public static void main(String[] args) throws Exception{
        try {
            HibernateUtility hu = HibernateUtility.getInstance("bp-data-model", getConfigs());

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

        // set association between collaboration groups
        for(ProcessInstanceGroupDAO groupDAO: groupDAOS){
            // get collaboration group of the group
            query = "select cg from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups apig where apig.ID = '"+groupDAO.getID()+"'";
            CollaborationGroupDAO collaborationGroupDAO = (CollaborationGroupDAO) hibernateUtility.loadIndividualItem(query);
            // get collaboration groups' hjids which have a reference to the group
            query = "select cg.hjid from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups apig where apig.ID in " +
                    "(select pig.ID from ProcessInstanceGroupDAO pig join pig.associatedGroupsItems agitem where agitem.item = '" + groupDAO.getID()+"')";
            List<Long> associatedCollaborationGroupIds = (List<Long>) hibernateUtility.loadAll(query);

            collaborationGroupDAO.setAssociatedCollaborationGroups(associatedCollaborationGroupIds);

            hibernateUtility.update(collaborationGroupDAO);
        }

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
        group.setAssociatedGroups(Arrays.asList(associatedGroup));
        hibernateUtility.persist(group);
        return group;
    }

    private static Map getConfigs(){
        Map map = new HashMap();

        if(environment.equals("local")){
            url = "jdbc:postgresql://localhost:5432/businessprocessengine?currentSchema=public";
            username = "postgres";
            password = "nimble";

            map.put("hibernate.connection.url",url);
            map.put("hibernate.connection.username",username);
            map.put("hibernate.connection.password",password);
            map.put("hibernate.enable_lazy_load_no_trans",true);
        }

        return map;
    }
}