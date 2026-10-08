package zeb.deluxeg4.utilityplus.commands;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;

public class KillCommand
implements CommandExecutor {
    public static final String SELF_KILL_METADATA = "utilityplus-self-kill";
    private final UtilityPlus plugin;
    private final Set<UUID> pendingConfirmation = ConcurrentHashMap.newKeySet();

    public KillCommand(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("utilityplus.kill")) {
            Messages.send(sender, "&cYou don't have permission!");
            return true;
        }
        if (!(sender instanceof Player)) {
            Messages.send(sender, "&cOnly players can use this command.");
            return true;
        }
        Player player = (Player)sender;
        UUID uuid = player.getUniqueId();
        if (this.pendingConfirmation.remove(uuid)) {
            player.setMetadata(SELF_KILL_METADATA, (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
            player.setHealth(0.0);
            return true;
        }
        this.pendingConfirmation.add(uuid);
        Messages.send((CommandSender)player, "&6Type /kill again to confirm.");
        PaperFoliaTasks.runForPlayerDelayed((Plugin)this.plugin, player, task -> this.pendingConfirmation.remove(uuid), 200L);
        return true;
    }
}
