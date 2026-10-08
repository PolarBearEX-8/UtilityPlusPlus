package zeb.deluxeg4.utilityplus.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

public class ChatManager {
    private final UtilityPlus plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private ScheduledTask pendingSaveTask;
    private final Set<UUID> globalMuted = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pmMuted = ConcurrentHashMap.newKeySet();
    private final Set<UUID> deathMessagesMuted = ConcurrentHashMap.newKeySet();
    private final Set<UUID> hardDeathMessagesMuted = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> lastPmSender = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, UUID> lastPmTarget = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, Set<String>> ignoredPlayers = new ConcurrentHashMap<UUID, Set<String>>();
    private final Map<UUID, Set<String>> hardIgnoredPlayers = new ConcurrentHashMap<UUID, Set<String>>();
    private final Map<UUID, Set<String>> ignoredDeathMessages = new ConcurrentHashMap<UUID, Set<String>>();

    public ChatManager(UtilityPlus plugin) {
        this.plugin = plugin;
        this.loadData();
    }

    public boolean isGlobalMuted(UUID uuid) {
        return this.globalMuted.contains(uuid);
    }

    public void muteGlobal(UUID uuid) {
        this.globalMuted.add(uuid);
    }

    public void unmuteGlobal(UUID uuid) {
        this.globalMuted.remove(uuid);
    }

    public boolean isPmMuted(UUID uuid) {
        return this.pmMuted.contains(uuid);
    }

    public void mutePm(UUID uuid) {
        this.pmMuted.add(uuid);
    }

    public void unmutePm(UUID uuid) {
        this.pmMuted.remove(uuid);
    }

    public UUID getLastPmSender(UUID uuid) {
        return this.lastPmSender.get(uuid);
    }

    public void setLastPmSender(UUID target, UUID sender) {
        this.lastPmSender.put(target, sender);
    }

    public UUID getLastPmTarget(UUID uuid) {
        return this.lastPmTarget.get(uuid);
    }

    public void setLastPmTarget(UUID sender, UUID target) {
        this.lastPmTarget.put(sender, target);
    }

    public boolean toggleGlobalMuted(UUID uuid) {
        if (this.globalMuted.contains(uuid)) {
            this.globalMuted.remove(uuid);
            return false;
        }
        this.globalMuted.add(uuid);
        return true;
    }

    public boolean togglePmMuted(UUID uuid) {
        if (this.pmMuted.contains(uuid)) {
            this.pmMuted.remove(uuid);
            return false;
        }
        this.pmMuted.add(uuid);
        return true;
    }

    public boolean toggleDeathMessages(UUID uuid) {
        if (this.deathMessagesMuted.contains(uuid)) {
            this.deathMessagesMuted.remove(uuid);
            return false;
        }
        this.deathMessagesMuted.add(uuid);
        return true;
    }

    public boolean toggleHardDeathMessages(UUID uuid) {
        if (this.hardDeathMessagesMuted.contains(uuid)) {
            this.hardDeathMessagesMuted.remove(uuid);
            this.saveLater();
            return false;
        }
        this.hardDeathMessagesMuted.add(uuid);
        this.saveLater();
        return true;
    }

    public boolean isDeathMessagesMuted(UUID uuid) {
        return this.deathMessagesMuted.contains(uuid) || this.hardDeathMessagesMuted.contains(uuid);
    }

    public boolean toggleIgnore(UUID viewer, String targetName) {
        return this.toggleName(this.ignoredPlayers.computeIfAbsent(viewer, ignored -> ConcurrentHashMap.newKeySet()), targetName);
    }

    public boolean toggleHardIgnore(UUID viewer, String targetName) {
        boolean ignored = this.toggleName(this.hardIgnoredPlayers.computeIfAbsent(viewer, uuid -> ConcurrentHashMap.newKeySet()), targetName);
        this.saveLater();
        return ignored;
    }

    public boolean toggleDeathMessageIgnore(UUID viewer, String targetName) {
        boolean ignored = this.toggleName(this.ignoredDeathMessages.computeIfAbsent(viewer, uuid -> ConcurrentHashMap.newKeySet()), targetName);
        this.saveLater();
        return ignored;
    }

    public boolean isIgnoring(UUID viewer, String targetName) {
        return this.containsName(this.ignoredPlayers.getOrDefault(viewer, Set.of()), targetName) || this.containsName(this.hardIgnoredPlayers.getOrDefault(viewer, Set.of()), targetName);
    }

