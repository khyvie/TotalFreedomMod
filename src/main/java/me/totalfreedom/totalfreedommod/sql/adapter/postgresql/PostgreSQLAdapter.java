package me.totalfreedom.totalfreedommod.sql.adapter.postgresql;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.sql.ConnectionHandler;
import me.totalfreedom.totalfreedommod.sql.StatementHandler;
import me.totalfreedom.totalfreedommod.sql.adapter.AdminRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.BanRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.DatabaseAdapter;
import me.totalfreedom.totalfreedommod.sql.adapter.DiscordLinkRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.PermbanRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.PunishmentHistoryRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.StrikeRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.sqlite.SQLitePunishmentHistoryRepository;
import me.totalfreedom.totalfreedommod.util.FLog;

import java.sql.SQLException;

/**
 * PostgreSQL-specific database adapter.
 * Uses PostgreSQL-specific SQL features like:
 * - SERIAL for auto-incrementing primary keys
 * - INSERT ... ON CONFLICT DO NOTHING for upsert operations
 * - Double quotes (") for identifier quoting
 * - TIMESTAMP for timestamp columns
 * - ILIKE for case-insensitive LIKE
 */
public class PostgreSQLAdapter extends DatabaseAdapter
{
    private PostgreSQLAdminRepository adminRepository;
    private PostgreSQLBanRepository banRepository;
    private PostgreSQLPermbanRepository permbanRepository;
    private PostgreSQLStrikeRepository strikeRepository;
    private PostgreSQLDiscordLinkRepository discordLinkRepository;
    private SQLitePunishmentHistoryRepository punishmentHistoryRepository;

    public PostgreSQLAdapter(TotalFreedomMod plugin, ConnectionHandler connectionHandler, StatementHandler statementHandler)
    {
        super(plugin, connectionHandler, statementHandler);
    }

    // ============================================
    // SQL Dialect Methods
    // ============================================

    @Override
    public String autoIncrementSyntax()
    {
        return "SERIAL";
    }

    @Override
    public String primaryKeySyntax()
    {
        return "PRIMARY KEY";
    }

    @Override
    public String textType()
    {
        return "TEXT";
    }

    @Override
    public String timestampType()
    {
        return "TIMESTAMP";
    }

    @Override
    public String booleanType()
    {
        return "BOOLEAN";
    }

    @Override
    public String insertIgnoreSyntax()
    {
        // PostgreSQL uses ON CONFLICT DO NOTHING instead of INSERT IGNORE
        return "INSERT";  // Base INSERT, ON CONFLICT added per-statement
    }

    @Override
    public String quoteIdentifier(String identifier)
    {
        return "\"" + identifier + "\"";
    }

    @Override
    public String currentTimestamp()
    {
        return "CURRENT_TIMESTAMP";
    }

    @Override
    public String caseInsensitiveLike()
    {
        return "ILIKE";
    }

    /**
     * PostgreSQL-specific ON CONFLICT DO NOTHING clause
     */
    public String onConflictDoNothing()
    {
        return "ON CONFLICT DO NOTHING";
    }

    // ============================================
    // Migration Methods
    // ============================================

    @Override
    public void runMigrations() throws SQLException
    {
        FLog.info("[PostgreSQL] Running database migrations...");

        createMigrationTable();
        createAdminsTable();
        createAdminIpsTable();
        createBansTable();
        createBanIpsTable();
        createPermbansTable();
        createPermbanIpsTable();
        createStrikesTable();
        createDiscordLinksTable();
        createPunishmentHistoryTable();
        createPunishmentHistoryIpsTable();

        FLog.info("[PostgreSQL] Database migrations complete.");
    }

