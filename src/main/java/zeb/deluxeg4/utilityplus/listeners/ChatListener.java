package zeb.deluxeg4.utilityplus.listeners;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Iterator;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PlayerChatNames;

public class ChatListener
implements Listener {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private final ChatManager chatManager;

    public ChatListener(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        event.message(PlayerChatNames.decorate(this.highlightMessage(event.message())));
        ChatRenderer originalRenderer = event.renderer();
        event.renderer((source, displayName, message, viewer) -> originalRenderer.render(source, PlayerChatNames.decorate(displayName), message, viewer));
        if (this.chatManager.isGlobalMuted(sender.getUniqueId())) {
            event.setCancelled(true);
            Messages.send((CommandSender)sender, "&6You have toggled off chat");
            return;
        }
        Iterator recipients = event.viewers().iterator();
        while (recipients.hasNext()) {
            Player recipient;
            Audience audience = (Audience)recipients.next();
            if (!(audience instanceof Player) || !this.chatManager.isGlobalMuted((recipient = (Player)audience).getUniqueId()) && !this.chatManager.isIgnoring(recipient.getUniqueId(), sender.getName())) continue;
            recipients.remove();
        }
    }

    private Component highlightMessage(Component message) {
        if (message == null) {
            return Component.empty();
        }
        String plain = PLAIN_TEXT.serialize(message);
        if (!plain.startsWith(">")) {
            return message;
        }
        return Component.text((String)plain, (TextColor)NamedTextColor.GREEN);
    }
}