    public boolean isIgnoringDeathMessage(UUID viewer, String targetName) {
        return this.containsName(this.ignoredDeathMessages.getOrDefault(viewer, Set.of()), targetName);
    }

    public Set<String> getHardIgnoredPlayers(UUID viewer) {
        return new HashSet<String>(this.hardIgnoredPlayers.getOrDefault(viewer, Set.of()));
    }

    public Set<String> getIgnoredPlayers(UUID viewer) {
        return new HashSet<String>(this.ignoredPlayers.getOrDefault(viewer, Set.of()));
    }

    public void reload() {
        this.hardIgnoredPlayers.clear();
        this.ignoredDeathMessages.clear();
        this.hardDeathMessagesMuted.clear();
        this.loadData();
    }

    public void saveData() {
        this.cancelPendingSave();
        this.saveNow();
    }

    private synchronized void saveLater() {
        if (this.pendingSaveTask != null && !this.pendingSaveTask.isCancelled()) {
            return;
        }
        this.pendingSaveTask = PaperFoliaTasks.runGlobalDelayed((Plugin)this.plugin, task -> {
            this.pendingSaveTask = null;
            this.saveNow();
        }, 600L);
    }

    private synchronized void saveNow() {
        if (this.dataConfig == null) {
            return;
        }
        this.dataConfig.set("hard-ignore", null);
        this.dataConfig.set("death-message-ignore", null);
        this.dataConfig.set("hard-death-messages-muted", this.hardDeathMessagesMuted.stream().map(UUID::toString).toList());
        for (Map.Entry<UUID, Set<String>> entry : this.hardIgnoredPlayers.entrySet()) {
            this.dataConfig.set("hard-ignore." + String.valueOf(entry.getKey()), entry.getValue().stream().sorted().toList());
        }
        for (Map.Entry<UUID, Set<String>> entry : this.ignoredDeathMessages.entrySet()) {
            this.dataConfig.set("death-message-ignore." + String.valueOf(entry.getKey()), entry.getValue().stream().sorted().toList());
        }
        try {
            this.dataConfig.save(this.dataFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("[ChatManager] Could not save chat.yml!");
        }
    }

    private synchronized void cancelPendingSave() {
        if (this.pendingSaveTask != null && !this.pendingSaveTask.isCancelled()) {
            this.pendingSaveTask.cancel();
        }
        this.pendingSaveTask = null;
    }

    private void loadData() {
        this.dataFile = new File(this.plugin.getDataFolder(), "chat.yml");
        if (!this.dataFile.exists()) {
            try {
                this.plugin.getDataFolder().mkdirs();
                this.dataFile.createNewFile();
            }
            catch (IOException e) {
                this.plugin.getLogger().severe("[ChatManager] Could not create chat.yml!");
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration((File)this.dataFile);
        for (String uuidString : this.dataConfig.getStringList("hard-death-messages-muted")) {
            this.parseUuid(uuidString, this.hardDeathMessagesMuted);
        }
        this.loadNameMap("hard-ignore", this.hardIgnoredPlayers);
        this.loadNameMap("death-message-ignore", this.ignoredDeathMessages);
    }

    private void loadNameMap(String path, Map<UUID, Set<String>> target) {
        if (!this.dataConfig.isConfigurationSection(path)) {
            return;
        }
        for (String uuidString : this.dataConfig.getConfigurationSection(path).getKeys(false)) {
            UUID uuid = this.parseUuid(uuidString, null);
            if (uuid == null) continue;
            ConcurrentHashMap.KeySetView names = ConcurrentHashMap.newKeySet();
            for (String name : this.dataConfig.getStringList(path + "." + uuidString)) {
                names.add(name);
            }
            target.put(uuid, names);
        }
    }

    private UUID parseUuid(String value, Set<UUID> target) {
        try {
            UUID uuid = UUID.fromString(value);
            if (target != null) {
                target.add(uuid);
            }
            return uuid;
        }
        catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean toggleName(Set<String> names, String targetName) {
        for (String existingName : names) {
            if (!existingName.equalsIgnoreCase(targetName)) continue;
            names.remove(existingName);
            return false;
        }
        names.add(targetName);
        return true;
    }

    private boolean containsName(Set<String> names, String targetName) {
        for (String name : names) {
            if (!name.equalsIgnoreCase(targetName)) continue;
            return true;
        }
        return false;
    }
}
