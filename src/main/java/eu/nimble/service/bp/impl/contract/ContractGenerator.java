package eu.nimble.service.bp.impl.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType;
import eu.nimble.service.model.ubl.iteminformationresponse.ItemInformationResponseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.ppaprequest.PpapRequestType;
import eu.nimble.service.model.ubl.ppapresponse.PpapResponseType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.JsonSerializationUtility;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ContractGenerator {
    private final Logger logger = LoggerFactory.getLogger(ContractGenerator.class);

    // list ids
    private final String incoterms_list_id = "INCOTERMS_LIST";
    private final String country_list_id = "COUNTRY_LIST";
    private final String payment_means_list_id = "PAYMENT_MEANS_LIST";

    private final String red_hex = "DC143C";
    private final String cyan_hex = "00FFFF";

    private final int logo_space = 300;
    private final int cell_space = 200;

    private final double purchaseDetailsScale = 60.0;
    private final double termsScale = 100.0;

    private boolean firstIIR = true;

    public void generateContract(String orderId,ZipOutputStream zos){
        OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId,DocumentType.ORDER);

        XWPFDocument orderTermsAndConditions = fillOrderTermsAndConditions(order);
        XWPFDocument purchaseDetails = fillPurchaseDetails(order);

        getAndPopulateClauses(order,zos,purchaseDetails);
        // additional documents of order
        purchaseDetails = createOrderAdditionalDocuments(order,zos,purchaseDetails);

        addDocxToZipFile("Standard Purchase Order Terms and Conditions.pdf",orderTermsAndConditions,zos);
        addDocxToZipFile("Company Purchase Details.pdf",purchaseDetails,zos);

    }

    public List<ClauseType> getTermsAndConditions(String orderId,String rfqId, String sellerPartyId, String buyerPartyId, String incoterms, String tradingTerm, String bearerToken){
        List<ClauseType> clauses = new ArrayList<>();

        OrderType order = null;
        RequestForQuotationType requestForQuotation = null;

        if(orderId != null){
            order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId,DocumentType.ORDER);
        } else if(rfqId != null){
            requestForQuotation = (RequestForQuotationType) DocumentPersistenceUtility.getUBLDocument(rfqId,DocumentType.REQUESTFORQUOTATION);
        }

        try {
            // use Order's terms and conditions
            if(order != null){
                clauses = getTermsAndConditionsContract(order).getClause();
            }
            // use Request for quotation's terms and conditions
            else if(requestForQuotation != null){
                clauses = requestForQuotation.getTermOrCondition();
            }
            // create clauses
            else {
                InputStream inputStream = null;
                try {
                    // read clauses from the json file
                    inputStream = ContractGenerator.class.getResourceAsStream("/contract-bundle/order_terms_and_conditions.json");

                    logger.info("Input stream is null: {}",inputStream == null);

                    String fileContent = IOUtils.toString(inputStream);

                    ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

                    // Clauses
                    clauses = objectMapper.readValue(fileContent, new TypeReference<List<ClauseType>>() {});

                    // update some trading terms using the party info
                    PartyType supplierParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,sellerPartyId);
                    PartyType customerParty = null;
                    if(buyerPartyId != null){
                        customerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,buyerPartyId);
                    }

                    for(ClauseType clause : clauses){
                        if(clause.getID().contentEquals("PURCHASE ORDER TERMS AND CONDITIONS")){
                            for(TradingTermType tradingTermType : clause.getTradingTerms()){
                                if(tradingTermType.getID().contentEquals("$seller_id")){
                                    TextType text = new TextType();
                                    text.setLanguageID("en");
                                    text.setValue(supplierParty.getPartyName().get(0).getName().getValue());
                                    tradingTermType.getValue().setValue(Collections.singletonList(text));
                                }
                                else if(tradingTermType.getID().contentEquals("$buyer_id") && customerParty != null){
                                    TextType text = new TextType();
                                    text.setLanguageID("en");
                                    text.setValue(customerParty.getPartyName().get(0).getName().getValue());
                                    tradingTermType.getValue().setValue(Collections.singletonList(text));
                                }
                            }
                        }
                        else if(clause.getID().contentEquals("INVOICES, PAYMENT, AND TAXES")){
                            for(TradingTermType tradingTermType : clause.getTradingTerms()){
                                if(tradingTermType.getID().contentEquals("$payment_id") && tradingTerm != null){
                                    CodeType code = new CodeType();
                                    code.setValue(tradingTerm);
                                    code.setListID(payment_means_list_id);
                                    tradingTermType.getValue().setValueCode(Collections.singletonList(code));
                                }
                            }
                        }
                        else if(clause.getID().contentEquals("MISCELLANEOUS")){
                            for(TradingTermType tradingTermType : clause.getTradingTerms()){
                                if(tradingTermType.getID().contentEquals("$notices_id") && customerParty != null){
                                    TextType text = new TextType();
                                    text.setLanguageID("en");
                                    text.setValue(constructAddress(customerParty.getPartyName().get(0).getName().getValue(),customerParty.getPostalAddress()));
                                    tradingTermType.getValue().setValue(Collections.singletonList(text));
                                }
                                else if(tradingTermType.getID().contentEquals("$incoterms_id") && !incoterms.contentEquals("")){
                                    CodeType code = new CodeType();
                                    code.setValue(incoterms);
                                    code.setListID(incoterms_list_id);
                                    tradingTermType.getValue().setValueCode(Collections.singletonList(code));
                                }
                                else if(tradingTermType.getID().contentEquals("$seller_website") && supplierParty.getWebsiteURI() != null && !supplierParty.getWebsiteURI().contentEquals("")){
                                    TextType text = new TextType();
                                    text.setLanguageID("en");
                                    text.setValue(supplierParty.getWebsiteURI());
                                    tradingTermType.getValue().setValue(Collections.singletonList(text));
                                }
                                else if(tradingTermType.getID().contentEquals("$seller_tel") && !supplierParty.getPerson().get(0).getContact().getTelephone().contentEquals("")){
                                    TextType text = new TextType();
                                    text.setLanguageID("en");
                                    text.setValue(supplierParty.getPerson().get(0).getContact().getTelephone());
                                    tradingTermType.getValue().setValue(Collections.singletonList(text));
                                }
                                else if(tradingTermType.getID().contentEquals("$buyer_country") &&  customerParty != null && customerParty.getPostalAddress().getCountry().getName() != null){
                                    CodeType code = new CodeType();
                                    code.setValue(customerParty.getPostalAddress().getCountry().getName().getValue());
                                    code.setListID(country_list_id);
                                    tradingTermType.getValue().setValueCode(Collections.singletonList(code));
                                }
                            }
                        }
                    }
                }
                catch (Exception e){
                    logger.error("Failed to create order terms and conditions for the order with id : {}", order.getID() != null ? order.getID() : "", e);
                }
                finally {
                    if(inputStream != null){
                        inputStream.close();
                    }
                }
            }
        }
        catch (Exception e){
            logger.error("Failed to create order terms and conditions for the order with id : {}", order.getID() != null ? order.getID() : "", e);
        }

        return clauses;
    }

    private XWPFDocument fillOrderTermsAndConditions(OrderType order){

        XWPFDocument document = null;
        try {
            InputStream file = ContractGenerator.class.getResourceAsStream("/contract-bundle/Standard_Purchase_Order_Terms_and_Conditions.docx");

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
                    else if(text != null && text.contains("$document_name")){
                        text = text.replace("$document_name","Purchase Order Terms and Conditions");

                        paragraph.setAlignment(ParagraphAlignment.CENTER);
                        run.setText(text,0);
                        run.setBold(true);
                        setColor(run,cyan_hex);
                    }
                }
            }

            // get clauses map
            Map<String,ClauseType> clausesMap = getClausesMap(getTermsAndConditionsContract(order).getClause());

            int rowIndex = 0;
            for (XWPFTable tbl : document.getTables() ) {
                for (XWPFTableRow row : tbl.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        // get the clause
                        ClauseType clause = clausesMap.get("$section_"+rowIndex);
                        // text of the section
                        String text = clause.getContent().get(0).getValue();
                        // get the paragraph
                        XWPFParagraph paragraph = cell.getParagraphs().get(0);
                        // section text which does not contain the name of section
                        String sectionText;
                        if(rowIndex == 0){
                            // get the section text
                            int newLineIndex = text.indexOf("\n\n")+2;
                            sectionText = text.substring(newLineIndex);
                        }
                        else{
                            // get the section name
                            int semicolonIndex = text.indexOf(":");
                            String sectionName = text.substring(0,semicolonIndex+1);
                            // use first run to represent the name of section
                            paragraph.getRuns().get(0).setText(sectionName,0);
                            paragraph.getRuns().get(0).setBold(true);
                            // get the section text
                            sectionText = text.substring(semicolonIndex+1);
                        }
                        fillTheRow(sectionText,paragraph,clause.getTradingTerms());
                    }
                    rowIndex++;
                }
            }
        }
        catch (Exception e){
            logger.error("Failed to fill in 'Standard Purchase Order Terms and Conditions.pdf' for the order with id : {}",order.getID(),e);
        }
        return document;
    }

    private void fillTheRow(String sectionText,XWPFParagraph paragraph,List<TradingTermType> tradingTerms){
        int indexOfParameter = sectionText.indexOf("$");
        while (indexOfParameter != -1){
            paragraph.createRun().setText(sectionText.substring(0,indexOfParameter),0);
            sectionText = sectionText.substring(indexOfParameter);

            // find the parameter
            int spaceIndex = sectionText.indexOf(" ");
            String parameter = sectionText.substring(0,spaceIndex);

            XWPFRun run = paragraph.createRun();
            for(TradingTermType tradingTerm : tradingTerms){
                if(tradingTerm.getID().contentEquals(parameter)){
                    // find the value of parameter
                    String value = "";
                    if(tradingTerm.getValue().getValueQualifier().contentEquals("STRING")){
                        value = tradingTerm.getValue().getValue().get(0).getValue();
                    } else if(tradingTerm.getValue().getValueQualifier().contentEquals("NUMBER")){
                        value = new DecimalFormat("##").format(tradingTerm.getValue().getValueDecimal().get(0));
                    } else if(tradingTerm.getValue().getValueQualifier().contentEquals("QUANTITY")){
                        value = new DecimalFormat("##").format(tradingTerm.getValue().getValueQuantity().get(0).getValue()) + " " + tradingTerm.getValue().getValueQuantity().get(0).getUnitCode();
                    } else if(tradingTerm.getValue().getValueQualifier().contentEquals("CODE")){
                        value = tradingTerm.getValue().getValueCode().get(0).getValue();
                    }

                    run.setText(value,0);
                    run.setUnderline(UnderlinePatterns.SINGLE);
                    setColor(run,red_hex);

                    break;
                }
            }

            sectionText = sectionText.substring(spaceIndex);

            indexOfParameter = sectionText.indexOf("$");
        }

        paragraph.createRun().setText(sectionText,0);
    }

    // returns the contract storing Terms and Conditions details
    private ContractType getTermsAndConditionsContract(OrderType order){
        ContractType termsAndConditionsContract = null;
        for(ContractType contract : order.getContract()){
            for(ClauseType clause : contract.getClause()){
                if(clause.getType() == null){
                    termsAndConditionsContract =  contract;
                    break;
                }
            }
        }
        return termsAndConditionsContract;
    }

    // returns clause id - Clause map
    private Map<String,ClauseType> getClausesMap(List<ClauseType> clauses){
        Map<String,ClauseType> idClauseMap = new HashMap<>();
        for(ClauseType clause:clauses){
            String text = clause.getContent().get(0).getValue();
            // check whether the clause belongs to the first section or not
            int semicolonIndex = text.indexOf(":");
            // first section
            if(semicolonIndex == -1){
                idClauseMap.put("$section_0",clause);
                continue;
            }
            // get the section number
            int dotIndex = text.indexOf(".");
            String sectionNumber = text.substring(0,dotIndex);
            // add the text to the map
            idClauseMap.put("$section_"+sectionNumber,clause);
        }
        return idClauseMap;
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
                                        text = text.replace("$company_id",order.getBuyerCustomerParty().getParty().getPartyName().get(0).getName().getValue());
                                        r.setText(text,0);
                                    }
                                    if(text.contains("$country_invoice_id")){
                                        if(order.getBuyerCustomerParty().getParty().getPostalAddress().getCountry().getName() != null){
                                            text = text.replace("$country_invoice_id",order.getBuyerCustomerParty().getParty().getPostalAddress().getCountry().getName().getValue());
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
                                            text = text.replace("$country_id",order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCountry().getName().getValue());
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
                                        if(!CollectionUtils.isEmpty(order.getSellerSupplierParty().getParty().getPerson()) && !order.getSellerSupplierParty().getParty().getPerson().get(0).getContact().getTelephone().contentEquals("")){
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
                                            text = text.replace("$country_supplier",order.getSellerSupplierParty().getParty().getPostalAddress().getCountry().getName().getValue());
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
                                        text = text.replace("$supplier_id",order.getSellerSupplierParty().getParty().getPartyName().get(0).getName().getValue());
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
                                        text = text.replace("$item_id",order.getOrderLine().get(0).getLineItem().getItem().getName().get(0).getValue());
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
            // if there is no contract, simple remove the tables and return
            if(order.getContract().size() <= 0){
                // Negotiation
                XWPFTable table = document.getTableArray(document.getTables().size()-1);
                document.removeBodyElement(document.getPosOfTable(table));
                // PPAP
                table = document.getTableArray(document.getTables().size()-1);
                document.removeBodyElement(document.getPosOfTable(table));
                table = getTable(document,"PPAP Notes/Additional Documents");
                document.removeBodyElement(document.getPosOfTable(table));
                // Item Information Request
                table = document.getTableArray(document.getTables().size()-1);
                document.removeBodyElement(document.getPosOfTable(table));
                table = getTable(document,"Item Information Request Notes/Additional Documents");
                document.removeBodyElement(document.getPosOfTable(table));
                return;
            }

            // get clauses
            Map<DocumentType,List<ClauseType>> clauses = getClauses(order);

            // create PPAP entry
            for(ClauseType clause : clauses.get(DocumentType.PPAPRESPONSE)){
                ZipEntry zipEntry = new ZipEntry("PPAP/PPAPDocuments/");
                zos.putNextEntry(zipEntry);

                createPPAPEntry(document, zos, clause);
            }
            // create Negotiation entry
            for(ClauseType clause : clauses.get(DocumentType.QUOTATION)){
                createNegotiationEntry(document, clause, zos);
            }
            // create Item information entries
            int numberOfItemInformationRequest = clauses.get(DocumentType.ITEMINFORMATIONRESPONSE).size();
            if(numberOfItemInformationRequest > 0){
                // create directories
                ZipEntry zipEntry = new ZipEntry("ItemInformation/TechnicalDataSheetResponses/");
                zos.putNextEntry(zipEntry);
                zipEntry = new ZipEntry("ItemInformation/TechnicalDataSheets/");
                zos.putNextEntry(zipEntry);
                for(ClauseType clause : clauses.get(DocumentType.ITEMINFORMATIONRESPONSE)){
                    createItemDetailsEntry(document, zos, clause, numberOfItemInformationRequest);
                    numberOfItemInformationRequest--;
                }
            }
            if (clauses.get(DocumentType.QUOTATION).size() == 0) {
                document.removeBodyElement(document.getPosOfTable(getTable(document, "Negotiation")));
                document.removeBodyElement(document.getPosOfTable(getTable(document, "Negotiation Notes/Additional Documents")));
            }
            if (clauses.get(DocumentType.PPAPRESPONSE).size() == 0) {
                document.removeBodyElement(document.getPosOfTable(getTable(document, "PPAP")));
                document.removeBodyElement(document.getPosOfTable(getTable(document, "PPAP Notes/Additional Documents")));
            }
            if (clauses.get(DocumentType.ITEMINFORMATIONRESPONSE).size() == 0) {
                document.removeBodyElement(document.getPosOfTable(getTable(document, "Item Information Request")));
                document.removeBodyElement(document.getPosOfTable(getTable(document, "Item Information Request Notes/Additional Documents")));
            }
        }
        catch (Exception e){
            logger.error("Failed to create entries for clauses",e);
        }

    }

    private Map<DocumentType,List<ClauseType>> getClauses(OrderType order){
        List<ClauseType> PPAPResponse = new ArrayList<>();
        List<ClauseType> quotation = new ArrayList<>();
        List<ClauseType> itemInformationResponse = new ArrayList<>();
        // check clauses
        for(ClauseType clause : order.getContract().get(0).getClause()){
            if (clause.getType().contentEquals(eu.nimble.service.model.ubl.extension.ClauseType.DOCUMENT.toString())){
                String documentType = ((DocumentClauseType) clause).getClauseDocumentRef().getDocumentType();
                if (documentType.contentEquals(DocumentType.PPAPRESPONSE.toString())) {
                    PPAPResponse.add(clause);
                } else if (documentType.contentEquals(DocumentType.ITEMINFORMATIONRESPONSE.toString())) {
                    itemInformationResponse.add(clause);
                } else if (documentType.contentEquals(DocumentType.QUOTATION.toString())) {
                    quotation.add(clause);
                }
            }
        }
        // create the map
        Map<DocumentType,List<ClauseType>> map = new HashMap<>();
        map.put(DocumentType.PPAPRESPONSE,PPAPResponse);
        map.put(DocumentType.QUOTATION,quotation);
        map.put(DocumentType.ITEMINFORMATIONRESPONSE,itemInformationResponse);
        return map;
    }

    private XWPFDocument createOrderAdditionalDocuments(OrderType order,ZipOutputStream zos,XWPFDocument document){
        OrderResponseSimpleType orderResponse = DocumentPersistenceUtility.getOrderResponseDocumentByOrderId(order.getID());
        try {
            // request
            for(DocumentReferenceType documentReference : order.getAdditionalDocumentReference()){
                byte[] bytes = SpringBridge.getInstance().getBinaryContentService().retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry2 = new ZipEntry("Order/BuyerDocuments/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry2);
                bos.writeTo(zos);
            }
            // response
            if(orderResponse != null){
                for(DocumentReferenceType documentReference : orderResponse.getAdditionalDocumentReference()){
                    byte[] bytes = SpringBridge.getInstance().getBinaryContentService().retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                    bos.write(bytes,0,bytes.length);

                    ZipEntry zipEntry2 = new ZipEntry("Order/SellerDocuments/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                    zos.putNextEntry(zipEntry2);
                    bos.writeTo(zos);
                }
            }
            // notes in table
            XWPFTable table = getTable(document,"Order Notes/Additional Documents");
            if(order.getNote().size() == 0){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-2);
                XWPFRun run = row.getCell(1).getParagraphs().get(0).createRun();
                run.setText("-");
            }
            else {
                String requestNote = "";
                for(String note: order.getNote()){
                    requestNote += note + "\n";
                }
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-2);
                XWPFRun run = row.getCell(1).getParagraphs().get(0).createRun();
                run.setText(requestNote);
                run.setItalic(true);
            }

            if(orderResponse.getNote().size() == 0){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-1);
                XWPFRun run = row.getCell(1).getParagraphs().get(0).createRun();
                run.setText("-");
            }
            else {
                String responseNote = "";
                for(String note: orderResponse.getNote()){
                    responseNote += note + "\n";
                }
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-1);
                XWPFRun run = row.getCell(1).getParagraphs().get(0).createRun();
                run.setText(responseNote);
                run.setItalic(true);
            }

            // additional documents in table
            // request
            if(order.getAdditionalDocumentReference().size() == 0){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-2);
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText("-");
            }
            for(DocumentReferenceType documentReference : order.getAdditionalDocumentReference()){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-2);
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run.setItalic(true);
            }
            if(orderResponse.getAdditionalDocumentReference().size() == 0){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-1);
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText("-");
            }
            // response
            for(DocumentReferenceType documentReference : orderResponse.getAdditionalDocumentReference()){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-1);
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run.setItalic(true);
            }
        }
        catch (Exception e){
            logger.error("Failed to create Order additional documents entry",e);
        }
        return document;
    }

    private void createPPAPEntry(XWPFDocument document,ZipOutputStream zos,ClauseType clause){
        try {
            PpapResponseType ppapResponse = (PpapResponseType) DocumentPersistenceUtility.getUBLDocument(((DocumentClauseType) clause).getClauseDocumentRef().getID(), DocumentType.PPAPRESPONSE);

            PpapRequestType ppapRequest = (PpapRequestType) DocumentPersistenceUtility.getUBLDocument(ppapResponse.getPpapDocumentReference().getID(),DocumentType.PPAPREQUEST);
            Map<String,List<String>> map = new HashMap<>();
            for(String documentType:ppapRequest.getDocumentType()){
                map.put(documentType,new ArrayList<>());
            }

            for(DocumentReferenceType documentReference : ppapResponse.getRequestedDocument()){
                byte[] bytes = SpringBridge.getInstance().getBinaryContentService().retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry2 = new ZipEntry("PPAP/PPAPDocuments/" +documentReference.getDocumentType()+"/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry2);
                bos.writeTo(zos);

                map.get(documentReference.getDocumentType()).add(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
            }

            // additional documents
            // request
            for(DocumentReferenceType documentReference : ppapRequest.getAdditionalDocumentReference()){
                byte[] bytes = SpringBridge.getInstance().getBinaryContentService().retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry2 = new ZipEntry("PPAP/BuyerDocuments/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry2);
                bos.writeTo(zos);
            }
            // response
            for(DocumentReferenceType documentReference : ppapResponse.getAdditionalDocumentReference()){
                byte[] bytes = SpringBridge.getInstance().getBinaryContentService().retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry2 = new ZipEntry("PPAP/SellerDocuments/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry2);
                bos.writeTo(zos);
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

            table = getTable(document,"PPAP Notes/Additional Documents");
            // notes
            if(ppapRequest.getNote().size() == 0){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-2);
                row.getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            else {
                String requestNote = "";
                for(String note:ppapRequest.getNote()){
                    requestNote += note + "\n";
                }
                XWPFRun run5 = table.getRows().get(table.getNumberOfRows()-2).getCell(1).getParagraphs().get(0).createRun();
                run5.setText(requestNote);
                run5.setItalic(true);
            }

            if(ppapResponse.getNote().size() == 0){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-1);
                row.getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            else {
                String responseNote = "";
                for(String note:ppapResponse.getNote()){
                    responseNote += note + "\n";
                }
                XWPFRun run5 = table.getRows().get(table.getNumberOfRows()-1).getCell(1).getParagraphs().get(0).createRun();
                run5.setText(responseNote);
                run5.setItalic(true);
            }

            // additional documents in table
            // request
            XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-2);
            if(ppapRequest.getAdditionalDocumentReference().size() == 0){
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText("-");
            }
            for(DocumentReferenceType documentReference : ppapRequest.getAdditionalDocumentReference()){
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run.setItalic(true);
            }
            XWPFTableRow row1 = table.getRows().get(table.getNumberOfRows()-1);
            if(ppapResponse.getAdditionalDocumentReference().size() == 0){
                XWPFRun run = row1.getCell(3).getParagraphs().get(0).createRun();
                run.setText("-");
            }
            // response
            for(DocumentReferenceType documentReference : ppapResponse.getAdditionalDocumentReference()){
                XWPFRun run = row1.getCell(3).getParagraphs().get(0).createRun();
                run.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run.setItalic(true);
            }

            table.setInsideVBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, null);
        }
        catch (Exception e){
            logger.error("Failed to create PPAP entry",e);
        }
    }

    private void createItemDetailsEntry(XWPFDocument document,ZipOutputStream zos,ClauseType clause,int id){
        try {
            XWPFTable table = getTable(document,"Item Information Request");
            XWPFTable noteAndDocumentTable = getTable(document,"Item Information Request Notes/Additional Documents");
            if(firstIIR){
                firstIIR = false;
                fillItemDetails(document,table,noteAndDocumentTable,zos,clause,id);
            }
            else{
                CTTbl ctTbl = CTTbl.Factory.newInstance();
                ctTbl.set(table.getCTTbl());

                XWPFTable copyTable = new XWPFTable(ctTbl,document);

                clearCell(copyTable.getRow(2).getCell(1));
                clearCell(copyTable.getRow(3).getCell(1));

                ctTbl = CTTbl.Factory.newInstance();
                ctTbl.set(noteAndDocumentTable.getCTTbl());

                XWPFTable copyNoteAndDocumentTable = new XWPFTable(ctTbl,document);
                clearCell(copyNoteAndDocumentTable.getRow(2).getCell(1));
                clearCell(copyNoteAndDocumentTable.getRow(2).getCell(3));
                clearCell(copyNoteAndDocumentTable.getRow(3).getCell(1));
                clearCell(copyNoteAndDocumentTable.getRow(3).getCell(3));

                fillItemDetails(document,copyTable,copyNoteAndDocumentTable,zos,clause,id);
                int pos = document.getPosOfTable(getTable(document,"Item Information Request Notes/Additional Documents"));
                document.insertTable(pos+1,copyTable);
                document.insertTable(pos+2,copyNoteAndDocumentTable);

            }

        }
        catch (Exception e){
            logger.error("Failed to create item details entry",e);
        }
    }

    private void createNegotiationEntry(XWPFDocument document,ClauseType clause,ZipOutputStream zos) throws Exception{
        QuotationType quotation = (QuotationType) DocumentPersistenceUtility.getUBLDocument(((DocumentClauseType) clause).getClauseDocumentRef().getID(), DocumentType.QUOTATION);
        RequestForQuotationType requestForQuotation = (RequestForQuotationType) DocumentPersistenceUtility.getUBLDocument(quotation.getRequestForQuotationDocumentReference().getID(),DocumentType.REQUESTFORQUOTATION);

        // additional documents
        // request
        for(DocumentReferenceType documentReference : requestForQuotation.getAdditionalDocumentReference()){
            byte[] bytes = SpringBridge.getInstance().getBinaryContentService().retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
            bos.write(bytes,0,bytes.length);

            ZipEntry zipEntry = new ZipEntry("Negotiation/BuyerDocuments/"+ documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
            zos.putNextEntry(zipEntry);
            bos.writeTo(zos);
        }
        // response
        for(DocumentReferenceType documentReference : quotation.getAdditionalDocumentReference()){
            byte[] bytes = SpringBridge.getInstance().getBinaryContentService().retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
            bos.write(bytes,0,bytes.length);

            ZipEntry zipEntry = new ZipEntry("Negotiation/SellerDocuments/"+ documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
            zos.putNextEntry(zipEntry);
            bos.writeTo(zos);
        }
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
                                    String country = quotation.getQuotationLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCountry().getName().getValue();
                                    if(country == null){
                                        country = "";
                                    }
                                    text = text.replace("$nego_country",country);
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_specTerms")){
                                    List<TextType> specialTerms = quotation.getQuotationLine().get(0).getLineItem().getDeliveryTerms().getSpecialTerms();
                                    if(specialTerms != null && specialTerms.size() > 0){
                                        text = text.replace("$nego_specTerms",specialTerms.get(0).getValue());
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
            // negotiation table 'Notes and Additional Documents' part
            // additional documents
            table = getTable(document,"Negotiation Notes/Additional Documents");
            if(requestForQuotation.getAdditionalDocumentReference().size() == 0){
                table.getRow(table.getNumberOfRows()-2).getCell(3).getParagraphs().get(0).createRun().setText("-");
            }
            for(DocumentReferenceType documentReference : requestForQuotation.getAdditionalDocumentReference()){
                XWPFRun run6 = table.getRow(table.getNumberOfRows()-2).getCell(3).getParagraphs().get(0).createRun();
                run6.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run6.setItalic(true);
            }
            if(quotation.getAdditionalDocumentReference().size() == 0){
                table.getRow(table.getNumberOfRows()-1).getCell(3).getParagraphs().get(0).createRun().setText("-");
            }
            for(DocumentReferenceType documentReference : quotation.getAdditionalDocumentReference()){
                XWPFRun run6 = table.getRow(table.getNumberOfRows()-1).getCell(3).getParagraphs().get(0).createRun();
                run6.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run6.setItalic(true);
            }
            // notes
            if(requestForQuotation.getNote().size() == 0){
                table.getRow(table.getNumberOfRows()-2).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            for(String note : requestForQuotation.getNote()){
                XWPFRun run6 = table.getRow(table.getNumberOfRows()-2).getCell(1).getParagraphs().get(0).createRun();
                run6.setText(note+"\n");
                run6.setItalic(true);
            }
            if(quotation.getNote().size() == 0){
                table.getRow(table.getNumberOfRows()-1).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            for(String note : quotation.getNote()){
                XWPFRun run6 = table.getRow(table.getNumberOfRows()-1).getCell(1).getParagraphs().get(0).createRun();
                run6.setText(note+"\n");
                run6.setItalic(true);
            }
        }
        catch (Exception e){
            logger.error("Failed to create negotiation entry",e);
        }

    }

    private void fillItemDetails(XWPFDocument document,XWPFTable table,XWPFTable noteAndDocumentTable,ZipOutputStream zos,ClauseType clause,int id){
        try {
            ItemInformationResponseType itemDetails = (ItemInformationResponseType) DocumentPersistenceUtility.getUBLDocument(((DocumentClauseType) clause).getClauseDocumentRef().getID(), DocumentType.ITEMINFORMATIONRESPONSE);

            ItemInformationRequestType itemInformationRequest = (ItemInformationRequestType) DocumentPersistenceUtility
                    .getUBLDocument(itemDetails.getItemInformationRequestDocumentReference().getID(),DocumentType.ITEMINFORMATIONREQUEST);
            List<DocumentReferenceType> documentReferences = itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getItemSpecificationDocumentReference();

            if(documentReferences.size() == 0){
                table.getRow(2).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            else {
                byte[] bytes = SpringBridge.getInstance().getBinaryContentService().retrieveContent(documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry = new ZipEntry("ItemInformation/ItemInformationRequest"+id+"/TechnicalDataSheets/"+ documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry);
                bos.writeTo(zos);

                XWPFRun run2 = table.getRow(2).getCell(1).getParagraphs().get(0).createRun();
                run2.setText(documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                run2.setItalic(true);
            }

            // additional documents
            // request
            if(itemInformationRequest.getAdditionalDocumentReference().size() == 0){
                noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-2).getCell(3).getParagraphs().get(0).createRun().setText("-");
            }
            for(DocumentReferenceType documentReference : itemInformationRequest.getAdditionalDocumentReference()){
                byte[] bytes = SpringBridge.getInstance().getBinaryContentService().retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry = new ZipEntry("ItemInformation/ItemInformationRequest"+id+"/BuyerDocuments/"+ documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry);
                bos.writeTo(zos);

                XWPFRun run5 = noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-2).getCell(3).getParagraphs().get(0).createRun();
                run5.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run5.setItalic(true);
            }
            // response
            if(itemDetails.getAdditionalDocumentReference().size() == 0){
                noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-1).getCell(3).getParagraphs().get(0).createRun().setText("-");
            }
            for(DocumentReferenceType documentReference : itemDetails.getAdditionalDocumentReference()){
                byte[] bytes = SpringBridge.getInstance().getBinaryContentService().retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry = new ZipEntry("ItemInformation/ItemInformationRequest"+id+"/SellerDocuments/"+ documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry);
                bos.writeTo(zos);

                XWPFRun run6 = noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-1).getCell(3).getParagraphs().get(0).createRun();
                run6.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run6.setItalic(true);
            }

            documentReferences = itemDetails.getItem().get(0).getItemSpecificationDocumentReference();

            if(documentReferences.size() == 0){
                table.getRow(3).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            else {
                byte[] bytes = SpringBridge.getInstance().getBinaryContentService().retrieveContent(documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry = new ZipEntry("ItemInformation/ItemInformationRequest"+id+"/TechnicalDataSheetResponses/"+documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry);
                bos.writeTo(zos);

                XWPFRun run3 = table.getRow(3).getCell(1).getParagraphs().get(0).createRun();
                run3.setText(documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                run3.setItalic(true);
            }

            if(itemInformationRequest.getNote().size() == 0){
                noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-2).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            else {
                String requestNote = "";
                for(String note:itemInformationRequest.getNote()){
                    requestNote += note + "\n";
                }
                XWPFRun run5 = noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-2).getCell(1).getParagraphs().get(0).createRun();
                run5.setText(requestNote);
                run5.setItalic(true);
            }
            if(itemDetails.getNote().size() == 0){
                noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-1).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            else {
                String responseNote = "";
                for(String note:itemDetails.getNote()){
                    responseNote += note + "\n";
                }
                XWPFRun run6 = noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-1).getCell(1).getParagraphs().get(0).createRun();
                run6.setText(responseNote);
                run6.setItalic(true);
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
                result = String.format(tradingTerm.getTradingTermFormat(),tradingTerm.getValue().getValue().toArray());
                selectedTradingTerms.add(result);
            }
            else {
                if(tradingTerm.getValue().getValue().get(0).getValue().toLowerCase().contentEquals("true")){
                    result = tradingTerm.getDescription().get(0).getValue();
                    selectedTradingTerms.add(result);
                }
            }
        }

        return StringUtils.join(selectedTradingTerms,',');
    }

    private String constructAddress(String company_name,AddressType address){
        String country = address.getCountry().getName().getValue();
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

        row.getCell(0).getCTTc().addNewTcPr().addNewTcBorders().addNewRight().setVal(STBorder.NIL);
        row.getCell(1).getCTTc().getTcPr().getTcBorders().addNewLeft().setVal(STBorder.NIL);
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
            ProcessDocumentMetadata processDocumentMetadata = ProcessDocumentMetadataDAOUtility.getDocumentMetadata(orderId);
            DateTimeFormatter bpFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
            LocalDateTime localTime = bpFormatter.parseLocalDateTime(processDocumentMetadata.getSubmissionDate());
            DateTime issueDate = new DateTime(localTime.toDateTime(), DateTimeZone.UTC);

            DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
            date = issueDate.toString(format);
        }
        else {
            ProcessDocumentMetadata responseMetadata = ProcessDocumentMetadataDAOUtility.getOrderResponseMetadataByOrderId(orderId);
            DateTimeFormatter bpFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
            LocalDateTime localTime = bpFormatter.parseLocalDateTime(responseMetadata.getSubmissionDate());
            DateTime issueDate = new DateTime(localTime.toDateTime(), DateTimeZone.UTC);

            DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
            date = issueDate.toString(format);
        }
        return date;

    }

}