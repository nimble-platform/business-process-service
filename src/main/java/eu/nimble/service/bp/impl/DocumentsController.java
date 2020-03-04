package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.model.dashboard.ExpectedOrder;
import eu.nimble.service.bp.model.hyperjaxb.GroupStatus;
import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.bp.util.camunda.CamundaEngine;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.CataloguePersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.document.IDocument;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.validation.IValidationUtil;
import feign.Response;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog;

/**
 * Created by suat on 12-Sep-19.
 */
@Controller
public class DocumentsController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @ApiOperation(value = "", notes = "Retrieves the expected orders for a specific party or for all parties. " +
            "When an order contains some associated products, the seller should make orders for those products to complete the original order." +
            "Such orders are called expected orders. This service returns the expected orders belonging to unshipped orders,i.e an order is not followed by a fulfillment process." +
            "If the unshipped orders are provided to the service, it simply creates ExpectedOrders for the given ones.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved expected orders"),
            @ApiResponse(code = 401, message = "Invalid token or role"),
            @ApiResponse(code = 500, message = "Unexpected error while retrieving the expected orders")
    })
    @RequestMapping(value = "/documents/expected-orders",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<Object> getExpectedOrders(
            @ApiParam(value = "Flag indicating whether the expected orders will be retrieved for a specific party or all parties", required = false) @RequestParam(value = "forAll", required = false, defaultValue = "false") Boolean forAll,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
            @ApiParam(value = "Identifier of the unshipped orders for which the associated documents will be retrieved", required = false) @RequestParam(value = "unShippedOrderIds", required = false) List<String> unShippedOrderIds
    ) throws Exception{
        try {
            // set request log of ExecutionContext
            String requestLog = "Getting expected orders";
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if (!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }
            // unShippedOrderIds are given, so create expected orders for them
            if(unShippedOrderIds != null && unShippedOrderIds.size() > 0){
                List<ExpectedOrder> expectedOrders = new ArrayList<>();
                List<String> documentIds = ProcessDocumentMetadataDAOUtility.getAssociatedDocumentIDsForUnShippedOrders(unShippedOrderIds);
                for (String documentId : documentIds) {
                    ExpectedOrder expectedOrder = createExpectedOrderForDocument(documentId);
                    expectedOrders.add(expectedOrder);
                }
                logger.info("Retrieved expected orders for unshipped orders:{}",unShippedOrderIds);

                return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(expectedOrders));
            }

            // get unshipped order ids
            List<String> unshippedOrderIds = null;
            if(!forAll) {
                // get person using the given bearer token
                String partyId;
                String federationId;
                try {
                    PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
                    // get party for the person
                    PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);
                    partyId = party.getPartyIdentification().get(0).getID();
                    federationId = party.getFederationInstanceID();
                } catch (IOException e) {
                    throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_EXTRACT_PARTY_INFO.toString(),Arrays.asList(bearerToken),e);
                }

                unshippedOrderIds = ProcessDocumentMetadataDAOUtility.getUnshippedOrderIds(partyId,federationId);

            } else {
                unshippedOrderIds = ProcessDocumentMetadataDAOUtility.getUnshippedOrderIds();
            }

            // the expected orders
            List<ExpectedOrder> expectedOrders = new ArrayList<>();

            if(unshippedOrderIds.size() > 0){
                // following map stores the ExpectedOrders having an associated process
                // key of the following map is the identifier of product, i.e hjid of catalogue line
                // key of the inner map is the set of unshipped order ids and the value of that is the corresponding ExpectedOrder
                Map<BigDecimal,Map<Set<String>, ExpectedOrder>> expectedOrdersHavingProcessMap = new HashMap<>();

                // get Expected Orders for the unshippedOrderIds
                List<ExpectedOrder> expectedOrdersForUnshippedOrder;
                // if delegate service is running, use it to get ExpectedOrders
                if(SpringBridge.getInstance().isDelegateServiceRunning()){
                    Response response = SpringBridge.getInstance().getDelegateClient().getExpectedOrders(bearerToken,forAll,unshippedOrderIds);
                    String responseBody = eu.nimble.service.bp.util.HttpResponseUtil.extractBodyFromFeignClientResponse(response);
                    expectedOrdersForUnshippedOrder = JsonSerializationUtility.getObjectMapper().readValue(responseBody,new TypeReference<List<ExpectedOrder>>(){});
                }
                else {
                    ResponseEntity responseEntity = getExpectedOrders(forAll,bearerToken,unShippedOrderIds);
                    expectedOrdersForUnshippedOrder = (List<ExpectedOrder>) responseEntity.getBody();
                }
                // first, create ExpectedOrders for the ones having an associated process
                for (ExpectedOrder expectedOrder : expectedOrdersForUnshippedOrder) {
                    BigDecimal lineHjid = expectedOrder.getLineHjid();
                    List<String> unShippedOrderIdsHavingCorrespondingNegotiation = expectedOrder.getUnShippedOrderIds();
                    String state = expectedOrder.getState();

                    // for some unshipped orders, we might have more than one associated process
                    // in this case,all processes are cancelled expect for the last one and we create a single ExpectedOrder for the last one
                    // if all associated processes are cancelled, then we create a single ExpectedOrder for any of them
                    if(expectedOrdersHavingProcessMap.containsKey(lineHjid) && expectedOrdersHavingProcessMap.get(lineHjid).containsKey(new HashSet<>(unShippedOrderIdsHavingCorrespondingNegotiation))){
                        if(expectedOrdersHavingProcessMap.get(lineHjid).get(new HashSet<>(unShippedOrderIdsHavingCorrespondingNegotiation)).getState().contentEquals("CANCELLED") && !state.contentEquals("CANCELLED")){
                            expectedOrdersHavingProcessMap.get(lineHjid).put(new HashSet<>(unShippedOrderIdsHavingCorrespondingNegotiation),expectedOrder);
                        }
                    }
                    // add it to the map
                    else{
                        Map<Set<String>,ExpectedOrder> unShippedOrdersExpectedOrderMap = new HashMap<>();
                        unShippedOrdersExpectedOrderMap.put(new HashSet<>(unShippedOrderIdsHavingCorrespondingNegotiation),expectedOrder);
                        expectedOrdersHavingProcessMap.put(lineHjid.stripTrailingZeros(),unShippedOrdersExpectedOrderMap);
                        // add it to the list
                        expectedOrders.add(expectedOrder);
                    }
                }
                // for the rest, we do not have an associated process, therefore, group them according to their products and
                // create ExpectedOrder for each
                if(unshippedOrderIds.size() > 0){
                    // the key of the following map is the hjid of product
                    // the value of that is the list of corresponding unshipped orders ids
                    // we'll use this map to create ExpectedOrder for each key in the map using the corresponding unshipped orders
                    Map<BigDecimal,List<String>> productOrderMap = new HashMap<>();
                    // populate productOrderMap
                    for (String unshippedOrderId : unshippedOrderIds) {
                        // get document
                        IDocument iDocument = DocumentPersistenceUtility.getUBLDocument(unshippedOrderId);
                        for (ItemType item : iDocument.getItemTypes()) {
                            for (ItemPropertyType itemPropertyType : item.getAdditionalItemProperty()) {
                                if(itemPropertyType.getAssociatedCatalogueLineID().size() > 0){
                                    for (BigDecimal catalogueHjid : itemPropertyType.getAssociatedCatalogueLineID()) {
                                        // we have this product in the map, therefore extend corresponding unshipped order ids with the new one
                                        if(productOrderMap.containsKey(catalogueHjid)){
                                            List<String> orderIds = productOrderMap.get(catalogueHjid);
                                            orderIds.add(unshippedOrderId);
                                            productOrderMap.put(catalogueHjid, orderIds);
                                        }
                                        // add product to map
                                        else{
                                            productOrderMap.put(catalogueHjid, new ArrayList<>(Arrays.asList(unshippedOrderId)));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // for each product, create an ExpectedOrder
                    for (BigDecimal catalogueLineHjid : productOrderMap.keySet()) {
                        // get unshipped order ids
                        List<String> unShippedOrdersIdsForProduct = productOrderMap.get(catalogueLineHjid);
                        // in some cases, we might have expected order having an associated process for this product as well
                        // therefore, we need to update associated unshipped order ids for this product to have a correct association
                        // between the expected orders and sales orders
                        if(expectedOrdersHavingProcessMap.containsKey(catalogueLineHjid.stripTrailingZeros())){
                            Set<Set<String>> keys = expectedOrdersHavingProcessMap.get(catalogueLineHjid.stripTrailingZeros()).keySet();
                            for (Set<String> key : keys) {
                                unShippedOrdersIdsForProduct.removeAll(key);
                            }
                        }
                        // continue if there is no associated unshipped orders
                        if(unShippedOrdersIdsForProduct.size() == 0){
                            continue;
                        }
                        // create ExpectedOrder and add it to the list
                        ExpectedOrder expectedOrder = new ExpectedOrder();
                        expectedOrder.setProcessType(null);
                        expectedOrder.setResponseMetadata(null);
                        expectedOrder.setState(null);
                        expectedOrder.setUnShippedOrderIds(unShippedOrdersIdsForProduct);
                        expectedOrder.setLineHjid(catalogueLineHjid);

                        expectedOrders.add(expectedOrder);
                    }
                }
            }
            logger.info("Retrieved expected orders for for all: {}", forAll);

            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(expectedOrders));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_EXPECTED_ORDERS.toString(),e);
        }
    }

    private ExpectedOrder createExpectedOrderForDocument(String documentId){
        IDocument iDocument = DocumentPersistenceUtility.getUBLDocument(documentId);
        // get unshipped order references
        List<String> unShippedOrderIdsHavingCorrespondingNegotiation = getUnShippedOrderReferences(iDocument.getAdditionalDocuments());
        // get the corresponding process document metadata
        ProcessDocumentMetadataDAO processDocumentMetadataDAO = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
        // get the process instance group containing the process
        List<ProcessInstanceGroupDAO> groups = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAOs(processDocumentMetadataDAO.getProcessInstanceID());
        // get the latest process instance in the group
        String latestProcessInstanceID = groups.get(0).getProcessInstanceIDs().get(groups.get(0).getProcessInstanceIDs().size()-1);
        // get the response DocumentMetadata if exists
        ProcessDocumentMetadata responseDocumentMetadata = null;
        List<ProcessDocumentMetadataDAO> processDocumentMetadataDAOS = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(latestProcessInstanceID);
        if(processDocumentMetadataDAOS.size() > 1){
            for (ProcessDocumentMetadataDAO documentMetadataDAO : processDocumentMetadataDAOS) {
                if(!documentMetadataDAO.getDocumentID().contentEquals(documentId)){
                    responseDocumentMetadata = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(documentMetadataDAO);
                }
            }
        }
        // if the collaboration is cancelled, state should be 'CANCELLED'
        // otherwise, we can simply use the state of process coming from the Camunda
        String state = null;
        HistoricProcessInstance historicProcessInstance = CamundaEngine.getProcessInstance(latestProcessInstanceID);
        if(groups.get(0).getStatus().equals(GroupStatus.CANCELLED) && groups.get(1).getStatus().equals(GroupStatus.CANCELLED)){
            state = "CANCELLED";
        }
        else{
            state = historicProcessInstance.getState();
        }
        // product id
        BigDecimal lineHjid = BigDecimal.valueOf(CataloguePersistenceUtility.getCatalogueLineHjid(iDocument.getItemTypes().get(0).getCatalogueDocumentReference().getID(),iDocument.getItemTypes().get(0).getManufacturersItemIdentification().getID())).stripTrailingZeros();

        // create ExpectedOrder
        ExpectedOrder expectedOrder = new ExpectedOrder();
        expectedOrder.setProcessType(historicProcessInstance.getProcessDefinitionKey());
        expectedOrder.setResponseMetadata(responseDocumentMetadata);
        expectedOrder.setState(state);
        expectedOrder.setUnShippedOrderIds(unShippedOrderIdsHavingCorrespondingNegotiation);
        expectedOrder.setProcessInstanceId(latestProcessInstanceID);
        expectedOrder.setLineHjid(lineHjid);

        return expectedOrder;
    }

    private List<String> getUnShippedOrderReferences(List<DocumentReferenceType> documentReferences){
        List<String> unShippedOrderIds = new ArrayList<>();
        for (DocumentReferenceType documentReference : documentReferences) {
            if(documentReference.getDocumentType().contentEquals("unShippedOrder")){
                unShippedOrderIds.add(documentReference.getID());
            }
        }
        return unShippedOrderIds;
    }

}
