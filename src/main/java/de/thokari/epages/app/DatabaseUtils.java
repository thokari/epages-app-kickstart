package de.thokari.epages.app;

import io.vertx.core.Future;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.sql.SQLConnection;

public abstract class DatabaseUtils {

    public static Future<SQLConnection> withConnection(final AsyncSQLClient dbClient) {
        Future<SQLConnection> future = Future.<SQLConnection>future();
        dbClient.getConnection(future.completer());
        return future;
    }
}
