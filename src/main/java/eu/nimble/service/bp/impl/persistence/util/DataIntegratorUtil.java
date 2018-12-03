package eu.nimble.service.bp.impl.persistence.util;

import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.despatchadvice.DespatchAdviceType;
import eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType;
import eu.nimble.service.model.ubl.iteminformationresponse.ItemInformationResponseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.ppaprequest.PpapRequestType;
import eu.nimble.service.model.ubl.ppapresponse.PpapResponseType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.receiptadvice.ReceiptAdviceType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.service.model.ubl.transportexecutionplan.TransportExecutionPlanType;
import eu.nimble.service.model.ubl.transportexecutionplanrequest.TransportExecutionPlanRequestType;

public class DataIntegratorUtil {

    public static void checkExistingParties(Object object){
        if(object instanceof OrderType) {
            OrderType order = (OrderType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(order.getSellerSupplierParty().getParty());
            order.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(order.getBuyerCustomerParty().getParty());
            order.getBuyerCustomerParty().setParty(buyerParty);
            for (OrderLineType orderLineType : order.getOrderLine()){
                orderLineType.getLineItem().getItem().setManufacturerParty(supplierParty);
            }

        } else if(object instanceof OrderResponseSimpleType) {
            OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(orderResponse.getSellerSupplierParty().getParty());
            orderResponse.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(orderResponse.getBuyerCustomerParty().getParty());
            orderResponse.getBuyerCustomerParty().setParty(buyerParty);


        } else if(object instanceof PpapRequestType){
            PpapRequestType ppapRequestType = (PpapRequestType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(ppapRequestType.getSellerSupplierParty().getParty());
            ppapRequestType.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(ppapRequestType.getBuyerCustomerParty().getParty());
            ppapRequestType.getBuyerCustomerParty().setParty(buyerParty);

            ppapRequestType.getLineItem().getItem().setManufacturerParty(supplierParty);

        } else if(object instanceof PpapResponseType){
            PpapResponseType ppapResponseType = (PpapResponseType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(ppapResponseType.getSellerSupplierParty().getParty());
            ppapResponseType.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(ppapResponseType.getBuyerCustomerParty().getParty());
            ppapResponseType.getBuyerCustomerParty().setParty(buyerParty);

        }  else if(object instanceof RequestForQuotationType) {
            RequestForQuotationType rfq = (RequestForQuotationType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(rfq.getSellerSupplierParty().getParty());
            rfq.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(rfq.getBuyerCustomerParty().getParty());
            rfq.getBuyerCustomerParty().setParty(buyerParty);

            for (RequestForQuotationLineType request : rfq.getRequestForQuotationLine()){
                request.getLineItem().getItem().setManufacturerParty(supplierParty);
            }

        } else if(object instanceof QuotationType) {
            QuotationType quotation = (QuotationType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(quotation.getSellerSupplierParty().getParty());
            quotation.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(quotation.getBuyerCustomerParty().getParty());
            quotation.getBuyerCustomerParty().setParty(buyerParty);

            for (QuotationLineType quotationLineType: quotation.getQuotationLine()){
                quotationLineType.getLineItem().getItem().setManufacturerParty(supplierParty);
            }

        } else if(object instanceof DespatchAdviceType) {
            DespatchAdviceType despatchAdvice = (DespatchAdviceType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(despatchAdvice.getDespatchSupplierParty().getParty());
            despatchAdvice.getDespatchSupplierParty().setParty(supplierParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(despatchAdvice.getDeliveryCustomerParty().getParty());
            despatchAdvice.getDeliveryCustomerParty().setParty(buyerParty);

            for (DespatchLineType despatchLineType: despatchAdvice.getDespatchLine()){
                PartyType manufacturerParty = CatalogueDAOUtility.getParty(despatchLineType.getItem().getManufacturerParty());
                despatchLineType.getItem().setManufacturerParty(manufacturerParty);
            }


        } else if(object instanceof ReceiptAdviceType) {
            ReceiptAdviceType receiptAdvice = (ReceiptAdviceType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(receiptAdvice.getDespatchSupplierParty().getParty());
            receiptAdvice.getDespatchSupplierParty().setParty(supplierParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(receiptAdvice.getDeliveryCustomerParty().getParty());
            receiptAdvice.getDeliveryCustomerParty().setParty(buyerParty);

            for (ReceiptLineType receiptLineType: receiptAdvice.getReceiptLine()){
                PartyType manufacturerParty = CatalogueDAOUtility.getParty(receiptLineType.getItem().getManufacturerParty());
                receiptLineType.getItem().setManufacturerParty(manufacturerParty);
            }


        } else if(object instanceof TransportExecutionPlanRequestType) {
            TransportExecutionPlanRequestType transportExecutionPlanRequestType = (TransportExecutionPlanRequestType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(transportExecutionPlanRequestType.getTransportServiceProviderParty());
            transportExecutionPlanRequestType.setTransportServiceProviderParty(supplierParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(transportExecutionPlanRequestType.getTransportUserParty());
            transportExecutionPlanRequestType.setTransportUserParty(buyerParty);

            PartyType manufacturerParty = CatalogueDAOUtility.getParty(transportExecutionPlanRequestType.getMainTransportationService().getManufacturerParty());
            transportExecutionPlanRequestType.getMainTransportationService().setManufacturerParty(manufacturerParty);

        } else if(object instanceof TransportExecutionPlanType) {
            TransportExecutionPlanType transportExecutionPlanType = (TransportExecutionPlanType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(transportExecutionPlanType.getTransportServiceProviderParty());
            transportExecutionPlanType.setTransportServiceProviderParty(supplierParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(transportExecutionPlanType.getTransportUserParty());
            transportExecutionPlanType.setTransportUserParty(buyerParty);

        } else if(object instanceof ItemInformationRequestType) {
            ItemInformationRequestType itemInformationRequest = (ItemInformationRequestType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(itemInformationRequest.getSellerSupplierParty().getParty());
            itemInformationRequest.getSellerSupplierParty().setParty(supplierParty);
            PartyType senderParty = CatalogueDAOUtility.getParty(itemInformationRequest.getSenderParty());
            itemInformationRequest.setSenderParty(senderParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(itemInformationRequest.getBuyerCustomerParty().getParty());
            itemInformationRequest.getBuyerCustomerParty().setParty(buyerParty);
            PartyType receiverParty = CatalogueDAOUtility.getParty(itemInformationRequest.getReceiverParty());
            itemInformationRequest.setReceiverParty(receiverParty);

            for(ItemInformationRequestLineType requestLineType:itemInformationRequest.getItemInformationRequestLine()){
                for (SalesItemType salesItemType: requestLineType.getSalesItem()){
                    PartyType manufacturerParty = CatalogueDAOUtility.getParty(salesItemType.getItem().getManufacturerParty());
                    salesItemType.getItem().setManufacturerParty(manufacturerParty);
                }
            }

        } else if(object instanceof ItemInformationResponseType) {
            ItemInformationResponseType itemInformationResponse = (ItemInformationResponseType) object;

            PartyType supplierParty = CatalogueDAOUtility.getParty(itemInformationResponse.getSellerSupplierParty().getParty());
            itemInformationResponse.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = CatalogueDAOUtility.getParty(itemInformationResponse.getBuyerCustomerParty().getParty());
            itemInformationResponse.getBuyerCustomerParty().setParty(buyerParty);

            for (ItemType itemType:itemInformationResponse.getItem()){
                PartyType manufacturerParty = CatalogueDAOUtility.getParty(itemType.getManufacturerParty());
                itemType.setManufacturerParty(manufacturerParty);
            }
        }
    }
}
