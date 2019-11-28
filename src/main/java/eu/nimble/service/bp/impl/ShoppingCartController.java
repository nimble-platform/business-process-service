package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.util.persistence.catalogue.CataloguePersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by suat on 11-Oct-19.
 */
@Controller
public class ShoppingCartController {

    private static final Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);

    @Autowired
    private IValidationUtil validationUtil;

    @ApiOperation(value = "", notes = "Creates an empty shopping cart for the user")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created a shopping cart in the form of CatalogueType", response = CatalogueType.class),
            @ApiResponse(code = 401, message = "Invalid user or role")
    })
    @RequestMapping(value = "/shopping-cart",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getShoppingCart(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        logger.info("Incoming request to get shopping cart");
        try {
            // validate role
            if (!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            // get person via the given bearer token
            PersonType person;
            try {
                person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);

            } catch (IOException e) {
                logger.error("Failed to retrieve for the token: {}", bearerToken);
                return HttpResponseUtil.createResponseEntityAndLog("Failed to create shopping cart instance", e, HttpStatus.UNAUTHORIZED);
            }

            CatalogueType cartCatalogue = CataloguePersistenceUtility.getShoppingCartWithPersonId(person.getID());

            logger.info("Completed request to get shopping cart");
            if (cartCatalogue == null) {
                return ResponseEntity.ok(null);
            } else {
                return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(cartCatalogue));
            }
        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to get shopping cart for the token: %s", bearerToken), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "", notes = "Creates an empty shopping cart for the user")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created a shopping cart in the form of CatalogueType", response = CatalogueType.class),
            @ApiResponse(code = 401, message = "Invalid user or role")
    })
    @RequestMapping(value = "/shopping-cart/new",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity createShoppingCart(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        logger.info("Incoming request to create shopping cart");
        CatalogueType cartCatalogue;
        try {
            // validate role
            if (!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            cartCatalogue = new CatalogueType();
            cartCatalogue.setID("SHOPPING_CART");
            cartCatalogue.setUUID(UUID.randomUUID().toString());

            // associate party and user
            // get person via the given bearer token
            PersonType person;
            try {
                person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);

            } catch (IOException e) {
                logger.error("Failed to retrieve for the token: {}", bearerToken);
                return HttpResponseUtil.createResponseEntityAndLog("Failed to create shopping cart instance", e, HttpStatus.UNAUTHORIZED);
            }

            List<PersonType> personList = new ArrayList<>();
            personList.add(person);
            // we create a dummy party as we cannot use the party instance referring to the user as there might be
            // several person associated to that party. We neither can copy the party as we have a unique constraint
            // on the party identification table. For shopping carts the party is not important. Our aim is to associate
            // it to a Person.
            PartyType party = new PartyType();
            party.setPerson(personList);
            cartCatalogue.setProviderParty(party);

            // persist the catalogue
            cartCatalogue = new JPARepositoryFactory().forCatalogueRepository(true).updateEntity(cartCatalogue);

            logger.info("Completed request to create shopping cart");
            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(cartCatalogue));

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to create a shopping cart for the token: %s", bearerToken), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "", notes = "Adds the specified product to the shopping cart")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Added the product to the shopping cart", response = CatalogueType.class),
            @ApiResponse(code = 400, message = "There is no product for the specified id"),
            @ApiResponse(code = 401, message = "Invalid user or role"),
            @ApiResponse(code = 412, message = "The client user does not have an associated shopping cart")
    })
    @RequestMapping(value = "/shopping-cart",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity addProductToShoppingCart(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                   @ApiParam(value = "Hjid of the product", required = true) @RequestParam(value = "productId", required = true) Long productId,
                                                   @ApiParam(value = "Quantity of the product in the shopping cart", required = false, defaultValue = "1") @RequestParam(value = "quantity", required = false, defaultValue = "1") Integer quantity) {
        try {
            logger.info("Incoming request to add product: {}, in {} quantity to the create shopping cart", productId, quantity);
            // validate role
            if (!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            // get person via the given bearer token
            PersonType person;
            try {
                person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);

            } catch (IOException e) {
                logger.error("Failed to retrieve for the token: {}", bearerToken);
                return HttpResponseUtil.createResponseEntityAndLog("Failed to create shopping cart instance", e, HttpStatus.UNAUTHORIZED);
            }

            // retrieve the product to be added to the shopping cart
            CatalogueLineType originalProduct = new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntityByHjid(CatalogueLineType.class, productId);
            if (originalProduct == null) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("There is no product for the given id: %d", productId), HttpStatus.BAD_REQUEST);
            }

            // retrieve the shopping cart instance
            CatalogueType cartCatalogue = CataloguePersistenceUtility.getShoppingCartWithPersonId(person.getID());
            if (cartCatalogue == null) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("There is no shopping cart for the user: %s", person.getID()), HttpStatus.PRECONDITION_FAILED);
            }

            // select only the first values from the dimensions and properties
            // remove hjid fields from the object so that we would get a copy of the original product
            CatalogueLineType cartProduct = JsonSerializationUtility.removeHjidFields(originalProduct, true);

            // set the quantity
            QuantityType quantityObject = new QuantityType();
            quantityObject.setValue(BigDecimal.valueOf(quantity));
            quantityObject.setUnitCode(cartProduct.getRequiredItemLocationQuantity().getPrice().getBaseQuantity().getUnitCode());
            cartProduct.getGoodsItem().setQuantity(quantityObject);

            // add the copy reduced product to the cart
            if (cartCatalogue.getCatalogueLine() == null) {
                cartCatalogue.setCatalogueLine(new ArrayList<>());
            }
            cartCatalogue.getCatalogueLine().add(cartProduct);
            cartCatalogue = new JPARepositoryFactory().forCatalogueRepository(true).updateEntity(cartCatalogue);

            logger.info("Completed request to add product: {}, in {} quantity to the create shopping cart", productId, quantity);
            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(cartCatalogue));

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while adding product: %d to the cart", productId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "", notes = "Removes the specified product from the shopping cart")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Removed the product to the shopping cart", response = CatalogueType.class),
            @ApiResponse(code = 400, message = "There is no product in the cart for the specified id"),
            @ApiResponse(code = 401, message = "Invalid user or role"),
            @ApiResponse(code = 412, message = "The client user does not have an associated shopping cart")
    })
    @RequestMapping(value = "/shopping-cart",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity removeProductFromShoppingCart(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                        @ApiParam(value = "Hjid of the product to be deleted from the cart", required = true) @RequestParam(value = "productId", required = true) Long productId) {
        try {
            logger.info("Incoming request to remove product from shopping cart: {}", productId);
            // validate role
            if (!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            // get person via the given bearer token
            PersonType person;
            try {
                person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);

            } catch (IOException e) {
                logger.error("Failed to retrieve for the token: {}", bearerToken);
                return HttpResponseUtil.createResponseEntityAndLog("Failed to create shopping cart instance", e, HttpStatus.UNAUTHORIZED);
            }

            // retrieve the shopping cart instance
            CatalogueType cartCatalogue = CataloguePersistenceUtility.getShoppingCartWithPersonId(person.getID());
            if (cartCatalogue == null) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("There is no shopping cart for the user: %s", person.getID()), HttpStatus.PRECONDITION_FAILED);
            }

            // add the copy reduced product to the cart
            if (cartCatalogue.getCatalogueLine() != null) {
                for(CatalogueLineType product : cartCatalogue.getCatalogueLine()) {
                    if(product.getHjid().equals(productId)) {
                        cartCatalogue.getCatalogueLine().remove(product);
                        break;
                    }
                }
            }
            cartCatalogue = new JPARepositoryFactory().forCatalogueRepository(true).updateEntity(cartCatalogue);

            logger.info("Completed request to remove product: {} from the create shopping cart", productId);
            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(cartCatalogue));

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while removing product: %d from the cart", productId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}


