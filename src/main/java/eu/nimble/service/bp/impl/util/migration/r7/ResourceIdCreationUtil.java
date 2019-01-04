package eu.nimble.service.bp.impl.util.migration.r7;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates @{@link eu.nimble.service.model.ubl.commonaggregatecomponents.ResourceType}s to create associations between
 * all the identifiers included in already published catalogues and parties. This is required since we introduced a
 * identifier check mechanism to prevent users to edit other's resources.
 *
 * Created by suat on 25-Dec-18.
 */
@Component
public class ResourceIdCreationUtil {
    private static Logger log = LoggerFactory.getLogger(ResourceIdCreationUtil.class);

    @Autowired
    private ResourceValidationUtility resourceValidationUtil;

    public void createResources() {
        // create resources for catalogues
        List<CatalogueType> existingCatalogues = new JPARepositoryFactory().forCatalogueRepository().getEntities(CatalogueType.class);

        for (CatalogueType catalogue : existingCatalogues) {
            try {
                if (catalogue.getUUID() == null || catalogue.getUUID().trim().contentEquals("")) {
                    log.info("No uuid in catalogue. hjid: {}", catalogue.getHjid());
                } else if (catalogue.getCatalogueLine().size() == 0) {
                    log.info("No catalogue line in catalogue: {}", catalogue.getUUID());
                } else if (catalogue.getProviderParty() == null) {
                    log.info("No provider party for catalogue: {}", catalogue.getUUID());
                } else {
                    resourceValidationUtil.insertHjidsForObject(catalogue, catalogue.getProviderParty().getID(), Configuration.Standard.UBL.toString());
                    log.info("Created resources for catalogue: {}", catalogue.getUUID(), catalogue.getProviderParty().getID());
                }
            } catch (Exception e) {
                log.error("Failed to create resources for catalogue: uuid: {}", catalogue.getUUID(), e);
            }
        }

        // create resources for documents
        List<ProcessDocumentMetadataDAO> documentMetadata = new JPARepositoryFactory().forBpRepository().getEntities(ProcessDocumentMetadataDAO.class);
        for(ProcessDocumentMetadataDAO metadata : documentMetadata) {
            Object document = DocumentPersistenceUtility.getUBLDocument(metadata.getDocumentID(), metadata.getType());
            try {
                resourceValidationUtil.insertHjidsForObject(document, metadata.getInitiatorID(), Configuration.Standard.UBL.toString());
                log.info("Created resources for document: {}, party: {}", metadata.getDocumentID(), metadata.getInitiatorID());
            } catch (Exception e) {
                log.error("Failed to create resources for document: id: {}, type: {}", metadata.getDocumentID(), metadata.getType(), e);
            }
        }

        log.info("Completed resource creation");
    }
}
