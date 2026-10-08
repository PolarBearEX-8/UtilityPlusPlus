package zeb.deluxeg4.utilityplus;

import java.util.List;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import zeb.deluxeg4.utilityplus.commands.BroadcastCommand;
import zeb.deluxeg4.utilityplus.commands.GamemodeCommand;
import zeb.deluxeg4.utilityplus.commands.HelpCommand;
import zeb.deluxeg4.utilityplus.commands.IgnoreCommand;
import zeb.deluxeg4.utilityplus.commands.IgnoreListCommand;
import zeb.deluxeg4.utilityplus.commands.InventorySeeCommand;
import zeb.deluxeg4.utilityplus.commands.KillCommand;
import zeb.deluxeg4.utilityplus.commands.OfflineTpCommand;
import zeb.deluxeg4.utilityplus.commands.OverclockCommand;
import zeb.deluxeg4.utilityplus.commands.PMCommand;
import zeb.deluxeg4.utilityplus.commands.PingCommand;
import zeb.deluxeg4.utilityplus.commands.ReloadCommand;
import zeb.deluxeg4.utilityplus.commands.STapwarp;
import zeb.deluxeg4.utilityplus.commands.StopNowCommand;
import zeb.deluxeg4.utilityplus.commands.TPSMoreCommand;
import zeb.deluxeg4.utilityplus.commands.ToggleChatCommand;
import zeb.deluxeg4.utilityplus.commands.ToggleDeathMessagesCommand;
import zeb.deluxeg4.utilityplus.commands.TogglePrivateMessagesCommand;
import zeb.deluxeg4.utilityplus.commands.UptimeCommand;
import zeb.deluxeg4.utilityplus.commands.VanishCommand;
import zeb.deluxeg4.utilityplus.invsee.InventorySeeMode;
import zeb.deluxeg4.utilityplus.invsee.InventorySeeSessionManager;
import zeb.deluxeg4.utilityplus.invsee.PendingInventoryOrderManager;
import zeb.deluxeg4.utilityplus.listeners.AnvilListener;
import zeb.deluxeg4.utilityplus.listeners.ChatListener;
import zeb.deluxeg4.utilityplus.listeners.DeathMessageListener;
import zeb.deluxeg4.utilityplus.listeners.InventorySeeListener;
import zeb.deluxeg4.utilityplus.listeners.JoinMessageListener;
import zeb.deluxeg4.utilityplus.listeners.PendingInventoryOrderListener;
import zeb.deluxeg4.utilityplus.listeners.SpawnListener;
import zeb.deluxeg4.utilityplus.listeners.TabListListener;
import zeb.deluxeg4.utilityplus.listeners.VanishListener;
import zeb.deluxeg4.utilityplus.managers.AnnouncementManager;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.managers.CpuMonitor;
import zeb.deluxeg4.utilityplus.managers.DeathMessageManager;
import zeb.deluxeg4.utilityplus.managers.SpawnManager;
import zeb.deluxeg4.utilityplus.managers.TabListManager;
import zeb.deluxeg4.utilityplus.managers.TickMonitor;
import zeb.deluxeg4.utilityplus.tabcomplete.TabCompleterManager;
import zeb.deluxeg4.utilityplus.util.Messages;
import zeb.deluxeg4.utilityplus.util.PlayerChatNames;

