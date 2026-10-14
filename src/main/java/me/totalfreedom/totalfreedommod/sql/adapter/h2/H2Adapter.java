package me.totalfreedom.totalfreedommod.sql.adapter.h2;

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
import me.totalfreedom.totalfreedommod.sql.adapter.mysql.MySQLAdminRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.mysql.MySQLBanRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.mysql.MySQLDiscordLinkRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.mysql.MySQLPermbanRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.mysql.MySQLStrikeRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.sqlite.SQLitePunishmentHistoryRepository;
import me.totalfreedom.totalfreedommod.util.FLog;

import java.sql.SQLException;

/**
 * H2 database adapter.
 * H2 is mostly MySQL-compatible, so we reuse MySQL repositories.
 * Key differences:
 * - Uses IDENTITY instead of AUTO_INCREMENT (but AUTO_INCREMENT also works)
 * - Uses MERGE for upsert operations (but INSERT IGNORE can be simulated)
 * - Double quotes (") for identifier quoting by default (can be configured)
 */
public class H2Adapter extends DatabaseAdapter
{
    private MySQLAdminRepository adminRepository;
    private MySQLBanRepository banRepository;
    private MySQLPermbanRepository permbanRepository;
    private MySQLStrikeRepository strikeRepository;
    private MySQLDiscordLinkRepository discordLinkRepository;
    private SQLitePunishmentHistoryRepository punishmentHistoryRepository;

    public H2Adapter(TotalFreedomMod plugin, ConnectionHandler connectionHandler, StatementHandler statementHandler)
    {
        super(plugin, connectionHandler, statementHandler);
    }

    // ============================================
    // SQL Dialect Methods
    // ============================================

    @Override
    public String autoIncrementSyntax()
    {
        // H2 supports AUTO_INCREMENT for MySQL compatibility
        return "INT AUTO_INCREMENT";
    }

    @Override
    public String primaryKeySyntax()
    {
        return "PRIMARY KEY";
    }

    @Override
    public String textType()
    {
        return "VARCHAR(65535)";  // H2 doesn't have TEXT by default, use large VARCHAR
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
        // H2: MERGE INTO for upsert, but we'll use a workaround
        return "INSERT";
    }

    @Override
    public String quoteIdentifier(String identifier)
    {
        return "\"" + identifier + "\"";
    }

    @Override
    public String currentTimestamp()
    {
        return "CURRENT_TIMESTAMP()";
    }

    @Override
    public String caseInsensitiveLike()
    {
        return "ILIKE";  // H2 supports ILIKE
    }

    // ============================================
    // Migration Methods
    // ============================================

    @Override
    public void runMigrations() throws SQLException
    {
        FLog.info("[H2] Running database migrations...");

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
        createPunishmentHistoryTable();
        createPunishmentHistoryIpsTable();

        FLog.info("[H2] Database migrations complete.");
    }

