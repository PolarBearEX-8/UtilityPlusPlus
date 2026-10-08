package zeb.deluxeg4.utilityplus.commands;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import zeb.deluxeg4.utilityplus.util.Messages;

public class UptimeCommand
implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("utilityplus.uptime")) {
            Messages.send(sender, "&cYou don't have permission to use this command.");
            return true;
        }
        sender.sendMessage(this.gradient("Server uptime: " + this.formatUptime()));
        return true;
    }

    private Component gradient(String text) {
        TextComponent result;
        int startRed = 110;
        int startGreen = 154;
        int startBlue = 199;
        int endRed = 114;
        int endGreen = 241;
        int endBlue = 204;
        int[] characters = text.codePoints().toArray();
        TextComponent gradient = result = Component.empty();
        for (int index = 0; index < characters.length; ++index) {
            double progress = characters.length <= 1 ? 0.0 : (double)index / (double)(characters.length - 1);
            int red = (int)Math.round(110.0 + 4.0 * progress);
            int green = (int)Math.round(154.0 + 87.0 * progress);
            int blue = (int)Math.round(199.0 + 5.0 * progress);
            gradient = gradient.append(Component.text((String)new String(Character.toChars(characters[index]))).color(TextColor.color((int)red, (int)green, (int)blue)));
        }
        return gradient;
    }

    private String formatUptime() {
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime());
        long days = totalSeconds / 86400L;
        long hours = totalSeconds % 86400L / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder uptime = new StringBuilder();
        if (days > 0L) {
            uptime.append(days).append("d ");
        }
        if (hours > 0L) {
            uptime.append(hours).append("h ");
        }
        if (minutes > 0L) {
            uptime.append(minutes).append("m ");
        }
        uptime.append(seconds).append("s");
        return uptime.toString();
    }
}
