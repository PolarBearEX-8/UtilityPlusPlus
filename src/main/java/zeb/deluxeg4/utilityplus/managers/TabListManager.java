package zeb.deluxeg4.utilityplus.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

public class TabListManager {
    private static final double DEFAULT_TPS = 20.0;
    private static final int MIN_UPDATE_INTERVAL_SECONDS = 1;
    private final UtilityPlus plugin;
    private final Map<UUID, String[]> lastSent = new ConcurrentHashMap<UUID, String[]>();
    private ScheduledTask updateTask;
    private boolean enabled;
    private long updateIntervalTicks;
    private List<String> headerLines;
    private List<String> footerLines;
    private volatile double lastKnownTps = 20.0;
    private volatile Method regionTpsMethod;
    private volatile boolean regionTpsMethodChecked;

    public TabListManager(UtilityPlus plugin) {
        this.plugin = plugin;
        this.reload();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void reload() {
        this.enabled = this.plugin.getConfig().getBoolean("tab-list.enabled", true);
        int updateIntervalSeconds = Math.max(1, this.plugin.getConfig().getInt("tab-list.update-interval", 5));
        this.updateIntervalTicks = (long)updateIntervalSeconds * 20L;
        this.headerLines = this.plugin.getConfig().getStringList("tab-list.header");
        this.footerLines = this.plugin.getConfig().getStringList("tab-list.footer");
        TabListManager tabListManager = this;
        synchronized (tabListManager) {
            this.regionTpsMethod = null;
            this.regionTpsMethodChecked = false;
        }
        this.lastSent.clear();
        this.stop();
        if (this.enabled) {
            this.start();
            this.updateAll();
        } else {
            this.clearAll();
        }
    }

    public void stop() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
            this.updateTask = null;
        }
    }

    public void update(Player player) {
        if (!player.isOnline()) {
            this.onPlayerQuit(player);
            return;
        }
        if (!this.enabled) {
            this.clear(player);
            return;
        }
        this.updatePlayerTabList(player, this.getPlayerRegionTps(player), this.formatUptime());
    }

    public void onPlayerQuit(Player player) {
        this.lastSent.remove(player.getUniqueId());
    }

    private void start() {
        this.updateTask = PaperFoliaTasks.runGlobalTimer((Plugin)this.plugin, task -> this.updateAll(), this.updateIntervalTicks, this.updateIntervalTicks);
    }

    private void updateAll() {
        String uptime = this.formatUptime();
        String onlineCount = String.valueOf(Bukkit.getOnlinePlayers().size());
        for (Player player : Bukkit.getOnlinePlayers()) {
            PaperFoliaTasks.runForPlayer((Plugin)this.plugin, player, () -> this.updateIfOnline(player, uptime, onlineCount));
        }
    }

    private void updateIfOnline(Player player, String uptime, String onlineCount) {
        if (player.isOnline()) {
            this.updatePlayerTabList(player, this.getPlayerRegionTps(player), uptime, onlineCount);
            return;
        }
        this.onPlayerQuit(player);
    }

    private void updatePlayerTabList(Player player, double tps, String uptime) {
        this.updatePlayerTabList(player, tps, uptime, String.valueOf(Bukkit.getOnlinePlayers().size()));
    }

    private void updatePlayerTabList(Player player, double tps, String uptime, String onlineCount) {
        String header = this.applyPlaceholderAPI(player, this.formatLines(this.headerLines, player, tps, uptime, onlineCount));
        String footer = this.applyPlaceholderAPI(player, this.formatLines(this.footerLines, player, tps, uptime, onlineCount));
        String[] previous = this.lastSent.get(player.getUniqueId());
        if (previous != null && previous[0].equals(header) && previous[1].equals(footer)) {
            return;
        }
        player.sendPlayerListHeaderAndFooter(Messages.legacy(header), Messages.legacy(footer));
        this.lastSent.put(player.getUniqueId(), new String[]{header, footer});
    }

    private String applyPlaceholderAPI(Player player, String text) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    private void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PaperFoliaTasks.runForPlayer((Plugin)this.plugin, player, () -> this.clear(player));
        }
    }

    private void clear(Player player) {
        player.sendPlayerListHeaderAndFooter((Component)Component.empty(), (Component)Component.empty());
        this.lastSent.remove(player.getUniqueId());
    }

    private String formatLines(List<String> lines, Player player, double tps, String uptime, String onlineCount) {
        return String.join((CharSequence)"\n", lines).replace("%server_tps%", this.formatTps(tps)).replace("%tps%", String.format(Locale.ROOT, "%.2f", Math.max(0.0, Math.min(20.0, tps)))).replace("%server_online%", onlineCount).replace("%player_ping%", String.valueOf(player.getPing())).replace("%server_uptime%", uptime);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private double getPlayerRegionTps(Player player) {
        if (!this.regionTpsMethodChecked) {
            TabListManager tabListManager = this;
            synchronized (tabListManager) {
                if (!this.regionTpsMethodChecked) {
                    try {
                        this.regionTpsMethod = Bukkit.getServer().getClass().getMethod("getRegionTPS", Location.class);
                    }
                    catch (NoSuchMethodException ignored) {
                        try {
                            this.regionTpsMethod = Bukkit.getServer().getClass().getMethod("getTPS", Location.class);
                        }
                        catch (NoSuchMethodException ignoredFallback) {
                            this.regionTpsMethod = null;
                        }
                    }
                    this.regionTpsMethodChecked = true;
                }
            }
        }
        if (this.regionTpsMethod != null) {
            try {
                Location location = player.getLocation();
                double[] tps = (double[])this.regionTpsMethod.invoke((Object)Bukkit.getServer(), location);
                if (tps != null && tps.length > 0) {
                    this.lastKnownTps = tps[0];
                    return this.lastKnownTps;
                }
            }
            catch (ReflectiveOperationException | RuntimeException exception) {
                // empty catch block
            }
        }
        return this.getServerTpsOrFallback();
    }

    private double getServerTpsOrFallback() {
        try {
            double[] tps = Bukkit.getTPS();
            if (tps != null && tps.length > 0) {
                this.lastKnownTps = tps[0];
            }
        }
        catch (UnsupportedOperationException unsupportedOperationException) {
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
        return this.lastKnownTps;
    }

    private String formatTps(double tps) {
        String color = tps < 12.0 ? "&c" : "";
        return color + String.format(Locale.US, "%.2f", Math.min(tps, 20.0));
    }

    private String formatUptime() {
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime());
        long days = totalSeconds / 86400L;
        long hours = totalSeconds % 86400L / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }
}
