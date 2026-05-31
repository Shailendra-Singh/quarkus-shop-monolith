package me.shail.resources.interceptors;

import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


public class RestAssuredJsonExtension implements BeforeAllCallback {

    private static boolean initialized = false;

    @Override
    public void beforeAll(@NonNull ExtensionContext context) {
        // Only register the global filter once for the entire test suite run
        if(!initialized){
            Filter jsonInterceptor = (requestSpec, responseSpec, ctx) -> {
                // Intercept the outbound request and append JSON content-type
                requestSpec.contentType(ContentType.JSON);

                // Proceed down the execution wire (Quarkus ports/paths are fully preserved here!)
                var response = ctx.next(requestSpec, responseSpec);

                // Intercept the inbound response and asert it's valid JSON
                // Conditional validation: Skip if it's an HTTP 204 No Content
                String httpMethod = requestSpec.getMethod();
                int statusCode = response.getStatusCode();

                // Skip JSON content validation if:
                // - It's a 204 No Content status
                // - It's a HEAD request method
                boolean skipJsonCheck = (statusCode == 204) || "HEAD".equalsIgnoreCase(httpMethod);

                if (!skipJsonCheck) {
                    response.then().contentType(ContentType.JSON);
                }

                return response;
            };

            // Register this interceptor globally without disturbing Quarkus configurations
            RestAssured.filters(jsonInterceptor);
            initialized = true;
        }
    }
}