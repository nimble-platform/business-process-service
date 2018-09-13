package eu.nimble.service.bp.impl.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.impl.util.persistence.DocumentDAOUtility;
import eu.nimble.service.bp.impl.util.serialization.Serializer;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType;
import eu.nimble.service.model.ubl.iteminformationresponse.ItemInformationResponseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.ppaprequest.PpapRequestType;
import eu.nimble.service.model.ubl.ppapresponse.PpapResponseType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ContractGenerator {
    private final Logger logger = LoggerFactory.getLogger(ContractGenerator.class);

    private final String payment_id_default = "2% 10 days, net 60 days";
    private final String incoterms_id_default = "Freight Prepaid";
    private final String buyer_country_default = "Spain";
    private final String seller_tel_default = "855-729-0137";
    private final String seller_website_default = "www.abc.com";
    private final String action_day_default = "10";
    private final String inspection_id_default = "7 years";
    private final String warranty_id_default = "10 days";
    private final String change_id_default ="5 days";
    private final String insurance_id_default ="15 days";
    private final String termination_id_default = "20 days";
    private final String shipment_id_default ="United Parcel Service, Rail Express, Air Express, Air Freight or Parcel Post";
    private final String arbitrator_id_default = "under the JAMS Streamlined Arbitration Rules and Procedures in the District of Columbia or";
    private final String agreement_id_default = "21 days";
    private final String failed_agreement_default ="by JAMS";
    private final String decision_id_default = "90 days";

    private final String red_hex = "DC143C";
    private final String blue_hex = "1464AD";

    private final int logo_space = 300;
    private final int cell_space = 200;

    private final double purchaseDetailsScale = 60.0;
    private final double termsScale = 100.0;

    private boolean firstIIR = true;

    public void generateContract(String orderId,ZipOutputStream zos){
        OrderType order = (OrderType) DocumentDAOUtility.getUBLDocument(orderId,DocumentType.ORDER);

        XWPFDocument orderTermsAndConditions = fillOrderTermsAndConditions(order);
        XWPFDocument purchaseDetails = fillPurchaseDetails(order);

        getAndPopulateClauses(order,zos,purchaseDetails);

        addDocxToZipFile("Standard Purchase Order Terms and Conditions.pdf",orderTermsAndConditions,zos);
        addDocxToZipFile("Company Purchase Details.pdf",purchaseDetails,zos);

    }

    public String generateOrderTermsAndConditionsAsText(String orderId,String sellerParty,String buyerParty,String incoterms,String tradingTerms){
        OrderType order = (OrderType) DocumentDAOUtility.getUBLDocument(orderId,DocumentType.ORDER);

        String text = "";
        try {
            if(order != null){
                InputStream file = ContractGenerator.class.getResourceAsStream("/contract-bundle/Standard Purchase Order Terms and Conditions_Text.docx");
                XWPFDocument document = new XWPFDocument(file);

                List<XWPFParagraph> paragraphs = document.getParagraphs();

                for (XWPFParagraph para : paragraphs) {
                    String text2 = para.getText();
                    if(text2 != null){
                        text = text + text2 + "\n";
                    }
                }
                file.close();

                // Fill placeholders
                text = text.replace("$seller_id",order.getSellerSupplierParty().getParty().getName());
                text = text.replace("$buyer_id",order.getBuyerCustomerParty().getParty().getName());

                if(order.getPaymentTerms().getTradingTerms().size() > 0){
                    text = text.replace("$payment_id",getTradingTerms(order.getPaymentTerms().getTradingTerms()));
                }
                else {
                    text = text.replace("$payment_id",payment_id_default);
                }

                if(order.getBuyerCustomerParty().getParty().getPostalAddress().getCountry().getName() != null){
                    text = text.replace("$buyer_country",order.getBuyerCustomerParty().getParty().getPostalAddress().getCountry().getName());
                }
                else {
                    text = text.replace("$buyer_country",buyer_country_default);
                }

                if(!order.getSellerSupplierParty().getParty().getPerson().get(0).getContact().getTelephone().contentEquals("")){
                    text = text.replace("$seller_tel",order.getSellerSupplierParty().getParty().getPerson().get(0).getContact().getTelephone());
                }
                else {
                    text = text.replace("$seller_tel",seller_tel_default);
                }

                if(!order.getSellerSupplierParty().getParty().getWebsiteURI().contentEquals("")){
                    text = text.replace("$seller_website",order.getSellerSupplierParty().getParty().getWebsiteURI());
                }
                else {
                    text = text.replace("$seller_website",seller_website_default);
                }

                if(order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getIncoterms() != null){
                    text = text.replace("$incoterms_id",order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getIncoterms());
                }
                else {
                    text = text.replace("$incoterms_id",incoterms_id_default);
                }

                text = text.replace("$notices_id",constructAddress(order.getBuyerCustomerParty().getParty().getName(),order.getBuyerCustomerParty().getParty().getPostalAddress()));

                // Use default values for the rest
                text = text.replace("$action_day",action_day_default);
                text = text.replace("$inspection_id",inspection_id_default);
                text = text.replace("$warranty_id",warranty_id_default);
                text = text.replace("$change_id",change_id_default);
                text = text.replace("$insurance_id",insurance_id_default);
                text = text.replace("$termination_id",termination_id_default);
                text = text.replace("$shipment_id",shipment_id_default);
                text = text.replace("$arbitrator_id",arbitrator_id_default);
                text = text.replace("$agreement_id",agreement_id_default);
                text = text.replace("$failed_agreement",failed_agreement_default);
                text = text.replace("$decision_id",decision_id_default);
            }
            else {
                ObjectMapper objectMapper = Serializer.getObjectMapperForContracts();
                List<TradingTermType> tradingTermTypeList = objectMapper.readValue(tradingTerms,objectMapper.getTypeFactory().constructCollectionType(List.class,TradingTermType.class));

                PartyType supplierParty = objectMapper.readValue(sellerParty,PartyType.class);
                PartyType customerParty = objectMapper.readValue(buyerParty,PartyType.class);

                InputStream file = ContractGenerator.class.getResourceAsStream("/contract-bundle/Standard Purchase Order Terms and Conditions_Text.docx");

                XWPFDocument document = new XWPFDocument(file);

                List<XWPFParagraph> paragraphs = document.getParagraphs();

                for (XWPFParagraph para : paragraphs) {
                    String text2 = para.getText();
                    if(text2 != null){
                        text = text + text2 + "\n";
                    }
                }

                // Fill placeholders
                text = text.replace("$seller_id",supplierParty.getName());
                text = text.replace("$buyer_id",customerParty.getName());

                if(tradingTermTypeList.size() > 0){
                    text = text.replace("$payment_id",getTradingTerms(tradingTermTypeList));
                }
                else {
                    text = text.replace("$payment_id",payment_id_default);
                }

                if(customerParty.getPostalAddress().getCountry().getName() != null){
                    text = text.replace("$buyer_country",customerParty.getPostalAddress().getCountry().getName());
                }
                else {
                    text = text.replace("$buyer_country",buyer_country_default);
                }

                if(!supplierParty.getPerson().get(0).getContact().getTelephone().contentEquals("")){
                    text = text.replace("$seller_tel",supplierParty.getPerson().get(0).getContact().getTelephone());
                }
                else {
                    text = text.replace("$seller_tel",seller_tel_default);
                }

                if(!supplierParty.getWebsiteURI().contentEquals("")){
                    text = text.replace("$seller_website",supplierParty.getWebsiteURI());
                }
                else {
                    text = text.replace("$seller_website",seller_website_default);
                }

                if(!incoterms.contentEquals("")){
                    text = text.replace("$incoterms_id",incoterms);
                }
                else {
                    text = text.replace("$incoterms_id",incoterms_id_default);
                }

                text = text.replace("$notices_id",constructAddress(customerParty.getName(),customerParty.getPostalAddress()));

                // Use default values for the rest
                text = text.replace("$action_day",action_day_default);
                text = text.replace("$inspection_id",inspection_id_default);
                text = text.replace("$warranty_id",warranty_id_default);
                text = text.replace("$change_id",change_id_default);
                text = text.replace("$insurance_id",insurance_id_default);
                text = text.replace("$termination_id",termination_id_default);
                text = text.replace("$shipment_id",shipment_id_default);
                text = text.replace("$arbitrator_id",arbitrator_id_default);
                text = text.replace("$agreement_id",agreement_id_default);
                text = text.replace("$failed_agreement",failed_agreement_default);
                text = text.replace("$decision_id",decision_id_default);
            }

        }
        catch (Exception e){
            logger.error("Failed to fill in 'Standard Purchase Order Terms and Conditions_Text.docx' for the order with id : {}",order.getID(),e);
        }
        return text;
    }

    private XWPFDocument fillOrderTermsAndConditions(OrderType order){

        XWPFDocument document = null;
        try {
            InputStream file = ContractGenerator.class.getResourceAsStream("/contract-bundle/Standard Purchase Order Terms and Conditions.docx");

            document = new XWPFDocument(file);

            // image
            for(XWPFParagraph paragraph : document.getParagraphs()){
                for(XWPFRun run : paragraph.getRuns()){
                    String text = run.getText(0);
                    if(text != null && text.contains("$logo_id")){
                        text = text.replace("$logo_id","");

                        setSpace(paragraph,logo_space);

                        run.setText(text,0);

                        BufferedImage bufferedImage = ImageIO.read(getClass().getResourceAsStream("/contract-bundle/nimble_logo.png"));
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "png", os);
                        int scaledWidth = (int)(bufferedImage.getWidth() * (termsScale/bufferedImage.getHeight()));

                        run.addPicture(new ByteArrayInputStream(os.toByteArray()),XWPFDocument.PICTURE_TYPE_PNG,"nimble_logo.png",Units.toEMU(scaledWidth),Units.toEMU(100));
                    }
                }
            }

            for (XWPFTable tbl : document.getTables() ) {
                for (XWPFTableRow row : tbl.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            for (XWPFRun r : p.getRuns()) {
                                String text = r.getText(0);
                                if(text != null){
                                    if(text.contains("$seller_id")){
                                        text = text.replace("$seller_id",order.getSellerSupplierParty().getParty().getName());
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,red_hex);
                                    }
                                    else if(text.contains("$buyer_id")){
                                        text = text.replace("$buyer_id",order.getBuyerCustomerParty().getParty().getName());
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,red_hex);
                                    }
                                    else if(text.contains("$payment_id")){
                                        if(order.getPaymentTerms().getTradingTerms().size() > 0){
                                            text = text.replace("$payment_id",getTradingTerms(order.getPaymentTerms().getTradingTerms()));
                                            r.setText(text,0);
                                            r.setUnderline(UnderlinePatterns.SINGLE);
                                            setColor(r,red_hex);
                                        }
                                        else {
                                            text = text.replace("$payment_id",payment_id_default);
                                            r.setText(text,0);
                                            r.setUnderline(UnderlinePatterns.SINGLE);
                                            setColor(r,blue_hex);
                                        }
                                    }
                                    else if(text.contains("$buyer_country")){
                                        if(order.getBuyerCustomerParty().getParty().getPostalAddress().getCountry().getName() != null){
                                            text = text.replace("$buyer_country",order.getBuyerCustomerParty().getParty().getPostalAddress().getCountry().getName());
                                            r.setText(text,0);
                                            r.setUnderline(UnderlinePatterns.SINGLE);
                                            setColor(r,red_hex);
                                        }
                                        else {
                                            text = text.replace("$buyer_country",buyer_country_default);
                                            r.setText(text,0);
                                            r.setUnderline(UnderlinePatterns.SINGLE);
                                            setColor(r,blue_hex);
                                        }
                                    }
                                    else if(text.contains("$seller_tel")){
                                        if(!order.getSellerSupplierParty().getParty().getPerson().get(0).getContact().getTelephone().contentEquals("")){
                                            text = text.replace("$seller_tel",order.getSellerSupplierParty().getParty().getPerson().get(0).getContact().getTelephone());
                                            r.setText(text,0);
                                            r.setUnderline(UnderlinePatterns.SINGLE);
                                            setColor(r,red_hex);
                                        }
                                        else {
                                            text = text.replace("$seller_tel",seller_tel_default);
                                            r.setText(text,0);
                                            r.setUnderline(UnderlinePatterns.SINGLE);
                                            setColor(r,blue_hex);
                                        }
                                    }
                                    else if(text.contains("$seller_website")){
                                        if(!order.getSellerSupplierParty().getParty().getWebsiteURI().contentEquals("")){
                                            text = text.replace("$seller_website",order.getSellerSupplierParty().getParty().getWebsiteURI());
                                            r.setText(text,0);
                                            r.setUnderline(UnderlinePatterns.SINGLE);
                                            setColor(r,red_hex);
                                        }
                                        else {
                                            text = text.replace("$seller_website",seller_website_default);
                                            r.setText(text,0);
                                            r.setUnderline(UnderlinePatterns.SINGLE);
                                            setColor(r,blue_hex);
                                        }
                                    }
                                    else if(text.contains("$incoterms_id")){
                                        if(order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getIncoterms() != null){
                                            text = text.replace("$incoterms_id",order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getIncoterms());
                                            r.setText(text,0);
                                            r.setUnderline(UnderlinePatterns.SINGLE);
                                            setColor(r,red_hex);
                                        }
                                        else {
                                            text = text.replace("$incoterms_id",incoterms_id_default);
                                            r.setText(text,0);
                                            r.setUnderline(UnderlinePatterns.SINGLE);
                                            setColor(r,blue_hex);
                                        }
                                    }
                                    else if(text.contains("$notices_id")){
                                        text = text.replace("$notices_id",constructAddress(order.getBuyerCustomerParty().getParty().getName(),order.getBuyerCustomerParty().getParty().getPostalAddress()));
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,red_hex);
                                    }
                                    // Use default values for the rest
                                    else if(text.contains("$action_day")){
                                        text = text.replace("$action_day",action_day_default);
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,blue_hex);
                                    }
                                    else if(text.contains("$inspection_id")){
                                        text = text.replace("$inspection_id",inspection_id_default);
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,blue_hex);
                                    }
                                    else if(text.contains("$warranty_id")){
                                        text = text.replace("$warranty_id",warranty_id_default);
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,blue_hex);
                                    }
                                    else if(text.contains("$change_id")){
                                        text = text.replace("$change_id",change_id_default);
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,blue_hex);
                                    }
                                    else if(text.contains("$insurance_id")){
                                        text = text.replace("$insurance_id",insurance_id_default);
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,blue_hex);
                                    }
                                    else if(text.contains("$termination_id")){
                                        text = text.replace("$termination_id",termination_id_default);
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,blue_hex);
                                    }
                                    else if(text.contains("$shipment_id")){
                                        text = text.replace("$shipment_id",shipment_id_default);
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,blue_hex);
                                    }
                                    else if(text.contains("$arbitrator_id")){
                                        text = text.replace("$arbitrator_id",arbitrator_id_default);
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,blue_hex);
                                    }
                                    else if(text.contains("$agreement_id")){
                                        text = text.replace("$agreement_id",agreement_id_default);
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,blue_hex);
                                    }
                                    else if(text.contains("$failed_agreement")){
                                        text = text.replace("$failed_agreement",failed_agreement_default);
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,blue_hex);
                                    }
                                    else if(text.contains("$decision_id")){
                                        text = text.replace("$decision_id",decision_id_default);
                                        r.setText(text,0);
                                        r.setUnderline(UnderlinePatterns.SINGLE);
                                        setColor(r,blue_hex);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e){
            logger.error("Failed to fill in 'Standard Purchase Order Terms and Conditions.pdf' for the order with id : {}",order.getID(),e);
        }
        return document;
    }

    private XWPFDocument fillPurchaseDetails(OrderType order){

        XWPFDocument document = null;
        try {
            InputStream file = ContractGenerator.class.getResourceAsStream("/contract-bundle/Purchase Details.docx");

            document = new XWPFDocument(file);

            boolean totalPriceExists = checkTotalPriceExistsInOrder(order);

            // Read the table
            for (XWPFTable tbl : document.getTables() ) {
                for (XWPFTableRow row : tbl.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            for (XWPFRun r : p.getRuns()) {
                                String text = r.getText(0);
                                if(text != null){
                                    if(text.contains("$logo_id")){
                                        text = text.replace("$logo_id","");
                                        r.setText(text,0);

                                        BufferedImage bufferedImage = ImageIO.read(getClass().getResourceAsStream("/contract-bundle/nimble_logo.png"));
                                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                                        ImageIO.write(bufferedImage, "png", os);
                                        int width = bufferedImage.getWidth();
                                        int height = bufferedImage.getHeight();

                                        int scaledWidth = (int)(width * (purchaseDetailsScale/height));

                                        r.addPicture(new ByteArrayInputStream(os.toByteArray()),XWPFDocument.PICTURE_TYPE_PNG,"nimble_logo.png",Units.toEMU(scaledWidth),Units.toEMU(60));
                                    }
                                    if(text.contains("$order_id")){
                                        text = text.replace("$order_id",order.getID());
                                        r.setText(text,0);
                                    }
                                    if(text.contains("$company_id")){
                                        text = text.replace("$company_id",order.getBuyerCustomerParty().getParty().getName());
                                        r.setText(text,0);
                                    }
                                    if(text.contains("$country_invoice_id")){
                                        if(order.getBuyerCustomerParty().getParty().getPostalAddress().getCountry().getName() != null){
                                            text = text.replace("$country_invoice_id",order.getBuyerCustomerParty().getParty().getPostalAddress().getCountry().getName());
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$country_invoice_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$street_invoice_id")){
                                        if(order.getBuyerCustomerParty().getParty().getPostalAddress().getStreetName()!= null){
                                            text = text.replace("$street_invoice_id",order.getBuyerCustomerParty().getParty().getPostalAddress().getStreetName());
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$street_invoice_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$building_invoice_id")){
                                        if(order.getBuyerCustomerParty().getParty().getPostalAddress().getBuildingNumber()!= null){
                                            text = text.replace("$building_invoice_id",order.getBuyerCustomerParty().getParty().getPostalAddress().getBuildingNumber());
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$building_invoice_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$country_id")){
                                        if(order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCountry().getName() != null){
                                            text = text.replace("$country_id",order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCountry().getName());
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$country_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$phone_id")){
                                        if(!order.getBuyerCustomerParty().getParty().getPerson().get(0).getContact().getTelephone().contentEquals("")){
                                            text = text.replace("$phone_id",order.getBuyerCustomerParty().getParty().getPerson().get(0).getContact().getTelephone());
                                            r.setText(text,0);
                                        }
                                        else{
                                            text = text.replace("$phone_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$fax_supplier")){
                                        if(order.getSellerSupplierParty().getParty().getContact() != null && order.getSellerSupplierParty().getParty().getContact().getTelefax() != null){
                                            text = text.replace("$fax_supplier",order.getSellerSupplierParty().getParty().getContact().getTelefax());
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$fax_supplier","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$phone_supplier")){
                                        if(!order.getSellerSupplierParty().getParty().getPerson().get(0).getContact().getTelephone().contentEquals("")){
                                            text = text.replace("$phone_supplier",order.getSellerSupplierParty().getParty().getPerson().get(0).getContact().getTelephone());
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$phone_supplier","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$street_id")){
                                        if(order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getStreetName() != null){
                                            text = text.replace("$street_id",order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getStreetName());
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$street_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$building_id")){
                                        text = text.replace("$building_id",order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getBuildingNumber());
                                        r.setText(text,0);
                                    }
                                    if(text.contains("$country_supplier")){
                                        if(order.getSellerSupplierParty().getParty().getPostalAddress().getCountry().getName() != null){
                                            text = text.replace("$country_supplier",order.getSellerSupplierParty().getParty().getPostalAddress().getCountry().getName());
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$country_supplier","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$building_supplier")){
                                        text = text.replace("$building_supplier",order.getSellerSupplierParty().getParty().getPostalAddress().getBuildingNumber());
                                        r.setText(text,0);
                                    }
                                    if(text.contains("$street_supplier")){
                                        if(order.getSellerSupplierParty().getParty().getPostalAddress().getStreetName() != null){
                                            text = text.replace("$street_supplier",order.getSellerSupplierParty().getParty().getPostalAddress().getStreetName());
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$street_supplier","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$supplier_id")){
                                        text = text.replace("$supplier_id",order.getSellerSupplierParty().getParty().getName());
                                        r.setFontSize(14);
                                        r.setText(text,0);
                                    }
                                    if(text.contains("$name_id")){
                                        text = text.replace("$name_id",order.getBuyerCustomerParty().getParty().getPerson().get(0).getFirstName()+" "+order.getBuyerCustomerParty().getParty().getPerson().get(0).getFamilyName());
                                        r.setItalic(true);
                                        r.setBold(true);
                                        r.setText(text,0);
                                    }
                                    if(text.contains("$title_id")){
                                        if(order.getBuyerCustomerParty().getParty().getPerson().get(0).getRole().size() > 0){
                                            text = text.replace("$title_id",order.getBuyerCustomerParty().getParty().getPerson().get(0).getRole().get(0));
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$title_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$item_id")){
                                        text = text.replace("$item_id",order.getOrderLine().get(0).getLineItem().getItem().getName());
                                        r.setText(text,0);
                                    }
                                    if(text.contains("$product_id")){
                                        text = text.replace("$product_id",order.getOrderLine().get(0).getLineItem().getItem().getManufacturersItemIdentification().getID());
                                        r.setText(text,0);
                                    }
                                    if(text.contains("$quantity_id")){
                                        if(totalPriceExists){
                                            text = text.replace("$quantity_id",new DecimalFormat(".00").format(order.getOrderLine().get(0).getLineItem().getQuantity().getValue())+" "+order.getOrderLine().get(0).getLineItem().getQuantity().getUnitCode());
                                            r.setText(text,0);
                                        }
                                        else{
                                            text = text.replace("$quantity_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$price_id")){
                                        if(totalPriceExists){
                                            text = text.replace("$price_id",new DecimalFormat(".00").format((order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getValue().divide(order.getOrderLine().get(0).getLineItem().getPrice().getBaseQuantity().getValue(),2)))+" "+order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getCurrencyID());
                                            r.setText(text,0);
                                        }
                                        else{
                                            text = text.replace("$price_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$total_id")){
                                        if(totalPriceExists){
                                            text = text.replace("$total_id",new DecimalFormat(".00").format((order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getValue().divide(order.getOrderLine().get(0).getLineItem().getPrice().getBaseQuantity().getValue(),2)).multiply(order.getOrderLine().get(0).getLineItem().getQuantity().getValue()))+
                                                    " "+order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getCurrencyID());
                                            r.setText(text,0);
                                        }
                                        else if(order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getValue() != null){
                                            text = text.replace("$total_id",new DecimalFormat(".00").format(order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getValue())+
                                                    " "+order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getCurrencyID());
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$total_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$city_id")){
                                        if(order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCityName() != null){
                                            text = text.replace("$city_id",order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCityName());
                                            r.setText(text,0);
                                        }
                                        else{
                                            text = text.replace("$city_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$zip_id")){
                                        if(order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getPostalZone() != null){
                                            text = text.replace("$zip_id",order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getPostalZone());
                                            r.setText(text,0);
                                        }
                                        else{
                                            text = text.replace("$zip_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$city_invoice_id")){
                                        if(order.getBuyerCustomerParty().getParty().getPostalAddress().getCityName() != null){
                                            text = text.replace("$city_invoice_id",order.getBuyerCustomerParty().getParty().getPostalAddress().getCityName());
                                            r.setText(text,0);
                                        }
                                        else{
                                            text = text.replace("$city_invoice_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$zip_invoice_id")){
                                        if(order.getBuyerCustomerParty().getParty().getPostalAddress().getPostalZone() != null){
                                            text = text.replace("$zip_invoice_id",order.getBuyerCustomerParty().getParty().getPostalAddress().getPostalZone());
                                            r.setText(text,0);
                                        }
                                        else{
                                            text = text.replace("$zip_invoice_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$city_supplier")){
                                        if(order.getSellerSupplierParty().getParty().getPostalAddress().getCityName() != null){
                                            text = text.replace("$city_supplier",order.getSellerSupplierParty().getParty().getPostalAddress().getCityName());
                                            r.setText(text,0);
                                        }
                                        else{
                                            text = text.replace("$city_supplier","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$zip_supplier")){
                                        if(order.getSellerSupplierParty().getParty().getPostalAddress().getPostalZone() != null){
                                            text = text.replace("$zip_supplier",order.getSellerSupplierParty().getParty().getPostalAddress().getPostalZone());
                                            r.setText(text,0);
                                        }
                                        else{
                                            text = text.replace("$zip_supplier","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$order_delPer")){
                                        BigDecimal value = order.getOrderLine().get(0).getLineItem().getDelivery().get(0).getRequestedDeliveryPeriod().getDurationMeasure().getValue();
                                        if(value != null){
                                            text = text.replace("$order_delPer",new DecimalFormat("##").format(value)+" "
                                                    +order.getOrderLine().get(0).getLineItem().getDelivery().get(0).getRequestedDeliveryPeriod().getDurationMeasure().getUnitCode());
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$order_delPer","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$order_paymentMeans")){
                                        text = text.replace("$order_paymentMeans",order.getPaymentMeans().getPaymentMeansCode().getValue());
                                        r.setText(text,0);
                                    }
                                    if(text.contains("$payment_id")){
                                        if(order.getPaymentTerms().getTradingTerms().size() > 0){
                                            text = text.replace("$payment_id",getTradingTerms(order.getPaymentTerms().getTradingTerms()));
                                            r.setText(text,0);
                                        }
                                        else {
                                            text = text.replace("$payment_id","");
                                            r.setText(text,0);
                                        }
                                    }
                                    if(text.contains("$issue_id")){
                                        text = text.replace("$issue_id",getDate("issue",order.getID()));
                                        r.setText(text,0);
                                    }
                                    if(text.contains("$conf_id")){
                                        text = text.replace("$conf_id",getDate("confirmation",order.getID()));
                                        r.setText(text,0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e){
            logger.error("Failed to fill in 'Company Purchase Details.pdf' for the order with id : {}",order.getID(),e);
        }
        return document;
    }

    private void addDocxToZipFile(String fileName,XWPFDocument document, ZipOutputStream zos) {
        try{
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            PdfOptions options = PdfOptions.create();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PdfConverter.getInstance().convert(document, bos, options);

            bos.writeTo(zos);
            zos.closeEntry();
            bos.close();
        }
        catch (Exception e){
            logger.error("Failed to create zip file",e);
        }
    }

    private void getAndPopulateClauses(OrderType order,ZipOutputStream zos,XWPFDocument document){
        try{
            if(order.getContract().size() <= 0){
                // Negotiation
                XWPFTable table = document.getTableArray(document.getTables().size()-1);
                document.removeBodyElement(document.getPosOfTable(table));
                // PPAP
                table = document.getTableArray(document.getTables().size()-1);
                document.removeBodyElement(document.getPosOfTable(table));
                // Item Information Request
                table = document.getTableArray(document.getTables().size()-1);
                document.removeBodyElement(document.getPosOfTable(table));
                return;
            }

            boolean ItemDetailsDirectoryCreated = false;
            boolean technicalDataSheetDirectoryCreated = false;

            boolean PPAPClauseExists = false;
            boolean negotiationClauseExists = false;

            int numberOfItemInformationRequest = 0;

            for(ClauseType clause : order.getContract().get(0).getClause()) {
                if (clause.getType().contentEquals("ITEM_DETAILS")) {
                    numberOfItemInformationRequest++;
                }
            }

            for(ClauseType clause : order.getContract().get(0).getClause()){
                if(clause.getType().contentEquals("PPAP")){
                    PPAPClauseExists = true;

                    ZipEntry zipEntry = new ZipEntry("PPAPDocuments/");
                    zos.putNextEntry(zipEntry);

                    createPPAPEntry(document,zos,clause);
                }
                else if(clause.getType().contentEquals("ITEM_DETAILS")){
                    if(!ItemDetailsDirectoryCreated){
                        ZipEntry zipEntry = new ZipEntry("TechnicalDataSheetResponses/");
                        zos.putNextEntry(zipEntry);
                        ItemDetailsDirectoryCreated = true;
                    }
                    if(!technicalDataSheetDirectoryCreated){
                        ZipEntry zipEntry = new ZipEntry("TechnicalDataSheets/");
                        zos.putNextEntry(zipEntry);
                        technicalDataSheetDirectoryCreated = true;
                    }

                    createItemDetailsEntry(document,zos,clause,numberOfItemInformationRequest);
                    numberOfItemInformationRequest--;
                }
                else if(clause.getType().contentEquals("NEGOTIATION")){
                    negotiationClauseExists = true;
                    createNegotiationEntry(document,clause);
                }
            }

            if(!negotiationClauseExists){
                document.removeBodyElement(document.getPosOfTable(getTable(document,"Negotiation")));
            }
            if(!PPAPClauseExists){
                document.removeBodyElement(document.getPosOfTable(getTable(document,"PPAP")));
            }
            if(!ItemDetailsDirectoryCreated){
                document.removeBodyElement(document.getPosOfTable(getTable(document,"Item Information Request")));
            }
        }
        catch (Exception e){
            logger.error("Failed to create entries for clauses",e);
        }

    }

    private void createPPAPEntry(XWPFDocument document,ZipOutputStream zos,ClauseType clause){
        try {
            PpapResponseType ppapResponse = (PpapResponseType) DocumentDAOUtility.getUBLDocument(((DocumentClauseType) clause).getClauseDocumentRef().getID(), DocumentType.PPAPRESPONSE);

            PpapRequestType ppapRequest = (PpapRequestType) DocumentDAOUtility.getUBLDocument(ppapResponse.getPpapDocumentReference().getID(),DocumentType.PPAPREQUEST);
            Map<String,List<String>> map = new HashMap<>();
            for(String documentType:ppapRequest.getDocumentType()){
                map.put(documentType,new ArrayList<>());
            }

            for(DocumentReferenceType documentReference : ppapResponse.getRequestedDocument()){
                byte[] bytes = documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry2 = new ZipEntry("PPAPDocuments/" +documentReference.getDocumentType()+"/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry2);
                bos.writeTo(zos);

                map.get(documentReference.getDocumentType()).add(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
            }

            XWPFTable table = getTable(document,"PPAP");

            // Traverse the map
            Set set = map.entrySet();
            Iterator iterator = set.iterator();
            while (iterator.hasNext()){
                Map.Entry entry = (Map.Entry)iterator.next();

                XWPFTableRow row = table.insertNewTableRow(2);
                row.addNewTableCell();
                row.addNewTableCell();


                row.getCell(0).getParagraphs().get(0).createRun().setText(entry.getKey().toString());

                if(entry.getValue().toString().contentEquals("[]")){
                    row.getCell(1).getParagraphs().get(0).createRun().setText("-");
                }
                else{
                    ArrayList arrayList = (ArrayList) entry.getValue();

                    for(int i=0;i < arrayList.size();i++){
                        XWPFRun run = row.getCell(1).getParagraphs().get(0).createRun();
                        run.setText(arrayList.get(i)+"\n");
                        run.setItalic(true);
                    }
                }

                setSpace(row.getCell(1).getParagraphs().get(0),cell_space);
                addRightBorder(row);
            }

            if(ppapRequest.getNote() != null){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-2);
                XWPFRun run = row.getCell(1).getParagraphs().get(0).createRun();
                run.setText(ppapRequest.getNote());
                run.setItalic(true);
            }
            else {
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-2);
                XWPFRun run = row.getCell(1).getParagraphs().get(0).createRun();
                run.setText("-");
            }
            if(ppapResponse.getNote() != null){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-1);
                XWPFRun run = row.getCell(1).getParagraphs().get(0).createRun();
                run.setText(ppapResponse.getNote());
                run.setItalic(true);
            }
            else{
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-1);
                XWPFRun run = row.getCell(1).getParagraphs().get(0).createRun();
                run.setText("-");
            }

            table.setInsideVBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, null);
        }
        catch (Exception e){
            logger.error("Failed to create PPAP entry",e);
        }
    }

    private void createItemDetailsEntry(XWPFDocument document,ZipOutputStream zos,ClauseType clause,int id){
        try {
            if(firstIIR){
                firstIIR = false;
                XWPFTable table = document.getTableArray(document.getTables().size()-1);

                fillItemDetails(table,zos,clause,id);
            }
            else{
                XWPFTable table = getTable(document,"Item Information Request");

                CTTbl ctTbl = CTTbl.Factory.newInstance();
                ctTbl.set(table.getCTTbl());

                XWPFTable copyTable = new XWPFTable(ctTbl,document);

                clearCell(copyTable.getRow(2).getCell(1));
                clearCell(copyTable.getRow(3).getCell(1));
                clearCell(copyTable.getRow(5).getCell(1));
                clearCell(copyTable.getRow(6).getCell(1));

                fillItemDetails(copyTable,zos,clause,id);
                int pos = document.getPosOfTable(getTable(document,"Item Information Request"));
                document.insertTable(pos,copyTable);
            }

        }
        catch (Exception e){
            logger.error("Failed to create item details entry",e);
        }
    }

    private void createNegotiationEntry(XWPFDocument document,ClauseType clause){
        QuotationType quotation = (QuotationType) DocumentDAOUtility.getUBLDocument(((DocumentClauseType) clause).getClauseDocumentRef().getID(), DocumentType.QUOTATION);

        XWPFTable table = getTable(document,"Negotiation");
        int totalPriceExists = 0;
        try {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        for (XWPFRun r : p.getRuns()) {
                            String text = r.getText(0);
                            if(text != null){
                                if(text.contains("$nego_price")){
                                    BigDecimal value = quotation.getQuotationLine().get(0).getLineItem().getPrice().getPriceAmount().getValue();
                                    if(value != null) {
                                        text = text.replace("$nego_price",new DecimalFormat(".00").format(value)+" "+
                                                quotation.getQuotationLine().get(0).getLineItem().getPrice().getPriceAmount().getCurrencyID());
                                        r.setText(text,0);

                                        totalPriceExists++;
                                    }
                                    else {
                                        text = text.replace("$nego_price","");
                                        r.setText(text,0);
                                    }
                                }
                                if(text.contains("$nego_base")){
                                    BigDecimal value = quotation.getQuotationLine().get(0).getLineItem().getPrice().getBaseQuantity().getValue();
                                    String unit = quotation.getQuotationLine().get(0).getLineItem().getPrice().getBaseQuantity().getUnitCode();
                                    if(value != null && unit != null){
                                        text = text.replace("$nego_base",new DecimalFormat(".00").format(value)+" "+unit);
                                        r.setText(text,0);

                                        totalPriceExists++;
                                    }
                                    else {
                                        text = text.replace("$nego_base","");
                                        r.setText(text,0);
                                    }
                                }
                                if(text.contains("$nego_quan")){
                                    BigDecimal value = quotation.getQuotationLine().get(0).getLineItem().getQuantity().getValue();
                                    if(value != null){
                                        text = text.replace("$nego_quan",new DecimalFormat(".00").format(value)+" "+
                                                quotation.getQuotationLine().get(0).getLineItem().getQuantity().getUnitCode());
                                        r.setText(text,0);

                                        totalPriceExists++;
                                    }
                                    else {
                                        text = text.replace("$nego_quan","");
                                        r.setText(text,0);
                                    }
                                }
                                if(text.contains("$nego_total")){
                                    if(totalPriceExists == 3){
                                        text = text.replace("$nego_total",new DecimalFormat(".00").format(quotation.getQuotationLine().get(0).getLineItem().getPrice().getPriceAmount().getValue().divide(quotation.getQuotationLine().get(0).getLineItem().getPrice().getBaseQuantity().getValue(),2).multiply(quotation.getQuotationLine().get(0).getLineItem().getQuantity().getValue()))+
                                                " "+quotation.getQuotationLine().get(0).getLineItem().getPrice().getPriceAmount().getCurrencyID());
                                        r.setText(text,0);
                                    }
                                    else {
                                        text = text.replace("$nego_total","");
                                        r.setText(text,0);
                                    }
                                }
                                if(text.contains("$nego_means")){
                                    text = text.replace("$nego_means",quotation.getPaymentMeans().getPaymentMeansCode().getValue());
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_terms")){
                                    text = text.replace("$nego_terms",getTradingTerms(quotation.getPaymentTerms().getTradingTerms()));
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_incoterms")){
                                    String incoterms = quotation.getQuotationLine().get(0).getLineItem().getDeliveryTerms().getIncoterms();
                                    if(incoterms != null){
                                        text = text.replace("$nego_incoterms",incoterms);
                                        r.setText(text,0);
                                    }
                                    else {
                                        text = text.replace("$nego_incoterms","");
                                        r.setText(text,0);
                                    }
                                }
                                if(text.contains("$nego_delPer")){
                                    BigDecimal value = quotation.getQuotationLine().get(0).getLineItem().getDelivery().get(0).getRequestedDeliveryPeriod().getDurationMeasure().getValue();
                                    if(value != null){
                                        text = text.replace("$nego_delPer",new DecimalFormat("##").format(value)+" "+quotation.getQuotationLine().get(0).getLineItem().getDelivery().get(0).getRequestedDeliveryPeriod().getDurationMeasure().getUnitCode());
                                        r.setText(text,0);
                                    }
                                    else {
                                        text = text.replace("$nego_delPer","");
                                        r.setText(text,0);
                                    }

                                }
                                if(text.contains("$nego_street")){
                                    text = text.replace("$nego_street",quotation.getQuotationLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getStreetName());
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_building")){
                                    text = text.replace("$nego_building",quotation.getQuotationLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getBuildingNumber());
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_city")){
                                    text = text.replace("$nego_city",quotation.getQuotationLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCityName());
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_postal")){
                                    text = text.replace("$nego_postal",quotation.getQuotationLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getPostalZone());
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_country")){
                                    String country = quotation.getQuotationLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCountry().getName();
                                    if(country == null){
                                        country = "";
                                    }
                                    text = text.replace("$nego_country",country);
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_specTerms")){
                                    String specialTerms = quotation.getQuotationLine().get(0).getLineItem().getDeliveryTerms().getSpecialTerms();
                                    if(specialTerms != null){
                                        text = text.replace("$nego_specTerms",specialTerms);
                                        r.setText(text,0);
                                    }
                                    else {
                                        text = text.replace("$nego_specTerms","");
                                        r.setText(text,0);
                                    }
                                }
                                if(text.contains("$nego_validity")){
                                    BigDecimal value = quotation.getQuotationLine().get(0).getLineItem().getWarrantyValidityPeriod().getDurationMeasure().getValue();
                                    String unit = quotation.getQuotationLine().get(0).getLineItem().getWarrantyValidityPeriod().getDurationMeasure().getUnitCode();
                                    if(value != null && unit != null){
                                        text = text.replace("$nego_validity",new DecimalFormat("##").format(value)+" "+unit);
                                        r.setText(text,0);
                                    }
                                    else {
                                        text = text.replace("$nego_validity","");
                                        r.setText(text,0);
                                    }
                                }
                                if(text.contains("$nego_response")){
                                    text = text.replace("$nego_response",quotation.getDocumentStatusCode().getName());
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_reason")){
                                    text = text.replace("$nego_reason",quotation.getDocumentStatusReasonCode().getName());
                                    r.setText(text,0);
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e){
            logger.error("Failed to create negotiation entry",e);
        }

    }

    private void fillItemDetails(XWPFTable table,ZipOutputStream zos,ClauseType clause,int id){
        try {
            ItemInformationResponseType itemDetails = (ItemInformationResponseType) DocumentDAOUtility.getUBLDocument(((DocumentClauseType) clause).getClauseDocumentRef().getID(), DocumentType.ITEMINFORMATIONRESPONSE);

            ItemInformationRequestType itemInformationRequest = (ItemInformationRequestType) DocumentDAOUtility
                    .getUBLDocument(itemDetails.getItemInformationRequestDocumentReference().getID(),DocumentType.ITEMINFORMATIONREQUEST);
            List<DocumentReferenceType> documentReferences = itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getItemSpecificationDocumentReference();

            if(documentReferences.size() == 0){
                table.getRow(2).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            else {
                byte[] bytes = documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry = new ZipEntry("TechnicalDataSheets/ItemInformationRequest"+id+"/"+ documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry);
                bos.writeTo(zos);

                XWPFRun run2 = table.getRow(2).getCell(1).getParagraphs().get(0).createRun();
                run2.setText(documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                run2.setItalic(true);
            }

            documentReferences = itemDetails.getItem().get(0).getItemSpecificationDocumentReference();

            if(documentReferences.size() == 0){
                table.getRow(3).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            else {
                byte[] bytes = documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry = new ZipEntry("TechnicalDataSheetResponses/ItemInformationRequest"+id+"/"+documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry);
                bos.writeTo(zos);

                XWPFRun run3 = table.getRow(3).getCell(1).getParagraphs().get(0).createRun();
                run3.setText(documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                run3.setItalic(true);
            }

            if(itemInformationRequest.getNote().get(0) != null) {
                XWPFRun run5 = table.getRow(5).getCell(1).getParagraphs().get(0).createRun();
                run5.setText(itemInformationRequest.getNote().get(0));
                run5.setItalic(true);
            }
            else {
                table.getRow(5).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }

            if(itemDetails.getNote().get(0) != null) {
                XWPFRun run6 = table.getRow(6).getCell(1).getParagraphs().get(0).createRun();
                run6.setText(itemDetails.getNote().get(0));
                run6.setItalic(true);
            }
            else {
                table.getRow(6).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
        }
        catch (Exception e){
            logger.error("Failed to fill item details",e);
        }

    }

    private boolean checkTotalPriceExistsInOrder(OrderType order){
        return order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getValue() != null &&
                order.getOrderLine().get(0).getLineItem().getPrice().getBaseQuantity().getValue() !=null &&
                order.getOrderLine().get(0).getLineItem().getQuantity().getValue() != null &&
                order.getOrderLine().get(0).getLineItem().getPrice().getBaseQuantity().getUnitCode().contentEquals(order.getOrderLine().get(0).getLineItem().getQuantity().getUnitCode());
    }

    private String getTradingTerms(List<TradingTermType> tradingTerms){
        List<String> selectedTradingTerms = new ArrayList<>();

        int size = tradingTerms.size();
        for(int i = 0; i < size;i++){
            TradingTermType tradingTerm = tradingTerms.get(i);
            String result = "";
            if(tradingTerm.getID().contains("Values")){
                result = String.format(tradingTerm.getTradingTermFormat(),tradingTerm.getValue().toArray());
                selectedTradingTerms.add(result);
            }
            else {
                if(tradingTerm.getValue().get(0).toLowerCase().contentEquals("true")){
                    result = tradingTerm.getDescription();
                    selectedTradingTerms.add(result);
                }
            }
        }

        return StringUtils.join(selectedTradingTerms,',');
    }

    private String constructAddress(String company_name,AddressType address){
        String country = address.getCountry().getName();
        if(country == null){
            country = "";
        }
        String addr = company_name + " at " +address.getStreetName()+","+address.getBuildingNumber()+","+address.getCityName()+","+country+","+address.getPostalZone();
        addr = addr.replace(",,",",");
        return addr;
    }

    private void addRightBorder(XWPFTableRow row){
        CTTc ctTc = row.getCell(1).getCTTc();
        CTTcPr ctTcPr = ctTc.addNewTcPr();
        ctTcPr.addNewVMerge().setVal(STMerge.RESTART);
        ctTcPr.addNewTcBorders().addNewRight();
    }

    private void setSpace(XWPFParagraph paragraph,int value){
        CTPPr ppr = paragraph.getCTP().getPPr();
        if(ppr == null){
            ppr = paragraph.getCTP().addNewPPr();
        }
        CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
        spacing.setAfter(BigInteger.valueOf(value));
    }

    private void clearCell(XWPFTableCell cell){
        for(XWPFRun run : cell.getParagraphs().get(0).getRuns()){
            run.setText("",0);
        }
    }

    private void setColor(XWPFRun run,String value){
        CTR ctr = run.getCTR();
        CTRPr ctrPr = ctr.getRPr();

        if(ctrPr == null){
            ctrPr = ctr.addNewRPr();
        }

        CTColor ctColor = CTColor.Factory.newInstance();
        ctColor.setVal(value);
        ctrPr.setColor(ctColor);
    }

    private XWPFTable getTable(XWPFDocument document,String type){
        XWPFTable table = null;

        for(IBodyElement bodyElement : document.getBodyElements()){
            if(bodyElement.getElementType() == BodyElementType.TABLE){
                XWPFTable tbl = (XWPFTable) bodyElement;
                if(tbl.getText().contains(type)){
                    table = tbl;
                    break;
                }
            }
        }
        return table;
    }

    private String getDate(String type,String orderId){
        String date = "";
        if(type.contentEquals("issue")){
            ProcessDocumentMetadata processDocumentMetadata = DocumentDAOUtility.getDocumentMetadata(orderId);
            DateTimeFormatter bpFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
            LocalDateTime localTime = bpFormatter.parseLocalDateTime(processDocumentMetadata.getSubmissionDate());
            DateTime issueDate = new DateTime(localTime.toDateTime(), DateTimeZone.UTC);

            DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
            date = issueDate.toString(format);
        }
        else {
            ProcessDocumentMetadata responseMetadata = DocumentDAOUtility.getCorrespondingResponseMetadata(orderId,DocumentType.ORDER);
            DateTimeFormatter bpFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
            LocalDateTime localTime = bpFormatter.parseLocalDateTime(responseMetadata.getSubmissionDate());
            DateTime issueDate = new DateTime(localTime.toDateTime(), DateTimeZone.UTC);

            DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
            date = issueDate.toString(format);
        }
        return date;

    }

}