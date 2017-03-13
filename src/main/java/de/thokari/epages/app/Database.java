package de.thokari.epages.app;

import de.thokari.epages.app.model.DatabaseConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLConnection;

public class Database {

    private static AsyncSQLClient dbClient = null;

    public static void init(Vertx vertx, DatabaseConfig databaseCfg) {
        if (dbClient == null) {
            dbClient = PostgreSQLClient.createShared(vertx, databaseCfg.toJsonObject());
        }
    }

    public static Future<SQLConnection> withConnection() {
        if (dbClient == null) {
            throw new RuntimeException("Database client not initialized");
        }
        Future<SQLConnection> future = Future.<SQLConnection>future();
        dbClient.getConnection(future.completer());
        return future;
    }
}
