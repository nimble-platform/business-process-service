package eu.nimble.service.bp.impl.mock;

import eu.nimble.common.rest.delegate.IDelegateClient;
import eu.nimble.service.bp.impl.CollaborationGroupsController;
import eu.nimble.service.bp.impl.DocumentController;
import eu.nimble.service.bp.impl.DocumentsController;
import eu.nimble.service.bp.impl.ProcessInstanceGroupController;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Profile("test")
@Component
public class DelegateServiceClientMock implements IDelegateClient {

    @Autowired
    private CollaborationGroupsController collaborationGroupsController;
    @Autowired
    private DocumentController documentController;
    @Autowired
    private DocumentsController documentsController;
    @Autowired
    private ProcessInstanceGroupController processInstanceGroupController;

    @Override
    public Response addFederatedMetadataToCollaborationGroup(String bearerToken,String federationId, String documentId,String body,String partyId,String delegateId) {
        try {
            ResponseEntity responseEntity = collaborationGroupsController.addFederatedMetadataToCollaborationGroup(documentId,body,partyId,federationId,bearerToken);
            String response = JsonSerializationUtility.getObjectMapper().writeValueAsString(responseEntity.getBody());
            return Response.builder().headers(new HashMap<>()).status(responseEntity.getStatusCodeValue()).body(response,Charset.defaultCharset()).build();
        } catch (Exception e) {
            return Response.builder().headers(new HashMap<>()).status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(null,Charset.defaultCharset()).build();
        }

    }

    @Override
    public Response getGroupIdTuple(String bearerToken, String federationId, String documentId,String partyId,String delegateId) {
        try {
            ResponseEntity responseEntity = documentController.getGroupIdTuple(documentId,partyId,bearerToken,federationId);
            String response = JsonSerializationUtility.getObjectMapper().writeValueAsString(responseEntity.getBody());
            return Response.builder().headers(new HashMap<>()).status(responseEntity.getStatusCodeValue()).body(response,Charset.defaultCharset()).build();
        } catch (Exception e) {
            return Response.builder().headers(new HashMap<>()).status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(null,Charset.defaultCharset()).build();
        }
    }

    @Override
    public Response getOrderDocument(String bearerToken, String processInstanceId, String orderResponseId, String delegateId) {
        try {
            ResponseEntity responseEntity = processInstanceGroupController.getOrderDocument(processInstanceId,orderResponseId,bearerToken);
            return Response.builder().headers(new HashMap<>()).status(responseEntity.getStatusCodeValue()).body(responseEntity.getBody().toString(),Charset.defaultCharset()).build();
        } catch (Exception e) {
            return Response.builder().headers(new HashMap<>()).status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(null,Charset.defaultCharset()).build();
        }
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
    public Response getParty(String bearerToken, String partyIds,boolean includeRoles,List<String> delegateIds) {
        String response;
        try {
            List<PartyType> parties = SpringBridge.getInstance().getiIdentityClientTyped().getParties(bearerToken, Arrays.asList(partyIds.split(",")),false);
            response = JsonSerializationUtility.getObjectMapper().writeValueAsString(parties);
        } catch (IOException e) {
            response = null;
        }
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body(response,Charset.defaultCharset()).build();
    }

    @Override
    public Response getPerson(String bearerToken, String personId, String delegateId) {
        String response;
        try {
            PersonType personType = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken,personId);
            response = JsonSerializationUtility.getObjectMapper().writeValueAsString(personType);
        } catch (IOException e) {
            response = null;
        }
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body(response,Charset.defaultCharset()).build();
    }

    @Override
    public Response getPersonViaToken(String bearerToken, String tokenToBeChecked, String delegateId) {
        String response;
        try {
            PersonType personType = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(tokenToBeChecked);
            response = JsonSerializationUtility.getObjectMapper().writeValueAsString(personType);
        } catch (IOException e) {
            response = null;
        }
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body(response,Charset.defaultCharset()).build();
    }

    @Override
    public Response getPartyByPersonID(String bearerToken, String personId, String delegateId) {
        String response;
        try {
            PartyType partyType = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(personId).get(0);
            response = JsonSerializationUtility.getObjectMapper().writeValueAsString(partyType);
        } catch (IOException e) {
            response = null;
        }
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body(response,Charset.defaultCharset()).build();
    }

    @Override
    public Response getCollaborationGroup(String bearerToken,String id, String delegateId) {
        try {
            ResponseEntity responseEntity = collaborationGroupsController.getCollaborationGroup(id,bearerToken);
            String response = JsonSerializationUtility.getObjectMapper().writeValueAsString(responseEntity.getBody());
            return Response.builder().headers(new HashMap<>()).status(responseEntity.getStatusCodeValue()).body(response,Charset.defaultCharset()).build();
        } catch (Exception e) {
            return Response.builder().headers(new HashMap<>()).status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(null,Charset.defaultCharset()).build();
        }
    }

    @Override
    public Response unMergeCollaborationGroup(String bearerToken, String groupId, String delegateId) {
        try {
            ResponseEntity responseEntity = collaborationGroupsController.unMergeCollaborationGroup(groupId,bearerToken);
            String response = JsonSerializationUtility.getObjectMapper().writeValueAsString(responseEntity.getBody());
            return Response.builder().headers(new HashMap<>()).status(responseEntity.getStatusCodeValue()).body(response,Charset.defaultCharset()).build();
        } catch (Exception e) {
            return Response.builder().headers(new HashMap<>()).status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(null,Charset.defaultCharset()).build();
        }
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

    @Override
    public Response getExpectedOrders(String bearerToken,Boolean forAll, List<String> unShippedOrderIds) {
        try {
            ResponseEntity responseEntity = documentsController.getExpectedOrders(forAll,bearerToken,unShippedOrderIds);
            return Response.builder().headers(new HashMap<>()).status(responseEntity.getStatusCodeValue()).body(responseEntity.getBody().toString(),Charset.defaultCharset()).build();
        } catch (Exception e) {
            return Response.builder().headers(new HashMap<>()).status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(null,Charset.defaultCharset()).build();
        }
    }

    @Override
    public Response getFederationId() {
        return Response.builder().headers(new HashMap<>()).status(200).body("TEST_INSTANCE",Charset.defaultCharset()).build();
    }

}
