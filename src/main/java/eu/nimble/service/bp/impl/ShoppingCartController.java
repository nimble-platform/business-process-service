package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.util.persistence.catalogue.CataloguePersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.validation.IValidationUtil;
import feign.Response;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by suat on 11-Oct-19.
 */
@Controller
public class ShoppingCartController {

    private static final Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @ApiOperation(value = "", notes = "Creates an empty shopping cart for the user")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created a shopping cart in the form of CatalogueType", response = CatalogueType.class),
            @ApiResponse(code = 401, message = "Invalid user or role")
    })
    @RequestMapping(value = "/shopping-cart",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getShoppingCart(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws Exception{
        // set request log of ExecutionContext
        String requestLog = "Incoming request to get shopping cart";
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);
        try {
            // validate role
            if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // get person via the given bearer token
            PersonType person;
            try {
                person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);

            } catch (IOException e) {
                logger.error("Failed to retrieve person for the token: {}", bearerToken);
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_FAILED_TO_CREATE_SHOPPING_CART.toString(),e);
            }

            CatalogueType cartCatalogue = CataloguePersistenceUtility.getShoppingCartWithPersonId(person.getID());

            logger.info("Completed request to get shopping cart");
            if (cartCatalogue == null) {
                return ResponseEntity.ok(null);
            } else {
                return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(cartCatalogue));
            }
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GET_SHOPPING_CART.toString(), Arrays.asList(bearerToken),e);
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
    public ResponseEntity createShoppingCart(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws Exception{
        // set request log of ExecutionContext
        String requestLog = "Incoming request to create shopping cart";
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);
        CatalogueType cartCatalogue;
        try {
            // validate role
            if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
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
                logger.error("Failed to retrieve person for the token: {}", bearerToken);
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_FAILED_TO_CREATE_SHOPPING_CART.toString(),e);
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
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_CREATE_SHOPPING_CART.toString(),Arrays.asList(bearerToken),e);
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
                                                   @ApiParam(value = "Quantity of the product in the shopping cart", required = false, defaultValue = "1") @RequestParam(value = "quantity", required = false, defaultValue = "1") Integer quantity,
                                                   @ApiParam(value = "Identifier of the instance which the product belongs to", required = true) @RequestHeader(value = "federationId", required = false) String federationId) throws Exception {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to add product: %s, in %s quantity to the create shopping cart", productId, quantity);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // get person via the given bearer token
            PersonType person;
            try {
                person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);

            } catch (IOException e) {
                logger.error("Failed to retrieve person for the token: {}", bearerToken);
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_FAILED_TO_CREATE_SHOPPING_CART.toString(),e);
            }

            // retrieve the product to be added to the shopping cart
            CatalogueLineType originalProduct = null;
            if(federationId != null && !federationId.contentEquals(SpringBridge.getInstance().getFederationId())){
                Response response = SpringBridge.getInstance().getDelegateClient().getCatalogLineByHjid(bearerToken,productId);
                originalProduct = JsonSerializationUtility.getObjectMapper().readValue(HttpResponseUtil.extractBodyFromFeignClientResponse(response),CatalogueLineType.class);
            }
            else {
                originalProduct = new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntityByHjid(CatalogueLineType.class, productId);
            }

            if (originalProduct == null) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NO_PRODUCT.toString(),Arrays.asList(productId.toString()));
            }

            // retrieve the shopping cart instance
            CatalogueType cartCatalogue = CataloguePersistenceUtility.getShoppingCartWithPersonId(person.getID());
            if (cartCatalogue == null) {
                throw new NimbleException(NimbleExceptionMessageCode.PRECONDITION_FAILED_NO_SHOPPING_CART.toString(),Arrays.asList(person.getID()));
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
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_ADD_PRODUCT_TO_SHOPPING_CART.toString(),Arrays.asList(productId.toString()),e);
        }
    }

