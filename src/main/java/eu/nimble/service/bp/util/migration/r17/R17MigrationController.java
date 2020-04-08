package eu.nimble.service.bp.util.migration.r17;

import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessVariablesDAO;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CommodityClassificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.document.IDocument;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import java.util.*;

@ApiIgnore
@Controller
public class R17MigrationController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @ApiOperation(value = "", notes = "Replace category name with uri in the category filter")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Replaced category name with uri in the category filter successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while replacing category name with uri in the category filter")
    })
    @RequestMapping(value = "/r17/migration/category-filter",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity updateCategoryFilter(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken
    ) {
        // set request log of ExecutionContext
        String requestLog = "Incoming request to replace category name with uri in the category filter";
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);

        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        GenericJPARepository businessProcessRepository = new JPARepositoryFactory().forBpRepository(true);

        // document id - commodity classifications map
        Map<String,List<CommodityClassificationType>> documentClassificationMap = new HashMap<>();

        // get all ProcessDocumentMetadataDAO
        List<ProcessDocumentMetadataDAO> processDocumentMetadataDAOS = businessProcessRepository.getEntities(ProcessDocumentMetadataDAO.class);

        logger.info("There are {} process document metadata dao",processDocumentMetadataDAOS.size());

        for (ProcessDocumentMetadataDAO processDocumentMetadataDAO : processDocumentMetadataDAOS) {
            // get document
            IDocument document = DocumentPersistenceUtility.getUBLDocument(processDocumentMetadataDAO.getDocumentID());
            if(document != null && document.getItemTypes() != null){
                // get commodity classifications
                List<CommodityClassificationType> commodityClassifications = new ArrayList<>();
                for (ItemType item : document.getItemTypes()) {
                    commodityClassifications.addAll(item.getCommodityClassification());
                }

                //add it to the map
                documentClassificationMap.put(document.getDocumentId(), new ArrayList<>(commodityClassifications));

                // replace category names with uris
                List<String> relatedProductCategoryUris = getCategoryUris(commodityClassifications,processDocumentMetadataDAO.getRelatedProductCategories());

                processDocumentMetadataDAO.getRelatedProductCategoriesItems().clear();
                processDocumentMetadataDAO.getRelatedProductCategories().clear();
                processDocumentMetadataDAO.setRelatedProductCategories(relatedProductCategoryUris);

                businessProcessRepository.updateEntity(processDocumentMetadataDAO);
            }
        }

        // get all process variables daos
        List<ProcessVariablesDAO> processVariablesDAOS = businessProcessRepository.getEntities(ProcessVariablesDAO.class);

        logger.info("There are {} process variables dao",processVariablesDAOS.size());
        for (ProcessVariablesDAO processVariablesDAO : processVariablesDAOS) {
            String documentId = processVariablesDAO.getContentUUID();
            // get commodity classifications
            List<CommodityClassificationType> commodityClassifications = null;
            if(documentClassificationMap.containsKey(documentId)){
                commodityClassifications = documentClassificationMap.get(documentId);
            } else{
                // get document
                IDocument document = DocumentPersistenceUtility.getUBLDocument(documentId);
                if(document != null && document.getItemTypes() != null){
                    // get commodity classifications
                    commodityClassifications = new ArrayList<>();
                    for (ItemType item : document.getItemTypes()) {
                        commodityClassifications.addAll(item.getCommodityClassification());
                    }
                }
            }

            if(commodityClassifications != null){
                // replace category names with uris
                List<String> relatedProductCategoryUris = getCategoryUris(commodityClassifications,processVariablesDAO.getRelatedProductCategories());

                processVariablesDAO.getRelatedProductCategoriesItems().clear();
                processVariablesDAO.getRelatedProductCategories().clear();
                processVariablesDAO.setRelatedProductCategories(relatedProductCategoryUris);

                businessProcessRepository.updateEntity(processVariablesDAO);
            }
        }

        // delete related product categories items with dangling references i.e, ones which do have not a category uri
        List<ProcessVariablesDAO.ProcessVariablesDAORelatedProductCategoriesItem> processVariablesRelatedProductCategories = businessProcessRepository.getEntities(ProcessVariablesDAO.ProcessVariablesDAORelatedProductCategoriesItem.class);
        for (ProcessVariablesDAO.ProcessVariablesDAORelatedProductCategoriesItem processVariablesRelatedProductCategory : processVariablesRelatedProductCategories) {
            if(processVariablesRelatedProductCategory.getItem() == null || !processVariablesRelatedProductCategory.getItem().startsWith("http")){
                businessProcessRepository.deleteEntity(processVariablesRelatedProductCategory);
            }
        }

        List<ProcessDocumentMetadataDAO.ProcessDocumentMetadataDAORelatedProductCategoriesItem> processDocumentMetadataDAORelatedProductCategoriesItems = businessProcessRepository.getEntities(ProcessDocumentMetadataDAO.ProcessDocumentMetadataDAORelatedProductCategoriesItem.class);
        for (ProcessDocumentMetadataDAO.ProcessDocumentMetadataDAORelatedProductCategoriesItem processDocumentMetadataDAORelatedProductCategoriesItem : processDocumentMetadataDAORelatedProductCategoriesItems) {
            if(processDocumentMetadataDAORelatedProductCategoriesItem.getItem() == null || !processDocumentMetadataDAORelatedProductCategoriesItem.getItem().startsWith("http")){
                businessProcessRepository.deleteEntity(processDocumentMetadataDAORelatedProductCategoriesItem);
            }
        }

        logger.info("Completed request to replace category name with uri in the category filter");
        return ResponseEntity.ok(null);
    }

    private List<String> getCategoryUris(List<CommodityClassificationType> commodityClassifications,List<String> relatedProductCategories){
        // replace category names with uris
        List<String> relatedProductCategoryUris = new ArrayList<>();

        for (String relatedProductCategory : relatedProductCategories) {
            if(relatedProductCategory != null){
                for (CommodityClassificationType commodityClassification : commodityClassifications) {
                    if(commodityClassification.getItemClassificationCode() != null &&commodityClassification.getItemClassificationCode().getName() != null && relatedProductCategory.contentEquals(commodityClassification.getItemClassificationCode().getName()) && commodityClassification.getItemClassificationCode().getURI() != null && !commodityClassification.getItemClassificationCode().getListID().contentEquals("Default")){
                        relatedProductCategoryUris.add(commodityClassification.getItemClassificationCode().getURI());
                        // remove it from the list since some categories might have the same name
                        commodityClassifications.remove(commodityClassification);
                        break;
                    }
                }
            }
        }
        return relatedProductCategoryUris;
    }
}