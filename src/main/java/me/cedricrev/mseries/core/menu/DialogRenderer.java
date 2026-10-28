package me.cedricrev.mseries.core.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.cedricrev.mseries.core.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class DialogRenderer implements Renderer {

    private static final Pattern VERSION = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private final Plugin plugin;
    private final Scheduler scheduler;
    private final String callbackCommand;
    private final Map<UUID, Map<String, Consumer<Player>>> pending = new ConcurrentHashMap<>();
    private final CommandSender quietSender;

    DialogRenderer(Plugin plugin, Scheduler scheduler, String callbackCommand) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.callbackCommand = callbackCommand;
        this.quietSender = quietSender();
    }

    static boolean supported() {
        Matcher matcher = VERSION.matcher(Bukkit.getBukkitVersion());
        if (!matcher.find()) {
            return false;
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        if (major > 1) {
            return true;
        }
        return minor > 21 || minor == 21 && patch >= 6;
    }

    @Override
    public void open(Player player, Menu menu) {
        Map<String, Consumer<Player>> actions = new ConcurrentHashMap<>();
        JsonObject dialog = new JsonObject();
        dialog.add("title", json(menu.title()));
        JsonArray body = new JsonArray();
        menu.body().ifPresent(text -> body.add(message(text)));
        dialog.addProperty("can_close_with_escape", true);
        dialog.addProperty("pause", false);
        if (menu.kind() == Menu.Kind.CONFIRM) {
            dialog.addProperty("type", "minecraft:confirmation");
            dialog.add("yes", action(player, menu.buttons().get(0), actions));
            dialog.add("no", action(player, menu.buttons().get(1), actions));
        } else {
            dialog.addProperty("type", "minecraft:multi_action");
            JsonArray buttons = new JsonArray();
            for (Button button : menu.buttons()) {
                if (button.chestOnly()) {
                    continue;
                }
                if (button.action().isEmpty()) {
                    body.add(message(button.label()));
                    button.lore().forEach(line -> body.add(message(line)));
                    continue;
                }
                buttons.add(action(player, button, actions));
            }
            dialog.add("actions", buttons);
            dialog.addProperty("columns", buttons.size() > 6 ? 2 : 1);
        }
        if (!body.isEmpty()) {
            dialog.add("body", body);
        }
        this.pending.put(player.getUniqueId(), actions);
        String command = "minecraft:dialog show " + player.getUniqueId() + " " + dialog;
        this.scheduler.run(() -> {
            try {
                Bukkit.dispatchCommand(this.quietSender, command);
            } catch (RuntimeException e) {
                this.plugin.getLogger().log(Level.WARNING, "Could not show a dialog to " + player.getName(), e);
            }
        });
    }

    void callback(Player player, String token) {
        Map<String, Consumer<Player>> actions = this.pending.remove(player.getUniqueId());
        if (actions == null) {
            return;
        }
        Consumer<Player> action = actions.get(token);
        if (action != null) {
            this.scheduler.runFor(player, () -> action.accept(player));
        }
    }

    void forget(UUID player) {
        this.pending.remove(player);
    }

    private JsonObject action(Player player, Button button, Map<String, Consumer<Player>> actions) {
        JsonObject entry = new JsonObject();
        entry.add("label", json(button.label()));
        if (!button.lore().isEmpty()) {
            entry.add("tooltip", json(Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(),
                    button.lore())));
        }
        String token = UUID.randomUUID().toString().substring(0, 8);
        button.action().ifPresent(action -> actions.put(token, action));
        JsonObject click = new JsonObject();
        click.addProperty("type", "minecraft:run_command");
        click.addProperty("command", "/" + this.callbackCommand + " " + token);
        entry.add("action", click);
        return entry;
    }

    private static JsonObject message(Component text) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "minecraft:plain_message");
        message.add("contents", json(text));
        return message;
    }

    private static JsonElement json(Component component) {
        return GsonComponentSerializer.gson().serializeToTree(component);
    }

    private CommandSender quietSender() {
        try {
            Method create = this.plugin.getServer().getClass().getMethod("createCommandSender", Consumer.class);
            Consumer<Object> ignore = feedback -> {
            };
            return (CommandSender) create.invoke(this.plugin.getServer(), ignore);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Bukkit.getConsoleSender();
        }
    }
}
