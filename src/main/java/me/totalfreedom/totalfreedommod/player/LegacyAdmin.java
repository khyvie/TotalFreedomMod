package me.totalfreedom.totalfreedommod.player;

import java.util.Optional;
import java.util.UUID;

import me.totalfreedom.totalfreedommod.util.FUtil;

/**
 * One admin as the retired admin list stored them, in whichever of its formats (SQL tables,
 * {@code admins.json}, {@code admins.yml}). Every importer turns its rows into these and lets this
 * decide where they land, so the rules below exist once.
 *
 * @param uuid          the stored UUID, if any. Often wrong: see {@link #targetUuid(boolean)}.
 * @param ip            the latest stored address, if any.
 * @param lastLoginUnix seconds since the epoch, or {@code 0} when never recorded.
 */
public record LegacyAdmin(String uuid, String username, String rankId, boolean active, String ip,
                          long lastLoginUnix, String loginMessage)
{
    /**
     * The UUID this admin's {@link PlayerData} should be keyed by.
     * <p>
     * On an online-mode server, a stored Mojang (version 4) UUID is the one the server will see,
     * so it's used and the admin is never matched by name. Anywhere else the stored UUID can't be
     * trusted: records were often given the premium UUID from a Mojang lookup, or an offline UUID
     * hashed from the lowercased name, and an offline-mode server issues neither. There it's the
     * name's offline UUID hashed the way vanilla does, which is the session UUID on an offline-mode
     * server and moves on the next join behind a proxy like any other old record.
     */
    public UUID targetUuid(boolean onlineMode)
    {
        return Optional.ofNullable(uuid)
                       .map(FUtil::parseUuid)
                       .filter(parsed -> onlineMode && parsed.version() == 4)
                       .orElseGet(() -> PlayerData.offlineUuid(username));
    }

    /**
     * Copies the admin's standing onto {@code data}, which must already be keyed by
     * {@link #targetUuid(boolean)}. Join times only ever move to cover the recorded login.
     */
    public void applyTo(PlayerData data)
    {
        data.setRankId(rankId);
        data.setAdminActive(active);
        data.setLoginMessage(loginMessage);
        if (ip != null)
            data.setIp(ip);

        final long lastLogin = lastLoginUnix > 0 ? lastLoginUnix : FUtil.getUnixTime();
        if (data.getLastJoinUnix() < lastLogin)
            data.setLastJoinUnix(lastLogin);
        if (data.getFirstJoinUnix() == 0L)
            data.setFirstJoinUnix(data.getLastJoinUnix());
    }

    /**
     * Whether a record found only by name may take this admin's standing: only while it is still
     * an unclaimed placeholder. One under a real UUID belongs to whoever holds the name now.
     */
    public static boolean isUnclaimedPlaceholder(PlayerData data)
    {
        return data.getUuid().equals(PlayerData.offlineUuid(data.getUsername()));
    }

    /**
     * {@code custom_rank} held the assigned rank and took precedence over {@code rank}, which held
     * a fixed tier name. A tier name lowercases into an id, the convention {@code ranks.json} uses.
     */
    public static String resolveRankId(String customRank, String legacyRank)
    {
        if (customRank != null && !customRank.isBlank())
            return customRank.toLowerCase();

        return legacyRank == null || legacyRank.isBlank() ? null : legacyRank.toLowerCase();
    }
}
