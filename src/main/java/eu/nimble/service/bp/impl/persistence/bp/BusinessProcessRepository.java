package eu.nimble.service.bp.impl.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessConfigurationDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessPreferencesDAO;
import eu.nimble.utility.persistence.GenericJPARepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by suat on 20-Nov-18.
 */
public interface BusinessProcessRepository extends JpaRepository<ProcessInstanceDAO, Long>, GenericJPARepository {
    @Query(value = "SELECT c FROM ProcessInstanceGroupDAO c WHERE c.hjid = :uuid")
    List<ProcessInstanceGroupDAO> getProcessInstanceGroupDAO(@Param("uuid") Long uuid);

    @Query(value = "SELECT conf FROM ProcessConfigurationDAO conf WHERE conf.partnerID = :partnerId")
    List<ProcessConfigurationDAO> getProcessConfigurations(@Param("partnerId") String partnerId);

    @Query(value = "SELECT conf FROM ProcessConfigurationDAO conf WHERE conf.partnerID = :partnerId AND conf.processID = :processId")
    List<ProcessConfigurationDAO> getProcessConfigurations(@Param("partnerId") String partnerId, @Param("processId") String processId);

    @Query(value = "SELECT conf FROM ProcessPreferencesDAO conf WHERE conf.partnerID = :partnerId")
    List<ProcessPreferencesDAO> getProcessPreferences(@Param("partnerId") String partnerId);
}
