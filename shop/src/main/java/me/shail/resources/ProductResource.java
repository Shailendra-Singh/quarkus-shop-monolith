package me.shail.resources;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.shail.dtos.ProductDto;
import me.shail.services.ProductService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {
    @Inject
    ProductService productService;

    @GET
    public Uni<List<ProductDto>> findAll() {
        return this.productService.findAll();
    }

    @GET
    @Path("/{id}")
    public Uni<ProductDto> findById(@PathParam("id") UUID productId) {
        return this.productService.findById(productId);
    }

    @GET
    @Path("/categories/{categoryId}")
    public Uni<List<ProductDto>> findByCategoryId(@PathParam("categoryId") UUID categoryId) {
        return this.productService.findByCategoryId(categoryId);
    }

    @HEAD
    @Path("/{id}")
    public Uni<Response> existsById(@PathParam("id") UUID productId) {
        return this.productService.existById(productId)
                .map(exists -> exists ?
                        Response.ok().build() :
                        Response.status(Response.Status.NOT_FOUND).build()
                );
    }

    @GET
    @Path("/counts")
    public Uni<Long> countAll() {
        return this.productService.countAll();
    }

    @GET
    @Path("/counts/categories/{categoryId}")
    public Uni<Long> countByCategoryId(@PathParam("categoryId") UUID categoryId) {
        return this.productService.countByCategoryId(categoryId);
    }

    @POST
    public Uni<ProductDto> create(ProductDto productDto) {
        return this.productService.create(productDto);
    }

    @PUT
    @Path("/{id}/categories/{categoryId}")
    public Uni<Response> addCategoryToProduct(@PathParam("id") UUID productId,
                                              @PathParam("categoryId") UUID categoryId) {
        return this.productService.addCategoryToProduct(productId, categoryId)
                .map(_ -> Response.noContent().build());
    }

    @POST
    @Path("/{id}/categories")
    public Uni<Response> addAllCategoriesToProduct(@PathParam("id") UUID productId,
                                                   Set<UUID> categoryIds) {
        return this.productService.addAllCategoriesToProduct(productId, categoryIds)
                .map(_ -> Response.noContent().build());
    }

    @DELETE
    @Path("/{id}/categories/{categoryId}")
    public Uni<Response> removeCategoryFromProduct(@PathParam("id") UUID productId,
                                                   @PathParam("categoryId") UUID categoryId) {
        return this.productService.removeCategoryFromProduct(productId, categoryId)
                .map(_ -> Response.noContent().build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> deleteById(@PathParam("id") UUID productId) {
        return this.productService.deleteById(productId)
                .map(_ -> Response.noContent().build());
    }
}