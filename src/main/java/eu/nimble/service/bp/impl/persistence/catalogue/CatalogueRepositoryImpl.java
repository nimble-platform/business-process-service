package eu.nimble.service.bp.impl.persistence.catalogue;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by suat on 20-Nov-18.
 */
@Component
@Primary
@Transactional(transactionManager = "ubldbTransactionManager")
public class CatalogueRepositoryImpl implements GenericCatalogueRepository {

    @PersistenceContext(unitName = eu.nimble.utility.Configuration.UBL_PERSISTENCE_UNIT_NAME)
    private EntityManager em;

    @Override
    public <T> T updateEntity(T entity) {
        entity = em.merge(entity);
        return entity;
    }

    @Override
    public <T> void deleteEntity(T entity) {
        em.remove(entity);
    }

    @Override
    public <T> void persistEntity(T entity) {
        em.persist(entity);
    }
}