public class UtilityPlus
extends JavaPlugin {
    private SpawnManager spawnManager;
    private ChatManager chatManager;
    private DeathMessageManager deathMessageManager;
    private TabListManager tabListManager;
    private AnnouncementManager announcementManager;
    private VanishCommand vanishCommand;
    private TickMonitor tickMonitor;
    private CpuMonitor cpuMonitor;
    private PendingInventoryOrderManager pendingInventoryOrderManager;
    private InventorySeeSessionManager inventorySeeSessionManager;
    private InventorySeeSessionManager enderChestSeeSessionManager;

    public void onEnable() {
        this.saveDefaultConfig();
        Messages.initialize(this);
        this.spawnManager = new SpawnManager(this);
        this.chatManager = new ChatManager(this);
        this.deathMessageManager = new DeathMessageManager(this);
        this.tabListManager = new TabListManager(this);
        this.announcementManager = new AnnouncementManager(this);
        this.tickMonitor = new TickMonitor(this);
        this.cpuMonitor = new CpuMonitor(this);
        this.pendingInventoryOrderManager = new PendingInventoryOrderManager(this);
        this.inventorySeeSessionManager = new InventorySeeSessionManager(this, InventorySeeMode.INVENTORY, this.pendingInventoryOrderManager);
        this.enderChestSeeSessionManager = new InventorySeeSessionManager(this, InventorySeeMode.ENDER_CHEST, this.pendingInventoryOrderManager);
        this.registerCommand("ignore", new IgnoreCommand(this.chatManager, false, false));
        this.registerCommand("ignorehard", new IgnoreCommand(this.chatManager, true, false));
        this.registerCommand("ignoredeathmsgs", new IgnoreCommand(this.chatManager, true, true));
        this.registerCommand("ignorelist", new IgnoreListCommand(this.chatManager));
        this.registerCommand("togglechat", new ToggleChatCommand(this.chatManager));
        this.registerCommand("toggleprivatemsgs", new TogglePrivateMessagesCommand(this.chatManager));
        this.registerCommand("toggledeathmsgs", new ToggleDeathMessagesCommand(this.chatManager, false));
        this.registerCommand("toggledeathmsgshard", new ToggleDeathMessagesCommand(this.chatManager, true));
        PMCommand privateMessageCommand = new PMCommand(this.chatManager);
        this.registerCommands(privateMessageCommand, "tell", "msg", "w", "whisper", "pm", "r", "reply", "l", "last");
        this.registerCommand("upreload", new ReloadCommand(this));
        this.vanishCommand = new VanishCommand(this);
        this.registerCommand("v", this.vanishCommand);
        BroadcastCommand broadcastCommand = new BroadcastCommand(this);
        this.registerCommands(broadcastCommand, "bc", "broadcast");
        GamemodeCommand gamemodeCommand = new GamemodeCommand();
        this.registerCommands(gamemodeCommand, "gmc", "gms", "gmsp", "gma");
        this.registerCommand("kill", new KillCommand(this));
        this.registerCommand("stopnow", new StopNowCommand(this));
        OverclockCommand overclockCommand = new OverclockCommand(this);
        this.registerCommand("overclock", (CommandExecutor)overclockCommand);
        InventorySeeCommand inventorySeeCommand = new InventorySeeCommand(this);
        this.registerCommands(inventorySeeCommand, "invsee", "enderchestsee");
        this.registerCommand("offlinetp", new OfflineTpCommand(this));
        this.registerCommand("s", new STapwarp());
        this.registerCommand("help", new HelpCommand(this));
        TPSMoreCommand tpsMoreCommand = new TPSMoreCommand(this.tickMonitor, this.cpuMonitor);
        this.registerCommands(tpsMoreCommand, "tpsmore", "tps");
        PingCommand pingCommand = new PingCommand();
        this.registerCommands(pingCommand, "ping", "pingall");
        this.registerCommand("uptime", new UptimeCommand());
        TabCompleterManager tabCompleter = new TabCompleterManager();
        List<String> commandNames = List.of("ignore", "ignorehard", "ignorelist", "ignoredeathmsgs", "togglechat", "toggleprivatemsgs", "toggledeathmsgs", "toggledeathmsgshard", "tell", "msg", "w", "whisper", "pm", "r", "reply", "l", "last", "upreload", "stopnow", "v", "bc", "broadcast", "gmc", "gms", "gmsp", "gma", "kill", "overclock", "invsee", "enderchestsee", "offlinetp", "s", "help", "tpsmore", "tps", "ping", "pingall", "uptime");
        this.registerTabCompleters(tabCompleter, commandNames);
        this.command("overclock").setTabCompleter((TabCompleter)overclockCommand);
        this.getServer().getPluginManager().registerEvents((Listener)new PlayerChatNames(), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new SpawnListener(this.spawnManager), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ChatListener(this.chatManager), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new AnvilListener(), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new JoinMessageListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new DeathMessageListener(this.chatManager, this.deathMessageManager, this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new TabListListener((Plugin)this, this.tabListManager), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new VanishListener(this, this.vanishCommand), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new InventorySeeListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new PendingInventoryOrderListener(this), (Plugin)this);
        this.getLogger().info("UtilityPlus enabled!");
    }

    public void onDisable() {
        if (this.spawnManager != null) {
            this.spawnManager.saveData();
        }
        if (this.chatManager != null) {
            this.chatManager.saveData();
        }
        if (this.tabListManager != null) {
            this.tabListManager.stop();
        }
        if (this.announcementManager != null) {
            this.announcementManager.stop();
        }
        if (this.vanishCommand != null) {
            this.vanishCommand.saveData();
        }
        if (this.inventorySeeSessionManager != null) {
            this.inventorySeeSessionManager.closeAll();
        }
        if (this.enderChestSeeSessionManager != null) {
            this.enderChestSeeSessionManager.closeAll();
        }
        this.getLogger().info("UtilityPlus disabled!");
    }

    public SpawnManager getSpawnManager() {
        return this.spawnManager;
    }

    public ChatManager getChatManager() {
        return this.chatManager;
    }

    public DeathMessageManager getDeathMessageManager() {
        return this.deathMessageManager;
    }

    public TabListManager getTabListManager() {
        return this.tabListManager;
    }

    public AnnouncementManager getAnnouncementManager() {
        return this.announcementManager;
    }

    public PendingInventoryOrderManager getPendingInventoryOrderManager() {
        return this.pendingInventoryOrderManager;
    }

    public InventorySeeSessionManager getInventorySeeSessionManager() {
        return this.inventorySeeSessionManager;
    }

    public InventorySeeSessionManager getEnderChestSeeSessionManager() {
        return this.enderChestSeeSessionManager;
    }

    private void registerCommands(CommandExecutor executor, String ... names) {
        for (String name : names) {
            this.registerCommand(name, executor);
        }
    }

    private void registerCommand(String name, CommandExecutor executor) {
        this.command(name).setExecutor(executor);
    }

    private void registerTabCompleters(TabCompleter completer, List<String> names) {
        for (String name : names) {
            this.command(name).setTabCompleter(completer);
        }
    }

    private PluginCommand command(String name) {
        PluginCommand command = this.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command '/" + name + "' is missing from plugin.yml");
        }
        return command;
    }
}
