package me.cedricrev.mseries.core.menu;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.cedricrev.mseries.core.scheduler.Scheduler;
import me.cedricrev.mseries.core.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

final class ChatPromptRenderer implements Listener {

    private static final class Prompt {

        private final Menu menu;
        private final Map<String, String> values;
        private int index;

        private Prompt(Menu menu, Map<String, String> values) {
            this.menu = menu;
            this.values = values;
        }
    }

    private final Scheduler scheduler;
    private final FormSubmitter submitter;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    ChatPromptRenderer(Scheduler scheduler, FormSubmitter submitter) {
        this.scheduler = scheduler;
        this.submitter = submitter;
    }

    void openForm(Player player, Menu menu, Map<String, String> values, Component error) {
        Map<String, String> answers = new LinkedHashMap<>(FormInput.initialValues(menu.fields()));
        answers.putAll(values);
        Prompt prompt = new Prompt(menu, answers);
        this.prompts.put(player.getUniqueId(), prompt);
        player.closeInventory();
        send(player, menu.title());
        if (error != null) {
            send(player, error);
        }
        menu.body().ifPresent(body -> send(player, body));
        send(player, menu.formParts().chatHint());
        ask(player, prompt);
    }

    void forget(UUID player) {
        this.prompts.remove(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Prompt prompt = this.prompts.get(player.getUniqueId());
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        this.scheduler.runFor(player, () -> answer(player, prompt, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        forget(event.getPlayer().getUniqueId());
    }

    private void answer(Player player, Prompt prompt, String message) {
        if (this.prompts.get(player.getUniqueId()) != prompt) {
            return;
        }
        Menu.FormParts form = prompt.menu.formParts();
        String text = message.strip();
        if (text.equalsIgnoreCase(form.cancelWord())) {
            this.prompts.remove(player.getUniqueId());
            prompt.menu.onClose().ifPresent(action -> action.accept(player));
            return;
        }
        Field field = form.fields().get(prompt.index);
        String value = text.isEmpty() ? prompt.values.getOrDefault(field.key(), "") : text;
        if (FormInput.check(field, value).isEmpty()) {
            send(player, form.invalid().apply(field));
            ask(player, prompt);
            return;
        }
        prompt.values.put(field.key(), value);
        prompt.index++;
        if (prompt.index < form.fields().size()) {
            ask(player, prompt);
            return;
        }
        this.prompts.remove(player.getUniqueId());
        this.submitter.submit(player, prompt.menu, prompt.values);
    }

    private void ask(Player player, Prompt prompt) {
        Field field = prompt.menu.fields().get(prompt.index);
        Component line = Component.text("> ", NamedTextColor.GOLD).append(field.label());
        String hint = hint(field);
        if (!hint.isEmpty()) {
            line = line.append(Component.text(" " + hint, NamedTextColor.GRAY));
        }
        String current = prompt.values.getOrDefault(field.key(), "");
        if (!current.isEmpty() && field.type() != Field.Type.TOGGLE) {
            String shown = field.type() == Field.Type.CHOICE ? label(field, current) : current;
            line = line.append(Component.text(" [" + shown + "]", NamedTextColor.DARK_GRAY));
        }
        send(player, line);
    }

    private static String hint(Field field) {
        return switch (field.type()) {
            case TEXT -> "";
            case NUMBER -> field.min() == Integer.MIN_VALUE && field.max() == Integer.MAX_VALUE
                    ? "" : "(" + field.min() + "-" + field.max() + ")";
            case CHOICE -> {
                StringJoiner options = new StringJoiner(", ", "(", ")");
                List<Field.Option> list = field.options();
                list.forEach(option -> options.add(Text.plain(option.label())));
                yield options.toString();
            }
            case TOGGLE -> "(yes/no)";
        };
    }

    private static String label(Field field, String id) {
        for (Field.Option option : field.options()) {
            if (option.id().equals(id)) {
                return Text.plain(option.label());
            }
        }
        return id;
    }

    private static void send(Player player, Component text) {
        player.sendMessage(Text.legacy(text));
    }
}
