package eu.nimble.service.bp.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType;
import eu.nimble.service.model.ubl.iteminformationresponse.ItemInformationResponseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.ppaprequest.PpapRequestType;
import eu.nimble.service.model.ubl.ppapresponse.PpapResponseType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.country.CountryUtil;
import eu.nimble.utility.persistence.binary.BinaryContentService;
import feign.Response;
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
import javax.xml.datatype.XMLGregorianCalendar;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ContractGenerator {
    private final Logger logger = LoggerFactory.getLogger(ContractGenerator.class);

    private BinaryContentService binaryContentService = new BinaryContentService();
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

    public void generateContract(OrderType order,ZipOutputStream zos) throws Exception{
        // get the order response
        OrderResponseSimpleType orderResponse = DocumentPersistenceUtility.getOrderResponseDocumentByOrderId(order.getID());
        List<XWPFDocument> orderTermsAndConditions = null;
        // the list of terms and conditions files used in the order
        List<DocumentReferenceType> termsAndConditionsFiles = ProcessDocumentMetadataDAOUtility.getTermsAndConditionsFiles(orderResponse);
        // if there is no file for the terms and conditions, create the standard T&Cs document using the order
        if(termsAndConditionsFiles.size() == 0){
            orderTermsAndConditions  = fillOrderTermsAndConditions(order);
        }

        List<XWPFDocument> purchaseDetails = fillPurchaseDetails(order,orderResponse);

        getAndPopulateClauses(order,zos,purchaseDetails);
        // additional documents of order
        createOrderAdditionalDocuments(order, orderResponse, zos, purchaseDetails);

        int orderLineSize = order.getOrderLine().size();
        // we use product names to create entries
        // since there may be multiple products with the same name, we need the map below to get the count of product names
        // then, we create entry names by combining the product name and count,i.e.,Product Name,Product Name_1,Product Name_2 etc.
        Map<String,Integer> productNamesCountMap = new HashMap<>();
        for (int i = 0; i<orderLineSize;i++) {
            XWPFDocument purchaseDetail = purchaseDetails.get(i);
            String productName = order.getOrderLine().get(i).getLineItem().getItem().getName().get(0).getValue();
            // update the map
            productNamesCountMap.merge(productName, 1, Integer::sum);
            // create a name for the entry
            int count = productNamesCountMap.get(productName);
            String entryName = count > 1  ? productName+"_"+count: productName;
            // create entries
            if(orderTermsAndConditions != null){
                addDocxToZipFile(entryName+"/Standard Purchase Order Terms and Conditions.pdf",orderTermsAndConditions.get(i),zos);
            } else{
                createTermsAndConditionsFiles(termsAndConditionsFiles,zos,entryName);
            }
            addDocxToZipFile(entryName+"/Company Purchase Details.pdf",purchaseDetail,zos);
        }
    }

    public List<ClauseType> getTermsAndConditions(String sellerPartyId,String buyerPartyId,String buyerFederationId, String incoterms, String tradingTerm, String bearerToken) throws Exception {
        List<ClauseType> clauses = new ArrayList<>();

        try {
            InputStream inputStream = null;
            try {
                // read clauses from the json file
                inputStream = ContractGenerator.class.getResourceAsStream("/contract-bundle/order_terms_and_conditions.json");

                String fileContent = IOUtils.toString(inputStream);

                ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

                // Clauses
                clauses = objectMapper.readValue(fileContent, new TypeReference<List<ClauseType>>() {});

                // update some trading terms using the party info
                PartyType supplierParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,sellerPartyId);
                PartyType customerParty = null;
                if(buyerPartyId != null){
                    if(!buyerFederationId.contentEquals(SpringBridge.getInstance().getFederationId())){
                        Response response = SpringBridge.getInstance().getDelegateClient().getParty(bearerToken,Long.parseLong(buyerPartyId),false,buyerFederationId);
                        customerParty = objectMapper.readValue(HttpResponseUtil.extractBodyFromFeignClientResponse(response),PartyType.class);
                    }
                    else {
                        customerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,buyerPartyId);
                    }
                }

                for(ClauseType clause : clauses){
                    if(clause.getID().contentEquals("1_PURCHASE ORDER TERMS AND CONDITIONS")){
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
                    else if(clause.getID().contentEquals("5_INVOICES, PAYMENT, AND TAXES")){
                        for(TradingTermType tradingTermType : clause.getTradingTerms()){
                            if(tradingTermType.getID().contentEquals("$payment_id") && !StringUtils.isEmpty(tradingTerm)){
                                CodeType code = new CodeType();
                                code.setValue(tradingTerm);
                                code.setListID(payment_means_list_id);
                                tradingTermType.getValue().setValueCode(Collections.singletonList(code));
                            }
                        }
                    }
                    else if(clause.getID().contentEquals("19_MISCELLANEOUS")){
                        for(TradingTermType tradingTermType : clause.getTradingTerms()){
                            if(tradingTermType.getID().contentEquals("$notices_id") && customerParty != null){
                                TextType text = new TextType();
                                text.setLanguageID("en");
                                text.setValue(constructAddress(customerParty.getPartyName().get(0).getName().getValue(),customerParty.getPostalAddress()));
                                tradingTermType.getValue().setValue(Collections.singletonList(text));
                            }
                            else if(tradingTermType.getID().contentEquals("$incoterms_id") && !StringUtils.isEmpty(incoterms)){
                                CodeType code = new CodeType();
                                code.setValue(incoterms);
                                code.setListID(incoterms_list_id);
                                tradingTermType.getValue().setValueCode(Collections.singletonList(code));
                            }
                            else if(tradingTermType.getID().contentEquals("$seller_website") && !StringUtils.isEmpty(supplierParty.getWebsiteURI())){
                                TextType text = new TextType();
                                text.setLanguageID("en");
                                text.setValue(supplierParty.getWebsiteURI());
                                tradingTermType.getValue().setValue(Collections.singletonList(text));
                            }
                            else if(tradingTermType.getID().contentEquals("$seller_tel")){
                                if(supplierParty.getPerson() == null || supplierParty.getPerson().size() == 0){
                                    logger.info("There is no person info in the party:{}",supplierParty);
                                }else if(!StringUtils.isEmpty(supplierParty.getPerson().get(0).getContact().getTelephone())){
                                    TextType text = new TextType();
                                    text.setLanguageID("en");
                                    text.setValue(supplierParty.getPerson().get(0).getContact().getTelephone());
                                    tradingTermType.getValue().setValue(Collections.singletonList(text));
                                }
                            }
                            else if(tradingTermType.getID().contentEquals("$buyer_country") &&  customerParty != null && customerParty.getPostalAddress() != null && customerParty.getPostalAddress().getCountry() != null
                                    && customerParty.getPostalAddress().getCountry().getIdentificationCode() != null){
                                CodeType code = new CodeType();
                                code.setValue(CountryUtil.getCountryNameByISOCode(customerParty.getPostalAddress().getCountry().getIdentificationCode().getValue()));
                                code.setListID(country_list_id);
                                tradingTermType.getValue().setValueCode(Collections.singletonList(code));
                            }
                        }
                    }
                }
            }
            catch (Exception e){
                logger.error("Failed to create terms and conditions for sellerPartyId: {}, buyerPartyId: {}, incoterms: {}, tradingTerm : {}", sellerPartyId, buyerPartyId, incoterms, tradingTerm, e);
                throw e;
            }
            finally {
                if(inputStream != null){
                    inputStream.close();
                }
            }
        }
        catch (Exception e){
            logger.error("Failed to create terms and conditions for sellerPartyId: {}, buyerPartyId: {}, incoterms: {}, tradingTerm : {}", sellerPartyId, buyerPartyId, incoterms, tradingTerm, e);
            throw e;
        }

        return clauses;
    }

    private List<XWPFDocument> fillOrderTermsAndConditions(OrderType order) throws Exception{

        List<XWPFDocument> documents = new ArrayList<>();

        for(OrderLineType orderLine :order.getOrderLine()){
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

                // get T&Cs clauses
                List<ClauseType> clauses = getTermsAndConditionsContract(order,order.getOrderLine().indexOf(orderLine)).getClause();

                int rowIndex = 0;
                for(ClauseType clause: clauses){
                    XWPFParagraph paragraph = document.createParagraph();
                    // set the clause title
                    paragraph.createRun();
                    paragraph.getRuns().get(0).setText(rowIndex+1+"."+clause.getContent().get(0).getValue()+":\n");
                    paragraph.getRuns().get(0).setBold(true);
                    // set the content of clause
                    setClauseContent(clause.getContent().get(0).getValue(),paragraph,clause.getTradingTerms());
                    rowIndex++;
                }

                documents.add(document);
            }
            catch (Exception e){
                logger.error("Failed to fill in 'Standard Purchase Order Terms and Conditions.pdf' for the order with id : {}",order.getID(),e);
                throw e;
            }
        }

        return documents;
    }


    /**
     * Creates the entries for the given terms and conditions files.
     * */
    public void createTermsAndConditionsFiles(List<DocumentReferenceType> termsAndConditionsFiles,ZipOutputStream zos,String entryName) throws IOException {
        try {
            for(DocumentReferenceType documentReference : termsAndConditionsFiles){
                byte[] bytes = binaryContentService.retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry2 = new ZipEntry(entryName+"/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry2);
                bos.writeTo(zos);
            }
        }
        catch (Exception e){
            logger.error("Failed to create terms and conditions entry",e);
            throw e;
        }
    }

    private void setClauseContent(String sectionText, XWPFParagraph paragraph, List<TradingTermType> tradingTerms){
        // get the identifiers of all trading terms
        List<String> tradingTermIds = new ArrayList<>();
        for (TradingTermType tradingTerm : tradingTerms) {
            tradingTermIds.add(tradingTerm.getID());
        }
        // get the details of first trading term in the given section text
        TradingTermIndex nextTradingTerm = getFirstTradingTermInText(sectionText,tradingTermIds);

        while (nextTradingTerm != null){
            paragraph.createRun().setText(sectionText.substring(0,nextTradingTerm.getIndex()),0);
            sectionText = sectionText.substring(nextTradingTerm.getIndex()+nextTradingTerm.getTradingTermId().length());

            XWPFRun run = paragraph.createRun();
            for(TradingTermType tradingTerm : tradingTerms){
                if(tradingTerm.getID().contentEquals(nextTradingTerm.getTradingTermId())){
                    // find the value of parameter
                    String value = "";
                    if(tradingTerm.getValue().getValueQualifier().contentEquals("STRING") && tradingTerm.getValue().getValue().get(0).getValue() != null && !tradingTerm.getValue().getValue().get(0).getValue().contentEquals("")){
                        value = tradingTerm.getValue().getValue().get(0).getValue();
                    } else if(tradingTerm.getValue().getValueQualifier().contentEquals("NUMBER") && tradingTerm.getValue().getValueDecimal().get(0) != null){
                        value = new DecimalFormat("##").format(tradingTerm.getValue().getValueDecimal().get(0));
                    } else if(tradingTerm.getValue().getValueQualifier().contentEquals("QUANTITY") && tradingTerm.getValue().getValueQuantity().get(0).getValue() != null && tradingTerm.getValue().getValueQuantity().get(0).getUnitCode() != null){
                        value = new DecimalFormat("##").format(tradingTerm.getValue().getValueQuantity().get(0).getValue()) + " " + tradingTerm.getValue().getValueQuantity().get(0).getUnitCode();
                    } else if(tradingTerm.getValue().getValueQualifier().contentEquals("CODE") && tradingTerm.getValue().getValueCode().get(0).getValue() != null && !tradingTerm.getValue().getValueCode().get(0).getValue().contentEquals("")){
                        value = tradingTerm.getValue().getValueCode().get(0).getValue();
                    }
                    // if no value is provided for the trading term, use its id
                    else {
                        value = tradingTerm.getID();
                    }

                    run.setText(value,0);
                    run.setUnderline(UnderlinePatterns.SINGLE);
                    setColor(run,red_hex);

                    break;
                }
            }

            nextTradingTerm = getFirstTradingTermInText(sectionText,tradingTermIds);
        }

        paragraph.createRun().setText(sectionText,0);
    }

    // returns the details (id and the index) of the first trading term in the given text if exists
    private TradingTermIndex getFirstTradingTermInText(String text, List<String> tradingTermId){
        if(tradingTermId.size() == 0){
            return null;
        }
        String firstTradingTerm = null;
        int tradingTermIndex = text.length();

        for (String s : tradingTermId) {
            int index = text.indexOf(s);
            if (index != -1 && index < tradingTermIndex) {
                firstTradingTerm = s;
                tradingTermIndex = index;
            }
        }

        if(firstTradingTerm == null){
            return null;
        }
        return new TradingTermIndex(tradingTermIndex, firstTradingTerm);
    }

    // returns the contract storing Terms and Conditions details for the item specified by the item index
    private ContractType getTermsAndConditionsContract(OrderType order,int itemIndex){
        List<ContractType> contractTypes = new ArrayList<>();
        if(order.getContract().size() > 0){
            for(ContractType contract : order.getContract()){
                for(ClauseType clause : contract.getClause()){
                    if(clause.getType() == null){
                        contractTypes.add(contract);
                    }
                }
            }
        }

        return contractTypes.get(itemIndex);
    }

    public static ContractType getNonTermOrConditionContract(OrderType order){
        if(order.getContract().size() > 0){
            for(ContractType contract : order.getContract()){
                for(ClauseType clause:contract.getClause()){
                    if(clause.getType() != null){
                        return contract;
                    }
                }
            }
        }
        return null;
    }

    private List<XWPFDocument> fillPurchaseDetails(OrderType order,OrderResponseSimpleType orderResponse) throws Exception{

        List<XWPFDocument> documents = new ArrayList<>();
        int orderLineSize = order.getOrderLine().size();
        for(int i = 0; i < orderLineSize;i++){
            OrderLineType orderLine = order.getOrderLine().get(i);
            XWPFDocument document = null;
            try {
                InputStream file = ContractGenerator.class.getResourceAsStream("/contract-bundle/Purchase Details.docx");

                document = new XWPFDocument(file);

                boolean totalPriceExists = checkTotalPriceExistsInOrder(orderLine);

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
                                            if(order.getBuyerCustomerParty().getParty().getPostalAddress().getCountry().getIdentificationCode() != null){
                                                text = text.replace("$country_invoice_id",CountryUtil.getCountryNameByISOCode(order.getBuyerCustomerParty().getParty().getPostalAddress().getCountry().getIdentificationCode().getValue()));
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
                                            if(orderLine.getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCountry().getIdentificationCode() != null
                                                    && orderLine.getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCountry().getIdentificationCode().getValue() != null){
                                                text = text.replace("$country_id",CountryUtil.getCountryNameByISOCode(orderLine.getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCountry().getIdentificationCode().getValue()));
                                                r.setText(text,0);
                                            }
                                            else {
                                                text = text.replace("$country_id","");
                                                r.setText(text,0);
                                            }
                                        }
                                        if(text.contains("$phone_id")){
                                            if(order.getBuyerCustomerParty().getParty().getPerson().get(0).getContact() != null && !order.getBuyerCustomerParty().getParty().getPerson().get(0).getContact().getTelephone().contentEquals("")){
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
                                            if(!CollectionUtils.isEmpty(order.getSellerSupplierParty().getParty().getPerson()) && order.getSellerSupplierParty().getParty().getPerson().get(0).getContact().getTelephone() != null && !order.getSellerSupplierParty().getParty().getPerson().get(0).getContact().getTelephone().contentEquals("")){
                                                text = text.replace("$phone_supplier",order.getSellerSupplierParty().getParty().getPerson().get(0).getContact().getTelephone());
                                                r.setText(text,0);
                                            }
                                            else {
                                                text = text.replace("$phone_supplier","");
                                                r.setText(text,0);
                                            }
                                        }
                                        if(text.contains("$street_id")){
                                            if(orderLine.getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getStreetName() != null){
                                                text = text.replace("$street_id",orderLine.getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getStreetName());
                                                r.setText(text,0);
                                            }
                                            else {
                                                text = text.replace("$street_id","");
                                                r.setText(text,0);
                                            }
                                        }
                                        if(text.contains("$building_id")){
                                            text = text.replace("$building_id",orderLine.getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getBuildingNumber());
                                            r.setText(text,0);
                                        }
                                        if(text.contains("$country_supplier")){
                                            if(order.getSellerSupplierParty().getParty().getPostalAddress().getCountry().getIdentificationCode() != null){
                                                text = text.replace("$country_supplier",CountryUtil.getCountryNameByISOCode(order.getSellerSupplierParty().getParty().getPostalAddress().getCountry().getIdentificationCode().getValue()));
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
                                            text = text.replace("$item_id",orderLine.getLineItem().getItem().getName().get(0).getValue());
                                            r.setText(text,0);
                                        }
                                        if(text.contains("$product_id")){
                                            text = text.replace("$product_id",orderLine.getLineItem().getItem().getManufacturersItemIdentification().getID());
                                            r.setText(text,0);
                                        }
                                        if(text.contains("$quantity_id")){
                                            if(totalPriceExists){
                                                text = text.replace("$quantity_id",new DecimalFormat(".00").format(orderLine.getLineItem().getQuantity().getValue())+" "+orderLine.getLineItem().getQuantity().getUnitCode());
                                                r.setText(text,0);
                                            }
                                            else{
                                                text = text.replace("$quantity_id","");
                                                r.setText(text,0);
                                            }
                                        }
                                        if(text.contains("$price_id")){
                                            if(totalPriceExists){
                                                text = text.replace("$price_id",new DecimalFormat(".00").format((orderLine.getLineItem().getPrice().getPriceAmount().getValue().divide(orderLine.getLineItem().getPrice().getBaseQuantity().getValue(),2)))+" "+orderLine.getLineItem().getPrice().getPriceAmount().getCurrencyID());
                                                r.setText(text,0);
                                            }
                                            else{
                                                text = text.replace("$price_id","");
                                                r.setText(text,0);
                                            }
                                        }
                                        if(text.contains("$total_id")){
                                            if(totalPriceExists){
                                                text = text.replace("$total_id",new DecimalFormat(".00").format((orderLine.getLineItem().getPrice().getPriceAmount().getValue().divide(orderLine.getLineItem().getPrice().getBaseQuantity().getValue(),2)).multiply(orderLine.getLineItem().getQuantity().getValue()))+
                                                        " "+orderLine.getLineItem().getPrice().getPriceAmount().getCurrencyID());
                                                r.setText(text,0);
                                            }
                                            else if(orderLine.getLineItem().getPrice().getPriceAmount().getValue() != null){
                                                text = text.replace("$total_id",new DecimalFormat(".00").format(orderLine.getLineItem().getPrice().getPriceAmount().getValue())+
                                                        " "+orderLine.getLineItem().getPrice().getPriceAmount().getCurrencyID());
                                                r.setText(text,0);
                                            }
                                            else {
                                                text = text.replace("$total_id","");
                                                r.setText(text,0);
                                            }
                                        }
                                        if(text.contains("$city_id")){
                                            if(orderLine.getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCityName() != null){
                                                text = text.replace("$city_id",orderLine.getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCityName());
                                                r.setText(text,0);
                                            }
                                            else{
                                                text = text.replace("$city_id","");
                                                r.setText(text,0);
                                            }
                                        }
                                        if(text.contains("$zip_id")){
                                            if(orderLine.getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getPostalZone() != null){
                                                text = text.replace("$zip_id",orderLine.getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getPostalZone());
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
                                            BigDecimal value = orderLine.getLineItem().getDelivery().get(0).getRequestedDeliveryPeriod().getDurationMeasure().getValue();
                                            if(value != null){
                                                text = text.replace("$order_delPer",new DecimalFormat("##").format(value)+" "
                                                        +orderLine.getLineItem().getDelivery().get(0).getRequestedDeliveryPeriod().getDurationMeasure().getUnitCode());
                                                r.setText(text,0);
                                            }
                                            else {
                                                text = text.replace("$order_delPer","");
                                                r.setText(text,0);
                                            }
                                        }
                                        if(text.contains("$order_paymentMeans")){
                                            text = text.replace("$order_paymentMeans",orderLine.getLineItem().getPaymentMeans().getPaymentMeansCode().getValue());
                                            r.setText(text,0);
                                        }
                                        if(text.contains("$payment_id")){
                                            if(orderLine.getLineItem().getPaymentTerms().getTradingTerms().size() > 0){
                                                text = text.replace("$payment_id",getTradingTerms(orderLine.getLineItem().getPaymentTerms().getTradingTerms()));
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

                // add data monitoring info to Purchase Order Comments section
                XWPFTable table = getTable(document,"Purchase Order Comments");
                // get the row for data monitoring info
                XWPFTableRow dataMonitoringRow = table.getRow(1);
                // get data monitoring text
                boolean isPromised = isDataMonitoringPromised(order,orderResponse,i);
                String dataMonitoringText = isPromised ? "\n\nYes" : "\n\nNo";

                XWPFRun run = dataMonitoringRow.getCell(0).getParagraphArray(5).createRun();
                run.setUnderline(UnderlinePatterns.SINGLE);
                run.setText("\n\nData Monitoring Service");

                dataMonitoringRow.getCell(0).getParagraphArray(5).createRun().setText(dataMonitoringText);

                // add delivery dates to Purchase Order Comments section
                run = dataMonitoringRow.getCell(0).getParagraphArray(5).createRun();
                run.setUnderline(UnderlinePatterns.SINGLE);
                run.setText("\n\nDelivery Dates");

                for (DeliveryType delivery : orderLine.getLineItem().getDelivery()) {
                    XMLGregorianCalendar endDate = delivery.getRequestedDeliveryPeriod().getEndDate();
                    QuantityType quantity = delivery.getShipment().getGoodsItem().get(0).getQuantity();
                    if(endDate != null && quantity != null){
                        dataMonitoringRow.getCell(0).getParagraphArray(5).createRun().setText("\n\nDelivery Date : "+endDate.toString()+" - Quantity : "+new DecimalFormat(".00").format(quantity.getValue())+" "+ quantity.getUnitCode());
                    }
                }
                documents.add(document);
            }
            catch (Exception e){
                logger.error("Failed to fill in 'Company Purchase Details.pdf' for the order with id : {}",order.getID(),e);
                throw e;
            }
        }
        return documents;
    }

    // check whether the data monitoring is promised for the item specified by the order line index
    private boolean isDataMonitoringPromised(OrderType order, OrderResponseSimpleType orderResponse,int index) {
        boolean dataMonitoringDemanded = false;
        ContractType contract = ContractGenerator.getNonTermOrConditionContract(order);
        if(contract != null){
            List<ClauseType> clauses = contract.getClause();
            for(ClauseType clause : clauses) {
                if(clause.getType().contentEquals(eu.nimble.service.model.ubl.extension.ClauseType.DOCUMENT.toString())) {
                    DocumentClauseType docClause = (DocumentClauseType) clause;
                    if(docClause.getClauseDocumentRef().getDocumentType().contentEquals(DocumentType.QUOTATION.toString())) {
                        QuotationType quotation = (QuotationType) DocumentPersistenceUtility.getUBLDocument(docClause.getClauseDocumentRef().getID(), DocumentType.QUOTATION);
                        if(quotation.getQuotationLine().get(index).getLineItem().isDataMonitoringRequested()){
                            dataMonitoringDemanded = true;
                            break;
                        }
                    }
                }
            }
        }

        return dataMonitoringDemanded && orderResponse.isAcceptedIndicator();
    }

    private void addDocxToZipFile(String fileName,XWPFDocument document, ZipOutputStream zos) throws Exception{
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
            throw e;
        }
    }

    private void getAndPopulateClauses(OrderType order,ZipOutputStream zos,List<XWPFDocument> documents) throws Exception{
        // if there is no contract, simple remove the tables and return
        if(order.getContract().size() <= 0){
            for (XWPFDocument document : documents) {
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
        }

        // get clauses
        Map<DocumentType,List<ClauseType>> clauses = getClauses(order);

        if(clauses.get(DocumentType.ITEMINFORMATIONRESPONSE).size() > 0){
            // create directories
            ZipEntry zipEntry = new ZipEntry("ItemInformation/TechnicalDataSheetResponses/");
            zos.putNextEntry(zipEntry);
            zipEntry = new ZipEntry("ItemInformation/TechnicalDataSheets/");
            zos.putNextEntry(zipEntry);
        }

        // Item Information Clauses (there can be multiple Item Information clauses in a contract)
        List<ClauseType> itemInformationClauses = clauses.get(DocumentType.ITEMINFORMATIONRESPONSE);
        List<ItemInformationResponseType> itemInformationResponses = new ArrayList<>();
        List<ItemInformationRequestType> itemInformationRequests = new ArrayList<>();
        // PPAP Clause (there can be one PPAP clause in a contract)
        List<ClauseType> ppapClauses = clauses.get(DocumentType.PPAPRESPONSE);
        PpapResponseType ppapResponse = null;
        PpapRequestType ppapRequest = null;
        // Negotiation Clause (there can be one Negotiation clause in a contract)
        List<ClauseType> negotiationClauses = clauses.get(DocumentType.QUOTATION);
        QuotationType quotation = null;
        RequestForQuotationType requestForQuotation = null;

        // create PPAP entry
        if(ppapClauses.size() == 1){
            ZipEntry zipEntry = new ZipEntry("PPAP/PPAPDocuments/");
            zos.putNextEntry(zipEntry);

            ppapResponse = (PpapResponseType) DocumentPersistenceUtility.getUBLDocument(((DocumentClauseType) ppapClauses.get(0)).getClauseDocumentRef().getID(), DocumentType.PPAPRESPONSE);
            ppapRequest = (PpapRequestType) DocumentPersistenceUtility.getUBLDocument(ppapResponse.getPpapDocumentReference().getID(),DocumentType.PPAPREQUEST);

            createPPAPAuxiliaryFiles(zos, ppapRequest,ppapResponse);
        }

        // create Negotiation entry
        if(negotiationClauses.size() == 1){
            quotation = (QuotationType) DocumentPersistenceUtility.getUBLDocument(((DocumentClauseType) negotiationClauses.get(0)).getClauseDocumentRef().getID(), DocumentType.QUOTATION);
            requestForQuotation = (RequestForQuotationType) DocumentPersistenceUtility.getUBLDocument(quotation.getRequestForQuotationDocumentReference().getID(),DocumentType.REQUESTFORQUOTATION);

            createNegotiationAuxiliaryFiles(zos,requestForQuotation,quotation);
        }
        // create Item information entries
        int numberOfItemInformationRequest = itemInformationClauses.size();
        if(numberOfItemInformationRequest > 0){
            for(ClauseType clause : itemInformationClauses){
                ItemInformationResponseType itemDetails = (ItemInformationResponseType) DocumentPersistenceUtility.getUBLDocument(((DocumentClauseType) clause).getClauseDocumentRef().getID(), DocumentType.ITEMINFORMATIONRESPONSE);

                ItemInformationRequestType itemInformationRequest = (ItemInformationRequestType) DocumentPersistenceUtility
                        .getUBLDocument(itemDetails.getItemInformationRequestDocumentReference().getID(),DocumentType.ITEMINFORMATIONREQUEST);

                itemInformationRequests.add(itemInformationRequest);
                itemInformationResponses.add(itemDetails);

                createItemDetailsAuxiliaryFiles(zos, itemInformationRequest,itemDetails, numberOfItemInformationRequest);
                numberOfItemInformationRequest--;
            }
        }

        int numberOfOrderLines = order.getOrderLine().size();
        for (int i = 0; i < numberOfOrderLines; i++) {
            XWPFDocument document = documents.get(i);
            try{
                // create PPAP entry
                if(ppapClauses.size() == 1){
                    fillPPAPTable(document, ppapRequest,ppapResponse);
                }
                // create Negotiation entry
                if(negotiationClauses.size() == 1){
                    fillNegotiationTable(document, i,requestForQuotation,quotation);
                }
                // create Item information entries
                numberOfItemInformationRequest = itemInformationClauses.size();
                if(numberOfItemInformationRequest > 0){
                    for(int j = 0; j < numberOfItemInformationRequest;j++){
                        fillOrGenerateItemDetailsTable(document,itemInformationRequests.get(j),itemInformationResponses.get(j));
                    }
                }
                if (negotiationClauses.size() == 0) {
                    document.removeBodyElement(document.getPosOfTable(getTable(document, "Negotiation")));
                    document.removeBodyElement(document.getPosOfTable(getTable(document, "Negotiation Notes/Additional Documents")));
                }
                if (ppapClauses.size() == 0) {
                    document.removeBodyElement(document.getPosOfTable(getTable(document, "PPAP")));
                    document.removeBodyElement(document.getPosOfTable(getTable(document, "PPAP Notes/Additional Documents")));
                }
                if (itemInformationClauses.size() == 0) {
                    document.removeBodyElement(document.getPosOfTable(getTable(document, "Item Information Request")));
                    document.removeBodyElement(document.getPosOfTable(getTable(document, "Item Information Request Notes/Additional Documents")));
                }
            }
            catch (Exception e){
                logger.error("Failed to create entries for clauses",e);
                throw e;
            }
        }
    }

    private Map<DocumentType,List<ClauseType>> getClauses(OrderType order){
        List<ClauseType> PPAPResponse = new ArrayList<>();
        List<ClauseType> quotation = new ArrayList<>();
        List<ClauseType> itemInformationResponse = new ArrayList<>();
        // check clauses
        ContractType contract = getNonTermOrConditionContract(order);
        if(contract != null){
            for(ClauseType clause : contract.getClause()){
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
        }
        // create the map
        Map<DocumentType,List<ClauseType>> map = new HashMap<>();
        map.put(DocumentType.PPAPRESPONSE,PPAPResponse);
        map.put(DocumentType.QUOTATION,quotation);
        map.put(DocumentType.ITEMINFORMATIONRESPONSE,itemInformationResponse);
        return map;
    }

    private void createOrderAdditionalDocuments(OrderType order,OrderResponseSimpleType orderResponse,ZipOutputStream zos,List<XWPFDocument> documents) throws Exception{
        try {
            for (XWPFDocument document : documents) {
                createOrderAuxiliaryFiles(order,orderResponse,zos,document);
                fillOrderAdditionalDocumentsTable(order,orderResponse,zos,document);
            }
        }
        catch (Exception e){
            logger.error("Failed to create Order additional documents entry",e);
            throw e;
        }
    }

    private XWPFDocument fillOrderAdditionalDocumentsTable(OrderType order,OrderResponseSimpleType orderResponse,ZipOutputStream zos,XWPFDocument document){
        try {
            List<DocumentReferenceType> orderAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(order.getAdditionalDocumentReference());
            List<DocumentReferenceType> orderResponseAuxiliaryFiles = null;
            if(orderResponse != null){
                orderResponseAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(orderResponse.getAdditionalDocumentReference());
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
            if(orderAuxiliaryFiles.size() == 0){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-2);
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText("-");
            }
            for(DocumentReferenceType documentReference : orderAuxiliaryFiles){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-2);
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run.setItalic(true);
            }
            if(orderResponseAuxiliaryFiles.size() == 0){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-1);
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText("-");
            }
            // response
            for(DocumentReferenceType documentReference : orderResponseAuxiliaryFiles){
                XWPFTableRow row = table.getRows().get(table.getNumberOfRows()-1);
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run.setItalic(true);
            }
        }
        catch (Exception e){
            logger.error("Failed to create Order additional documents entry",e);
            throw e;
        }
        return document;
    }

    private XWPFDocument createOrderAuxiliaryFiles(OrderType order,OrderResponseSimpleType orderResponse,ZipOutputStream zos,XWPFDocument document) throws Exception{
        try {
            List<DocumentReferenceType> orderAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(order.getAdditionalDocumentReference());
            List<DocumentReferenceType> orderResponseAuxiliaryFiles = null;
            // request
            for(DocumentReferenceType documentReference : orderAuxiliaryFiles){
                byte[] bytes = binaryContentService.retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry2 = new ZipEntry("Order/BuyerDocuments/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry2);
                bos.writeTo(zos);
            }
            // response
            if(orderResponse != null){
                orderResponseAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(orderResponse.getAdditionalDocumentReference());
                for(DocumentReferenceType documentReference : orderResponseAuxiliaryFiles){
                    byte[] bytes = binaryContentService.retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                    bos.write(bytes,0,bytes.length);

                    ZipEntry zipEntry2 = new ZipEntry("Order/SellerDocuments/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                    zos.putNextEntry(zipEntry2);
                    bos.writeTo(zos);
                }
            }
        }
        catch (Exception e){
            logger.error("Failed to create Order additional documents entry",e);
            throw e;
        }
        return document;
    }

    private void createPPAPAuxiliaryFiles(ZipOutputStream zos,PpapRequestType ppapRequest,PpapResponseType ppapResponse)  throws Exception{
        Map<String,List<String>> map = new HashMap<>();
        for(String documentType:ppapRequest.getDocumentType()){
            map.put(documentType,new ArrayList<>());
        }

        for(DocumentReferenceType documentReference : ppapResponse.getRequestedDocument()){
            byte[] bytes = binaryContentService.retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
            bos.write(bytes,0,bytes.length);

            ZipEntry zipEntry2 = new ZipEntry("PPAP/PPAPDocuments/" +documentReference.getDocumentType()+"/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
            zos.putNextEntry(zipEntry2);
            bos.writeTo(zos);

            map.get(documentReference.getDocumentType()).add(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
        }

        // additional documents
        // request
        List<DocumentReferenceType> ppapRequestAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(ppapRequest.getAdditionalDocumentReference());
        for(DocumentReferenceType documentReference : ppapRequestAuxiliaryFiles){
            byte[] bytes = binaryContentService.retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
            bos.write(bytes,0,bytes.length);

            ZipEntry zipEntry2 = new ZipEntry("PPAP/BuyerDocuments/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
            zos.putNextEntry(zipEntry2);
            bos.writeTo(zos);
        }
        // response
        List<DocumentReferenceType> ppapResponseAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(ppapResponse.getAdditionalDocumentReference());
        for(DocumentReferenceType documentReference : ppapResponseAuxiliaryFiles){
            byte[] bytes = binaryContentService.retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
            bos.write(bytes,0,bytes.length);

            ZipEntry zipEntry2 = new ZipEntry("PPAP/SellerDocuments/"+documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
            zos.putNextEntry(zipEntry2);
            bos.writeTo(zos);
        }
    }

    private void fillPPAPTable(XWPFDocument document,PpapRequestType ppapRequest,PpapResponseType ppapResponse) throws Exception{
        try {
            List<DocumentReferenceType> ppapRequestAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(ppapRequest.getAdditionalDocumentReference());
            List<DocumentReferenceType> ppapResponseAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(ppapResponse.getAdditionalDocumentReference());
            Map<String,List<String>> map = new HashMap<>();
            for(String documentType:ppapRequest.getDocumentType()){
                map.put(documentType,new ArrayList<>());
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
            if(ppapRequestAuxiliaryFiles.size() == 0){
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText("-");
            }
            for(DocumentReferenceType documentReference : ppapRequestAuxiliaryFiles){
                XWPFRun run = row.getCell(3).getParagraphs().get(0).createRun();
                run.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run.setItalic(true);
            }
            XWPFTableRow row1 = table.getRows().get(table.getNumberOfRows()-1);
            if(ppapResponseAuxiliaryFiles.size() == 0){
                XWPFRun run = row1.getCell(3).getParagraphs().get(0).createRun();
                run.setText("-");
            }
            // response
            for(DocumentReferenceType documentReference : ppapResponseAuxiliaryFiles){
                XWPFRun run = row1.getCell(3).getParagraphs().get(0).createRun();
                run.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run.setItalic(true);
            }

            table.setInsideVBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, null);
        }
        catch (Exception e){
            logger.error("Failed to create PPAP entry",e);
            throw e;
        }
    }

    private void fillOrGenerateItemDetailsTable(XWPFDocument document,ItemInformationRequestType itemInformationRequest,ItemInformationResponseType itemDetails){
        try {
            XWPFTable table = getTable(document,"Item Information Request");
            XWPFTable noteAndDocumentTable = getTable(document,"Item Information Request Notes/Additional Documents");
            if(firstIIR){
                firstIIR = false;
                fillItemDetailsTable(table,noteAndDocumentTable,itemInformationRequest,itemDetails);
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

                fillItemDetailsTable(copyTable,copyNoteAndDocumentTable,itemInformationRequest,itemDetails);
                int pos = document.getPosOfTable(getTable(document,"Item Information Request Notes/Additional Documents"));
                document.insertTable(pos+1,copyTable);
                document.insertTable(pos+2,copyNoteAndDocumentTable);

            }

        }
        catch (Exception e){
            logger.error("Failed to create item details entry",e);
            throw e;
        }
    }

    private void createNegotiationAuxiliaryFiles(ZipOutputStream zos,RequestForQuotationType requestForQuotation, QuotationType quotation) throws Exception{
        // additional documents
        // request
        List<DocumentReferenceType> rfqAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(requestForQuotation.getAdditionalDocumentReference());
        for(DocumentReferenceType documentReference : rfqAuxiliaryFiles){
            byte[] bytes = binaryContentService.retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
            bos.write(bytes,0,bytes.length);

            ZipEntry zipEntry = new ZipEntry("Negotiation/BuyerDocuments/"+ documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
            zos.putNextEntry(zipEntry);
            bos.writeTo(zos);
        }
        // response
        List<DocumentReferenceType> quotationAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(quotation.getAdditionalDocumentReference());
        for(DocumentReferenceType documentReference : quotationAuxiliaryFiles){
            byte[] bytes = binaryContentService.retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
            bos.write(bytes,0,bytes.length);

            ZipEntry zipEntry = new ZipEntry("Negotiation/SellerDocuments/"+ documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
            zos.putNextEntry(zipEntry);
            bos.writeTo(zos);
        }
    }

    private void fillNegotiationTable(XWPFDocument document, int itemIndex,RequestForQuotationType requestForQuotation,QuotationType quotation){
        List<DocumentReferenceType> rfqAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(requestForQuotation.getAdditionalDocumentReference());
        List<DocumentReferenceType> quotationAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(quotation.getAdditionalDocumentReference());
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
                                    BigDecimal value = quotation.getQuotationLine().get(itemIndex).getLineItem().getPrice().getPriceAmount().getValue();
                                    if(value != null) {
                                        text = text.replace("$nego_price",new DecimalFormat(".00").format(value)+" "+
                                                quotation.getQuotationLine().get(itemIndex).getLineItem().getPrice().getPriceAmount().getCurrencyID());
                                        r.setText(text,0);

                                        totalPriceExists++;
                                    }
                                    else {
                                        text = text.replace("$nego_price","");
                                        r.setText(text,0);
                                    }
                                }
                                if(text.contains("$nego_base")){
                                    BigDecimal value = quotation.getQuotationLine().get(itemIndex).getLineItem().getPrice().getBaseQuantity().getValue();
                                    String unit = quotation.getQuotationLine().get(itemIndex).getLineItem().getPrice().getBaseQuantity().getUnitCode();
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
                                    BigDecimal value = quotation.getQuotationLine().get(itemIndex).getLineItem().getQuantity().getValue();
                                    if(value != null){
                                        text = text.replace("$nego_quan",new DecimalFormat(".00").format(value)+" "+
                                                quotation.getQuotationLine().get(itemIndex).getLineItem().getQuantity().getUnitCode());
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
                                        text = text.replace("$nego_total",new DecimalFormat(".00").format(quotation.getQuotationLine().get(itemIndex).getLineItem().getPrice().getPriceAmount().getValue().divide(quotation.getQuotationLine().get(itemIndex).getLineItem().getPrice().getBaseQuantity().getValue(),2).multiply(quotation.getQuotationLine().get(itemIndex).getLineItem().getQuantity().getValue()))+
                                                " "+quotation.getQuotationLine().get(itemIndex).getLineItem().getPrice().getPriceAmount().getCurrencyID());
                                        r.setText(text,0);
                                    }
                                    else {
                                        text = text.replace("$nego_total","");
                                        r.setText(text,0);
                                    }
                                }
                                if(text.contains("$nego_means")){
                                    text = text.replace("$nego_means",quotation.getQuotationLine().get(itemIndex).getLineItem().getPaymentMeans().getPaymentMeansCode().getValue());
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_terms")){
                                    text = text.replace("$nego_terms",getTradingTerms(quotation.getQuotationLine().get(itemIndex).getLineItem().getPaymentTerms().getTradingTerms()));
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_incoterms")){
                                    String incoterms = quotation.getQuotationLine().get(itemIndex).getLineItem().getDeliveryTerms().getIncoterms();
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
                                    BigDecimal value = quotation.getQuotationLine().get(itemIndex).getLineItem().getDelivery().get(0).getRequestedDeliveryPeriod().getDurationMeasure().getValue();
                                    if(value != null){
                                        text = text.replace("$nego_delPer",new DecimalFormat("##").format(value)+" "+quotation.getQuotationLine().get(itemIndex).getLineItem().getDelivery().get(0).getRequestedDeliveryPeriod().getDurationMeasure().getUnitCode());
                                        r.setText(text,0);
                                    }
                                    else {
                                        text = text.replace("$nego_delPer","");
                                        r.setText(text,0);
                                    }

                                }
                                if(text.contains("$nego_street")){
                                    text = text.replace("$nego_street",quotation.getQuotationLine().get(itemIndex).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getStreetName());
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_building")){
                                    text = text.replace("$nego_building",quotation.getQuotationLine().get(itemIndex).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getBuildingNumber());
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_city")){
                                    text = text.replace("$nego_city",quotation.getQuotationLine().get(itemIndex).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCityName());
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_postal")){
                                    text = text.replace("$nego_postal",quotation.getQuotationLine().get(itemIndex).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getPostalZone());
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_country")){
                                    String country = "";
                                    if(quotation.getQuotationLine().get(itemIndex).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCountry().getIdentificationCode() != null){
                                        country =  CountryUtil.getCountryNameByISOCode(quotation.getQuotationLine().get(itemIndex).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress().getCountry().getIdentificationCode().getValue());
                                    }
                                    text = text.replace("$nego_country",country);
                                    r.setText(text,0);
                                }
                                if(text.contains("$nego_specTerms")){
                                    List<TextType> specialTerms = quotation.getQuotationLine().get(itemIndex).getLineItem().getDeliveryTerms().getSpecialTerms();
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
                                    BigDecimal value = quotation.getQuotationLine().get(itemIndex).getLineItem().getWarrantyValidityPeriod().getDurationMeasure().getValue();
                                    String unit = quotation.getQuotationLine().get(itemIndex).getLineItem().getWarrantyValidityPeriod().getDurationMeasure().getUnitCode();
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
            // add data monitoring info
            // firstly, get data monitoring text
            String dataMonitoringText = "No";
            if(requestForQuotation.getRequestForQuotationLine().get(itemIndex).getLineItem().isDataMonitoringRequested()){
                if(quotation.getQuotationLine().get(itemIndex).getLineItem().isDataMonitoringRequested()){
                    dataMonitoringText = "Yes / Data monitoring service confirmed";
                }
                else {
                    dataMonitoringText = "Yes / Data monitoring service not provided";
                }
            }
            // create a row for data monitoring info
            XWPFTableRow dataMonitoringRow = table.insertNewTableRow(6);
            XWPFRun run = dataMonitoringRow.createCell().getParagraphs().get(0).createRun();
            run.setUnderline(UnderlinePatterns.SINGLE);
            run.setText("Data Monitoring Service Requested");
            dataMonitoringRow.createCell().getParagraphs().get(0).createRun().setText(dataMonitoringText);
            // remove the border between columns
            dataMonitoringRow.getCell(0).getCTTc().addNewTcPr().addNewTcBorders().addNewRight().setVal(STBorder.NIL);
            dataMonitoringRow.getCell(1).getCTTc().addNewTcPr().addNewTcBorders().addNewLeft().setVal(STBorder.NIL);

            // create rows for delivery dates
            int rowIndex = 20;
            XWPFTableRow deliveryDateRow = table.insertNewTableRow(rowIndex++);
            run = deliveryDateRow.createCell().getParagraphs().get(0).createRun();
            run.setBold(true);
            run.setText("Delivery Dates");
            deliveryDateRow.createCell().getParagraphs().get(0).createRun().setText("");
            // remove the border between columns
            deliveryDateRow.getCell(0).getCTTc().addNewTcPr().addNewTcBorders().addNewRight().setVal(STBorder.NIL);
            deliveryDateRow.getCell(1).getCTTc().addNewTcPr().addNewTcBorders().addNewLeft().setVal(STBorder.NIL);

            for (DeliveryType delivery : quotation.getQuotationLine().get(itemIndex).getLineItem().getDelivery()) {
                XMLGregorianCalendar endDate = delivery.getRequestedDeliveryPeriod().getEndDate();
                QuantityType quantity = delivery.getShipment().getGoodsItem().get(0).getQuantity();
                if(endDate != null && quantity != null){
                    XWPFTableRow row = table.insertNewTableRow(rowIndex++);
                    row.createCell().getParagraphs().get(0).createRun().setText("Delivery Date : "+endDate.toString());
                    row.createCell().getParagraphs().get(0).createRun().setText("Quantity : "+new DecimalFormat(".00").format(quantity.getValue())+" "+ quantity.getUnitCode());
                    // remove the border between columns
                    row.getCell(0).getCTTc().addNewTcPr().addNewTcBorders().addNewRight().setVal(STBorder.NIL);
                    row.getCell(1).getCTTc().addNewTcPr().addNewTcBorders().addNewLeft().setVal(STBorder.NIL);
                }
            }
            // negotiation table 'Notes and Additional Documents' part
            // additional documents
            table = getTable(document,"Negotiation Notes/Additional Documents");
            if(rfqAuxiliaryFiles.size() == 0){
                table.getRow(table.getNumberOfRows()-2).getCell(3).getParagraphs().get(0).createRun().setText("-");
            }
            for(DocumentReferenceType documentReference : rfqAuxiliaryFiles){
                XWPFRun run6 = table.getRow(table.getNumberOfRows()-2).getCell(3).getParagraphs().get(0).createRun();
                run6.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run6.setItalic(true);
            }
            if(quotationAuxiliaryFiles.size() == 0){
                table.getRow(table.getNumberOfRows()-1).getCell(3).getParagraphs().get(0).createRun().setText("-");
            }
            for(DocumentReferenceType documentReference : quotationAuxiliaryFiles){
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
            throw e;
        }

    }

    private void createItemDetailsAuxiliaryFiles(ZipOutputStream zos, ItemInformationRequestType itemInformationRequest,ItemInformationResponseType itemDetails, int id) throws IOException {
        try {
            List<DocumentReferenceType> documentReferences = itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getItemSpecificationDocumentReference();

            if(documentReferences.size() != 0){
                byte[] bytes = binaryContentService.retrieveContent(documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry = new ZipEntry("ItemInformation/ItemInformationRequest"+id+"/TechnicalDataSheets/"+ documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry);
                bos.writeTo(zos);
            }

            // additional documents
            List<DocumentReferenceType> itemInformationRequestAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(itemInformationRequest.getAdditionalDocumentReference());
            List<DocumentReferenceType> itemDetailsAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(itemDetails.getAdditionalDocumentReference());
            for(DocumentReferenceType documentReference : itemInformationRequestAuxiliaryFiles){
                byte[] bytes = binaryContentService.retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry = new ZipEntry("ItemInformation/ItemInformationRequest"+id+"/BuyerDocuments/"+ documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry);
                bos.writeTo(zos);
            }
            // response
            for(DocumentReferenceType documentReference : itemDetailsAuxiliaryFiles){
                byte[] bytes = binaryContentService.retrieveContent(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry = new ZipEntry("ItemInformation/ItemInformationRequest"+id+"/SellerDocuments/"+ documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry);
                bos.writeTo(zos);
            }

            documentReferences = itemDetails.getItem().get(0).getItemSpecificationDocumentReference();

            if(documentReferences.size() != 0){
                byte[] bytes = binaryContentService.retrieveContent(documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri()).getValue();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
                bos.write(bytes,0,bytes.length);

                ZipEntry zipEntry = new ZipEntry("ItemInformation/ItemInformationRequest"+id+"/TechnicalDataSheetResponses/"+documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                zos.putNextEntry(zipEntry);
                bos.writeTo(zos);
            }
        }
        catch (Exception e){
            logger.error("Failed to fill item details",e);
            throw e;
        }

    }

    private void fillItemDetailsTable(XWPFTable table, XWPFTable noteAndDocumentTable, ItemInformationRequestType itemInformationRequest,ItemInformationResponseType itemDetails) {
        try {
            List<DocumentReferenceType> documentReferences = itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getItemSpecificationDocumentReference();

            if(documentReferences.size() == 0){
                table.getRow(2).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            else {
                XWPFRun run2 = table.getRow(2).getCell(1).getParagraphs().get(0).createRun();
                run2.setText(documentReferences.get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());
                run2.setItalic(true);
            }

            // additional documents
            List<DocumentReferenceType> itemInformationRequestAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(itemInformationRequest.getAdditionalDocumentReference());
            List<DocumentReferenceType> itemDetailsAuxiliaryFiles = ProcessDocumentMetadataDAOUtility.getAuxiliaryFiles(itemDetails.getAdditionalDocumentReference());
            // request
            if(itemInformationRequestAuxiliaryFiles.size() == 0){
                noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-2).getCell(3).getParagraphs().get(0).createRun().setText("-");
            }
            for(DocumentReferenceType documentReference : itemInformationRequestAuxiliaryFiles){
                XWPFRun run5 = noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-2).getCell(3).getParagraphs().get(0).createRun();
                run5.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run5.setItalic(true);
            }
            // response
            if(itemDetailsAuxiliaryFiles.size() == 0){
                noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-1).getCell(3).getParagraphs().get(0).createRun().setText("-");
            }
            for(DocumentReferenceType documentReference : itemDetailsAuxiliaryFiles){
                XWPFRun run6 = noteAndDocumentTable.getRow(noteAndDocumentTable.getNumberOfRows()-1).getCell(3).getParagraphs().get(0).createRun();
                run6.setText(documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getFileName()+"\n");
                run6.setItalic(true);
            }

            documentReferences = itemDetails.getItem().get(0).getItemSpecificationDocumentReference();

            if(documentReferences.size() == 0){
                table.getRow(3).getCell(1).getParagraphs().get(0).createRun().setText("-");
            }
            else {
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
            throw e;
        }

    }

    private boolean checkTotalPriceExistsInOrder(OrderLineType orderLine){
        return orderLine.getLineItem().getPrice().getPriceAmount().getValue() != null &&
                orderLine.getLineItem().getPrice().getBaseQuantity().getValue() !=null &&
                orderLine.getLineItem().getQuantity().getValue() != null &&
                orderLine.getLineItem().getPrice().getBaseQuantity().getUnitCode() != null &&
                orderLine.getLineItem().getQuantity().getUnitCode() != null &&
                orderLine.getLineItem().getPrice().getBaseQuantity().getUnitCode().contentEquals(orderLine.getLineItem().getQuantity().getUnitCode());
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
        String country = CountryUtil.getCountryNameByISOCode(address.getCountry().getIdentificationCode().getValue());
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

    private static class TradingTermIndex{
        private int index;
        private String tradingTermId;

        TradingTermIndex(int index, String tradingTermId) {
            this.index = index;
            this.tradingTermId = tradingTermId;
        }

        int getIndex() {
            return index;
        }

        String getTradingTermId() {
            return tradingTermId;
        }
    }

}