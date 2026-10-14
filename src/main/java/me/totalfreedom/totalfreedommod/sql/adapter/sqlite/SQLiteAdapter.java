package me.totalfreedom.totalfreedommod.sql.adapter.sqlite;

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
import me.totalfreedom.totalfreedommod.util.FLog;

import java.sql.SQLException;

/**
 * SQLite-specific database adapter.
 * Uses SQLite-specific SQL features like:
 * - INTEGER PRIMARY KEY AUTOINCREMENT for primary keys
 * - INSERT OR IGNORE for upsert operations
 * - TEXT for all string/timestamp columns
 * - LOWER() for case-insensitive comparisons
 */
public class SQLiteAdapter extends DatabaseAdapter
{
    private SQLiteAdminRepository adminRepository;
    private SQLiteBanRepository banRepository;
    private SQLitePermbanRepository permbanRepository;
    private SQLiteStrikeRepository strikeRepository;
    private SQLiteDiscordLinkRepository discordLinkRepository;
    private SQLitePunishmentHistoryRepository punishmentHistoryRepository;

    public SQLiteAdapter(TotalFreedomMod plugin, ConnectionHandler connectionHandler, StatementHandler statementHandler)
    {
        super(plugin, connectionHandler, statementHandler);
    }

    // ============================================
    // SQL Dialect Methods
    // ============================================

    @Override
    public String autoIncrementSyntax()
    {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    @Override
    public String primaryKeySyntax()
    {
        return ""; // Already included in autoIncrementSyntax for SQLite
    }

    @Override
    public String textType()
    {
        return "TEXT";
    }

    @Override
    public String timestampType()
    {
        return "TEXT"; // SQLite stores timestamps as TEXT (ISO format)
    }

    @Override
    public String booleanType()
    {
        return "INTEGER"; // SQLite uses 0/1 for boolean
    }

    @Override
    public String insertIgnoreSyntax()
    {
        return "INSERT OR IGNORE";
    }

    @Override
    public String quoteIdentifier(String identifier)
    {
        return identifier; // SQLite doesn't require identifier quoting for simple names
    }

    @Override
    public String currentTimestamp()
    {
        return "CURRENT_TIMESTAMP";
    }

    @Override
    public String caseInsensitiveLike()
    {
        return "LIKE"; // SQLite LIKE is case-insensitive by default
    }

    // ============================================
    // Migration Methods
    // ============================================

    @Override
    public void runMigrations() throws SQLException
    {
        FLog.info("[SQLite] Running database migrations...");

        // Enable foreign keys for SQLite
        statementHandler.executeUpdate("PRAGMA foreign_keys = ON");

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

        FLog.info("[SQLite] Database migrations complete.");
    }

    private void createMigrationTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS migrations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                version TEXT NOT NULL UNIQUE,
                applied_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createAdminsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS admins (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL UNIQUE,
                username TEXT NOT NULL,
                rank TEXT NOT NULL,
                active INTEGER DEFAULT 1,
                last_login TEXT,
                login_message TEXT,
                custom_rank TEXT
            )
            """;
        statementHandler.executeUpdate(sql);

        // Migration for existing tables
        try
        {
            statementHandler.executeUpdate("ALTER TABLE admins ADD COLUMN custom_rank TEXT");
        }
        catch (SQLException ignored)
        {
            // Column already exists or table doesn't exist yet (handled by CREATE TABLE IF NOT EXISTS)
        }

        // Create indexes
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_admins_username ON admins(username)");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_admins_active ON admins(active)");
    }

    private void createAdminIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS admin_ips (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                admin_id INTEGER NOT NULL,
                ip TEXT NOT NULL,
                UNIQUE (admin_id, ip),
                FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE CASCADE
            )
            """;
        statementHandler.executeUpdate(sql);
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_admin_ips_ip ON admin_ips(ip)");
    }

