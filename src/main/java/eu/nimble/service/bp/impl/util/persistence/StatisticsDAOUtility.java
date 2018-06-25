package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.utility.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 12-Jun-18.
 */
public class StatisticsDAOUtility {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsDAOUtility.class);

    public static double getTradingVolume(Integer partyId, String role, String startDate, String endDate, String status) {
        String query = "select sum(order_.anticipatedMonetaryTotal.payableAmount.value) from OrderType order_ where order_.anticipatedMonetaryTotal.payableAmount.value is not null";
        if (partyId != null || role != null || startDate != null || endDate != null || status != null) {
            List<String> documentTypes = new ArrayList<>();
            documentTypes.add("ORDER");
            List<String> orderIds = DAOUtility.getDocumentIds(partyId, documentTypes, role, startDate, endDate, status);

            // no orders for the specified criteria
            if(orderIds.size() == 0) {
                logger.info("No orders for the specified criteria");
                return 0;
            }

            query += " and (";
            for (int i = 0; i < orderIds.size() - 1; i++) {
                query += " order_.ID = '" + orderIds.get(i) + "' or ";
            }
            query += " order_.ID = '" + orderIds.get(orderIds.size()-1) + "')";
        }

        double tradingVolume = ((BigDecimal) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query)).doubleValue();
        return tradingVolume;
    }

    public static List<String> getInactiveParties() {
        return null;
    }
}
