package eu.nimble.service.bp.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class BusinessProcessContextHandler {
    private Map<String,BusinessProcessContext> businessProcessContextMap;
    public static BusinessProcessContextHandler businessProcessContextHandler;

    public static BusinessProcessContextHandler getBusinessProcessContextHandler(){
        if(businessProcessContextHandler == null){
            businessProcessContextHandler = new BusinessProcessContextHandler();
            businessProcessContextHandler.businessProcessContextMap = new HashMap<>();
        }
        return businessProcessContextHandler;
    }

    public BusinessProcessContext getBusinessProcessContext(String id) {
        BusinessProcessContext businessProcessContext;

        // if id is null, then create a new BusinessProcessContext and return it
        // otherwise return the corresponding BusinessProcessContext
        if(id == null){
            String createdId = UUID.randomUUID().toString();
            while (businessProcessContextMap.containsKey(createdId)){
                createdId = UUID.randomUUID().toString();
            }
            BusinessProcessContext businessProcessContext1 = new BusinessProcessContext();
            businessProcessContext1.setId(createdId);
            businessProcessContextMap.put(createdId,businessProcessContext1);

            businessProcessContext = businessProcessContext1;
        }
        else {
            businessProcessContext = businessProcessContextMap.get(id);
        }
        return businessProcessContext;
    }

    public void deleteBusinessProcessContext(String id){
        businessProcessContextMap.remove(id);
    }
}
