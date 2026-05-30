package me.shail.resources;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.shail.dtos.CategoryDto;
import me.shail.services.CategoryService;

import java.util.List;
import java.util.UUID;

@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {
    @Inject
    CategoryService categoryService;

    @GET
    public Uni<List<CategoryDto>> findAll() {
        return this.categoryService.findAll();
    }

    @GET
    @Path("/{id}")
    public Uni<CategoryDto> findById(@PathParam("id") UUID categoryId) {
        return this.categoryService.findById(categoryId);
    }

    @HEAD
    @Path("/{id}")
    public Uni<Response> existsById(@PathParam("id") UUID categoryId) {
        return this.categoryService.existById(categoryId)
                .map(exists -> exists ?
                        Response.ok().build() :
                        Response.status(Response.Status.NOT_FOUND).build()
                );
    }

    @DELETE
    @Path("/{id}/products")
    public Uni<Response> removeAllProductsFromCategory(@PathParam("id") UUID categoryId) {
        return this.categoryService.removeAllProductsFromCategory(categoryId)
                .map(rowsAffected -> {
                    if (rowsAffected == 0)
                        return Response.ok("No products to be unlinked. Category is empty")
                                .build();
                    return Response.noContent().build();
                });
    }

    @POST
    public Uni<CategoryDto> create(CategoryDto categoryDto, @QueryParam("parentCategoryId") UUID parent_category_id) {
        return this.categoryService.create(categoryDto, parent_category_id);
    }
}
