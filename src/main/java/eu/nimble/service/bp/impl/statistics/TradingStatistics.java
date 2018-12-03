//package eu.nimble.service.bp.impl.statistics;
//
//import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
//import eu.nimble.service.bp.impl.persistence.util.DAOUtility;
//import org.springframework.stereotype.Component;
//
//import javax.print.Doc;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Created by suat on 07-Jun-18.
// */
//@Component
//public class TradingStatistics {
//    // total orders
//    // total confirmed orders
//    // total business requests
//    // total volume
//    public int getTotalNumberOfDocuments(DocumentType documentType) {
//        return getTotalNumberOfDocuments(documentType, null);
//    }
//
//    public int getTotalNumberOfDocuments(DocumentType documentType, Integer partyId) {
////        String /
////        if(documentType == DocumentType.ORDER) {
////
////        }
//        return 0;
//    }
//
//    public int getTotalNumberOfDocuments(DocumentType documentType, Integer partyId, String role, String startDate, String endDate) {
//        List<DocumentType> documentTypes = new ArrayList<>();
//        documentTypes.add(documentType);
//        return DAOUtility.getTransactionCount(partyId, documentTypes, role, startDate, endDate);
//    }
//
//}
