package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.nimble.service.bp.bom.BPMessageGenerator;
import eu.nimble.service.bp.model.hyperjaxb.CollaborationGroupDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.util.bp.BusinessProcessUtility;
import eu.nimble.service.bp.util.bp.ClassProcessTypeMap;
import eu.nimble.service.bp.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.document.IDocument;
import eu.nimble.service.model.ubl.iteminformationresponse.ItemInformationResponseType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.ppapresponse.PpapResponseType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.receiptadvice.ReceiptAdviceType;
import eu.nimble.service.model.ubl.transportexecutionplan.TransportExecutionPlanType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.serialization.IDocumentDeserializer;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;

@ApiIgnore
@Controller
public class EFactoryExternalAPIController {
    private final Logger logger = LoggerFactory.getLogger(EFactoryExternalAPIController.class);

    @Autowired
    private StartController startController;
    @Autowired
    private ContinueController continueController;

    // TODO: we need to get a token for business-process service
    private final String token = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxYnNrM09PZkNzdWF0LXV1X0lqU2JxX2QwMmtZM2NteXJheUpXeE93MmlZIn0.eyJqdGkiOiJmMzEzOGMzOS1mMWM4LTRmMDYtOGJkZC0zMzBiM2I4ZmE2NTYiLCJleHAiOjE1MzAwODg3MzQsIm5iZiI6MCwiaWF0IjoxNTMwMDAyMzM0LCJpc3MiOiJodHRwOi8va2V5Y2xvYWs6ODA4MC9hdXRoL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOiJuaW1ibGVfY2xpZW50Iiwic3ViIjoiMWVlNmIyNzEtM2MyMy00YTZiLWJlMTktYmI3ZWJmNjVlYTVjIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibmltYmxlX2NsaWVudCIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6ImIyMmQyZDE5LTNhY2ItNDUyMC1iNWFlLTdkOGU2MGQ3ODQ4YyIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsibGVnYWxfcmVwcmVzZW50YXRpdmUiLCJuaW1ibGVfdXNlciIsImluaXRpYWxfcmVwcmVzZW50YXRpdmUiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sIm5hbWUiOiJhbGkgY2FuIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiY2FuQGdtYWlsLmNvbSIsImdpdmVuX25hbWUiOiJhbGkiLCJmYW1pbHlfbmFtZSI6ImNhbiIsImVtYWlsIjoiY2FuQGdtYWlsLmNvbSJ9.Un1K0t37Ln3VN51i-Is_";

    @ApiOperation(value = "",notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = ""),
            @ApiResponse(code = 400, message = "")
    })
    @RequestMapping(value = "/process-document",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity processDocument(@ApiParam(value = "Serialized form of the document", required = true) @RequestBody String documentAsString) {
        logger.info("Getting request to process document");

        /**
         * Deserialize the given document using the custom deserializer {@link IDocumentDeserializer}
         * */
        // get the ObjectMapper
        ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();

        // add deserializer to be able deserialize IDocument instances properly
        SimpleModule module = new SimpleModule();
        module.addDeserializer(IDocument.class, new IDocumentDeserializer());
        mapper.registerModule(module);

        IDocument document;
        try {
            document = mapper.readValue(documentAsString,IDocument.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize document",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to deserialize document");
        }

        /**
         * Check whether the seller party exists or not
         * */
        String sellerPartyId = document.getSellerPartyId();
        try {
            PartyType sellerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(token,sellerPartyId);

            if(sellerParty == null){
                String msg = String.format("There does not exist a party for : %s", sellerPartyId);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }
        } catch (IOException e) {
            String msg = String.format("Failed to retrieve party information for : %s", sellerPartyId);
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }

        /**
         * Start or complete the business process instance
         * if the given document is a request document, use {@link StartController#startProcessInstance(String, ProcessInstanceInputMessage, String, String, String)} to start a process instance.
         * else, use {@link ContinueController#continueProcessInstance(ProcessInstanceInputMessage, String, String, String)} to complete the process instance.
         * */
        // check whether it is a request or response document
        boolean isInitialDocument = BusinessProcessUtility.isInitialDocument(document.getClass());

        ProcessInstance processInstance;
        if(isInitialDocument){
            // create ProcessInstanceInputMessage
            ProcessInstanceInputMessage processInstanceInputMessage;
            try {
                processInstanceInputMessage = BPMessageGenerator.createProcessInstanceInputMessage(document,document.getItemType(),null,"",token);
            } catch (Exception e) {
                String msg = "Failed to create process instance input message for the document";
                logger.error(msg,e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
            }

            processInstance = startController.startProcessInstance(token,processInstanceInputMessage,null,null,null).getBody();
        }
        else{
            // to complete the process, we need to know process instance id
            String processInstanceId = getProcessInstanceId(document);

            // create ProcessInstanceInputMessage
            ProcessInstanceInputMessage processInstanceInputMessage;
            try {
                processInstanceInputMessage = BPMessageGenerator.createProcessInstanceInputMessage(document,document.getItemType(),null,processInstanceId,token);
            } catch (Exception e) {
                String msg = "Failed to create process instance input message for the document";
                logger.error(msg,e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
            }

            // get the collaboration group containing the process instance for the responder party
            CollaborationGroupDAO collaborationGroup = CollaborationGroupDAOUtility.getCollaborationGroupDAO(processInstanceInputMessage.getVariables().getResponderID(),processInstanceId);

            // get the identifier of process instance group containing the process instance
            String gid = null;
            for(ProcessInstanceGroupDAO processInstanceGroupDAO:collaborationGroup.getAssociatedProcessInstanceGroups()){
                if(processInstanceGroupDAO.getProcessInstanceIDs().contains(processInstanceId)){
                    gid = processInstanceGroupDAO.getID();
                    break;
                }
            }

            processInstance = continueController.continueProcessInstance(processInstanceInputMessage,gid,collaborationGroup.getHjid().toString(),token).getBody();

            /**
             * Send response document to the initiator party
             * */
            // get corresponding process type
            String processId = ClassProcessTypeMap.getProcessType(document.getClass());
            // get the initiator party id
            PartyType initiatorParty = document.getBuyerParty();
            // for Fulfilment, it's vice versa
            if(processId.contentEquals("Fulfilment")){
                initiatorParty = document.getSellerParty();
            }

            // get the initiator party
            try {
                if(initiatorParty.getContact().getOtherCommunication().size() > 0 && initiatorParty.getContact().getOtherCommunication().get(0).getChannelCode().getName().contentEquals("REST")){
                    String endpoint = initiatorParty.getContact().getOtherCommunication().get(0).getChannelCode().getValue();
                    HttpResponse<String> response = Unirest.post(endpoint)
                            .body(documentAsString)
                            .asString();

                    if(response.getStatus() != 200){
                        logger.error("Failed send the document to the initiator party {}, endpoint: {} : {}",initiatorParty, endpoint, response.getBody());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                    }
                }
            } catch (Exception e) {
                String msg = String.format("Failed to send the document to the initiator party : %s", initiatorParty);
                logger.error(msg,e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }

        logger.info("Completed the request to process document");
        return ResponseEntity.ok(processInstance);
    }

    private String getProcessInstanceId(IDocument responseDocument){
        // get the request document id
        String requestDocumentId = responseDocument.getRequestDocumentId();

        // get the process instance id
        return ProcessDocumentMetadataDAOUtility.findByDocumentID(requestDocumentId).getProcessInstanceID();
    }

}