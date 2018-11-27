package eu.nimble.service.bp.impl.persistence.catalogue;

import org.apache.commons.lang.ArrayUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by suat on 20-Nov-18.
 */
@Component
@Primary
@Transactional(transactionManager = "ubldbTransactionManager")
public class CatalogueRepositoryImpl implements CustomCatalogueRepository {

    private static final String QUERY_DELETE_BY_HJID = "DELETE FROM %s item WHERE item.hjid = :hjid";

    @PersistenceContext(unitName = eu.nimble.utility.Configuration.UBL_PERSISTENCE_UNIT_NAME)
    private EntityManager em;

    @Override
    public <T> T getSingleEntity(String queryStr, String[] parameterNames, Object[] parameterValues) {
        Query query = em.createQuery(queryStr);
        if(!ArrayUtils.isEmpty(parameterNames) && !ArrayUtils.isEmpty(parameterValues)) {
            if(parameterNames.length != parameterValues.length) {
                throw new RuntimeException("Non matching sizes of parameter names ");
            }
            for(int i=0; i<parameterNames.length; i++) {
                query.setParameter(parameterNames[i], parameterValues[i]);
            }
        }

        List<T> result = query.getResultList();
        if (result == null || result.size() == 0) {
            return null;
        } else {
            return result.get(0);
        }
    }

    @Override
    public <T> T getSingleEntityByHjid(Class<T> klass, long hjid) {
        return em.find(klass, hjid);
    }


    @Override
    public List<T> getEntities(String queryStr, String[] parameterNames, Object[] parameterValues) {
        Query query = em.createQuery(queryStr);
        if(!ArrayUtils.isEmpty(parameterNames) && !ArrayUtils.isEmpty(parameterValues)) {
            if(parameterNames.length != parameterValues.length) {
                throw new RuntimeException("Non matching sizes of parameter names ");
            }
            for(int i=0; i<parameterNames.length; i++) {
                query.setParameter(parameterNames[i], parameterValues[i]);
            }
        }

        List<T> result = query.getResultList();
        return result;
    }

    @Override
    public <T> T updateEntity(T entity) {
        entity = em.merge(entity);
        return entity;
    }

    @Override
    public <T> void deleteEntity(T entity) {
        if(!em.contains(entity)) {
            entity = em.merge(entity);
            em.remove(entity);
        }
    }

    @Override
    public <T> void deleteEntityByHjid(Class<T> klass, long hjid) {
        String queryStr = String.format(QUERY_DELETE_BY_HJID, klass.getSimpleName());
        Query query = em.createQuery(queryStr);
        query.setParameter("hjid", hjid);
        query.executeUpdate();
    }

    @Override
    public <T> void persistEntity(T entity) {
        em.persist(entity);
    }
}