    private void createMigrationTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "migrations" (
                "id" SERIAL PRIMARY KEY,
                "version" VARCHAR(50) NOT NULL UNIQUE,
                "applied_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createAdminsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "admins" (
                "id" SERIAL PRIMARY KEY,
                "uuid" VARCHAR(36) NOT NULL UNIQUE,
                "username" VARCHAR(16) NOT NULL,
                "rank" VARCHAR(32) NOT NULL,
                "active" BOOLEAN DEFAULT TRUE,
                "last_login" TIMESTAMP,
                "login_message" TEXT
            )
            """;
        statementHandler.executeUpdate(sql);

        // Create indexes separately (PostgreSQL style)
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_admins_username ON \"admins\"(\"username\")");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_admins_active ON \"admins\"(\"active\")");
    }

    private void createAdminIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "admin_ips" (
                "id" SERIAL PRIMARY KEY,
                "admin_id" INTEGER NOT NULL REFERENCES "admins"("id") ON DELETE CASCADE,
                "ip" VARCHAR(45) NOT NULL,
                UNIQUE ("admin_id", "ip")
            )
            """;
        statementHandler.executeUpdate(sql);
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_admin_ips_ip ON \"admin_ips\"(\"ip\")");
    }

    private void createBansTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "bans" (
                "id" SERIAL PRIMARY KEY,
                "uuid" VARCHAR(36),
                "username" VARCHAR(16),
                "banned_by" VARCHAR(16),
                "banned_by_uuid" VARCHAR(36),
                "reason" TEXT,
                "expire_at" TIMESTAMP
            )
            """;
        statementHandler.executeUpdate(sql);

        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bans_uuid ON \"bans\"(\"uuid\")");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bans_username ON \"bans\"(\"username\")");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bans_expire ON \"bans\"(\"expire_at\")");
    }

    private void createBanIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "ban_ips" (
                "id" SERIAL PRIMARY KEY,
                "ban_id" INTEGER NOT NULL REFERENCES "bans"("id") ON DELETE CASCADE,
                "ip" VARCHAR(45) NOT NULL,
                UNIQUE ("ban_id", "ip")
            )
            """;
        statementHandler.executeUpdate(sql);
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ban_ips_ip ON \"ban_ips\"(\"ip\")");
    }

    private void createPermbansTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "permbans" (
                "id" SERIAL PRIMARY KEY,
                "uuid" VARCHAR(36),
                "username" VARCHAR(16),
                "reason" TEXT
            )
            """;
        statementHandler.executeUpdate(sql);

        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_permbans_uuid ON \"permbans\"(\"uuid\")");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_permbans_username ON \"permbans\"(\"username\")");
    }

    private void createPermbanIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "permban_ips" (
                "id" SERIAL PRIMARY KEY,
                "permban_id" INTEGER NOT NULL REFERENCES "permbans"("id") ON DELETE CASCADE,
                "ip" VARCHAR(45) NOT NULL,
                UNIQUE ("permban_id", "ip")
            )
            """;
        statementHandler.executeUpdate(sql);
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_permban_ips_ip ON \"permban_ips\"(\"ip\")");
    }

    private void createStrikesTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "strikes" (
                "ip" VARCHAR(45) PRIMARY KEY,
                "strike_count" INTEGER NOT NULL DEFAULT 0,
                "last_strike_unix" BIGINT NOT NULL DEFAULT 0,
                "last_username" VARCHAR(16),
                "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createDiscordLinksTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "discord_links" (
                "id" SERIAL PRIMARY KEY,
                "admin_uuid" VARCHAR(36) NOT NULL UNIQUE,
                "discord_user_id" VARCHAR(32) NOT NULL UNIQUE,
                "linked_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createPunishmentHistoryTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "punishment_history" (
                "id" BIGSERIAL PRIMARY KEY,
                "target_uuid" VARCHAR(36),
                "target_name" VARCHAR(255),
                "actor_uuid" VARCHAR(36),
                "actor_name" VARCHAR(255),
                "punishment_type" VARCHAR(64) NOT NULL,
                "reason" TEXT,
                "applied_at" BIGINT NOT NULL,
                "expires_at" BIGINT,
                "source" VARCHAR(64)
            )
            """;
        statementHandler.executeUpdate(sql);
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishment_history_target_uuid ON \"punishment_history\"(\"target_uuid\", \"applied_at\" DESC, \"id\" DESC)");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishment_history_target_name ON \"punishment_history\"(\"target_name\")");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishment_history_applied_at ON \"punishment_history\"(\"applied_at\" DESC, \"id\" DESC)");
    }

    private void createPunishmentHistoryIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "punishment_history_ips" (
                "id" BIGSERIAL PRIMARY KEY,
                "history_id" BIGINT NOT NULL REFERENCES "punishment_history"("id") ON DELETE CASCADE,
                "ip" VARCHAR(45) NOT NULL,
                UNIQUE ("history_id", "ip")
            )
            """;
        statementHandler.executeUpdate(sql);
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishment_history_ips_ip ON \"punishment_history_ips\"(\"ip\")");
    }

    // ============================================
    // Repository Getters
    // ============================================

    @Override
    public AdminRepository getAdminRepository()
    {
        if (adminRepository == null)
        {
            adminRepository = new PostgreSQLAdminRepository(plugin, statementHandler);
        }
        return adminRepository;
    }

    @Override
    public BanRepository getBanRepository()
    {
        if (banRepository == null)
        {
            banRepository = new PostgreSQLBanRepository(plugin, statementHandler);
        }
        return banRepository;
    }

    @Override
    public PermbanRepository getPermbanRepository()
    {
        if (permbanRepository == null)
        {
            permbanRepository = new PostgreSQLPermbanRepository(plugin, statementHandler);
        }
        return permbanRepository;
    }

    @Override
    public StrikeRepository getStrikeRepository()
    {
        if (strikeRepository == null)
        {
            strikeRepository = new PostgreSQLStrikeRepository(plugin, statementHandler);
        }
        return strikeRepository;
    }

    @Override
    public DiscordLinkRepository getDiscordLinkRepository()
    {
        if (discordLinkRepository == null)
        {
            discordLinkRepository = new PostgreSQLDiscordLinkRepository(plugin, statementHandler);
        }
        return discordLinkRepository;
    }

    @Override
    public PunishmentHistoryRepository getPunishmentHistoryRepository()
    {
        if (punishmentHistoryRepository == null)
        {
            punishmentHistoryRepository = new SQLitePunishmentHistoryRepository(statementHandler);
        }
        return punishmentHistoryRepository;
    }
}
