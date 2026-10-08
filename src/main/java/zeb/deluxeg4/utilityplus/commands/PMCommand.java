package zeb.deluxeg4.utilityplus.commands;

import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

public class PMCommand
implements CommandExecutor {
    private final ChatManager chatManager;

    public PMCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Messages.send(sender, "&cThis command can only be used by players!");
            return true;
        }
        Player from = (Player)sender;
        if (label.equalsIgnoreCase("r") || label.equalsIgnoreCase("reply")) {
            return this.handleReply(from, args);
        }
        if (label.equalsIgnoreCase("l") || label.equalsIgnoreCase("last")) {
            return this.handleLast(from, args);
        }
        if (args.length < 2) {
            this.sendBadCommand(from);
            return true;
        }
        Player to = from.getServer().getPlayer(args[0]);
        if (to == null || !to.isOnline()) {
            Messages.send((CommandSender)from, Messages.config("player-not-online", "&6This player is not online."));
            return true;
        }
        this.sendPM(from, to, this.buildMessage(args, 1));
        return true;
    }

    private boolean handleReply(Player from, String[] args) {
        if (args.length < 1) {
            this.sendBadCommand(from);
            return true;
        }
        UUID lastSenderUUID = this.chatManager.getLastPmSender(from.getUniqueId());
        if (lastSenderUUID == null) {
            this.sendBadCommand(from);
            return true;
        }
        Player to = from.getServer().getPlayer(lastSenderUUID);
        if (to == null || !to.isOnline()) {
            Messages.send((CommandSender)from, "&cThat player is no longer online.");
            return true;
        }
        this.sendPM(from, to, this.buildMessage(args, 0));
        return true;
    }

    private boolean handleLast(Player from, String[] args) {
        if (args.length < 1) {
            this.sendBadCommand(from);
            return true;
        }
        UUID lastTargetUUID = this.chatManager.getLastPmTarget(from.getUniqueId());
        if (lastTargetUUID == null) {
            this.sendBadCommand(from);
            return true;
        }
        Player to = from.getServer().getPlayer(lastTargetUUID);
        if (to == null || !to.isOnline()) {
            this.sendBadCommand(from);
            return true;
        }
        this.sendPM(from, to, this.buildMessage(args, 0));
        return true;
    }

    private void sendBadCommand(Player player) {
        Messages.send((CommandSender)player, Messages.config("bad-command", "&4Bad command. Type /help for all commands."));
    }

    private void sendPM(Player from, Player to, String message) {
        String toSender = "&dto " + to.getName() + ": " + message;
        String toTarget = "&d" + from.getName() + " whispers: " + message;
        UtilityPlus plugin = (UtilityPlus)JavaPlugin.getPlugin(UtilityPlus.class);
        Messages.send((CommandSender)from, toSender);
        PaperFoliaTasks.send((Plugin)plugin, to, toTarget);
        this.chatManager.setLastPmSender(to.getUniqueId(), from.getUniqueId());
        this.chatManager.setLastPmSender(from.getUniqueId(), to.getUniqueId());
        this.chatManager.setLastPmTarget(from.getUniqueId(), to.getUniqueId());
        this.chatManager.setLastPmTarget(to.getUniqueId(), from.getUniqueId());
    }

    private String buildMessage(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; ++i) {
            if (i > startIndex) {
                sb.append(" ");
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
