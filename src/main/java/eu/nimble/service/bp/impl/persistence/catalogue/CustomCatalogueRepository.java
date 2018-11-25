package eu.nimble.service.bp.impl.persistence.catalogue;

import java.util.List;

/**
 * Created by suat on 20-Nov-18.
 */
public interface CustomCatalogueRepository {
    <T> T getSingleEntityByHjid(Class<T> klass, long hjid);

    <T> T getSingleEntity(String query, String[] parameterNames, Object[] parameterValues);

    <T> List<T> getEntities(String query, String[] parameterNames, Object[] parameterValues);

    <T> T updateEntity(T entity);

    <T> void deleteEntity(T entity);

    <T> void deleteEntityByHjid(Class<T> klass, long hjid);

    <T> void persistEntity(T entity);
}
