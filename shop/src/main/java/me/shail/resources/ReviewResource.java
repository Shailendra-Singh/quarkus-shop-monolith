package me.shail.resources;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.shail.dtos.ReviewDto;
import me.shail.services.ReviewService;

import java.util.List;
import java.util.UUID;

@Path("/reviews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReviewResource {
    @Inject
    ReviewService reviewService;

    @GET
    @Path("/{id}")
    public Uni<ReviewDto> findById(@PathParam("id") UUID reviewId) {
        return this.reviewService.findById(reviewId);
    }

    @HEAD
    @Path("/{id}")
    public Uni<Response> existById(@PathParam("id") UUID reviewId) {
        return this.reviewService.existById(reviewId).map(
                exists -> exists ?
                        Response.ok().build() :
                        Response.status(Response.Status.NOT_FOUND).build()
        );
    }

    @GET
    @Path("/products/{productId}")
    public Uni<List<ReviewDto>> findReviewsByProductId(@PathParam("productId") UUID productId) {
        return this.reviewService.findReviewsByProductId(productId);
    }

    @GET
    @Path("/products/{productId}/count")
    public Uni<Long> countReviewsByProductId(@PathParam("productId") UUID productId) {
        return this.reviewService.countReviewsByProductId(productId);
    }

    @POST
    @Path("/products/{productId}")
    public Uni<ReviewDto> create(ReviewDto reviewDto, @PathParam("productId") UUID productId) {
        return this.reviewService.create(reviewDto, productId);
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> deleteById(@PathParam("id") UUID reviewId) {
        return this.reviewService.deleteById(reviewId)
                .map(_ -> Response.noContent().build());
    }

    @DELETE
    @Path("/products/{productId}")
    public Uni<Response> deleteAllReviewsByProductId(@PathParam("productId") UUID productId) {
        return this.reviewService.deleteAllReviewsByProductId(productId)
                .map(_ -> Response.noContent().build());
    }
}