//    @ApiOperation(value = "", notes = "Increases the quantity of the specified product in the shopping cart")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Increased the quantity of the product in the shopping cart", response = CatalogueType.class),
//            @ApiResponse(code = 400, message = "There is no product for the specified id"),
//            @ApiResponse(code = 401, message = "Invalid user or role"),
//            @ApiResponse(code = 412, message = "The client user does not have an associated shopping cart")
//    })
//    @RequestMapping(value = "/shopping-cart/{cartItemId}",
//            produces = {"application/json"},
//            method = RequestMethod.POST)
//    public ResponseEntity increaseProductQuantityInShoppingCart(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
//                                                   @ApiParam(value = "Hjid of the product", required = true) @PathVariable(value = "cartItemId", required = true) Long productId,
//                                                   @ApiParam(value = "Increase amount", required = true, defaultValue = "1") @RequestParam(value = "quantity", required = true, defaultValue = "1") Integer quantity) throws Exception {
//        try {
//            // set request log of ExecutionContext
//            String requestLog = String.format("Incoming request to increase the quantity of product product: %s, quantity: %s", productId, quantity);
//            executionContext.setRequestLog(requestLog);
//
//            logger.info(requestLog);
//            // validate role
//            if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
//                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
//            }
//
//            // fetch the and update catalogue line
//            GenericJPARepository repository = new JPARepositoryFactory().forCatalogueRepository();
//            CatalogueLineType cartProduct = new JPARepositoryFactory().forCatalogueRepository().getSingleEntityByHjid(CatalogueLineType.class, productId);
//            cartProduct.getGoodsItem().getQuantity().setValue(new BigDecimal(cartProduct.getGoodsItem().getQuantity().getValue().toBigInteger().intValue() + quantity));
//            repository.updateEntity(repository);
//
//            logger.info("Completed request to increase the quantity of product product: {}, quantity: {}", productId, quantity);
//            return ResponseEntity.ok().build();
//
//        } catch (Exception e) {
//            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_REMOVE_PRODUCTS_FROM_SHOPPING_CART.toString(), Collections.singletonList(productIds.toString()),e);
//        }
//    }

    @ApiOperation(value = "", notes = "Removes the specified products from the shopping cart")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Removed the products to the shopping cart", response = CatalogueType.class),
            @ApiResponse(code = 400, message = "There is no product in the cart for the specified id"),
            @ApiResponse(code = 401, message = "Invalid user or role"),
            @ApiResponse(code = 412, message = "The client user does not have an associated shopping cart")
    })
    @RequestMapping(value = "/shopping-cart",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity removeProductsFromShoppingCart(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                        @ApiParam(value = "Hjid of the products to be deleted from the cart", required = true) @RequestParam(value = "productIds", required = true) List<Long> productIds) throws Exception {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to remove products from shopping cart: %s", productIds);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if (!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // get person via the given bearer token
            PersonType person;
            try {
                person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);

            } catch (IOException e) {
                logger.error("Failed to retrieve person for the token: {}", bearerToken);
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_FAILED_TO_CREATE_SHOPPING_CART.toString(),e);
            }

            // retrieve the shopping cart instance
            CatalogueType cartCatalogue = CataloguePersistenceUtility.getShoppingCartWithPersonId(person.getID());
            if (cartCatalogue == null) {
                throw new NimbleException(NimbleExceptionMessageCode.PRECONDITION_FAILED_NO_SHOPPING_CART.toString(),Arrays.asList(person.getID()));
            }

            // add the copy reduced product to the cart
            if (cartCatalogue.getCatalogueLine() != null) {
                List<CatalogueLineType> catalogueLinesToBeRemoved = new ArrayList<>();
                for(CatalogueLineType product : cartCatalogue.getCatalogueLine()) {
                    if(productIds.contains(product.getHjid())){
                        catalogueLinesToBeRemoved.add(product);
                    }
                }
                cartCatalogue.getCatalogueLine().removeAll(catalogueLinesToBeRemoved);
            }

            cartCatalogue = new JPARepositoryFactory().forCatalogueRepository(true).updateEntity(cartCatalogue);

            logger.info("Completed request to remove products: {} from the create shopping cart", productIds);
            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(cartCatalogue));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_REMOVE_PRODUCTS_FROM_SHOPPING_CART.toString(), Collections.singletonList(productIds.toString()),e);
        }
    }
}


