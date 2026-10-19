package me.totalfreedom.totalfreedommod.sql;

import java.util.List;

/**
 * @param source    where the punishment came from. {@code manual-*} sources are staff issuing it
 *                  by hand; only those count toward punishment points.
 * @param revokedAt epoch millis of a successful appeal, or {@code null}. A revoked entry stays in
 *                  the history but no longer counts toward points.
 */
public record PunishmentHistoryEntry(
        long id,
        String targetUuid,
        String targetName,
        String actorUuid,
        String actorName,
        String punishmentType,
        String reason,
        long appliedAt,
        Long expiryAt,
        String source,
        List<String> ips,
        Long revokedAt,
        String revokedBy)
{
    public PunishmentHistoryEntry
    {
        ips = ips == null ? List.of() : List.copyOf(ips);
    }

    public boolean hasExpiry()
    {
        return expiryAt != null;
    }

    public boolean isRevoked()
    {
        return revokedAt != null;
    }

    public PunishmentHistoryEntry withTargetUuid(String uuid)
    {
        return new PunishmentHistoryEntry(id, uuid, targetName, actorUuid, actorName, punishmentType, reason,
                appliedAt, expiryAt, source, ips, revokedAt, revokedBy);
    }
}