    private void createBansTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS bans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT,
                username TEXT,
                banned_by TEXT,
                banned_by_uuid TEXT,
                reason TEXT,
                expire_at TEXT
            )
            """;
        statementHandler.executeUpdate(sql);

        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bans_uuid ON bans(uuid)");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bans_username ON bans(username)");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bans_expire ON bans(expire_at)");
    }

    private void createBanIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS ban_ips (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ban_id INTEGER NOT NULL,
                ip TEXT NOT NULL,
                UNIQUE (ban_id, ip),
                FOREIGN KEY (ban_id) REFERENCES bans(id) ON DELETE CASCADE
            )
            """;
        statementHandler.executeUpdate(sql);
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ban_ips_ip ON ban_ips(ip)");
    }

    private void createPermbansTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS permbans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT,
                username TEXT,
                reason TEXT
            )
            """;
        statementHandler.executeUpdate(sql);

        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_permbans_uuid ON permbans(uuid)");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_permbans_username ON permbans(username)");
    }

    private void createPermbanIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS permban_ips (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                permban_id INTEGER NOT NULL,
                ip TEXT NOT NULL,
                UNIQUE (permban_id, ip),
                FOREIGN KEY (permban_id) REFERENCES permbans(id) ON DELETE CASCADE
            )
            """;
        statementHandler.executeUpdate(sql);
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_permban_ips_ip ON permban_ips(ip)");
    }

    private void createStrikesTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS strikes (
                ip TEXT PRIMARY KEY,
                strike_count INTEGER NOT NULL DEFAULT 0,
                last_strike_unix INTEGER NOT NULL DEFAULT 0,
                last_username TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createDiscordLinksTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS discord_links (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                admin_uuid TEXT NOT NULL UNIQUE,
                discord_user_id TEXT NOT NULL UNIQUE,
                linked_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createPunishmentHistoryTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS punishment_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                target_uuid TEXT,
                target_name TEXT,
                actor_uuid TEXT,
                actor_name TEXT,
                punishment_type TEXT NOT NULL,
                reason TEXT,
                applied_at INTEGER NOT NULL,
                expires_at INTEGER,
                source TEXT
            )
            """;
        statementHandler.executeUpdate(sql);
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishment_history_target_uuid ON punishment_history(target_uuid, applied_at DESC, id DESC)");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishment_history_target_name ON punishment_history(target_name)");
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishment_history_applied_at ON punishment_history(applied_at DESC, id DESC)");
    }

    private void createPunishmentHistoryIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS punishment_history_ips (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                history_id INTEGER NOT NULL,
                ip TEXT NOT NULL,
                UNIQUE (history_id, ip),
                FOREIGN KEY (history_id) REFERENCES punishment_history(id) ON DELETE CASCADE
            )
            """;
        statementHandler.executeUpdate(sql);
        statementHandler.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishment_history_ips_ip ON punishment_history_ips(ip)");
    }

    // ============================================
    // Repository Getters
    // ============================================

    @Override
    public AdminRepository getAdminRepository()
    {
        if (adminRepository == null)
        {
            adminRepository = new SQLiteAdminRepository(plugin, statementHandler);
        }
        return adminRepository;
    }

    @Override
    public BanRepository getBanRepository()
    {
        if (banRepository == null)
        {
            banRepository = new SQLiteBanRepository(plugin, statementHandler);
        }
        return banRepository;
    }

    @Override
    public PermbanRepository getPermbanRepository()
    {
        if (permbanRepository == null)
        {
            permbanRepository = new SQLitePermbanRepository(plugin, statementHandler);
        }
        return permbanRepository;
    }

    @Override
    public StrikeRepository getStrikeRepository()
    {
        if (strikeRepository == null)
        {
            strikeRepository = new SQLiteStrikeRepository(plugin, statementHandler);
        }
        return strikeRepository;
    }

    @Override
    public DiscordLinkRepository getDiscordLinkRepository()
    {
        if (discordLinkRepository == null)
        {
            discordLinkRepository = new SQLiteDiscordLinkRepository(plugin, statementHandler);
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
