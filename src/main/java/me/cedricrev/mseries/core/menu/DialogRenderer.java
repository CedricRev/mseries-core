package me.cedricrev.mseries.core.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
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
    private final FormSubmitter submitter;
    private final Map<UUID, Map<String, BiConsumer<Player, String>>> pending = new ConcurrentHashMap<>();
    private final CommandSender quietSender;

    DialogRenderer(Plugin plugin, Scheduler scheduler, String callbackCommand, FormSubmitter submitter) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.callbackCommand = callbackCommand;
        this.submitter = submitter;
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
        Map<String, BiConsumer<Player, String>> actions = new ConcurrentHashMap<>();
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
        show(player, dialog, actions);
    }

    void openForm(Player player, Menu menu, Map<String, String> values, Component error) {
        Menu.FormParts form = menu.formParts();
        List<Field> fields = form.fields();
        Map<String, BiConsumer<Player, String>> actions = new ConcurrentHashMap<>();
        JsonObject dialog = new JsonObject();
        dialog.addProperty("type", "minecraft:confirmation");
        dialog.add("title", json(menu.title()));
        dialog.addProperty("can_close_with_escape", true);
        dialog.addProperty("pause", false);
        JsonArray body = new JsonArray();
        if (error != null) {
            body.add(message(error));
        }
        menu.body().ifPresent(text -> body.add(message(text)));
        if (!body.isEmpty()) {
            dialog.add("body", body);
        }
        JsonArray inputs = new JsonArray();
        StringBuilder template = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            String key = "f" + i;
            inputs.add(input(field, key, values.getOrDefault(field.key(), field.initial())));
            if (i > 0) {
                template.append(FormInput.SEPARATOR);
            }
            template.append("$(").append(key).append(')');
        }
        dialog.add("inputs", inputs);
        String submitToken = token();
        actions.put(submitToken, (target, joined) -> {
            Optional<List<String>> parts = FormInput.split(joined, fields.size());
            Map<String, String> answers = new LinkedHashMap<>();
            if (parts.isPresent()) {
                for (int i = 0; i < fields.size(); i++) {
                    Field field = fields.get(i);
                    String answer = parts.get().get(i);
                    answers.put(field.key(), field.type() == Field.Type.CHOICE ? optionId(field, answer) : answer);
                }
            }
            this.submitter.submit(target, menu, answers);
        });
        JsonObject yes = new JsonObject();
        yes.add("label", json(form.submit()));
        JsonObject submit = new JsonObject();
        submit.addProperty("type", "minecraft:dynamic/run_command");
        submit.addProperty("template", "/" + this.callbackCommand + " " + submitToken + " " + template);
        yes.add("action", submit);
        dialog.add("yes", yes);
        Button.Builder no = Button.of(form.cancel());
        menu.onClose().ifPresent(no::action);
        dialog.add("no", action(player, no.build(), actions));
        show(player, dialog, actions);
    }

    void callback(Player player, String token, String values) {
        Map<String, BiConsumer<Player, String>> actions = this.pending.remove(player.getUniqueId());
        if (actions == null) {
            return;
        }
        BiConsumer<Player, String> action = actions.get(token);
        if (action != null) {
            this.scheduler.runFor(player, () -> action.accept(player, values));
        }
    }

    void forget(UUID player) {
        this.pending.remove(player);
    }

    private void show(Player player, JsonObject dialog, Map<String, BiConsumer<Player, String>> actions) {
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

    private static JsonObject input(Field field, String key, String value) {
        JsonObject input = new JsonObject();
        input.addProperty("key", key);
        input.add("label", json(field.label()));
        switch (field.type()) {
            case TEXT, NUMBER -> {
                input.addProperty("type", "minecraft:text");
                input.addProperty("initial", value);
                input.addProperty("max_length", field.type() == Field.Type.TEXT ? Math.max(1, field.max()) : 11);
            }
            case CHOICE -> {
                input.addProperty("type", "minecraft:single_option");
                JsonArray options = new JsonArray();
                int selected = field.optionIndex(value);
                for (int i = 0; i < field.options().size(); i++) {
                    JsonObject option = new JsonObject();
                    option.addProperty("id", Integer.toString(i));
                    option.add("display", json(field.options().get(i).label()));
                    option.addProperty("initial", i == selected);
                    options.add(option);
                }
                input.add("options", options);
            }
            case TOGGLE -> {
                input.addProperty("type", "minecraft:boolean");
                input.addProperty("initial", Boolean.parseBoolean(value));
                input.addProperty("on_true", "true");
                input.addProperty("on_false", "false");
            }
        }
        return input;
    }

    private static String optionId(Field field, String index) {
        try {
            int i = Integer.parseInt(index.strip());
            return i >= 0 && i < field.options().size() ? field.options().get(i).id() : "";
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private static String token() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private JsonObject action(Player player, Button button, Map<String, BiConsumer<Player, String>> actions) {
        JsonObject entry = new JsonObject();
        entry.add("label", json(button.label()));
        if (!button.lore().isEmpty()) {
            entry.add("tooltip", json(Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(),
                    button.lore())));
        }
        String token = token();
        button.action().ifPresent(action -> actions.put(token, (target, values) -> action.accept(target)));
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
