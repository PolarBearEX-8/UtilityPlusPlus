package zeb.deluxeg4.utilityplus.commands;

import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;

public class IgnoreCommand
implements CommandExecutor {
    private final ChatManager chatManager;
    private final boolean hard;
    private final boolean deathMessages;

    public IgnoreCommand(ChatManager chatManager, boolean hard, boolean deathMessages) {
        this.chatManager = chatManager;
        this.hard = hard;
        this.deathMessages = deathMessages;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean enabled;
        String targetName;
        Set<String> existingIgnores;
        if (!(sender instanceof Player)) {
            Messages.send(sender, "&cThis command can only be used by players.");
            return true;
        }
        Player player = (Player)sender;
        if (args.length < 1) {
            Messages.send((CommandSender)player, Messages.config("bad-command", "&4Bad command. Type /help for all commands."));
            return true;
        }
        Player target = Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> onlinePlayer.getName().equalsIgnoreCase(args[0])).findFirst().orElse(null);
        existingIgnores = this.deathMessages ? Set.of() : (this.hard ? this.chatManager.getHardIgnoredPlayers(player.getUniqueId()) : this.chatManager.getIgnoredPlayers(player.getUniqueId()));
        if (target != null) {
            targetName = target.getName();
        } else if (!existingIgnores.isEmpty()) {
            targetName = existingIgnores.stream().filter(name -> name.equalsIgnoreCase(args[0])).findFirst().orElse(null);
            if (targetName == null) {
                Messages.send((CommandSender)player, Messages.config("player-not-online", "&6This player is not online."));
                return true;
            }
        } else {
            Messages.send((CommandSender)player, Messages.config("player-not-online", "&6This player is not online."));
            return true;
        }
        if (targetName.equalsIgnoreCase(player.getName())) {
            Messages.send((CommandSender)player, Messages.config("ignore.self", "&6You can not ignore yourself."));
            return true;
        }
        enabled = this.deathMessages ? this.chatManager.toggleDeathMessageIgnore(player.getUniqueId(), targetName) : (this.hard ? this.chatManager.toggleHardIgnore(player.getUniqueId(), targetName) : this.chatManager.toggleIgnore(player.getUniqueId(), targetName));
        if (this.deathMessages) {
            this.sendConfigured(player, enabled ? "ignore.death-enabled" : "ignore.death-disabled", enabled ? "&6You will no longer see this players death messages." : "&6You will now see this players death messages.", targetName);
        } else if (this.hard) {
            this.sendConfigured(player, enabled ? "ignore.hard-enabled" : "ignore.hard-disabled", enabled ? "&6Permanently ignoring &3{player}.&6 This is saved in &8/ignorelist." : "&6No longer permanently ignoring &3{player}.", targetName, true);
        } else {
            this.sendConfigured(player, enabled ? "ignore.normal-enabled" : "ignore.normal-disabled", enabled ? "&6Now ignoring &3{player}" : "&6No longer ignoring &3{player}.", targetName, true);
        }
        return true;
    }

    private void sendConfigured(Player player, String path, String fallback, String targetName) {
        this.sendConfigured(player, path, fallback, targetName, false);
    }

    private void sendConfigured(Player player, String path, String fallback, String targetName, boolean withoutPlayerNameHover) {
        String message = Messages.config(path, fallback).replace("{player}", targetName);
        if (withoutPlayerNameHover) {
            Messages.sendUndecorated((CommandSender)player, message);
            return;
        }
        Messages.send((CommandSender)player, message);
    }
}
