package me.cedricrev.mseries.core.menu;

import java.util.Locale;
import java.util.function.Supplier;
import me.cedricrev.mseries.core.command.CommandActor;
import me.cedricrev.mseries.core.command.CommandService;
import me.cedricrev.mseries.core.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;

public final class Menus implements Listener {

    private final Plugin plugin;
    private final Scheduler scheduler;
    private final ChestRenderer chests;
    private final DialogRenderer dialogs;
    private final FormRenderer forms;

    private Menus(Plugin plugin, Scheduler scheduler, CommandService commands) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.chests = new ChestRenderer(scheduler);
        String callback = plugin.getName().toLowerCase(Locale.ROOT) + "-menu";
        this.dialogs = DialogRenderer.supported() ? new DialogRenderer(plugin, scheduler, callback) : null;
        this.forms = plugin.getServer().getPluginManager().isPluginEnabled("floodgate")
                ? new FormRenderer(scheduler) : null;
        if (this.dialogs != null) {
            CommandManager<CommandActor> manager = commands.manager();
            manager.command(manager.commandBuilder(callback)
                    .required("token", StringParser.stringParser())
                    .handler(context -> context.sender().player()
                            .ifPresent(player -> this.dialogs.callback(player, context.get("token")))));
        }
    }

    public static Menus create(Plugin plugin, Scheduler scheduler, CommandService commands) {
        Menus menus = new Menus(plugin, scheduler, commands);
        plugin.getServer().getPluginManager().registerEvents(menus.chests, plugin);
        plugin.getServer().getPluginManager().registerEvents(menus, plugin);
        return menus;
    }

    public MenuStyle styleFor(Player player) {
        if (this.forms != null && FormRenderer.isBedrock(player.getUniqueId())) {
            return MenuStyle.BEDROCK_FORM;
        }
        return this.dialogs != null ? MenuStyle.JAVA_DIALOG : MenuStyle.CHEST;
    }

    public void open(Player player, Menu menu) {
        open(player, () -> menu, 0);
    }

    public void open(Player player, Supplier<Menu> menu, long refreshTicks) {
        this.scheduler.runFor(player, () -> {
            Menu built = menu.get();
            switch (styleFor(player)) {
                case BEDROCK_FORM -> this.forms.open(player, built);
                case JAVA_DIALOG -> this.dialogs.open(player, built);
                case CHEST -> this.chests.open(player, menu, built, refreshTicks);
            }
        });
    }

    public void openChest(Player player, Supplier<Menu> menu, long refreshTicks) {
        this.scheduler.runFor(player, () -> this.chests.open(player, menu, menu.get(), refreshTicks));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (this.dialogs != null) {
            this.dialogs.forget(event.getPlayer().getUniqueId());
        }
    }

    public Plugin plugin() {
        return this.plugin;
    }
}
