package eu.nimble.service.bp.util.migration.r13;

import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import eu.nimble.service.model.ubl.despatchadvice.DespatchAdviceType;
import eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType;
import eu.nimble.service.model.ubl.iteminformationresponse.ItemInformationResponseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.ppaprequest.PpapRequestType;
import eu.nimble.service.model.ubl.ppapresponse.PpapResponseType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.receiptadvice.ReceiptAdviceType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.service.model.ubl.transportexecutionplan.TransportExecutionPlanType;
import eu.nimble.service.model.ubl.transportexecutionplanrequest.TransportExecutionPlanRequestType;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApiIgnore
@Controller
public class R13MigrationController {

    @Autowired
    private DataSource camunda;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String QUERY_GET_DUPLICATE_IDS = "SELECT ID FROM %s GROUP BY ID HAVING count(ID) > 1";
    private final String QUERY_GET_DOCUMENTS_BY_ID = "FROM %s WHERE ID = :id";
    private final String QUERY_GET_DUPLICATE_METADATA_IDS = "SELECT documentID FROM ProcessDocumentMetadataDAO GROUP BY documentID HAVING count(documentID) > 1";
    private final String QUERY_GET_DUPLICATE_METADATAS = "FROM ProcessDocumentMetadataDAO dao WHERE dao.documentID in (SELECT documentID FROM ProcessDocumentMetadataDAO GROUP BY documentID HAVING count(documentID) > 1)";
    private final String QUERY_CHECK_EXISTENCE_OF_ID = "SELECT count(metadata) FROM ProcessDocumentMetadataDAO metadata WHERE metadata.documentID = :id";
    private final String QUERY_GET_DOCUMENT_REFERENCE_TYPE = "SELECT documentReference FROM DocumentReferenceType documentReference WHERE documentReference.ID = :id";
    // CAMUNDA queries
    private final String QUERY_UPDATE_ACT_HI_VARINST = "UPDATE act_hi_varinst SET text_ = ? WHERE text_ = ? AND proc_inst_id_ = ?";
    private final String QUERY_UPDATE_ACT_RU_VARIABLE = "UPDATE act_ru_variable SET text_ = ? WHERE text_ = ? AND proc_inst_id_ = ?";



