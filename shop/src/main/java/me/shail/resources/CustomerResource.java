package me.shail.resources;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.shail.dtos.CustomerDto;
import me.shail.services.CustomerService;

import java.util.List;
import java.util.UUID;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {
    @Inject
    CustomerService customerService;

    @GET
    public Uni<List<CustomerDto>> findAll() {
        return this.customerService.findAll();
    }

    @GET
    @Path("/{id}")
    public Uni<CustomerDto> findById(@PathParam("id") UUID id) {
        return this.customerService.findById(id);
    }

    @GET
    @Path("/status/enabled")
    public Uni<List<CustomerDto>> findAllEnabled() {
        return this.customerService.findAllByState(true);
    }

    @GET
    @Path("/status/disabled")
    public Uni<List<CustomerDto>> findAllDisabled() {
        return this.customerService.findAllByState(false);
    }

    @POST
    public Uni<CustomerDto> create(CustomerDto customerDto) {
        return this.customerService.create(customerDto);
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@PathParam("id") UUID id) {
        return this.customerService.delete(id).map(_ ->
                Response.noContent().build()
        );
    }
}



