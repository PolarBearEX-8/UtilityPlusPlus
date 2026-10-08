package zeb.deluxeg4.papifolia;

import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * PlaceholderAPI's BungeeCord placeholders using Folia-safe schedulers.
 */
public final class BungeeExpansion extends PlaceholderExpansion
        implements PluginMessageListener, Taskable, Configurable {

    private static final String CHANNEL = "BungeeCord";
    private static final String GET_SERVERS = "GetServers";
    private static final String PLAYER_COUNT = "PlayerCount";
    private static final String CHECK_INTERVAL = "check_interval";

    private final Map<String, Integer> counts = new ConcurrentHashMap<>();
    private final AtomicInteger carrierCursor = new AtomicInteger();
    private volatile ScheduledTask pollingTask;

    @Override
    public String getIdentifier() {
        return "bungee";
    }

    @Override
    public String getAuthor() {
        return "UtilityPlus contributors";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public Map<String, Object> getDefaults() {
        return Collections.singletonMap(CHECK_INTERVAL, 5);
    }

    @Override
    public String onRequest(org.bukkit.OfflinePlayer player, String identifier) {
        String key = identifier.toLowerCase(Locale.ROOT);
        if (key.equals("total") || key.equals("all")) {
            return String.valueOf(counts.values().stream().mapToInt(Integer::intValue).sum());
        }
        return String.valueOf(counts.getOrDefault(key, 0));
    }

    @Override
    public synchronized void start() {
        stop();
        Plugin plugin = PlaceholderAPIPlugin.getInstance();
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);

        long intervalTicks = Math.max(1L, getLong(CHECK_INTERVAL, 5L)) * 20L;
        pollingTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> poll(plugin),
                40L,
                intervalTicks
        );
    }

    @Override
    public synchronized void stop() {
        Plugin plugin = PlaceholderAPIPlugin.getInstance();
        ScheduledTask task = pollingTask;
        pollingTask = null;
        if (task != null) {
            task.cancel();
        }
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        counts.clear();
    }

    private void poll(Plugin plugin) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) {
            return;
        }

        if (counts.isEmpty()) {
            sendToCarrier(plugin, online, GET_SERVERS, null);
            return;
        }

        for (String server : counts.keySet()) {
            sendToCarrier(plugin, online, PLAYER_COUNT, server);
        }
    }

    private void sendToCarrier(Plugin plugin, List<Player> players, String subChannel, String server) {
        int index = Math.floorMod(carrierCursor.getAndIncrement(), players.size());
        Player carrier = players.get(index);
        carrier.getScheduler().run(plugin, task -> {
            if (!carrier.isOnline()) {
                return;
            }
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (DataOutputStream output = new DataOutputStream(bytes)) {
                    output.writeUTF(subChannel);
                    if (server != null) {
                        output.writeUTF(server);
                    }
                }
                carrier.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not create a Velocity player-count request", exception);
            }
        }, () -> { });
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) {
            return;
        }

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            String subChannel = input.readUTF();
            if (GET_SERVERS.equals(subChannel)) {
                for (String server : input.readUTF().split(",\\s*")) {
                    if (!server.isBlank()) {
                        counts.putIfAbsent(server.toLowerCase(Locale.ROOT), 0);
                    }
                }
            } else if (PLAYER_COUNT.equals(subChannel)) {
                String server = input.readUTF().toLowerCase(Locale.ROOT);
                counts.put(server, input.readInt());
            }
        } catch (IOException exception) {
            PlaceholderAPIPlugin.getInstance().getLogger().log(
                    Level.WARNING,
                    "Could not parse a Velocity player-count response",
                    exception
            );
        }
    }
}
