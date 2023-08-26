package de.intelligence.antiautoupdate.persistence;

import java.io.Closeable;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JDBCSQLiteHelper implements Closeable {

    private static final String JDBC_TEMPLATE = "jdbc:sqlite:%s";

    protected final File file;
    protected final String tableName;

    private Connection conn;

    public JDBCSQLiteHelper(File file, String tableName) {
        this.file = file;
        this.tableName = tableName;

        this.initDriver();
        this.initConnection();
    }

    private void initDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public final void initConnection() {
        try {
            this.conn = DriverManager.getConnection(String.format(JDBC_TEMPLATE, this.file));
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private <E> E prepareStatement(String statement, ExceptionFunction<PreparedStatement, E, ? super Exception> func, Object... vars) {
        try (PreparedStatement s = this.conn.prepareStatement(statement)) {
            for (var i = 0; i < vars.length; i++) {
                s.setObject(i + 1, vars[i]);
            }
            return func.apply(s);
        } catch (Exception sqlExc) {
            throw new RuntimeException("Could not process statement:" + sqlExc.getMessage() + statement);
        }
    }

    protected final <E> E executeQuery(String statement, ExceptionFunction<ResultSet, E, SQLException> func, Object... vars) {
        return this.prepareStatement(statement, s -> func.apply(s.executeQuery()), vars);
    }

    protected final void executeUpdate(String statement, Object... vars) {
        this.prepareStatement(statement, PreparedStatement::executeUpdate, vars);
    }

    protected final String prepareTable(String query) {
        return String.format(query, this.tableName);
    }

    @Override
    public final void close() {
        if (this.conn != null) {
            try {
                this.conn.close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
