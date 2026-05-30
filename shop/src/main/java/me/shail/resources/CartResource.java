package me.shail.resources;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.shail.dtos.CartDto;
import me.shail.services.CartService;

import java.util.List;
import java.util.UUID;

@Path("/carts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CartResource {
    @Inject
    CartService cartService;

    @GET
    public Uni<List<CartDto>> listAll() {
        return this.cartService.listAll();
    }

    @GET
    @Path("/active")
    public Uni<List<CartDto>> findAllActiveCarts() {
        return this.cartService.findAllActiveCarts();
    }

    @GET
    @Path("/{id}")
    public Uni<CartDto> findById(@PathParam("id") UUID id) {
        return this.cartService.findById(id);
    }

    @GET
    @Path("/customer/{customerId}")
    public Uni<CartDto> getActiveCart(@PathParam("customerId") UUID customerId) {
        return this.cartService.getActiveCart(customerId);
    }

    @POST
    @Path("/customer/{customerId}")
    public Uni<CartDto> create(@PathParam("customerId") UUID customerId) {
        return this.cartService.create(customerId);
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@PathParam("id") UUID id) {
        return this.cartService.delete(id)
                .map(_ -> Response.noContent().build());
    }
}