package zeb.deluxeg4.utilityplus.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;

public class IgnoreListCommand
implements CommandExecutor {
    private static final int PAGE_SIZE = 9;
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private final ChatManager chatManager;

    public IgnoreListCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            Messages.send(sender, "&cThis command can only be used by players.");
            return true;
        }
        Player player = (Player)sender;
        Set<String> hardIgnoredPlayers = this.chatManager.getHardIgnoredPlayers(player.getUniqueId());
        Set<String> softIgnoredPlayers = this.chatManager.getIgnoredPlayers(player.getUniqueId());
        HashSet<String> ignored = new HashSet<String>(hardIgnoredPlayers);
        ignored.addAll(softIgnoredPlayers);
        if (ignored.isEmpty()) {
            Messages.send((CommandSender)player, Messages.config("ignore-list.empty", "&6No players ignored."));
            return true;
        }
        List<String> names = ignored.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        int totalPages = (names.size() + 9 - 1) / 9;
        int page = this.parsePage(args, totalPages);
        if (page < 1) {
            String path = totalPages == 1 ? "ignore-list.invalid-page-singular" : "ignore-list.invalid-page-plural";
            String fallback = totalPages == 1 ? "&cInvalid page argument, there are only {pages} page." : "&cInvalid page argument, there are only {pages} pages.";
            Messages.send((CommandSender)player, Messages.config(path, fallback).replace("{pages}", String.valueOf(totalPages)));
            return true;
        }
        this.sendHeader(player, page, totalPages);
        int start = (page - 1) * 9;
        int end = Math.min(start + 9, names.size());
        for (String name : names.subList(start, end)) {
            String displayName = Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> onlinePlayer.getName().equalsIgnoreCase(name)).map(Player::getName).findFirst().orElse(name);
            String rowPrefix = Messages.config("ignore-list.player-row-prefix", "&3{player} &7[").replace("{player}", displayName);
            TextComponent row = AMPERSAND.deserialize(rowPrefix);
            boolean hardIgnored = this.containsIgnoreCase(hardIgnoredPlayers, name);
            boolean softIgnored = this.containsIgnoreCase(softIgnoredPlayers, name);
            if (hardIgnored) {
                row = row.append(this.configured("ignore-list.hard-label", "&6hard").hoverEvent((HoverEventSource)HoverEvent.showText((Component)AMPERSAND.deserialize(Messages.config("ignore-list.hard-hover", "&6Click to remove the permanent ignore")))).clickEvent(ClickEvent.runCommand((String)("/ignorehard " + displayName))));
            }
            if (hardIgnored && softIgnored) {
                row = row.append((Component)AMPERSAND.deserialize("&7, "));
            }
            if (softIgnored) {
                row = row.append(this.configured("ignore-list.soft-label", "&6soft").hoverEvent((HoverEventSource)HoverEvent.showText((Component)AMPERSAND.deserialize(Messages.config("ignore-list.soft-hover", "&6Click to remove the soft ignore")))).clickEvent(ClickEvent.runCommand((String)("/ignore " + displayName))));
            }
            row = row.append(this.configured("ignore-list.player-row-suffix", "&7]"));
            player.sendMessage((Component)row);
        }
        return true;
    }

    private boolean containsIgnoreCase(Set<String> names, String target) {
        return names.stream().anyMatch(name -> name.equalsIgnoreCase(target));
    }

    private int parsePage(String[] args, int totalPages) {
        if (args.length == 0) {
            return 1;
        }
        if (args.length != 1) {
            return -1;
        }
        try {
            int page = Integer.parseInt(args[0]);
            return page >= 1 && page <= totalPages ? page : -1;
        }
        catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void sendHeader(Player player, int page, int totalPages) {
        Component previous = page > 1 ? this.pageButton("ignore-list.previous-active", "&b[<]", "ignore-list.previous-hover", "&6Click to go to the previous page", page - 1) : this.configured("ignore-list.previous-inactive", "&7[<]");
        Component next = page < totalPages ? this.pageButton("ignore-list.next-active", "&b[>]", "ignore-list.next-hover", "&6Click to go to the next page", page + 1) : this.configured("ignore-list.next-inactive", "&7[>]");
        String pageCounter = Messages.config("ignore-list.page-counter", " &7{page}/{pages} ").replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(totalPages));
        player.sendMessage(this.configured("ignore-list.title", "&6Ignored players &7").append(previous).append((Component)AMPERSAND.deserialize(pageCounter)).append(next).append(this.configured("ignore-list.suffix", "&7]")));
    }

    private Component pageButton(String labelPath, String labelFallback, String hoverPath, String hoverFallback, int page) {
        return this.configured(labelPath, labelFallback).hoverEvent((HoverEventSource)HoverEvent.showText((Component)this.configured(hoverPath, hoverFallback))).clickEvent(ClickEvent.runCommand((String)("/ignorelist " + page)));
    }

    private Component configured(String path, String fallback) {
        return AMPERSAND.deserialize(Messages.config(path, fallback));
    }
}
