package me.shail.resources;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.shail.dtos.PaymentDto;
import me.shail.services.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {
    @Inject
    PaymentService paymentService;

    @GET
    public Uni<Response> findByPriceRange(@QueryParam("maxAmount") BigDecimal maxAmount) {
        if (maxAmount == null) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Query parameter 'maxAmount' is mandatory.")
                            .build()
            );
        }

        if (maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Query parameter 'maxAmount' is should be positive.")
                            .build()
            );
        }

        return this.paymentService.findByPriceRange(maxAmount)
                .map(payments -> Response.ok(payments).build());
    }

    @GET
    @Path("/{id}")
    public Uni<PaymentDto> findById(@PathParam("id") UUID paymentId) {
        return this.paymentService.findById(paymentId);
    }

    @POST
    @Path("/orders/{orderId}")
    public Uni<PaymentDto> create(@PathParam("orderId") UUID orderId) {
        return this.paymentService.create(orderId);
    }

    @POST
    @Path("/{id}/refund")
    public Uni<Response> generateRefund(@PathParam("id") UUID paymentId) {
        return this.paymentService.generateRefund(paymentId)
                .map(_ -> Response.ok().build());
    }
}
