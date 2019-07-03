package eu.nimble.service.bp.impl.contract;

import eu.nimble.service.bp.impl.exception.BusinessProcessException;
import eu.nimble.service.bp.impl.util.persistence.catalogue.ContractPersistenceUtility;
import eu.nimble.service.bp.impl.util.persistence.catalogue.PartyPersistenceUtility;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.service.model.ubl.digitalagreement.DigitalAgreementType;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

/**
 * Created by suat on 08-May-19.
 */
@Component
public class FrameContractService {

    private static final Logger logger = LoggerFactory.getLogger(FrameContractService.class);

    public DigitalAgreementType createOrUpdateFrameContract(String sellerId, String buyerId, ItemType item, QuantityType duration, String quotationId) {
        DigitalAgreementType frameContract = ContractPersistenceUtility.getFrameContractAgreementById(sellerId, buyerId, item.getManufacturersItemIdentification().getID());
        // create new frame contract
        if(frameContract == null) {
            frameContract = createDigitalAgreement(
                    sellerId, buyerId, item, duration, quotationId);

        } else {
            frameContract = updateFrameContractDuration(
                    sellerId, frameContract, duration, quotationId);
        }
        return frameContract;
    }

    /**
     * Tries to create a {@link DigitalAgreementType} with the given information.
     * @return the persisted {@link DigitalAgreementType} instance if everything went alright, otherwise throws a {@link eu.nimble.service.bp.impl.exception.BusinessProcessException}
     */
    public DigitalAgreementType createDigitalAgreement(String sellerId, String buyerId, ItemType item, QuantityType duration, String quotationId) {
        PartyType sellerParty = PartyPersistenceUtility.getParty(sellerId, true);
        PartyType buyerParty = PartyPersistenceUtility.getParty(buyerId, true);

        DigitalAgreementType frameContract = new DigitalAgreementType();
        frameContract.setItem(item);
        DigitalAgreementTermsType terms = new DigitalAgreementTermsType();
        PeriodType period = new PeriodType();
        period.setDurationMeasure(duration);

        XMLGregorianCalendar[] gregorianDates = getStartAndEndDatesInGregorianFormat(duration);
        period.setStartDate(gregorianDates[0]);
        period.setEndDate(gregorianDates[1]);
        terms.setValidityPeriod(period);
        frameContract.setDigitalAgreementTerms(terms);

        DocumentReferenceType quotationReference = new DocumentReferenceType();
        quotationReference.setID(quotationId);
        frameContract.setQuotationReference(quotationReference);

        frameContract.getParticipantParty().add(sellerParty);
        frameContract.getParticipantParty().add(buyerParty);

        EntityIdAwareRepositoryWrapper repository = new EntityIdAwareRepositoryWrapper(sellerId);
        frameContract = repository.updateEntityForPersistCases(frameContract);
        return frameContract;
    }

    /**
     * Updates the duration of the frame contract duration
     */
    public DigitalAgreementType updateFrameContractDuration(String sellerId, DigitalAgreementType frameContract, QuantityType duration, String quotationId) {
        XMLGregorianCalendar[] gregorianDates = getStartAndEndDatesInGregorianFormat(duration);
        frameContract.getDigitalAgreementTerms().getValidityPeriod().setStartDate(gregorianDates[0]);
        frameContract.getDigitalAgreementTerms().getValidityPeriod().setEndDate(gregorianDates[1]);

        frameContract.getQuotationReference().setID(quotationId);

        EntityIdAwareRepositoryWrapper repository = new EntityIdAwareRepositoryWrapper(sellerId);
        frameContract = repository.updateEntity(frameContract);
        return frameContract;
    }

    private XMLGregorianCalendar[] getStartAndEndDatesInGregorianFormat(QuantityType duration) {
        DateTime now = DateTime.now();
        DateTime endDate;
        if(duration.getUnitCode().compareToIgnoreCase("day(s)") == 0) {
            endDate = now.plusDays(duration.getValue().intValue());
        } else if(duration.getUnitCode().compareToIgnoreCase("week(s)") == 0) {
            endDate = now.plusWeeks(duration.getValue().intValue());
        } else if(duration.getUnitCode().compareToIgnoreCase("month(s)") == 0) {
            endDate = now.plusMonths(duration.getValue().intValue());
        } else if(duration.getUnitCode().compareToIgnoreCase("year(s)") == 0) {
            endDate = now.plusYears(duration.getValue().intValue());
        } else {
            String msg = String.format("Unexpected unit encountered. %s", duration.getUnitCode());
            logger.warn(msg);
            throw new BusinessProcessException(msg);
        }

        return transformJodaDatesToXMLGregorian(now, endDate);
    }

    public static XMLGregorianCalendar[] transformJodaDatesToXMLGregorian(DateTime start, DateTime end) {
        GregorianCalendar c = new GregorianCalendar();
        XMLGregorianCalendar[] gregorianDates = new XMLGregorianCalendar[2];
        try {
            c.setTimeInMillis(start.getMillis());
            gregorianDates[0] = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            c.setTimeInMillis(end.getMillis());
            gregorianDates[1] = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            return gregorianDates;

        } catch (DatatypeConfigurationException e) {
            String msg = "Failed to get gregorian dates for the contract duration";
            logger.warn(msg, e);
            throw new BusinessProcessException(msg, e);
        }
    }
}