    @ApiOperation(value = "", notes = "This script makes sure that each document has a unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated duplicate document ids successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token")
    })
    @RequestMapping(value = "/r13/migration/update-duplicate-ids",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity updateDuplicateDocumentIds(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken
    ) {
        logger.info("Incoming request to update duplicate document ids");

        // check token
        ResponseEntity tokenCheck = eu.nimble.service.bp.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        Connection connection = null;
        try {
            // get repositories and connection
            GenericJPARepository bpRepo = new JPARepositoryFactory().forBpRepository(true);
            GenericJPARepository catalogueRepo = new JPARepositoryFactory().forCatalogueRepository(true);
            connection = camunda.getConnection();
            // get duplicate metadatas
            List<ProcessDocumentMetadataDAO> metadataDAOS = bpRepo.getEntities(QUERY_GET_DUPLICATE_METADATAS);
            for(ProcessDocumentMetadataDAO metadataDAO: metadataDAOS){
                String documentId = metadataDAO.getDocumentID();
                // get DocumentReferenceType
                List<DocumentReferenceType> documentReferences = catalogueRepo.getEntities(QUERY_GET_DOCUMENT_REFERENCE_TYPE, new String[]{"id"}, new Object[]{documentId}, 1, 0);
                // generate a UUID
                String uuid = getUUID(bpRepo);

                // update metadataDAO
                metadataDAO.setDocumentID(uuid);
                bpRepo.updateEntity(metadataDAO);
                // update the document
                updateDocument(metadataDAO.getType(), documentId, catalogueRepo, uuid);
                // update DocumentReferenceType
                if(documentReferences != null && documentReferences.size() > 0){
                    documentReferences.get(0).setID(uuid);
                    catalogueRepo.updateEntity(documentReferences.get(0));
                }
                // update act_hi_varinst
                PreparedStatement ps = connection.prepareStatement(QUERY_UPDATE_ACT_HI_VARINST);
                ps.setString(1, uuid);
                ps.setString(2, documentId);
                ps.setString(3, metadataDAO.getProcessInstanceID());
                ps.executeUpdate();
                // update act_ru_variable
                ps = connection.prepareStatement(QUERY_UPDATE_ACT_RU_VARIABLE);
                ps.setString(1, uuid);
                ps.setString(2, documentId);
                ps.setString(3, metadataDAO.getProcessInstanceID());
                ps.executeUpdate();
                ps.close();
            }

//            // check whether there are duplicate entries
//            // UBLDB
//            List<String> tableNames = getTableNames();
//            for (String tableName : tableNames) {
//                // get duplicate ids
//                List<String> duplicateIds = catalogueRepo.getEntities(String.format(QUERY_GET_DUPLICATE_IDS, tableName));
//                if (duplicateIds.size() > 0) {
//                    logger.error("In table {}, duplicates exist for the documents with following ids: {}", tableName, duplicateIds);
//                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//                }
//            }
              // BP
//            // get duplicate ids
//            List<String> duplicateIds = bpRepo.getEntities(QUERY_GET_DUPLICATE_METADATA_IDS);
//            if(duplicateIds.size() > 0){
//                logger.error("In table ProcessDocumentMetadataDAO, duplicates exist for the documents with following ids: {}", duplicateIds);
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//            }
        }
        catch (Exception e){
            logger.error("Failed to update duplicate document ids",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update duplicate document ids");
        }
        finally {
            if(connection != null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Failed to close connection",e);
                }
            }
        }
        logger.info("Completed request to update duplicate document ids");
        return ResponseEntity.ok("Completed request to update duplicate document ids");
    }

    // get table names for UBL documents
    private List<String> getTableNames() {
        List<String> tableNames = new ArrayList<>();

        tableNames.add("ItemInformationRequestType");
        tableNames.add("ItemInformationResponseType");
        tableNames.add("RequestForQuotationType");
        tableNames.add("QuotationType");
        tableNames.add("PpapRequestType");
        tableNames.add("PpapResponseType");
        tableNames.add("OrderType");
        tableNames.add("OrderResponseSimpleType");
        tableNames.add("DespatchAdviceType");
        tableNames.add("ReceiptAdviceType");
        tableNames.add("TransportExecutionPlanRequestType");
        tableNames.add("TransportExecutionPlanType");

        return tableNames;
    }

    // updates the identifier of document
    private void updateDocument(DocumentType documentType, String documentId, GenericJPARepository catalogueRepo, String uuid) {

        switch (documentType){
            case ITEMINFORMATIONREQUEST:
                List<ItemInformationRequestType> itemInformationRequestType = catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "ItemInformationRequestType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                itemInformationRequestType.get(0).setID(uuid);
                catalogueRepo.updateEntity(itemInformationRequestType.get(0));
                break;
            case ITEMINFORMATIONRESPONSE:
                List<ItemInformationResponseType> itemInformationResponseType = catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "ItemInformationResponseType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                itemInformationResponseType.get(0).setID(uuid);
                catalogueRepo.updateEntity(itemInformationResponseType.get(0));
                break;
            case PPAPREQUEST:
                List<PpapRequestType> ppapRequestType = catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "PpapRequestType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                ppapRequestType.get(0).setID(uuid);
                catalogueRepo.updateEntity(ppapRequestType.get(0));
                break;
            case PPAPRESPONSE:
                List<PpapResponseType> ppapResponseType = catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "PpapResponseType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                ppapResponseType.get(0).setID(uuid);
                catalogueRepo.updateEntity(ppapResponseType.get(0));
                break;
            case REQUESTFORQUOTATION:
                List<RequestForQuotationType> requestForQuotationType = catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "RequestForQuotationType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                requestForQuotationType.get(0).setID(uuid);
                catalogueRepo.updateEntity(requestForQuotationType.get(0));
                break;
            case QUOTATION:
                List<QuotationType> quotationType = catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "QuotationType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                quotationType.get(0).setID(uuid);
                catalogueRepo.updateEntity(quotationType.get(0));
                break;
            case ORDER:
                List<OrderType> orderType = catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "OrderType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                orderType.get(0).setID(uuid);
                catalogueRepo.updateEntity(orderType.get(0));
                break;
            case ORDERRESPONSESIMPLE:
                List<OrderResponseSimpleType> orderResponseSimpleType = catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "OrderResponseSimpleType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                orderResponseSimpleType.get(0).setID(uuid);
                catalogueRepo.updateEntity(orderResponseSimpleType.get(0));
                break;
            case RECEIPTADVICE:
                List<ReceiptAdviceType> receiptAdviceType = catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "ReceiptAdviceType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                receiptAdviceType.get(0).setID(uuid);
                catalogueRepo.updateEntity(receiptAdviceType.get(0));
                break;
            case DESPATCHADVICE:
                List<DespatchAdviceType> despatchAdviceType = catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "DespatchAdviceType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                despatchAdviceType.get(0).setID(uuid);
                catalogueRepo.updateEntity(despatchAdviceType.get(0));
                break;
            case TRANSPORTEXECUTIONPLAN:
                List<TransportExecutionPlanType> transportExecutionPlanType = catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "TransportExecutionPlanType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                transportExecutionPlanType.get(0).setID(uuid);
                catalogueRepo.updateEntity(transportExecutionPlanType.get(0));
                break;
            case TRANSPORTEXECUTIONPLANREQUEST:
                List<TransportExecutionPlanRequestType> transportExecutionPlanRequestType =  catalogueRepo.getEntities(String.format(QUERY_GET_DOCUMENTS_BY_ID, "TransportExecutionPlanRequestType"), new String[]{"id"}, new Object[]{documentId}, 1, 0);
                transportExecutionPlanRequestType.get(0).setID(uuid);
                catalogueRepo.updateEntity(transportExecutionPlanRequestType.get(0));
        }
    }

    // generates a UUID
    private String getUUID(GenericJPARepository bpRepo){
        String uuid = UUID.randomUUID().toString();
        while (true){
            Long size = bpRepo.getSingleEntity(QUERY_CHECK_EXISTENCE_OF_ID,new String[]{"id"},new Object[]{uuid});
            if(size == 0){
                return uuid;
            }
            uuid = UUID.randomUUID().toString();
        }
    }
}