package me.totalfreedom.totalfreedommod.player;

import me.totalfreedom.api.FreedomAPI;
import me.totalfreedom.api.player.IPlayerList;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.sql.PersistenceQueue;
import me.totalfreedom.api.sql.adapter.PlayerRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.util.JsonUtil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Loads, caches and saves {@link PlayerData}, keyed by UUID.
 * <p>
 * Storage is SQL ({@code player_data}) when the database is up, with a JSON snapshot per player at
 * {@code players/<uuid>.json} either way. Records written before UUIDs were stored are picked up on
 * that player's first join: see {@link #loadForSession(UUID, String)}.
 * <p>
 * Staff are ordinary records that hold a rank. Every one of them is kept in {@link #staff} so they
 * can be listed and looked up while offline, which is what the old admin list was for.
 */
public class PlayerList extends FreedomService implements IPlayerList
{

    private static final long SHUTDOWN_FLUSH_TIMEOUT_MS = 10L * 1000L;
    private static final String JSON_SUFFIX = ".json";

    /**
     * Data for every online player. Concurrent because pre-login fills it off the main thread and
     * async chat handlers read it.
     */
    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();

    /**
     * Every record holding a staff rank, active or not, online or not. Holds the live instance
     * while the player is online.
     */
    private final Map<UUID, PlayerData> staff = new ConcurrentHashMap<>();
    private final File configFolder;
    private final PersistenceQueue writes = new PersistenceQueue("player data");

    public PlayerList(FreedomAPI plugin)
    {
        super(plugin);

        this.configFolder = new File(plugin.getDataFolder(), "players");
    }

    public Map<UUID, PlayerData> getDataMap()
    {
        return dataMap;
    }

    public File getConfigFolder()
    {
        if (!configFolder.exists())
        {
            configFolder.mkdirs();
        }

        return configFolder;
    }

    @Override
    public void onStart()
    {
        dataMap.clear();
        staff.clear();

        // The JSON snapshots first, so staff are known before the database finishes connecting.
        new LegacyStaffJsonMigration(plugin, this).run();
        loadStaffFromJson();
        if (plugin.database() != null)
            plugin.database().whenReady(this::loadStaffFromSql);

        // Preload online players
        server.getOnlinePlayers().forEach(this::getData);
    }

    @Override
    public void onStop()
    {
        save();
        writes.await(SHUTDOWN_FLUSH_TIMEOUT_MS);
    }

    /**
     * Queue a write of every loaded player record.
     */
    public void save()
    {
        List.copyOf(dataMap.values()).forEach(this::saveOne);
    }

    public void saveAsync()
    {
        save();
    }

    public void saveData(PlayerData data)
    {
        saveOne(data);
    }

    public void clearSavedTag(Player player)
    {
        final PlayerData data = getData(player);
        if (data.getSavedTag() != null)
        {
            data.setSavedTag(null);
            saveData(data);
        }
    }

    public boolean saveCurrentTag(Player player)
    {
        final PlayerData data = getData(player);
        final String tag = data.getInternalTag();
        if (tag == null || tag.isEmpty())
        {
            return false;
        }

        data.setSavedTag(tag);
        saveData(data);
        return true;
    }

    public String getIp(OfflinePlayer player)
    {
        return Optional.ofNullable(getData(player.getUniqueId()))
                       .map(PlayerData::getIp)
                       .orElse(null);
    }

    /**
     * The data for an online player, loading or creating it on first use. Safe off the main
     * thread. May not return null.
     */
    public PlayerData getData(Player player)
    {
        return dataMap.computeIfAbsent(player.getUniqueId(), uuid ->
                indexStaff(loadForSession(uuid, player.getName()).orElseGet(() -> createData(player))));
    }

    /**
     * The live instance when this player is online, otherwise a fresh load. May return null.
     */
    public PlayerData getData(UUID uuid)
    {
        return Optional.ofNullable(dataMap.get(uuid))
                       .or(() -> Optional.ofNullable(load(uuid)).map(this::bind))
                       .orElse(null);
    }

    /**
     * The live instance when {@code username} is online, otherwise the stored record that last
     * joined under that name. May return null.
     */
    public PlayerData getData(String username)
    {
        return Optional.ofNullable(server.getPlayerExact(username))
                       .map(this::getData)
                       .or(() -> Optional.ofNullable(loadByUsername(username)).map(this::bind))
                       .orElse(null);
    }

    public Collection<PlayerData> getAllData()
    {
        if (usingSql())
        {
            try
            {
                return plugin.database().getPlayerRepository().loadAll().values()
                             .stream()
                             .map(this::liveOrBound)
                             .toList();
            }
            catch (Exception ex)
            {
                FLog.warn("Failed to load all player data from SQL, falling back to JSON: " + ex.getMessage());
            }
        }

        return Arrays.stream(Objects.requireNonNullElse(getConfigFolder().listFiles(), new File[0]))
                     .filter(file -> file.getName().endsWith(JSON_SUFFIX))
                     .map(this::readJsonFile)
                     .filter(Objects::nonNull)
                     .filter(PlayerData::isValid)
                     .map(this::liveOrBound)
                     .toList();
    }

    public Collection<PlayerData> getLoadedData()
    {
        return dataMap.values();
    }

    public Collection<PlayerData> getStaff()
    {
        return staff.values()
                    .stream()
                    .map(this::liveOrBound)
                    .toList();
    }

    public Optional<PlayerData> getStaffByName(String username)
    {
        return findStaff(data -> data.getUsername().equalsIgnoreCase(username));
    }

    public Optional<PlayerData> getStaffByIp(String ip)
    {
        return findStaff(data -> data.isActiveStaff() && data.getStoredIp().filter(ip::equals).isPresent());
    }

    /**
     * Matches on the first three octets as well, so a ban on an address next to a staff member's
     * still finds them.
     */
    public Optional<PlayerData> getStaffByIpFuzzy(String ip)
    {
        return getStaffByIp(ip)
                .or(() -> findStaff(data -> data.isActiveStaff()
                        && data.getStoredIp().filter(stored -> FUtil.fuzzyIpMatch(ip, stored, 3)).isPresent()));
    }

    public Optional<PlayerData> getStaffByDiscordId(String discordId)
    {
        return findStaff(data -> data.getDiscordId().filter(discordId::equals).isPresent());
    }

    /**
     * {@code player}'s record when they hold an active staff rank and this session passes the
     * identity check; empty otherwise.
     */
    public Optional<PlayerData> getVerifiedStaff(Player player)
    {
        return Optional.of(getData(player))
                       .filter(PlayerData::isActiveStaff)
                       .filter(data -> identityMatches(data, sessionIp(player)));
    }

    /**
     * The same test made at login, before there is a {@link Player} to ask.
     */
    public boolean isVerifiedStaffLogin(UUID uuid, String ip)
    {
        return Optional.ofNullable(dataMap.get(uuid))
                       .or(() -> Optional.ofNullable(staff.get(uuid)))
                       .filter(PlayerData::isActiveStaff)
                       .filter(data -> identityMatches(data, ip))
                       .isPresent();
    }

    /**
     * Whether {@code player} is on an active staff record but this session fails the identity check.
     */
    public boolean isImpostor(Player player)
    {
        return Optional.ofNullable(dataMap.get(player.getUniqueId()))
                       .or(() -> Optional.ofNullable(staff.get(player.getUniqueId())))
                       .filter(PlayerData::isActiveStaff)
                       .filter(data -> !identityMatches(data, sessionIp(player)))
                       .isPresent();
    }

    public boolean isIdentityMatched(Player player)
    {
        return identityMatches(getData(player), sessionIp(player));
    }

    public Set<Player> getOnlineStaff()
    {
        return server.getOnlinePlayers()
                     .stream()
                     .filter(plugin.ranks()::isAdmin)
                     .collect(Collectors.toSet());
    }

    /**
     * Deactivates every non-senior staff member who hasn't joined within the configured threshold.
     * They keep their rank on record, so {@code /saconfig add} brings them straight back.
     */
    public void deactivateOldStaff(boolean verbose)
    {
        final long threshold = ConfigEntry.ADMINLIST_CLEAN_THESHOLD_HOURS.getInteger();

        getStaff().stream()
                  .filter(PlayerData::isActiveStaff)
                  .filter(data -> data.getRankId().map(plugin.ranks()::grantsSeniorStatus).filter(Boolean::booleanValue).isEmpty())
                  // A record with no recorded join has nothing to age out against.
                  .filter(data -> data.getLastJoinUnix() > 0)
                  .filter(data -> inactiveHours(data) >= threshold)
                  .forEach(data ->
                  {
                      if (verbose)
                      {
                          FUtil.adminAction("TotalFreedomMod", String.format("Deactivating admin %s, inactive for %d hours",
                                  data.getUsername(), inactiveHours(data)), true);
                      }

                      data.setAdminActive(false);
                      saveOne(data);
                  });
    }

    /**
     * Links {@code discordId} to {@code data}, taking it off any other record first, and gives the
     * record a Discord identifier if it doesn't have one.
     *
     * @return the record's identifier.
     */
    public String linkDiscord(PlayerData data, String discordId)
    {
        getStaff().stream()
                  .filter(other -> !other.getUuid().equals(data.getUuid()))
                  .filter(other -> other.getDiscordId().filter(discordId::equals).isPresent())
                  .forEach(other ->
                  {
                      other.setDiscordId(null);
                      saveOne(other);
                  });

        data.setDiscordId(discordId);
        if (data.getDiscordIdentifier().isEmpty())
            data.setDiscordIdentifier(PlayerData.newDiscordIdentifier(this::isDiscordIdentifierTaken));

        saveOne(data);
        return data.getDiscordIdentifier().orElseThrow();
    }

    public void unlinkDiscord(PlayerData data)
    {
        data.setDiscordId(null);
        saveOne(data);
    }

    public int purgeAllData()
    {
        int deleted = 0;
        for (File file : getConfigFolder().listFiles())
        {
            deleted += file.delete() ? 1 : 0;
        }

        if (usingSql())
        {
            writes.enqueue(plugin.database().getPlayerRepository().deleteAll()
                    .onErrorResume(ex ->
                    {
                        FLog.error(String.format("Could not purge player data from SQL: %s", ex.getMessage()));
                        return Mono.empty();
                    }));
        }

        dataMap.clear();
        return deleted;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        final PlayerData data = getData(player);

        // The record follows the UUID, so a renamed player keeps theirs; only the name needs catching up.
        data.setUsername(player.getName());

        if (data.isMuted())
        {
            data.setMuted(true);
            player.sendMessage(Component.text("You are still muted.", NamedTextColor.RED));
        }

        if (data.isFrozen())
        {
            data.getFreezeData().setFrozen(true);
            player.sendMessage(Component.text("You are still frozen.", NamedTextColor.AQUA));
        }

        final String savedTag = data.getSavedTag();
        if (savedTag != null)
        {
            data.setTag(savedTag);
        }

        if (data.hasCustomNickname())
        {
            player.displayName(data.hasCustomNickname() ?
                    data.getDisplayedNickname() :
                    Component.text(player.getName(), player.isOp() ? NamedTextColor.RED : NamedTextColor.WHITE));
        }
        data.setLastJoinUnix(FUtil.getUnixTime());

        // A staff record's address is part of its identity check, so only a session that already
        // passes the check may move it. Otherwise an impostor would make their own address the
        // one that passes next time.
        final String ip = sessionIp(player);
        if (ip != null && (!data.isStaff() || identityMatches(data, ip)))
            data.setIp(ip);
        if (plugin.isEnabled())
        {
            saveOne(data);
        }
    }

    /**
     * Warm {@link #dataMap} before the player reaches the main thread. This event already runs
     * off it, so the storage lookup (and any move of an old record) that {@code onPlayerJoin}
     * would otherwise make happens here instead, and join finds a cached entry.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            return;

        // Returning null leaves no mapping, so a first-time player is created on join instead.
        dataMap.computeIfAbsent(event.getUniqueId(), uuid -> loadForSession(uuid, event.getName())
                .map(this::indexStaff)
                .orElse(null));
    }

    /**
     * A login refused after pre-login (whitelist, full server, a kick from another plugin) never
     * produces a quit, so the record pre-login cached would otherwise stay in {@link #dataMap}.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event)
    {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED)
            dataMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        final PlayerData data = dataMap.remove(event.getPlayer().getUniqueId());

        if (data != null && plugin.isEnabled())
        {
            saveOne(data);
        }
    }

    protected File getConfigFile(UUID uuid)
    {
        return new File(getConfigFolder(), uuid + JSON_SUFFIX);
    }

    private boolean usingSql()
    {
        return plugin.database() != null && plugin.database().isInitialized();
    }

    /**
     * The record for a joining player, if there is one.
     * <p>
     * Normally that's just the record stored under their UUID. Failing that, a record that only
     * knew the player by name is adopted: either a row the SQL migration keyed by the name's
     * offline UUID, or a {@code players/<name>.json} from before files were named by UUID. On an
     * offline-mode server the offline UUID is the session UUID, so the first lookup already finds
     * the row and only the old file needs tidying. Either way the record ends up stored under the
     * session UUID and the old copy is removed once that has been written.
     */
    private Optional<PlayerData> loadForSession(UUID uuid, String username)
    {
        final File legacyFile = getLegacyConfigFile(username);
        final UUID placeholder = PlayerData.offlineUuid(username);
        final Optional<PlayerData> stored = Optional.ofNullable(load(uuid));
        if (stored.isPresent())
        {
            // An old name file only provably belongs to this record when the name's offline UUID is
            // this session's. Otherwise it may be someone who used the name before, still to be adopted.
            if (placeholder.equals(uuid) && legacyFile.exists())
                moveRecord(stored.get(), uuid, legacyFile);

            return stored.map(this::bind);
        }

        final Optional<PlayerData> adopted = Optional.ofNullable(placeholder.equals(uuid) ? null : load(placeholder))
                                                     .or(() -> Optional.ofNullable(legacyFile.exists() ? readJsonFile(legacyFile) : null))
                                                     .filter(PlayerData::isValid);

        adopted.ifPresent(data ->
        {
            final UUID previousUuid = data.getUuid();
            data.setUuid(uuid);
            FLog.info(String.format("Moving player data for %s from %s to %s.", username, previousUuid, uuid));
            moveRecord(data, previousUuid, legacyFile);
        });

        return adopted.map(this::bind);
    }

    /**
     * The stored record keyed by {@code uuid}, from SQL when it's up and the JSON snapshot otherwise.
     */
    private PlayerData load(UUID uuid)
    {
        return Optional.ofNullable(usingSql() ? loadFromSql(uuid) : null)
                       .orElseGet(() -> loadFromJson(uuid));
    }

    /**
     * Names aren't keys, so this goes through whatever can map a name to a UUID: the username column
     * in SQL, then a {@code players/<name>.json} from before files were named by UUID, then the
     * server's own name cache.
     */
    private PlayerData loadByUsername(String username)
    {
        if (usingSql())
        {
            try
            {
                final Optional<PlayerData> row = plugin.database().getPlayerRepository().findByUsername(username);
                if (row.isPresent())
                    return loadFromSql(row.get().getUuid());
            }
            catch (Exception ex)
            {
                FLog.warn("Failed to look up player data for " + username + " in SQL, falling back to JSON: " + ex.getMessage());
            }
        }

        final File legacyFile = getLegacyConfigFile(username);
        if (legacyFile.exists())
            return readJsonFile(legacyFile);

        return Optional.ofNullable(server.getOfflinePlayerIfCached(username))
                       .map(OfflinePlayer::getUniqueId)
                       .map(this::loadFromJson)
                       .orElse(null);
    }

    private PlayerData loadFromSql(UUID uuid)
    {
        try
        {
            final PlayerRepository repo = plugin.database().getPlayerRepository();
            reconcileFromJsonIfNewer(uuid, repo);

            return repo.findByUuid(uuid).orElse(null);
        }
        catch (Exception ex)
        {
            FLog.warn("Failed to load player data for " + uuid + " from SQL, falling back to JSON: " + ex.getMessage());
            return null;
        }
    }

    /**
     * If this player's JSON snapshot was written more recently than the database's last update
     * for them, re-import it into SQL.
     */
    private void reconcileFromJsonIfNewer(UUID uuid, PlayerRepository repo)
    {
        final File jsonFile = getConfigFile(uuid);
        if (!jsonFile.exists())
        {
            return;
        }

        try
        {
            final Long sqlUpdatedAt = repo.getUpdatedAt(uuid);
            if (!FUtil.isSnapshotNewer(jsonFile.lastModified(), sqlUpdatedAt))
                return;

            final PlayerData jsonData = readJsonFile(jsonFile);
            if (jsonData == null || !jsonData.isValid())
            {
                return;
            }

            FLog.info(jsonFile.getName() + " is newer than the database; re-importing player data for " + jsonData.getUsername() + " from it.");
            repo.saveOrUpdate(jsonData);
        }
        catch (Exception ex)
        {
            FLog.warn("Failed to reconcile player data for " + uuid + " into the database: " + ex.getMessage());
        }
    }

    private PlayerData loadFromJson(UUID uuid)
    {
        final File configFile = getConfigFile(uuid);
        if (!configFile.exists())
        {
            return null;
        }

        final PlayerData data = readJsonFile(configFile);

        if (data == null || !data.isValid())
        {
            FLog.warn("Could not load player data entry: " + uuid + ". Entry is not valid!");
            configFile.delete();
            return null;
        }

        return data;
    }

    /**
     * Files written before records carried a UUID come back without one; those get the offline
     * UUID of their name, the same placeholder the SQL migration uses.
     */
    PlayerData readJsonFile(File file)
    {
        try (FileReader reader = new FileReader(file))
        {
            final JsonElement json = JsonParser.parseReader(reader);
            final PlayerData data = JsonUtil.GSON.fromJson(json, PlayerData.class);
            if (data == null)
                return null;

            if (data.getUuid() == null && data.getUsername() != null)
                data.setUuid(PlayerData.offlineUuid(data.getUsername()));

            // Older files kept a list of addresses, oldest first, instead of one.
            if (data.getStoredIp().isEmpty() && json.isJsonObject())
                legacyIp(json.getAsJsonObject()).ifPresent(data::setIp);

            return data;
        }
        catch (Exception ex)
        {
            FLog.error("Could not read player data from " + file.getName() + ": " + ex.getMessage());
            return null;
        }
    }

    /**
     * Reads the pre-JSON per-player {@code players/<name>.yml} format. Retained only for the
     * one-time legacy-install migration path (not called during normal startup).
     */
    private PlayerData loadLegacyYaml(String username)
    {
        final File legacyFile = new File(getConfigFolder(), username + ".yml");
        if (!legacyFile.exists())
        {
            return null;
        }

        final YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyFile);
        final PlayerData data = new PlayerData(username);
        data.loadFrom(config);
        return data;
    }

    /**
     * Queue one player's SQL write plus its JSON snapshot refresh. Safe from an event handler:
     * the round trip runs off the main thread.
     */
    private void saveOne(final PlayerData data)
    {
        indexStaff(data);

        if (!usingSql())
        {
            writes.enqueue(writeJsonAsync(data).then());
            return;
        }

        writes.enqueue(plugin.database().getPlayerRepository().save(data)
                .onErrorResume(ex ->
                {
                    FLog.error(String.format("Could not save player data for %s to SQL: %s",
                            data.getUsername(), ex.getMessage()));
                    FLog.error(ex);
                    return Mono.empty();
                })
                .then(writeJsonAsync(data))
                .then());
    }

    /**
     * Writes {@code data} under its current UUID, then removes the copies it was found under: the
     * SQL row and snapshot keyed by {@code previousUuid} (when that differs) and {@code legacyFile}.
     * Each removal waits for the write it depends on, so a failed write never costs the old copy.
     */
    private void moveRecord(PlayerData data, UUID previousUuid, File legacyFile)
    {
        final boolean rekeyed = !previousUuid.equals(data.getUuid());
        final Mono<Boolean> jsonMove = writeJsonAsync(data)
                .filter(Boolean::booleanValue)
                .doOnNext(written ->
                {
                    if (rekeyed)
                        getConfigFile(previousUuid).delete();
                    legacyFile.delete();
                });

        if (!usingSql())
        {
            writes.enqueue(jsonMove.then());
            return;
        }

        final PlayerRepository repo = plugin.database().getPlayerRepository();
        writes.enqueue(repo.save(data)
                .then(rekeyed ? repo.deleteAsync(previousUuid).then() : Mono.<Void>empty())
                .then(jsonMove)
                .onErrorResume(ex ->
                {
                    FLog.error(String.format("Could not move player data for %s: %s", data.getUsername(), ex.getMessage()));
                    return Mono.empty();
                })
                .then());
    }

    /**
     * @return a Mono emitting whether the snapshot was written.
     */
    private Mono<Boolean> writeJsonAsync(final PlayerData data)
    {
        return Mono.fromCallable(() -> saveToJson(data))
                .subscribeOn(Schedulers.boundedElastic());
    }

    boolean saveToJson(PlayerData data)
    {
        final File configFile = getConfigFile(data.getUuid());
        try (FileWriter writer = new FileWriter(configFile))
        {
            JsonUtil.GSON.toJson(data, PlayerData.class, writer);
            return true;
        }
        catch (IOException ex)
        {
            FLog.error("Could not save player data for " + data.getUsername());
            FLog.error(ex);
            return false;
        }
    }

    private PlayerData createData(Player player)
    {
        FLog.info("Creating new player data entry for " + player.getName());

        final long unix = FUtil.getUnixTime();
        final PlayerData data = bind(new PlayerData(player));
        data.setFirstJoinUnix(unix);
        data.setLastJoinUnix(unix);
        data.setIp(player.getAddress().getAddress().getHostAddress());
        saveOne(data);

        return data;
    }

    /**
     * Prefers the live instance for an online player, so callers don't edit a stale copy of them.
     */
    private PlayerData liveOrBound(PlayerData data)
    {
        return Optional.ofNullable(dataMap.get(data.getUuid())).orElseGet(() -> bind(data));
    }

    private PlayerData bind(PlayerData data)
    {
        data.bind(plugin);
        return data;
    }

    /**
     * Keeps {@link #staff} in step with {@code data}, which may have gained or lost a rank.
     */
    private PlayerData indexStaff(PlayerData data)
    {
        if (data.isStaff())
            staff.put(data.getUuid(), data);
        else
            staff.remove(data.getUuid());

        return data;
    }

    private Optional<PlayerData> findStaff(Predicate<PlayerData> filter)
    {
        return getStaff().stream()
                         .filter(filter)
                         .findFirst();
    }

    boolean isDiscordIdentifierTaken(String identifier)
    {
        return staff.values()
                    .stream()
                    .anyMatch(data -> data.getDiscordIdentifier().filter(identifier::equals).isPresent());
    }

    /**
     * Until there is a challenge to prove who a session is (see the proposed changelog), staff are
     * matched as before: Mojang vouches for the UUID in online mode, {@code use_uuid_only} opts into
     * trusting it anyway, and otherwise the session has to come from the stored address.
     */
    private boolean identityMatches(PlayerData data, String sessionIp)
    {
        return server.getOnlineMode()
                || ConfigEntry.ADMINLIST_USE_UUID_ONLY.getBoolean()
                || (sessionIp != null && data.getStoredIp().filter(sessionIp::equals).isPresent());
    }

    private static String sessionIp(Player player)
    {
        return Optional.ofNullable(player.getAddress())
                       .map(address -> address.getAddress().getHostAddress())
                       .orElse(null);
    }

    private static long inactiveHours(PlayerData data)
    {
        return TimeUnit.SECONDS.toHours(FUtil.getUnixTime() - data.getLastJoinUnix());
    }

    private static Optional<String> legacyIp(JsonObject json)
    {
        return Optional.ofNullable(json.get("ips"))
                       .filter(JsonElement::isJsonArray)
                       .map(JsonElement::getAsJsonArray)
                       .filter(ips -> !ips.isEmpty())
                       .map(ips -> ips.get(ips.size() - 1).getAsString());
    }

    private void loadStaffFromJson()
    {
        Arrays.stream(Objects.requireNonNullElse(getConfigFolder().listFiles(), new File[0]))
              .filter(file -> file.getName().endsWith(JSON_SUFFIX))
              .map(this::readJsonFile)
              .filter(Objects::nonNull)
              .filter(PlayerData::isStaff)
              .map(this::bind)
              .forEach(data -> staff.put(data.getUuid(), data));

        FLog.info(String.format("Loaded %d staff record(s) from JSON.", staff.size()));
        deactivateOldStaff(false);
    }

    /**
     * Replaces the JSON-built index with the database's, keeping the live instance of anyone
     * online. Runs on the main thread once the database is up.
     */
    private void loadStaffFromSql()
    {
        plugin.database().readAsync("PlayerList/loadStaff", plugin.database().getPlayerRepository().findStaffAsync(),
                rows ->
                {
                    staff.clear();
                    rows.stream()
                        .map(this::liveOrBound)
                        .forEach(data -> staff.put(data.getUuid(), data));
                    dataMap.values().forEach(this::indexStaff);

                    FLog.info(String.format("Loaded %d staff record(s) from SQL.", staff.size()));
                    deactivateOldStaff(false);
                },
                () -> FLog.warn("Could not load staff from SQL; keeping the JSON snapshot's."));
    }

    /**
     * Where this player's snapshot lived before files were named by UUID.
     */
    File getLegacyConfigFile(String username)
    {
        return new File(getConfigFolder(), username.toLowerCase() + JSON_SUFFIX);
    }
}
