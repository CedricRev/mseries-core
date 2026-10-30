package me.cedricrev.mseries.core.menu;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import me.cedricrev.mseries.core.command.CommandActor;
import me.cedricrev.mseries.core.command.CommandService;
import me.cedricrev.mseries.core.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;

public final class Menus implements Listener, FormSubmitter {

    private final Plugin plugin;
    private final Scheduler scheduler;
    private final ChestRenderer chests;
    private final DialogRenderer dialogs;
    private final FormRenderer forms;
    private final ChatPromptRenderer prompts;

    private Menus(Plugin plugin, Scheduler scheduler, CommandService commands) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.chests = new ChestRenderer(scheduler);
        String callback = plugin.getName().toLowerCase(Locale.ROOT) + "-menu";
        this.dialogs = DialogRenderer.supported() ? new DialogRenderer(plugin, scheduler, callback, this) : null;
        this.forms = plugin.getServer().getPluginManager().isPluginEnabled("floodgate")
                ? new FormRenderer(scheduler, this) : null;
        this.prompts = new ChatPromptRenderer(scheduler, this);
        if (this.dialogs != null) {
            CommandManager<CommandActor> manager = commands.manager();
            manager.command(manager.commandBuilder(callback)
                    .required("token", StringParser.stringParser())
                    .optional("values", StringParser.greedyStringParser())
                    .handler(context -> context.sender().player()
                            .ifPresent(player -> this.dialogs.callback(player, context.get("token"),
                                    context.<String>optional("values").orElse("")))));
        }
    }

    public static Menus create(Plugin plugin, Scheduler scheduler, CommandService commands) {
        Menus menus = new Menus(plugin, scheduler, commands);
        plugin.getServer().getPluginManager().registerEvents(menus.chests, plugin);
        plugin.getServer().getPluginManager().registerEvents(menus.prompts, plugin);
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
            if (built.kind() == Menu.Kind.FORM) {
                openForm(player, built, styleFor(player), Map.of(), null);
                return;
            }
            this.prompts.forget(player.getUniqueId());
            switch (styleFor(player)) {
                case BEDROCK_FORM -> this.forms.open(player, built);
                case JAVA_DIALOG -> this.dialogs.open(player, built);
                case CHEST -> this.chests.open(player, menu, built, refreshTicks);
            }
        });
    }

    public void openChest(Player player, Supplier<Menu> menu, long refreshTicks) {
        this.scheduler.runFor(player, () -> {
            Menu built = menu.get();
            if (built.kind() == Menu.Kind.FORM) {
                openForm(player, built, MenuStyle.CHEST, Map.of(), null);
                return;
            }
            this.prompts.forget(player.getUniqueId());
            this.chests.open(player, menu, built, refreshTicks);
        });
    }

    @Override
    public void submit(Player player, Menu menu, Map<String, String> values) {
        FormInput.Result result = FormInput.parse(menu.fields(), values);
        Menu.FormParts form = menu.formParts();
        if (result.valid()) {
            form.onSubmit().accept(player, result.response());
            return;
        }
        Component error = form.invalid().apply(result.invalid());
        openForm(player, menu, styleFor(player), values, error);
    }

    private void openForm(Player player, Menu menu, MenuStyle style, Map<String, String> values, Component error) {
        this.prompts.forget(player.getUniqueId());
        switch (style) {
            case BEDROCK_FORM -> this.forms.openForm(player, menu, values, error);
            case JAVA_DIALOG -> this.dialogs.openForm(player, menu, values, error);
            case CHEST -> this.prompts.openForm(player, menu, values, error);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (this.dialogs != null) {
            this.dialogs.forget(event.getPlayer().getUniqueId());
        }
        this.prompts.forget(event.getPlayer().getUniqueId());
    }

    public Plugin plugin() {
        return this.plugin;
    }
}
