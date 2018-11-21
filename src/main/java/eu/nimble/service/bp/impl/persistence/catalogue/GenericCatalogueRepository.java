package eu.nimble.service.bp.impl.persistence.catalogue;

import org.apache.poi.ss.formula.functions.T;

/**
 * Created by suat on 20-Nov-18.
 */
public interface GenericCatalogueRepository {
    <T> T updateEntity(T entity);

    <T> void deleteEntity(T entity);

    <T> void persistEntity(T entity);
}
