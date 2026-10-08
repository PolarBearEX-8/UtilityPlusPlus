package zeb.deluxeg4.utilityplus.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import zeb.deluxeg4.utilityplus.util.Messages;

public final class PlayerChatNames
implements Listener {
    private static final Set<String> ONLINE_NAMES = ConcurrentHashMap.newKeySet();
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    public PlayerChatNames() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ONLINE_NAMES.add(player.getName());
        }
    }

    public static Component decorate(Component message) {
        Component decorated = message;
        ArrayList<String> names = new ArrayList<String>(ONLINE_NAMES);
        names.sort(Comparator.comparingInt(String::length).reversed());
        for (String name : names) {
            Pattern pattern = Pattern.compile("(?i)(?<![\\p{L}\\p{N}_])" + Pattern.quote(name) + "(?![\\p{L}\\p{N}_])");
            TextComponent hover = AMPERSAND.deserialize(Messages.config("player-name.hover", "&6Message &3{player}").replace("{player}", name));
            String command = Messages.config("player-name.suggest-command", "/w {player}").replace("{player}", name);
            Component replacement = ((TextComponent)Component.text((String)name).hoverEvent((HoverEventSource)HoverEvent.showText((Component)hover))).clickEvent(ClickEvent.suggestCommand((String)command));
            decorated = decorated.replaceText((TextReplacementConfig)TextReplacementConfig.builder().match(pattern).replacement((ComponentLike)replacement).build());
        }
        return decorated;
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        ONLINE_NAMES.add(event.getPlayer().getName());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        ONLINE_NAMES.remove(event.getPlayer().getName());
    }
}
