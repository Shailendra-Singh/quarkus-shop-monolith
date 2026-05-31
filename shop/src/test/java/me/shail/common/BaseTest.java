package me.shail.common;

import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    Mutiny.SessionFactory sessionFactory;

    // Cache the truncation query string so we don't recalculate it constantly
    private static String cachedTruncateQuery = null;

    @BeforeEach
    public void clearDatabase() {
        if (cachedTruncateQuery == null) {
            // First-time initialization: Build and cache the query string
            buildAndCacheQuery();
        }

        // If the database has no custom tables yet, skip entirely
        if (cachedTruncateQuery.isEmpty()) {
            return;
        }

        // Blistering fast execution using the pre-cached SQL string
        sessionFactory.withTransaction((session, _) ->
                session.createNativeQuery(cachedTruncateQuery).executeUpdate().replaceWithVoid()
        ).await().indefinitely();
    }

    private synchronized void buildAndCacheQuery() {
        if (cachedTruncateQuery != null) return;


        sessionFactory
                .withSession(session ->
                        session.createNativeQuery(
                                "SELECT table_name " +
                                        "FROM information_schema.tables " +
                                        "WHERE table_schema='public'"
                        ).getResultList()
                )
                .onItem().invoke(tableNames -> {
                    if (tableNames.isEmpty()) {
                        cachedTruncateQuery = "";
                        return;
                    }

                    StringBuilder queryBuilder = new StringBuilder("TRUNCATE TABLE ");
                    boolean first = true;

                    for (Object tableName : tableNames) {
                        String name = tableName.toString();
                        if (!name.toLowerCase().contains("flyway") &&
                                !name.toLowerCase().contains("databasechangelog")) {

                            if (!first) queryBuilder.append(", ");
                            queryBuilder.append(name);
                            first = false;
                        }
                    }

                    if (!first) {
                        queryBuilder.append(" CASCADE;");
                        cachedTruncateQuery = queryBuilder.toString();
                    } else {
                        cachedTruncateQuery = "";
                    }
                }).await().indefinitely(); // Block only ONCE per test class initialization
    }
}