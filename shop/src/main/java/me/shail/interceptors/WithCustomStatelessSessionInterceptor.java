package me.shail.interceptors;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.hibernate.SessionException;
import org.hibernate.reactive.mutiny.Mutiny;

@WithCustomStatelessSession
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 10) // Ensures it runs early in the request chain
public class WithCustomStatelessSessionInterceptor {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    Mutiny.SessionFactory sessionFactory;

    @AroundInvoke
    public Object manageStatelessSession(InvocationContext context) throws Exception {
        // Verify the intercepted method returns a Mutiny Uni
        if (Uni.class.isAssignableFrom(context.getMethod().getReturnType())) {
            // Wrap the proceeding invocation inside Hibernate's stateless session
            //noinspection ReactiveStreamsUnusedPublisher
            return sessionFactory.withStatelessSession(statelessSession -> {
                try {
                    // Proceed with the original method execution
                    return (Uni<?>) context.proceed();
                } catch (Exception e) {
                    return Uni.createFrom().failure(e);
                }
            });
        }

        // Throw exception if caller is non-reactive
        throw new SessionException("Should be used on Mutiny streams");
    }
}