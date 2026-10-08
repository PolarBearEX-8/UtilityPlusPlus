package zeb.deluxeg4.utilityplus.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.Messages;

public class ReloadCommand
implements CommandExecutor {
    private final UtilityPlus plugin;

    public ReloadCommand(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("utilityplus.reload")) {
            Messages.send(sender, "&cYou don't have permission!");
            return true;
        }
        Messages.send(sender, "&eReloading UtilityPlus...");
        this.plugin.getSpawnManager().saveData();
        this.plugin.reloadConfig();
        Messages.copyMessageDefaults();
        this.plugin.getSpawnManager().reload();
        this.plugin.getChatManager().reload();
        this.plugin.getDeathMessageManager().reload();
        this.plugin.getTabListManager().reload();
        this.plugin.getAnnouncementManager().reload();
        Messages.send(sender, "&a&lUtilityPlus reloaded!");
        Messages.send(sender, "&7config.yml &aOK  &7spawn &aOK  &7announcement &aOK");
        return true;
    }
}
