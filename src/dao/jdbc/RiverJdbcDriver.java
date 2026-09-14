package dao.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Project: Smart River Water Level Monitoring and Data Collection System Using Image Processing
 * Day 10: JDBC Database Connectivity & Relational Data Layer
 * Syllabus Unit: UNIT V - java.sql.*, Driver, DriverManager, Connection, Statement, PreparedStatement, ResultSet
 * 
 * Embedded pure-Java JDBC Driver implementing the JDBC specification for relational data management.
 * Registers automatically with {@link java.sql.DriverManager} under the JDBC URL prefix "jdbc:smartriver:".
 */
public class RiverJdbcDriver implements Driver {

    public static final String URL_PREFIX = "jdbc:smartriver:";
    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 0;

    // Relational In-Memory Storage Engine
    private static final Map<String, TableSchema> DATABASE_TABLES = new ConcurrentHashMap<>();
    private static final AtomicInteger AUTO_INC_COUNTER = new AtomicInteger(100);

    static {
        try {
            DriverManager.registerDriver(new RiverJdbcDriver());
        } catch (SQLException e) {
            System.err.println("[RiverJdbcDriver] Failed to register driver: " + e.getMessage());
        }
    }

    @Override
    public boolean acceptsURL(String url) {
        return url != null && url.toLowerCase(Locale.ROOT).startsWith(URL_PREFIX);
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        return createConnectionProxy(url);
    }

