package me.totalfreedom.totalfreedommod.sql.adapter.mysql;

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
 * MySQL-specific database adapter.
 * Works with both MySQL and MariaDB as they share SQL syntax.
 * Uses MySQL-specific SQL features like:
 * - INT AUTO_INCREMENT for primary keys
 * - INSERT IGNORE for upsert operations
 * - Backtick (`) for identifier quoting
 * - DATETIME for timestamp columns
 * - LOWER() for case-insensitive comparisons (collation may also handle this)
 */
public class MySQLAdapter extends DatabaseAdapter
{
    private MySQLAdminRepository adminRepository;
    private MySQLBanRepository banRepository;
    private MySQLPermbanRepository permbanRepository;
    private MySQLStrikeRepository strikeRepository;
    private MySQLDiscordLinkRepository discordLinkRepository;
    private SQLitePunishmentHistoryRepository punishmentHistoryRepository;

    public MySQLAdapter(TotalFreedomMod plugin, ConnectionHandler connectionHandler, StatementHandler statementHandler)
    {
        super(plugin, connectionHandler, statementHandler);
    }

    // ============================================
    // SQL Dialect Methods
    // ============================================

    @Override
    public String autoIncrementSyntax()
    {
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
        return "TEXT";
    }

    @Override
    public String timestampType()
    {
        return "DATETIME";
    }

    @Override
    public String booleanType()
    {
        return "TINYINT(1)";
    }

    @Override
    public String insertIgnoreSyntax()
    {
        return "INSERT IGNORE";
    }

    @Override
    public String quoteIdentifier(String identifier)
    {
        return "`" + identifier + "`";
    }

    @Override
    public String currentTimestamp()
    {
        return "NOW()";
    }

    @Override
    public String caseInsensitiveLike()
    {
        // MySQL is case-insensitive by default with utf8_general_ci collation
        return "LIKE";
    }

    // ============================================
    // Migration Methods
    // ============================================

    @Override
    public void runMigrations() throws SQLException
    {
        FLog.info("[MySQL] Running database migrations...");

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

        FLog.info("[MySQL] Database migrations complete.");
    }

    private void createMigrationTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS `migrations` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `version` VARCHAR(50) NOT NULL UNIQUE,
                `applied_at` DATETIME DEFAULT NOW()
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createAdminsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS `admins` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `uuid` VARCHAR(36) NOT NULL UNIQUE,
                `username` VARCHAR(16) NOT NULL,
                `rank` VARCHAR(32) NOT NULL,
                `active` TINYINT(1) DEFAULT 1,
                `last_login` DATETIME,
                `login_message` TEXT,
                INDEX `idx_admins_username` (`username`),
                INDEX `idx_admins_active` (`active`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createAdminIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS `admin_ips` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `admin_id` INT NOT NULL,
                `ip` VARCHAR(45) NOT NULL,
                UNIQUE KEY `uk_admin_ip` (`admin_id`, `ip`),
                INDEX `idx_admin_ips_ip` (`ip`),
                FOREIGN KEY (`admin_id`) REFERENCES `admins`(`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createBansTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS `bans` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `uuid` VARCHAR(36),
                `username` VARCHAR(16),
                `banned_by` VARCHAR(16),
                `banned_by_uuid` VARCHAR(36),
                `reason` TEXT,
                `expire_at` DATETIME,
                INDEX `idx_bans_uuid` (`uuid`),
                INDEX `idx_bans_username` (`username`),
                INDEX `idx_bans_expire` (`expire_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createBanIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS `ban_ips` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `ban_id` INT NOT NULL,
                `ip` VARCHAR(45) NOT NULL,
                UNIQUE KEY `uk_ban_ip` (`ban_id`, `ip`),
                INDEX `idx_ban_ips_ip` (`ip`),
                FOREIGN KEY (`ban_id`) REFERENCES `bans`(`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createPermbansTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS `permbans` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `uuid` VARCHAR(36),
                `username` VARCHAR(16),
                `reason` TEXT,
                INDEX `idx_permbans_uuid` (`uuid`),
                INDEX `idx_permbans_username` (`username`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createPermbanIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS `permban_ips` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `permban_id` INT NOT NULL,
                `ip` VARCHAR(45) NOT NULL,
                UNIQUE KEY `uk_permban_ip` (`permban_id`, `ip`),
                INDEX `idx_permban_ips_ip` (`ip`),
                FOREIGN KEY (`permban_id`) REFERENCES `permbans`(`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createStrikesTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS `strikes` (
                `ip` VARCHAR(45) PRIMARY KEY,
                `strike_count` INT NOT NULL DEFAULT 0,
                `last_strike_unix` BIGINT NOT NULL DEFAULT 0,
                `last_username` VARCHAR(16),
                `created_at` DATETIME DEFAULT NOW()
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createDiscordLinksTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS `discord_links` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `admin_uuid` VARCHAR(36) NOT NULL UNIQUE,
                `discord_user_id` VARCHAR(32) NOT NULL UNIQUE,
                `linked_at` DATETIME NOT NULL DEFAULT NOW()
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createPunishmentHistoryTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS `punishment_history` (
                `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                `target_uuid` VARCHAR(36),
                `target_name` VARCHAR(255),
                `actor_uuid` VARCHAR(36),
                `actor_name` VARCHAR(255),
                `punishment_type` VARCHAR(64) NOT NULL,
                `reason` TEXT,
                `applied_at` BIGINT NOT NULL,
                `expires_at` BIGINT,
                `source` VARCHAR(64),
                INDEX `idx_punishment_history_target_uuid` (`target_uuid`, `applied_at` DESC, `id` DESC),
                INDEX `idx_punishment_history_target_name` (`target_name`),
                INDEX `idx_punishment_history_applied_at` (`applied_at` DESC, `id` DESC)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        statementHandler.executeUpdate(sql);
    }

    private void createPunishmentHistoryIpsTable() throws SQLException
    {
        String sql = """
            CREATE TABLE IF NOT EXISTS `punishment_history_ips` (
                `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                `history_id` BIGINT NOT NULL,
                `ip` VARCHAR(45) NOT NULL,
                UNIQUE KEY `uk_punishment_history_ip` (`history_id`, `ip`),
                INDEX `idx_punishment_history_ips_ip` (`ip`),
                FOREIGN KEY (`history_id`) REFERENCES `punishment_history`(`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        statementHandler.executeUpdate(sql);
    }

    // ============================================
    // Repository Getters
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
