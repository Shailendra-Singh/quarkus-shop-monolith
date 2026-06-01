package me.shail.resources;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.shail.dtos.OrderItemDto;
import me.shail.services.OrderItemService;

import java.util.List;
import java.util.UUID;

@Path("/order-items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderItemResource {
    @Inject
    OrderItemService orderItemService;

    @GET
    @Path("/{id}")
    public Uni<OrderItemDto> findById(@PathParam("id") UUID orderItemId) {
        return this.orderItemService.findById(orderItemId);
    }

    @GET
    @Path("/orders/{orderId}")
    public Uni<List<OrderItemDto>> findAllByOrderId(@PathParam("orderId") UUID orderId) {
        return this.orderItemService.findAllByOrderId(orderId);
    }

    @POST
    public Uni<OrderItemDto> create(OrderItemDto orderItemDto) {
        return this.orderItemService.create(orderItemDto);
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> deleteById(@PathParam("id") UUID orderItemId) {
        return this.orderItemService.deleteById(orderItemId)
                .map(_ -> Response.noContent().build());
    }
}