    private void createMigrationTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "migrations" (
                "id" INT AUTO_INCREMENT PRIMARY KEY,
                "version" VARCHAR(50) NOT NULL UNIQUE,
                "applied_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
            )
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createAdminsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "admins" (
                "id" INT AUTO_INCREMENT PRIMARY KEY,
                "uuid" VARCHAR(36) NOT NULL UNIQUE,
                "username" VARCHAR(16) NOT NULL,
                "rank" VARCHAR(32) NOT NULL,
                "active" BOOLEAN DEFAULT TRUE,
                "last_login" TIMESTAMP,
                "login_message" VARCHAR(65535)
            )
            """;
        statementHandler.executeUpdate(sql);

        // Create indexes
        try { statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_admins_username ON \"admins\"(\"username\")"); } catch (SQLException ignored) {}
        try { statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_admins_active ON \"admins\"(\"active\")"); } catch (SQLException ignored) {}
    }

    private void createAdminIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "admin_ips" (
                "id" INT AUTO_INCREMENT PRIMARY KEY,
                "admin_id" INT NOT NULL,
                "ip" VARCHAR(45) NOT NULL,
                UNIQUE ("admin_id", "ip"),
                FOREIGN KEY ("admin_id") REFERENCES "admins"("id") ON DELETE CASCADE
            )
            """;
        statementHandler.executeUpdate(sql);
        try { statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_admin_ips_ip ON \"admin_ips\"(\"ip\")"); } catch (SQLException ignored) {}
    }

    private void createBansTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "bans" (
                "id" INT AUTO_INCREMENT PRIMARY KEY,
                "uuid" VARCHAR(36),
                "username" VARCHAR(16),
                "banned_by" VARCHAR(16),
                "banned_by_uuid" VARCHAR(36),
                "reason" VARCHAR(65535),
                "expire_at" TIMESTAMP
            )
            """;
        statementHandler.executeUpdate(sql);

        try { statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bans_uuid ON \"bans\"(\"uuid\")"); } catch (SQLException ignored) {}
        try { statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bans_username ON \"bans\"(\"username\")"); } catch (SQLException ignored) {}
        try { statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bans_expire ON \"bans\"(\"expire_at\")"); } catch (SQLException ignored) {}
    }

    private void createBanIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "ban_ips" (
                "id" INT AUTO_INCREMENT PRIMARY KEY,
                "ban_id" INT NOT NULL,
                "ip" VARCHAR(45) NOT NULL,
                UNIQUE ("ban_id", "ip"),
                FOREIGN KEY ("ban_id") REFERENCES "bans"("id") ON DELETE CASCADE
            )
            """;
        statementHandler.executeUpdate(sql);
        try { statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ban_ips_ip ON \"ban_ips\"(\"ip\")"); } catch (SQLException ignored) {}
    }

    private void createPermbansTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "permbans" (
                "id" INT AUTO_INCREMENT PRIMARY KEY,
                "uuid" VARCHAR(36),
                "username" VARCHAR(16),
                "reason" VARCHAR(65535)
            )
            """;
        statementHandler.executeUpdate(sql);

        try { statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_permbans_uuid ON \"permbans\"(\"uuid\")"); } catch (SQLException ignored) {}
        try { statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_permbans_username ON \"permbans\"(\"username\")"); } catch (SQLException ignored) {}
    }

    private void createPermbanIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "permban_ips" (
                "id" INT AUTO_INCREMENT PRIMARY KEY,
                "permban_id" INT NOT NULL,
                "ip" VARCHAR(45) NOT NULL,
                UNIQUE ("permban_id", "ip"),
                FOREIGN KEY ("permban_id") REFERENCES "permbans"("id") ON DELETE CASCADE
            )
            """;
        statementHandler.executeUpdate(sql);
        try { statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_permban_ips_ip ON \"permban_ips\"(\"ip\")"); } catch (SQLException ignored) {}
    }

    private void createStrikesTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "strikes" (
                "ip" VARCHAR(45) PRIMARY KEY,
                "strike_count" INT NOT NULL DEFAULT 0,
                "last_strike_unix" BIGINT NOT NULL DEFAULT 0,
                "last_username" VARCHAR(16),
                "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
            )
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createDiscordLinksTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "discord_links" (
                "id" INT AUTO_INCREMENT PRIMARY KEY,
                "admin_uuid" VARCHAR(36) NOT NULL UNIQUE,
                "discord_user_id" VARCHAR(32) NOT NULL UNIQUE,
                "linked_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
            )
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createPunishmentHistoryTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS "punishment_history" (
                "id" INT AUTO_INCREMENT PRIMARY KEY,
                "target_uuid" VARCHAR(36),
                "target_name" VARCHAR(255),
                "actor_uuid" VARCHAR(36),
                "actor_name" VARCHAR(255),
                "punishment_type" VARCHAR(64) NOT NULL,
                "reason" VARCHAR(65535),
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
                "id" INT AUTO_INCREMENT PRIMARY KEY,
                "history_id" INT NOT NULL,
                "ip" VARCHAR(45) NOT NULL,
                UNIQUE ("history_id", "ip"),
                FOREIGN KEY ("history_id") REFERENCES "punishment_history"("id") ON DELETE CASCADE
            )
            """;
        statementHandler.executeUpdate(sql);
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishment_history_ips_ip ON \"punishment_history_ips\"(\"ip\")");
    }

    // ============================================
    // Repository Getters
    // H2 is MySQL-compatible, so we reuse MySQL repositories
    // ============================================

    @Override
    public AdminRepository getAdminRepository()
    {
        if (adminRepository == null)
        {
            adminRepository = new MySQLAdminRepository(plugin, statementHandler);
        }
        return adminRepository;
    }

    @Override
    public BanRepository getBanRepository()
    {
        if (banRepository == null)
        {
            banRepository = new MySQLBanRepository(plugin, statementHandler);
        }
        return banRepository;
    }

    @Override
    public PermbanRepository getPermbanRepository()
    {
        if (permbanRepository == null)
        {
            permbanRepository = new MySQLPermbanRepository(plugin, statementHandler);
        }
        return permbanRepository;
    }

    @Override
    public StrikeRepository getStrikeRepository()
    {
        if (strikeRepository == null)
        {
            strikeRepository = new MySQLStrikeRepository(plugin, statementHandler);
        }
        return strikeRepository;
    }

    @Override
    public DiscordLinkRepository getDiscordLinkRepository()
    {
        if (discordLinkRepository == null)
        {
            discordLinkRepository = new MySQLDiscordLinkRepository(plugin, statementHandler);
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
