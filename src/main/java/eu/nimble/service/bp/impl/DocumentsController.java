package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;

import static eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog;

/**
 * Created by suat on 12-Sep-19.
 */
@Controller
public class DocumentsController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IValidationUtil validationUtil;

    @ApiOperation(value = "", notes = "Retrieves the unshipped order ids for a specific party or for all parties. " +
            "Unshipped order indicates that an order is not followed by a fulfillment process.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved unshipped orders identifiers"),
            @ApiResponse(code = 401, message = "Invalid token or role"),
            @ApiResponse(code = 500, message = "Unexpected error while retrieving the unshipped order identifiers")
    })
    @RequestMapping(value = "/documents/unshipped-order-ids",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<Object> getDocumentJsonContent(
            @ApiParam(value = "Flag indicating whether the unshipped orders will be retrieved for a specific party or all parties", required = false) @RequestParam(value = "forAll", required = false, defaultValue = "false") Boolean forAll,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken
    ) {
        try {
            logger.info("Getting unshipped order ids");
            // validate role
            if (!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            List<String> unshippedOrderIds = null;
            if(!forAll) {
                // get person using the given bearer token
                String partyId;
                try {
                    PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
                    // get party for the person
                    PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);
                    partyId = party.getPartyIdentification().get(0).getID();
                } catch (IOException e) {
                    return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to extract party if from token: %s", bearerToken), e, HttpStatus.INTERNAL_SERVER_ERROR);
                }

                unshippedOrderIds = ProcessDocumentMetadataDAOUtility.getUnshippedOrderIds(partyId);

            } else {
                unshippedOrderIds = ProcessDocumentMetadataDAOUtility.getUnshippedOrderIds();
            }

            logger.info("Retrieved unshipped order ids for for all: {}", forAll);
            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(unshippedOrderIds));

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while getting the identifiers of the unshipped orders"), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
