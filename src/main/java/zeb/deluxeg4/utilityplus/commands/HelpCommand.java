package zeb.deluxeg4.utilityplus.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import zeb.deluxeg4.utilityplus.UtilityPlus;

public class HelpCommand
implements CommandExecutor {
    private final UtilityPlus plugin;

    public HelpCommand(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            return true;
        }
        sender.sendMessage("");
        Component link = ((TextComponent)Component.text((String)"2b2t-th.org/commands").color((TextColor)NamedTextColor.GOLD)).clickEvent(ClickEvent.openUrl((String)"https://2b2t-th.org/commands"));
        sender.sendMessage(link);
        return true;
    }
}