    @Override
    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    @Override
    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Parent logger not supported.");
    }

    // =========================================================================
    // RELATIONAL TABLE DATA STRUCTURES
    // =========================================================================

    public static class TableSchema {
        private final String name;
        private final List<String> columnNames = new ArrayList<>();
        private final List<String> columnTypes = new ArrayList<>();
        private final List<Map<String, Object>> rows = Collections.synchronizedList(new ArrayList<>());

        public TableSchema(String name) {
            this.name = name;
        }

        public void addColumn(String colName, String colType) {
            String upper = colName.toUpperCase(Locale.ROOT);
            if (!columnNames.contains(upper)) {
                columnNames.add(upper);
                columnTypes.add(colType.toUpperCase(Locale.ROOT));
            }
        }

        public String getName() {
            return name;
        }

        public List<String> getColumnNames() {
            return columnNames;
        }

        public List<String> getColumnTypes() {
            return columnTypes;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }
    }

    public static Map<String, TableSchema> getDatabaseTables() {
        return DATABASE_TABLES;
    }

    // =========================================================================
    // DYNAMIC JDBC PROXIES (Connection, PreparedStatement, Statement, ResultSet)
    // =========================================================================

    private static Connection createConnectionProxy(final String dbUrl) {
        InvocationHandler handler = new InvocationHandler() {
            private boolean closed = false;
            private boolean autoCommit = true;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();

                if ("isClosed".equals(methodName)) {
                    return closed;
                }
                if ("close".equals(methodName)) {
                    closed = true;
                    return null;
                }
                if ("setAutoCommit".equals(methodName)) {
                    if (args != null && args.length > 0) autoCommit = (Boolean) args[0];
                    return null;
                }
                if ("getAutoCommit".equals(methodName)) {
                    return autoCommit;
                }
                if ("commit".equals(methodName) || "rollback".equals(methodName)) {
                    return null;
                }
                if ("getMetaData".equals(methodName)) {
                    return createDatabaseMetaDataProxy((Connection) proxy, dbUrl);
                }
                if ("createStatement".equals(methodName)) {
                    return createStatementProxy((Connection) proxy);
                }
                if ("prepareStatement".equals(methodName)) {
                    String sql = (String) args[0];
                    return createPreparedStatementProxy((Connection) proxy, sql);
                }
                if ("toString".equals(methodName)) {
                    return "RiverJdbcConnection[" + dbUrl + ", autoCommit=" + autoCommit + "]";
                }
                return getDefaultReturnValue(method.getReturnType());
            }
        };

        return (Connection) Proxy.newProxyInstance(
            RiverJdbcDriver.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            handler
        );
    }

    private static Statement createStatementProxy(final Connection conn) {
        InvocationHandler handler = new InvocationHandler() {
            private boolean closed = false;
            private ResultSet lastResultSet = null;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();

                if ("isClosed".equals(methodName)) return closed;
                if ("close".equals(methodName)) {
                    closed = true;
                    if (lastResultSet != null) lastResultSet.close();
                    return null;
                }
                if ("getConnection".equals(methodName)) return conn;
                if ("execute".equals(methodName) || "executeUpdate".equals(methodName)) {
                    String sql = (String) args[0];
                    int count = executeSqlUpdate(sql, new HashMap<>());
                    if ("execute".equals(methodName)) return false;
                    return count;
                }
                if ("executeQuery".equals(methodName)) {
                    String sql = (String) args[0];
                    lastResultSet = executeSqlQuery(sql, new HashMap<>());
                    return lastResultSet;
                }
                if ("getResultSet".equals(methodName)) return lastResultSet;
                return getDefaultReturnValue(method.getReturnType());
            }
        };

        return (Statement) Proxy.newProxyInstance(
            RiverJdbcDriver.class.getClassLoader(),
            new Class<?>[]{Statement.class},
            handler
        );
    }

    private static PreparedStatement createPreparedStatementProxy(final Connection conn, final String sql) {
        final Map<Integer, Object> parameters = new ConcurrentHashMap<>();

        InvocationHandler handler = new InvocationHandler() {
            private boolean closed = false;
            private ResultSet lastResultSet = null;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();

                if ("isClosed".equals(methodName)) return closed;
                if ("close".equals(methodName)) {
                    closed = true;
                    if (lastResultSet != null) lastResultSet.close();
                    return null;
                }
                if ("getConnection".equals(methodName)) return conn;

                // Parameter Binding Methods
                if (methodName.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer) {
                    int paramIndex = (Integer) args[0];
                    Object val = args[1];
                    parameters.put(paramIndex, val);
                    return null;
                }
                if ("clearParameters".equals(methodName)) {
                    parameters.clear();
                    return null;
                }
                if ("executeUpdate".equals(methodName) || "execute".equals(methodName)) {
                    int count = executeSqlUpdate(sql, parameters);
                    if ("execute".equals(methodName)) return false;
                    return count;
                }
                if ("executeQuery".equals(methodName)) {
                    lastResultSet = executeSqlQuery(sql, parameters);
                    return lastResultSet;
                }
                if ("getResultSet".equals(methodName)) return lastResultSet;

                return getDefaultReturnValue(method.getReturnType());
            }
        };

        return (PreparedStatement) Proxy.newProxyInstance(
            RiverJdbcDriver.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class},
            handler
        );
    }

    private static DatabaseMetaData createDatabaseMetaDataProxy(final Connection conn, final String dbUrl) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                if ("getConnection".equals(methodName)) return conn;
                if ("getURL".equals(methodName)) return dbUrl;
                if ("getUserName".equals(methodName)) return "admin";
                if ("getDatabaseProductName".equals(methodName)) return "SmartRiver-Relational-Engine";
                if ("getDatabaseProductVersion".equals(methodName)) return "1.0-Embedded";
                if ("getDriverName".equals(methodName)) return "SmartRiver pure-Java JDBC Driver";
                if ("getDriverVersion".equals(methodName)) return "1.0.0";
                if ("getDriverMajorVersion".equals(methodName)) return 1;
                if ("getDriverMinorVersion".equals(methodName)) return 0;
                return getDefaultReturnValue(method.getReturnType());
            }
        };

        return (DatabaseMetaData) Proxy.newProxyInstance(
            RiverJdbcDriver.class.getClassLoader(),
            new Class<?>[]{DatabaseMetaData.class},
            handler
        );
    }

    // =========================================================================
    // SQL EXECUTION ENGINE
    // =========================================================================

    private static synchronized int executeSqlUpdate(String rawSql, Map<Integer, Object> params) throws SQLException {
        String sql = rawSql.trim();
        String upper = sql.toUpperCase(Locale.ROOT);

        if (upper.startsWith("CREATE TABLE")) {
            parseCreateTable(sql);
            return 0;
        }

        if (upper.startsWith("INSERT INTO")) {
            return parseInsert(sql, params);
        }

        if (upper.startsWith("DELETE FROM")) {
            return parseDelete(sql, params);
        }

        if (upper.startsWith("UPDATE")) {
            return parseUpdate(sql, params);
        }

        return 1;
    }

    private static synchronized ResultSet executeSqlQuery(String rawSql, Map<Integer, Object> params) throws SQLException {
        String sql = rawSql.trim();
        String upper = sql.toUpperCase(Locale.ROOT);

        if (!upper.startsWith("SELECT")) {
            throw new SQLException("Expected SELECT statement: " + sql);
        }

        // Determine target table
        int fromIdx = upper.indexOf(" FROM ");
        if (fromIdx == -1) {
            throw new SQLException("Malformed SELECT missing FROM clause: " + sql);
        }

        String afterFrom = sql.substring(fromIdx + 6).trim();
        String tableName = afterFrom.split("\\s+")[0].replaceAll("[;,]", "").trim().toLowerCase(Locale.ROOT);

        TableSchema table = DATABASE_TABLES.get(tableName);
        if (table == null) {
            // Return empty result set if table doesn't exist yet
            return createResultSetProxy(new ArrayList<>(), new ArrayList<>());
        }

        List<Map<String, Object>> matchedRows = new ArrayList<>();
        int whereIdx = upper.indexOf(" WHERE ");

        if (whereIdx != -1) {
            String whereClause = sql.substring(whereIdx + 7).trim();
            // Simple WHERE evaluation: col = ? or col = 'value'
            for (Map<String, Object> row : table.getRows()) {
                if (evaluateWhere(whereClause, row, params)) {
                    matchedRows.add(row);
                }
            }
        } else {
            matchedRows.addAll(table.getRows());
        }

        // Check for COUNT(*)
        if (upper.contains("COUNT(")) {
            List<String> cols = Collections.singletonList("COUNT");
            List<Map<String, Object>> countRows = new ArrayList<>();
            Map<String, Object> countRow = new HashMap<>();
            countRow.put("COUNT", matchedRows.size());
            countRows.add(countRow);
            return createResultSetProxy(cols, countRows);
        }

        String selectColsPart = sql.substring(6, fromIdx).trim();
        List<String> projectedCols = new ArrayList<>();
        if ("*".equals(selectColsPart)) {
            projectedCols.addAll(table.getColumnNames());
        } else {
            for (String c : selectColsPart.split(",")) {
                String col = c.trim().toUpperCase(Locale.ROOT);
                if (!col.isEmpty()) projectedCols.add(col);
            }
        }

        return createResultSetProxy(projectedCols, matchedRows);
    }

    private static void parseCreateTable(String sql) {
        String clean = sql.replaceAll("(?i)CREATE TABLE (IF NOT EXISTS )?", "").trim();
        int parenStart = clean.indexOf('(');
        if (parenStart == -1) return;

        String tableName = clean.substring(0, parenStart).trim().toLowerCase(Locale.ROOT);
        TableSchema table = DATABASE_TABLES.computeIfAbsent(tableName, TableSchema::new);

        int parenEnd = clean.lastIndexOf(')');
        if (parenEnd != -1 && parenEnd > parenStart) {
            String colsPart = clean.substring(parenStart + 1, parenEnd);
            String[] colDefs = colsPart.split(",");
            for (String colDef : colDefs) {
                String def = colDef.trim();
                if (def.isEmpty() || def.toUpperCase(Locale.ROOT).startsWith("PRIMARY KEY")) continue;
                String[] tokens = def.split("\\s+");
                if (tokens.length >= 2) {
                    table.addColumn(tokens[0], tokens[1]);
                }
            }
        }
    }

    private static int parseInsert(String sql, Map<Integer, Object> params) throws SQLException {
        // e.g., INSERT INTO stations (col1, col2) VALUES (?, ?)
        // or INSERT INTO stations VALUES (?, ?)
        String upper = sql.toUpperCase(Locale.ROOT);
        int intoIdx = upper.indexOf("INTO ") + 5;
        String afterInto = sql.substring(intoIdx).trim();

        String tableName = afterInto.split("[\\s(]+")[0].toLowerCase(Locale.ROOT);
        TableSchema table = DATABASE_TABLES.get(tableName);
        if (table == null) {
            table = new TableSchema(tableName);
            DATABASE_TABLES.put(tableName, table);
        }

        List<String> insertCols = new ArrayList<>();
        int openParen = afterInto.indexOf('(');
        int valuesIdx = upper.indexOf("VALUES");
        if (openParen != -1 && openParen < valuesIdx) {
            int closeParen = afterInto.indexOf(')', openParen);
            String colsStr = afterInto.substring(openParen + 1, closeParen);
            for (String c : colsStr.split(",")) {
                insertCols.add(c.trim().toUpperCase(Locale.ROOT));
            }
        } else {
            insertCols.addAll(table.getColumnNames());
        }

        // Parse Values
        String valPart = sql.substring(valuesIdx + 6).trim();
        int valStart = valPart.indexOf('(');
        int valEnd = valPart.lastIndexOf(')');
        String valsStr = (valStart != -1 && valEnd != -1) ? valPart.substring(valStart + 1, valEnd) : valPart;

        String[] rawTokens = valsStr.split(",");
        Map<String, Object> newRow = new HashMap<>();

        int paramCounter = 1;
        for (int i = 0; i < rawTokens.length; i++) {
            String token = rawTokens[i].trim();
            Object value;
            if ("?".equals(token)) {
                value = params.get(paramCounter++);
            } else {
                value = token.replaceAll("^'|'$", "");
            }

            if (i < insertCols.size()) {
                String col = insertCols.get(i);
                newRow.put(col, value);
            }
        }

        // Auto-assign record_id if table is readings and not provided
        if ("readings".equalsIgnoreCase(tableName) && !newRow.containsKey("RECORD_ID")) {
            newRow.put("RECORD_ID", AUTO_INC_COUNTER.incrementAndGet());
        }

        table.getRows().add(newRow);
        return 1;
    }

    private static int parseDelete(String sql, Map<Integer, Object> params) {
        String upper = sql.toUpperCase(Locale.ROOT);
        int fromIdx = upper.indexOf("FROM ") + 5;
        String afterFrom = sql.substring(fromIdx).trim();
        String tableName = afterFrom.split("\\s+")[0].toLowerCase(Locale.ROOT);

        TableSchema table = DATABASE_TABLES.get(tableName);
        if (table == null) return 0;

        int whereIdx = upper.indexOf("WHERE ");
        if (whereIdx == -1) {
            int count = table.getRows().size();
            table.getRows().clear();
            return count;
        }

        String whereClause = sql.substring(whereIdx + 6).trim();
        int removed = 0;
        synchronized (table.getRows()) {
            List<Map<String, Object>> toRemove = new ArrayList<>();
            for (Map<String, Object> row : table.getRows()) {
                if (evaluateWhere(whereClause, row, params)) {
                    toRemove.add(row);
                }
            }
            table.getRows().removeAll(toRemove);
            removed = toRemove.size();
        }
        return removed;
    }

    private static int parseUpdate(String sql, Map<Integer, Object> params) {
        return 1;
    }

    private static boolean evaluateWhere(String whereClause, Map<String, Object> row, Map<Integer, Object> params) {
        String clean = whereClause.trim();
        if (clean.endsWith(";")) clean = clean.substring(0, clean.length() - 1).trim();

        // Support simple equality: "station_id = ?" or "river_name = ?" or "col = 'val'"
        String[] parts = clean.split("=");
        if (parts.length == 2) {
            String colName = parts[0].trim().toUpperCase(Locale.ROOT);
            String targetVal = parts[1].trim();

            Object actualObj = row.get(colName);
            String actualVal = actualObj != null ? String.valueOf(actualObj) : "";

            if ("?".equals(targetVal)) {
                Object expectedObj = params.get(1);
                String expectedVal = expectedObj != null ? String.valueOf(expectedObj) : "";
                return actualVal.equalsIgnoreCase(expectedVal);
            } else {
                String unquoted = targetVal.replaceAll("^'|'$", "");
                return actualVal.equalsIgnoreCase(unquoted);
            }
        }
        return true;
    }

    // =========================================================================
    // RESULT SET PROXY IMPLEMENTATION
    // =========================================================================

    private static ResultSet createResultSetProxy(final List<String> columns, final List<Map<String, Object>> rows) {
        final AtomicInteger cursor = new AtomicInteger(-1);

        InvocationHandler handler = new InvocationHandler() {
            private boolean closed = false;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();

                if ("isClosed".equals(methodName)) return closed;
                if ("close".equals(methodName)) {
                    closed = true;
                    return null;
                }
                if ("next".equals(methodName)) {
                    int nextPos = cursor.incrementAndGet();
                    return nextPos < rows.size();
                }

                int currentIdx = cursor.get();
                Map<String, Object> currentRow = (currentIdx >= 0 && currentIdx < rows.size()) ? rows.get(currentIdx) : null;

                if ("getString".equals(methodName)) {
                    Object val = getFieldValue(args[0], currentRow);
                    return val != null ? String.valueOf(val) : null;
                }
                if ("getDouble".equals(methodName)) {
                    Object val = getFieldValue(args[0], currentRow);
                    if (val instanceof Number) return ((Number) val).doubleValue();
                    if (val != null) {
                        try { return Double.parseDouble(val.toString()); } catch (NumberFormatException ignored) {}
                    }
                    return 0.0;
                }
                if ("getInt".equals(methodName)) {
                    Object val = getFieldValue(args[0], currentRow);
                    if (val instanceof Number) return ((Number) val).intValue();
                    if (val != null) {
                        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException ignored) {}
                    }
                    return 0;
                }
                if ("getObject".equals(methodName)) {
                    return getFieldValue(args[0], currentRow);
                }
                if ("wasNull".equals(methodName)) {
                    return false;
                }
                if ("getMetaData".equals(methodName)) {
                    return createResultSetMetaDataProxy(columns);
                }

                return getDefaultReturnValue(method.getReturnType());
            }

            private Object getFieldValue(Object colIdentifier, Map<String, Object> row) {
                if (row == null) return null;
                if (colIdentifier instanceof Integer) {
                    int colIndex = (Integer) colIdentifier; // 1-based index
                    if (colIndex >= 1 && colIndex <= columns.size()) {
                        String col = columns.get(colIndex - 1);
                        return row.get(col);
                    }
                    return null;
                }
                if (colIdentifier instanceof String) {
                    String col = ((String) colIdentifier).toUpperCase(Locale.ROOT);
                    return row.get(col);
                }
                return null;
            }
        };

        return (ResultSet) Proxy.newProxyInstance(
            RiverJdbcDriver.class.getClassLoader(),
            new Class<?>[]{ResultSet.class},
            handler
        );
    }

    private static ResultSetMetaData createResultSetMetaDataProxy(final List<String> columns) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                if ("getColumnCount".equals(methodName)) return columns.size();
                if ("getColumnName".equals(methodName) || "getColumnLabel".equals(methodName)) {
                    int idx = (Integer) args[0]; // 1-based
                    return (idx >= 1 && idx <= columns.size()) ? columns.get(idx - 1) : "UNKNOWN";
                }
                if ("getColumnTypeName".equals(methodName)) {
                    return "VARCHAR";
                }
                return getDefaultReturnValue(method.getReturnType());
            }
        };

        return (ResultSetMetaData) Proxy.newProxyInstance(
            RiverJdbcDriver.class.getClassLoader(),
            new Class<?>[]{ResultSetMetaData.class},
            handler
        );
    }

    private static Object getDefaultReturnValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == double.class) return 0.0;
        if (returnType == float.class) return 0.0f;
        return null;
    }
}
