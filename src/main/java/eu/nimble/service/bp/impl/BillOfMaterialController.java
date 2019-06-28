package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.nimble.common.rest.identity.model.NegotiationSettings;
import eu.nimble.service.bp.impl.bom.BPMessageGenerator;
import eu.nimble.service.bp.impl.util.HttpResponseUtil;
import eu.nimble.service.bp.impl.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.utility.JsonSerializationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@Controller
public class BillOfMaterialController {

    private final Logger logger = LoggerFactory.getLogger(BillOfMaterialController.class);

    @Autowired
    private StartController startController;
    @Autowired
    private CollaborationGroupsController collaborationGroupsController;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Creates negotiations for the given line items for the given party. If there is a frame contract between parties and useFrameContract parameter is set to true, " +
            "then the service creates an order for the product using the details of frame contract.The person who starts the process is saved as the creator of process. This information is derived " +
            "from the given bearer token.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created negotiations for line items for the given party"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while creating negotiations for line items for the given party")
    })
    @RequestMapping(value = "/billOfMaterial",
            method = RequestMethod.POST,
            produces = {"application/json"})
    public ResponseEntity createNegotiationsForLineItems(@ApiParam(value = "Serialized form of line items which are used to create request for quotations.An example line items serialization can be found in:" +
                                                    "https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/line_items.json", required = true) @RequestBody String lineItemsJson,
                                                         @ApiParam(value = "Identifier of the party which creates negotiations for Bill of Materials", required = true) @RequestParam(value = "partyId") String partyId,
                                                         @ApiParam(value = "If this parameter is true and a valid frame contract exists between parties, then an order is started for the product using the details of frame contract") @RequestParam(value = "useFrameContract", defaultValue = "false") Boolean useFrameContract,
                                                         @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        logger.info("Creating negotiations for line items for party: {}, useFrameContract: {}", partyId, useFrameContract);
        // check token
        ResponseEntity tokenCheck = HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        try {
            // deserialize LineItems
            List<LineItemType> lineItems = JsonSerializationUtility.getObjectMapper().readValue(lineItemsJson, new TypeReference<List<LineItemType>>() {
            });

            // get buyer party and its negotiation settings
            NegotiationSettings negotiationSettings = SpringBridge.getInstance().getiIdentityClientTyped().getNegotiationSettings(partyId);
            // get person using the given bearer token
            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);

            String hjidOfBaseGroup = null;
            List<String> hjidOfGroupsToBeMerged = new ArrayList<>();

            // for each line item, create a RFQ
            for (LineItemType lineItem : lineItems) {
                // create ProcessInstanceInputMessage for line item
                ProcessInstanceInputMessage processInstanceInputMessage = BPMessageGenerator.createBPMessageForLineItem(lineItem, useFrameContract, partyId, negotiationSettings, person.getID(), bearerToken);
                // start the process and get process instance id since we need this info to find collaboration group of process
                String processInstanceId = startController.startProcessInstance(bearerToken, processInstanceInputMessage, null, null, null, null).getBody().getProcessInstanceID();

                if (hjidOfBaseGroup == null) {
                    hjidOfBaseGroup = CollaborationGroupDAOUtility.getCollaborationGroupHjidByProcessInstanceIdAndPartyId(processInstanceId, partyId).toString();
                } else {
                    hjidOfGroupsToBeMerged.add(CollaborationGroupDAOUtility.getCollaborationGroupHjidByProcessInstanceIdAndPartyId(processInstanceId, partyId).toString());
                }
            }

            // merge groups to create a project
            if (hjidOfGroupsToBeMerged.size() > 0) {
                collaborationGroupsController.mergeCollaborationGroups(bearerToken, hjidOfBaseGroup, hjidOfGroupsToBeMerged);
            }
        } catch (Exception e) {
            String msg = String.format("Unexpected error while creating negotiations for line items for party : %s", partyId);
            logger.error(msg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }

        logger.info("Created negotiations for line items for party: {}, useFrameContract: {}", partyId, useFrameContract);
        return ResponseEntity.ok(null);
    }


}
