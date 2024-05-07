package com.orders.routes.stripe.handlers;

import com.orders.routes.stripe.dto.CheckoutSessionReq;
import com.orders.routes.stripe.dto.CheckoutSessionRes;
import com.orders.routes.stripe.dto.MenuItem;
import com.orders.routes.stripe.dto.Restaurant;
import com.orders.model.CartItem;
import com.orders.model.Order;
import com.orders.routes.stripe.repository.StripeOrdersRepository;
import com.orders.utils.BaseHandler;
import com.orders.utils.ValidationHandler;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData;
import com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.ProductData;
import com.stripe.param.checkout.SessionCreateParams.ShippingOption.ShippingRateData;
import com.stripe.param.checkout.SessionCreateParams.ShippingOption;
import com.stripe.param.checkout.SessionCreateParams.ShippingOption.ShippingRateData.FixedAmount;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.stripe.net.Webhook;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class StripeRoutesHandler extends BaseHandler {

    private StripeOrdersRepository stripeOrdersRepository;
    private StripeClient stripe;
    private ValidationHandler validationHandler;

    @Autowired
    public StripeRoutesHandler(StripeOrdersRepository stripeOrdersRepository, ValidationHandler validationHandler, ReactiveJwtDecoder jwtDecoder) {
        this.stripeOrdersRepository = stripeOrdersRepository;
        this.stripe = new StripeClient(System.getenv("STRIPE_SK_TEST_KEY"));
        this.validationHandler = validationHandler;
        initializeBaseHandler(System.getenv("RESTAURANT_SVC_ADDRESS"), jwtDecoder);
    }

    public StripeRoutesHandler() {
    }

    public Mono<ServerResponse> stripeCheckoutWebhook(ServerRequest req) {
        return req.bodyToMono(String.class)
                .flatMap(payload -> {
                    String stripeSignature = req.headers().firstHeader("stripe-signature");
                    Mono<Event> monoEvent = Mono.fromCallable(() -> {
                        return Webhook.constructEvent(
                                payload,
                                stripeSignature,
                                System.getenv("STRIPE_ENDPOINT_SECRET")
                        );
                    }).flatMap(Mono::just);
                    return monoEvent
                            .flatMap(event -> {
                                EventDataObjectDeserializer eventDataObjectDeserializer = event.getDataObjectDeserializer();
                                StripeObject stripeObject = null;
                                if (eventDataObjectDeserializer.getObject().isPresent()) {
                                    stripeObject = eventDataObjectDeserializer.getObject().get();
                                } else {
                                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "eventDataObjectDeserializer was null"));
                                }
                                if (event.getType().equals("checkout.session.completed")) {
                                    String orderId = ((Session) stripeObject).getMetadata().get("orderId");
                                    Long totalAmount = ((Session) stripeObject).getAmountTotal();
                                    return this.stripeOrdersRepository.findById(orderId)
                                            .flatMap(order -> {
                                                order.setStatus(Order.Status.PAID);
                                                order.setTotalAmount(totalAmount);
                                                return this.stripeOrdersRepository.save(order)
                                                        .flatMap(savedOrder -> ServerResponse.status(HttpStatus.OK).build());
                                            });
                                }
                                return ServerResponse.status(HttpStatus.OK).build();
                            });
                })
                .onErrorMap(StripeException.class, e -> {
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe API Error" + e.getMessage());
                });
    }

    public Mono<List<SessionCreateParams.LineItem>> createLineItems(Mono<Restaurant> restaurantMono, List<CartItem> cartItems) {
        return restaurantMono
                .flatMap(restaurant -> {
                    Map<String, MenuItem> menuItems = restaurant.getMenuItems().stream()
                            .collect(Collectors.toMap(MenuItem::getId, menuItem -> menuItem));
                    return Flux.fromIterable(cartItems)
                            .flatMap(cartItem -> {
                                MenuItem menuItem = menuItems.get(cartItem.getId());
                                // each cartItem must match an object in the fetched restaurant's menuItem list
                                // for the sake of validating prices
                                // if an item does not match, throw error (or return Mono.error?)
                                // when an item is matched, assign the matched item to menuItem
                                if (menuItem == null) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant not found"));
                                } else {
                                    return Mono.just(createLineItem(menuItem, cartItem.getQuantity()));
                                }
                            })
                            .collectList();
                });
    }

    private SessionCreateParams.LineItem createLineItem(MenuItem menuItem, Integer quantity) {
        ProductData productData = ProductData
                .builder()
                .setName(menuItem.getName())
                .build();
        PriceData priceData = LineItem
                .PriceData
                .builder()
                .setCurrency("usd")
                .setUnitAmount(menuItem.getPrice())
                .setProductData(productData)
                .build();
        return LineItem
                .builder()
                .setPriceData(priceData)
                .setQuantity((long) quantity)
                .build();
    }

    public Mono<ServerResponse> createCheckoutSession(ServerRequest req) {
        String authorizationHeader = req.headers().firstHeader("Authorization");
        return req.bodyToMono(CheckoutSessionReq.class)
                .flatMap(checkoutSessionReq -> {
                    this.validationHandler.validate(checkoutSessionReq, "checkoutSessionReq");
                    Mono<Restaurant> restaurant = this.webClient.get()
                            .uri("/" + checkoutSessionReq.getRestaurantSlug())
                            .retrieve()
                            .bodyToMono(Restaurant.class)
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found")));
                    List<CartItem> cartItems = checkoutSessionReq.getCartItems();
                    Mono<List<SessionCreateParams.LineItem>> lineItems = createLineItems(restaurant, cartItems);
                    return this.getAuth0IdFromToken(authorizationHeader)
                            .flatMap(auth0Id -> {
                                return restaurant
                                        .flatMap(restaurantFlat -> {
                                            ObjectId orderId = new ObjectId();
                                            Order order = new Order();
                                            order.setId(orderId);
                                            order.setRestaurantId(restaurantFlat.getId());
                                            order.setStatus(Order.Status.PLACED);
                                            order.setDeliveryDetails(checkoutSessionReq.getDeliveryDetails());
                                            order.setCartItems(cartItems);
                                            order.setDeliveryPrice(restaurantFlat.getDeliveryPrice());
                                            order.setUserId(auth0Id);
                                            return stripeOrdersRepository.save(order)
                                                    .flatMap(orderFlat -> {
                                                        return this.createSession(
                                                                        lineItems,
                                                                        orderFlat.getId(),
                                                                        restaurantFlat.getDeliveryPrice(),
                                                                        restaurantFlat.getId()
                                                                )
                                                                .flatMap(session -> {
                                                                    CheckoutSessionRes res = new CheckoutSessionRes(session.getUrl());
                                                                    return ServerResponse
                                                                            .status(HttpStatus.CREATED)
                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                            .bodyValue(res);
                                                                });
                                                    });
                                        });
                            });
                });
    }

    private Mono<Session> createSession(
            Mono<List<SessionCreateParams.LineItem>> lineItems,
            ObjectId orderId,
            Long deliveryPrice,
            String restaurantId
    ) {
        return lineItems.flatMap(lineItemsFlat -> {
                    RequestOptions options = RequestOptions.builder()
                            .setConnectTimeout(30 * 1000)
                            .setReadTimeout(80 * 1000)
                            .build();
                    FixedAmount fixedAmount =
                            FixedAmount.builder()
                                    .setAmount((long) deliveryPrice)
                                    .setCurrency("usd").build();
                    ShippingRateData shippingRateData = SessionCreateParams
                            .ShippingOption
                            .ShippingRateData
                            .builder()
                            .setDisplayName("Delivery")
                            .setType(ShippingRateData.Type.FIXED_AMOUNT)
                            .setFixedAmount(fixedAmount)
                            .build();
                    ShippingOption shippingOption = ShippingOption
                            .builder()
                            .setShippingRateData(shippingRateData)
                            .build();
                    SessionCreateParams params = SessionCreateParams.builder()
                            .setSuccessUrl(System.getenv("CLIENT_SVC_ADDRESS") + "/order-status?success=true")
                            .setCancelUrl(System.getenv("CLIENT_SVC_ADDRESS") + "/detail?canceled=true")
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .addShippingOption(shippingOption)
                            .putMetadata("orderId", orderId.toString())
                            .putMetadata("restaurantId", restaurantId)
                            .addAllLineItem(lineItemsFlat)
                            .build();
                    return Mono.fromCallable(() -> this.stripe
                                    .checkout()
                                    .sessions()
                                    .create(params, options))
                            .flatMap(session -> {
                                if (session.getSuccessUrl().isBlank()) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Success URL is blank"));
                                }
                                return Mono.just(session);
                            });
                })
                .onErrorMap(StripeException.class, e -> {
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe API Error");
                });
    }
}
