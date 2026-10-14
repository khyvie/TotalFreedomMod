package me.totalfreedom.totalfreedommod.sql.adapter;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.sql.ConnectionHandler;
import me.totalfreedom.totalfreedommod.sql.StatementHandler;

import java.sql.SQLException;

/**
 * Abstract base class for database adapters.
 * Each database type (SQLite, MySQL, PostgreSQL, etc.) has its own implementation
 * with database-specific SQL syntax and initialization.
 * 
 * Database-specific implementations handle:
 * - Table creation and migrations
 * - SQL dialect differences (INSERT IGNORE vs ON CONFLICT, etc.)
 * - Repository creation with database-specific queries
 */
public abstract class DatabaseAdapter
{
    protected final TotalFreedomMod plugin;
    protected final ConnectionHandler connectionHandler;
    protected final StatementHandler statementHandler;

    protected DatabaseAdapter(TotalFreedomMod plugin, ConnectionHandler connectionHandler, StatementHandler statementHandler)
    {
        this.plugin = plugin;
        this.connectionHandler = connectionHandler;
        this.statementHandler = statementHandler;
    }

    // ============================================
    // Lifecycle Methods
    // ============================================

    /**
     * Run database migrations (create tables, indexes, etc.)
     * Called during initialization.
     */
    public abstract void runMigrations() throws SQLException;

    /**
     * Initialize the database. Runs migrations.
     */
    public void initialize() throws SQLException
    {
        runMigrations();
    }

    /**
     * Close database connections and cleanup resources.
     */
    public void shutdown()
    {
        connectionHandler.closeConnection();
    }

    // ============================================
    // Repository Getters
    // ============================================

    /**
     * Get the admin repository for this database type.
     */
    public abstract AdminRepository getAdminRepository();

    /**
     * Get the ban repository for this database type.
     */
    public abstract BanRepository getBanRepository();

    /**
     * Get the permban repository for this database type.
     */
    public abstract PermbanRepository getPermbanRepository();

    /**
     * Get the strike repository for this database type.
     */
    public abstract StrikeRepository getStrikeRepository();

    /**
     * Get the Discord link repository for this database type.
     */
    public abstract DiscordLinkRepository getDiscordLinkRepository();

    /**
     * Get the punishment history repository for this database type.
     */
    public abstract PunishmentHistoryRepository getPunishmentHistoryRepository();

    // ============================================
    // SQL Dialect Methods (override for differences)
    // ============================================

    /**
     * Get the auto-increment syntax for primary keys.
     * SQLite: INTEGER PRIMARY KEY AUTOINCREMENT
     * MySQL/MariaDB: INT AUTO_INCREMENT
     * PostgreSQL: SERIAL
     * H2: INT AUTO_INCREMENT
     */
    public abstract String autoIncrementSyntax();

    /**
     * Get the primary key syntax.
     */
    public abstract String primaryKeySyntax();

    /**
     * Get the text/string type for this database.
     * SQLite: TEXT
     * MySQL/MariaDB: TEXT or VARCHAR
     * PostgreSQL: TEXT
     */
    public abstract String textType();

    /**
     * Get the timestamp/datetime type for this database.
     * SQLite: TEXT (stored as ISO string)
     * MySQL/MariaDB: DATETIME
     * PostgreSQL: TIMESTAMP
     */
    public abstract String timestampType();

    /**
     * Get the boolean type for this database.
     * SQLite: INTEGER (0/1)
     * MySQL/MariaDB: TINYINT(1)
     * PostgreSQL: BOOLEAN
     */
    public abstract String booleanType();

    /**
     * Get the INSERT IGNORE / INSERT OR IGNORE syntax prefix.
     * SQLite: INSERT OR IGNORE
     * MySQL/MariaDB: INSERT IGNORE
     * PostgreSQL: INSERT (use ON CONFLICT DO NOTHING suffix)
     */
    public abstract String insertIgnoreSyntax();

    /**
     * Quote an identifier (table name, column name) for this database.
     * SQLite: No quoting or double quotes
     * MySQL/MariaDB: `identifier` (backticks)
     * PostgreSQL: "identifier" (double quotes)
     */
    public abstract String quoteIdentifier(String identifier);

    /**
     * Get the current timestamp function.
     * SQLite: CURRENT_TIMESTAMP or datetime('now')
     * MySQL/MariaDB: NOW()
     * PostgreSQL: CURRENT_TIMESTAMP
     */
    public abstract String currentTimestamp();

    /**
     * Get the case-insensitive LIKE operator.
     * SQLite: LIKE (case-insensitive by default)
     * MySQL: LIKE (depends on collation)
     * PostgreSQL: ILIKE
     */
    public abstract String caseInsensitiveLike();
}
