package me.shail.resources;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.shail.dtos.OrderDto;
import me.shail.services.OrderService;

import java.util.List;
import java.util.UUID;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {
    @Inject
    OrderService orderService;

    @GET
    public Uni<List<OrderDto>> listAll() {
        return this.orderService.listAll();
    }

    @GET
    @Path("/customers/{customerId}")
    public Uni<List<OrderDto>> findAllByCustomer(@PathParam("customerId") UUID customerId) {
        return this.orderService.findAllByCustomer(customerId);
    }

    @GET
    @Path("/payments/{paymentId}")
    public Uni<OrderDto> findOrderByPaymentId(@PathParam("paymentId") UUID paymentId) {
        return this.orderService.findOrderByPaymentId(paymentId);
    }

    @GET
    @Path("/{id}")
    public Uni<OrderDto> findById(@PathParam("id") UUID orderId) {
        return orderService.findById(orderId);
    }

    @HEAD
    @Path("/{id}")
    public Uni<Response> existsById(@PathParam("id") UUID orderId) {
        return this.orderService.existById(orderId)
                .map(exists -> exists ?
                        Response.ok().build() :
                        Response.status(Response.Status.NOT_FOUND).build()
                );
    }

    @POST
    public Uni<OrderDto> create(OrderDto orderDto) {
        return orderService.create(orderDto);
    }

    @POST
    @Path("/{orderId}/cancel")
    public Uni<Response> cancel(@PathParam("orderId") UUID orderId) {
        return orderService.cancel(orderId).map(_ -> Response.noContent().build());
    }
}