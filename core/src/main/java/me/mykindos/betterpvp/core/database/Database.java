package me.mykindos.betterpvp.core.database;

import com.google.inject.Singleton;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.MariaDBDatabaseConnection;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.StatementValue;
import me.mykindos.betterpvp.core.utilities.UtilServer;

import javax.inject.Inject;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Singleton
public class Database {

    @Getter
    private final Core core;

    @Getter
    private final IDatabaseConnection connection;

    @Inject
    public Database(Core core) {
        this(core, new MariaDBDatabaseConnection(core.getConfig()));
    }

    protected Database(Core core, IDatabaseConnection connection) {
        this.core = core;
        this.connection = connection;
    }

    /**
     * @param statement The statement and values
     */
    public void executeUpdateAsync(Statement statement) {
        UtilServer.runTaskAsync(core, () -> executeUpdate(statement));
    }

    /**
     * @param statement The statement and values
     */
    public void executeUpdate(Statement statement) {
        Connection connection = getConnection().getDatabaseConnection();
        try {
            @Cleanup
            PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery());
            for (int i = 1; i <= statement.getValues().length; i++) {
                StatementValue<?> val = statement.getValues()[i - 1];
                preparedStatement.setObject(i, val.getValue(), val.getType());
            }
            preparedStatement.executeUpdate();

        } catch (SQLException ex) {
            log.error("Error executing update: {}", statement.getQuery(), ex);
        }
    }

    public void executeBatch(List<Statement> statements, boolean async) {
        executeBatch(statements, async, null);
    }

    public void executeBatch(List<Statement> statements, boolean async, Consumer<ResultSet> callback) {
        if (async) {
            UtilServer.runTaskAsync(core, () -> executeBatch(statements, callback));
        } else {
            executeBatch(statements, callback);
        }
    }

    private void executeBatch(List<Statement> statements, Consumer<ResultSet> callback) {
        Connection connection = getConnection().getDatabaseConnection();
        if (statements.isEmpty()) {
            return;
        }
        try {
            // Assume all statement queries are the same
            connection.setAutoCommit(false);
            @Cleanup
            PreparedStatement preparedStatement = connection.prepareStatement(statements.get(0).getQuery());
            for (Statement statement : statements) {
                for (int i = 1; i <= statement.getValues().length; i++) {
                    StatementValue<?> val = statement.getValues()[i - 1];
                    preparedStatement.setObject(i, val.getValue(), val.getType());
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();

            if (callback != null) {
                callback.accept(preparedStatement.getGeneratedKeys());
            }
        } catch (SQLException ex) {
            log.error("Error executing batch", ex);
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                log.error("Failed to rollback batch", rollbackException);
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                log.error("Failed to enable autocommit after batch", e);
            }
        }
    }

    /**
     * @param statement The statement and values
     */
    public CachedRowSet executeQuery(Statement statement) {
        Connection connection = getConnection().getDatabaseConnection();
        CachedRowSet rowset = null;

        try {
            RowSetFactory factory = RowSetProvider.newFactory();
            rowset = factory.createCachedRowSet();
            @Cleanup
            PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery());
            for (int i = 1; i <= statement.getValues().length; i++) {
                StatementValue<?> val = statement.getValues()[i - 1];
                preparedStatement.setObject(i, val.getValue(), val.getType());
            }
            rowset.populate(preparedStatement.executeQuery());

        } catch (SQLException ex) {
            log.error("Error executing query: {}", statement.getQuery(), ex);
        }

        return rowset;
    }

    @SneakyThrows
    public void executeProcedure(Statement statement, int fetchSize, Consumer<CachedRowSet> consumer) {
        Connection connection = getConnection().getDatabaseConnection();
        CachedRowSet result;

        try {
            RowSetFactory factory = RowSetProvider.newFactory();
            result = factory.createCachedRowSet();
            if (fetchSize != -1) result.setFetchSize(fetchSize);
            @Cleanup
            CallableStatement callable = connection.prepareCall(statement.getQuery());
            for (int i = 1; i <= statement.getValues().length; i++) {
                StatementValue<?> val = statement.getValues()[i - 1];
                callable.setObject(i, val.getValue(), val.getType());
            }
            callable.execute();
            result.populate(callable.getResultSet());
            consumer.accept(result);
            result.close();
        } catch (SQLException ex) {
            log.info("Error executing procedure: {}", statement.getQuery(), ex);
        }
    }

}