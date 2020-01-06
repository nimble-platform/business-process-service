package eu.nimble.service.bp.impl.mock;

import eu.nimble.common.rest.delegate.IDelegateClient;
import eu.nimble.service.bp.impl.CollaborationGroupsController;
import eu.nimble.service.bp.impl.DocumentController;
import eu.nimble.service.bp.impl.ProcessInstanceGroupController;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import feign.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;

@Profile("test")
@Component
public class DelegateServiceClientMock implements IDelegateClient {

    @Autowired
    private CollaborationGroupsController collaborationGroupsController;
    @Autowired
    private DocumentController documentController;
    @Autowired
    private ProcessInstanceGroupController processInstanceGroupController;

    @Override
    public Response addFederatedMetadataToCollaborationGroup(String bearerToken,String federationId, String documentId,String body,String partyId,String delegateId) {
        ResponseEntity responseEntity = collaborationGroupsController.addFederatedMetadataToCollaborationGroup(documentId,body,partyId,federationId,bearerToken);
        String response;
        try {
            response = JsonSerializationUtility.getObjectMapper().writeValueAsString(responseEntity.getBody());
        } catch (IOException e) {
            response = null;
        }
        return Response.builder().headers(new HashMap<>()).status(responseEntity.getStatusCodeValue()).body(response,Charset.defaultCharset()).build();
    }

    @Override
    public Response getGroupIdTuple(String bearerToken, String federationId, String documentId,String partyId,String delegateId) {
        ResponseEntity responseEntity = documentController.getGroupIdTuple(documentId,partyId,bearerToken,federationId);
        String response;
        try {
            response = JsonSerializationUtility.getObjectMapper().writeValueAsString(responseEntity.getBody());
        } catch (IOException e) {
            response = null;
        }
        return Response.builder().headers(new HashMap<>()).status(responseEntity.getStatusCodeValue()).body(response,Charset.defaultCharset()).build();
    }

    @Override
    public Response getOrderDocument(String bearerToken, String processInstanceId, String orderResponseId, String delegateId) {
        ResponseEntity responseEntity = processInstanceGroupController.getOrderDocument(processInstanceId,orderResponseId,bearerToken);
        return Response.builder().headers(new HashMap<>()).status(responseEntity.getStatusCodeValue()).body(responseEntity.getBody().toString(),Charset.defaultCharset()).build();
    }

    @Override
    public Response getParty(String bearerToken,Long partyId, boolean includeRoles,String delegateId) {
        String response;
        try {
            PartyType partyType = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,partyId.toString(),includeRoles);
            response = JsonSerializationUtility.getObjectMapper().writeValueAsString(partyType);
        } catch (IOException e) {
            response = null;
        }
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body(response,Charset.defaultCharset()).build();
    }

    @Override
    public Response getCollaborationGroup(String bearerToken,String id, String delegateId) {
        ResponseEntity responseEntity = collaborationGroupsController.getCollaborationGroup(id,bearerToken);
        String response;
        try {
            response = JsonSerializationUtility.getObjectMapper().writeValueAsString(responseEntity.getBody());
        } catch (IOException e) {
            response = null;
        }
        return Response.builder().headers(new HashMap<>()).status(responseEntity.getStatusCodeValue()).body(response,Charset.defaultCharset()).build();
    }

    @Override
    public Response unMergeCollaborationGroup(String bearerToken, String groupId, String delegateId) {
        ResponseEntity responseEntity = collaborationGroupsController.unMergeCollaborationGroup(groupId,bearerToken);
        String response;
        try {
            response = JsonSerializationUtility.getObjectMapper().writeValueAsString(responseEntity.getBody());
        } catch (IOException e) {
            response = null;
        }
        return Response.builder().headers(new HashMap<>()).status(responseEntity.getStatusCodeValue()).body(response,Charset.defaultCharset()).build();
    }

    @Override
    public Response getCatalogLineByHjid(String bearerToken,Long hjid) {
        CatalogueLineType originalProduct = new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntityByHjid(CatalogueLineType.class, hjid);
        String response;
        try {
            response = JsonSerializationUtility.getObjectMapper().writeValueAsString(originalProduct);
        } catch (IOException e) {
            response = null;
        }
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body(response,Charset.defaultCharset()).build();
    }

}
