package eu.nimble.service.bp.impl.util.persistence;

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
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;

public class DataIntegratorUtil {

    public static void checkExistingParties(Object object){
        if(object instanceof OrderType) {
            OrderType order = (OrderType) object;

            PartyType supplierParty = getParty(order.getSellerSupplierParty().getParty());
            order.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = getParty(order.getBuyerCustomerParty().getParty());
            order.getBuyerCustomerParty().setParty(buyerParty);
            for (OrderLineType orderLineType : order.getOrderLine()){
                orderLineType.getLineItem().getItem().setManufacturerParty(supplierParty);
            }

        } else if(object instanceof OrderResponseSimpleType) {
            OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) object;

            PartyType supplierParty = getParty(orderResponse.getSellerSupplierParty().getParty());
            orderResponse.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = getParty(orderResponse.getBuyerCustomerParty().getParty());
            orderResponse.getBuyerCustomerParty().setParty(buyerParty);


        } else if(object instanceof PpapRequestType){
            PpapRequestType ppapRequestType = (PpapRequestType) object;

            PartyType supplierParty = getParty(ppapRequestType.getSellerSupplierParty().getParty());
            ppapRequestType.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = getParty(ppapRequestType.getBuyerCustomerParty().getParty());
            ppapRequestType.getBuyerCustomerParty().setParty(buyerParty);

            ppapRequestType.getLineItem().getItem().setManufacturerParty(supplierParty);

        } else if(object instanceof PpapResponseType){
            PpapResponseType ppapResponseType = (PpapResponseType) object;

            PartyType supplierParty = getParty(ppapResponseType.getSellerSupplierParty().getParty());
            ppapResponseType.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = getParty(ppapResponseType.getBuyerCustomerParty().getParty());
            ppapResponseType.getBuyerCustomerParty().setParty(buyerParty);

        }  else if(object instanceof RequestForQuotationType) {
            RequestForQuotationType rfq = (RequestForQuotationType) object;

            PartyType supplierParty = getParty(rfq.getSellerSupplierParty().getParty());
            rfq.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = getParty(rfq.getBuyerCustomerParty().getParty());
            rfq.getBuyerCustomerParty().setParty(buyerParty);

            for (RequestForQuotationLineType request : rfq.getRequestForQuotationLine()){
                request.getLineItem().getItem().setManufacturerParty(supplierParty);
            }

        } else if(object instanceof QuotationType) {
            QuotationType quotation = (QuotationType) object;

            PartyType supplierParty = getParty(quotation.getSellerSupplierParty().getParty());
            quotation.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = getParty(quotation.getBuyerCustomerParty().getParty());
            quotation.getBuyerCustomerParty().setParty(buyerParty);

            for (QuotationLineType quotationLineType: quotation.getQuotationLine()){
                quotationLineType.getLineItem().getItem().setManufacturerParty(supplierParty);
            }

        } else if(object instanceof DespatchAdviceType) {
            DespatchAdviceType despatchAdvice = (DespatchAdviceType) object;

            PartyType supplierParty = getParty(despatchAdvice.getDespatchSupplierParty().getParty());
            despatchAdvice.getDespatchSupplierParty().setParty(supplierParty);
            PartyType buyerParty = getParty(despatchAdvice.getDeliveryCustomerParty().getParty());
            despatchAdvice.getDeliveryCustomerParty().setParty(buyerParty);

            for (DespatchLineType despatchLineType: despatchAdvice.getDespatchLine()){
                PartyType manufacturerParty = getParty(despatchLineType.getItem().getManufacturerParty());
                despatchLineType.getItem().setManufacturerParty(manufacturerParty);
            }


        } else if(object instanceof ReceiptAdviceType) {
            ReceiptAdviceType receiptAdvice = (ReceiptAdviceType) object;

            PartyType supplierParty = getParty(receiptAdvice.getDespatchSupplierParty().getParty());
            receiptAdvice.getDespatchSupplierParty().setParty(supplierParty);
            PartyType buyerParty = getParty(receiptAdvice.getDeliveryCustomerParty().getParty());
            receiptAdvice.getDeliveryCustomerParty().setParty(buyerParty);

            for (ReceiptLineType receiptLineType: receiptAdvice.getReceiptLine()){
                PartyType manufacturerParty = getParty(receiptLineType.getItem().getManufacturerParty());
                receiptLineType.getItem().setManufacturerParty(manufacturerParty);
            }


        } else if(object instanceof TransportExecutionPlanRequestType) {
            TransportExecutionPlanRequestType transportExecutionPlanRequestType = (TransportExecutionPlanRequestType) object;

            PartyType supplierParty = getParty(transportExecutionPlanRequestType.getTransportServiceProviderParty());
            transportExecutionPlanRequestType.setTransportServiceProviderParty(supplierParty);
            PartyType buyerParty = getParty(transportExecutionPlanRequestType.getTransportUserParty());
            transportExecutionPlanRequestType.setTransportUserParty(buyerParty);

            PartyType manufacturerParty = getParty(transportExecutionPlanRequestType.getMainTransportationService().getManufacturerParty());
            transportExecutionPlanRequestType.getMainTransportationService().setManufacturerParty(manufacturerParty);

        } else if(object instanceof TransportExecutionPlanType) {
            TransportExecutionPlanType transportExecutionPlanType = (TransportExecutionPlanType) object;

            PartyType supplierParty = getParty(transportExecutionPlanType.getTransportServiceProviderParty());
            transportExecutionPlanType.setTransportServiceProviderParty(supplierParty);
            PartyType buyerParty = getParty(transportExecutionPlanType.getTransportUserParty());
            transportExecutionPlanType.setTransportUserParty(buyerParty);

        } else if(object instanceof ItemInformationRequestType) {
            ItemInformationRequestType itemInformationRequest = (ItemInformationRequestType) object;

            PartyType supplierParty = getParty(itemInformationRequest.getSellerSupplierParty().getParty());
            itemInformationRequest.getSellerSupplierParty().setParty(supplierParty);
            PartyType senderParty = getParty(itemInformationRequest.getSenderParty());
            itemInformationRequest.setSenderParty(senderParty);
            PartyType buyerParty = getParty(itemInformationRequest.getBuyerCustomerParty().getParty());
            itemInformationRequest.getBuyerCustomerParty().setParty(buyerParty);
            PartyType receiverParty = getParty(itemInformationRequest.getReceiverParty());
            itemInformationRequest.setReceiverParty(receiverParty);

            for(ItemInformationRequestLineType requestLineType:itemInformationRequest.getItemInformationRequestLine()){
                for (SalesItemType salesItemType: requestLineType.getSalesItem()){
                    PartyType manufacturerParty = getParty(salesItemType.getItem().getManufacturerParty());
                    salesItemType.getItem().setManufacturerParty(manufacturerParty);
                }
            }

        } else if(object instanceof ItemInformationResponseType) {
            ItemInformationResponseType itemInformationResponse = (ItemInformationResponseType) object;

            PartyType supplierParty = getParty(itemInformationResponse.getSellerSupplierParty().getParty());
            itemInformationResponse.getSellerSupplierParty().setParty(supplierParty);
            PartyType buyerParty = getParty(itemInformationResponse.getBuyerCustomerParty().getParty());
            itemInformationResponse.getBuyerCustomerParty().setParty(buyerParty);

            for (ItemType itemType:itemInformationResponse.getItem()){
                PartyType manufacturerParty = getParty(itemType.getManufacturerParty());
                itemType.setManufacturerParty(manufacturerParty);
            }
        }
    }

    private static PartyType getParty(PartyType party){
        if(party == null){
            return null;
        }
        String query = "SELECT party FROM PartyType party WHERE party.ID = '"+party.getID()+"'";
        PartyType partyType = HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).load(query);
        if(partyType == null){
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(party);
            return party;
        }
        return partyType;
    }

}
