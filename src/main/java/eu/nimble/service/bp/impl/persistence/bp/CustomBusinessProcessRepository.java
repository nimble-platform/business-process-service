package eu.nimble.service.bp.impl.persistence.bp;

import org.apache.poi.ss.formula.functions.T;

import java.util.List;

/**
 * Created by suat on 23-Nov-18.
 */
public interface CustomBusinessProcessRepository {
    <T> T getSingleEntityByHjid(Class<T> klass, long hjid);

    <T> T getSingleEntity(String query, String[] parameterNames, Object[] parameterValues);

    <T> List<T> getEntities(String query);

    <T> List<T> getEntities(String query, String[] parameterNames, Object[] parameterValues);

    <T> List<T> getEntities(String query, String[] parameterNames, Object[] parameterValues, Integer limit, Integer offset);

    <T> T updateEntity(T entity);

    <T> void deleteEntity(T entity);

    <T> void deleteEntityByHjid(Class<T> klass, long hjid);

    <T> void persistEntity(T entity);
}
