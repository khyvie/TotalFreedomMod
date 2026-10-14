package me.totalfreedom.totalfreedommod.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class StatementHandler
{
    private final ConnectionHandler connectionHandler;

    public StatementHandler(ConnectionHandler connectionHandler)
    {
        this.connectionHandler = connectionHandler;
    }

    private Connection getConnection() throws SQLException
    {
        try
        {
            return connectionHandler.getConnection().join();
        }
        catch (Exception e)
        {
            throw new SQLException("Failed to obtain connection", e);
        }
    }

    public PreparedStatement prepareStatement(String sql, Object... params) throws SQLException
    {
        PreparedStatement statement = getConnection().prepareStatement(sql);
        setParameters(statement, params);
        return statement;
    }

    public ResultSet executeQuery(String sql, Object... params) throws SQLException
    {
        PreparedStatement statement = prepareStatement(sql, params);
        return statement.executeQuery();
    }

    public int executeUpdate(String sql, Object... params) throws SQLException
    {
        try (PreparedStatement statement = prepareStatement(sql, params))
        {
            return statement.executeUpdate();
        }
    }

    public long executeInsert(String sql, Object... params) throws SQLException
    {
        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            setParameters(statement, params);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys())
            {
                if (keys.next())
                {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Insert did not return a generated key");
    }

    public CompletableFuture<ResultSet> executeQueryAsync(String sql, Object... params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                return executeQuery(sql, params);
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Integer> executeUpdateAsync(String sql, Object... params)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                return executeUpdate(sql, params);
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    private void setParameters(PreparedStatement statement, Object... params) throws SQLException
    {
        int index = 1;
        for (Object param : params)
        {
            index = setParameter(statement, index, param);
        }
    }

    private int setParameter(PreparedStatement statement, int index, Object param) throws SQLException
    {
        if (param == null)
        {
            statement.setObject(index, null);
            return index + 1;
        }

        if (param instanceof String[])
        {
            for (String s : (String[]) param)
            {
                statement.setString(index++, s);
            }
            return index;
        }

        if (param instanceof Collection<?>)
        {
            for (Object item : (Collection<?>) param)
            {
                if (item instanceof String)
                {
                    statement.setString(index++, (String) item);
                }
                else
                {
                    statement.setObject(index++, item);
                }
            }
            return index;
        }

        if (param instanceof String)
        {
            statement.setString(index, (String) param);
        }
        else if (param instanceof Integer || param.getClass() == int.class)
        {
            statement.setInt(index, (Integer) param);
        }
        else if (param instanceof Long || param.getClass() == long.class)
        {
            statement.setLong(index, (Long) param);
        }
        else if (param instanceof Double || param.getClass() == double.class)
        {
            statement.setDouble(index, (Double) param);
        }
        else if (param instanceof Float || param.getClass() == float.class)
        {
            statement.setFloat(index, (Float) param);
        }
        else if (param instanceof Boolean || param.getClass() == boolean.class)
        {
            statement.setBoolean(index, (Boolean) param);
        }
        else if (param instanceof Byte || param.getClass() == byte.class)
        {
            statement.setByte(index, (Byte) param);
        }
        else if (param instanceof Short || param.getClass() == short.class)
        {
            statement.setShort(index, (Short) param);
        }
        else
        {
            statement.setObject(index, param);
        }

        return index + 1;
    }

    public void close()
    {
        connectionHandler.shutdown();
    }
}