package me.cedricrev.mseries.core.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import me.cedricrev.mseries.core.scheduler.Scheduler;
import me.cedricrev.mseries.core.text.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

public final class PackForm {

    private record Entry(String marker, String label, String image, Consumer<Player> action) {

        boolean info() {
            return this.action == null;
        }
    }

    private final Scheduler scheduler;
    private final List<String> content = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private String titleMarker = "";
    private String title = "";
    private Consumer<Player> onClose;

    private PackForm(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public static PackForm create(Scheduler scheduler) {
        return new PackForm(scheduler);
    }

    public static boolean available(Player player) {
        return Bukkit.getPluginManager().isPluginEnabled("floodgate") && FormRenderer.isBedrock(player.getUniqueId());
    }

    public static String marker(char group, char item) {
        return "§r§" + group + "§" + item + "§r";
    }

    public PackForm title(String marker, Component title) {
        this.titleMarker = marker == null ? "" : marker;
        this.title = Text.bedrock(title);
        return this;
    }

    public PackForm content(Component text) {
        String line = Text.bedrock(text);
        if (!line.isEmpty()) {
            this.content.add(line);
        }
        return this;
    }

    public PackForm button(String marker, Component label, String image, Consumer<Player> action) {
        this.entries.add(new Entry(marker == null ? "" : marker, Text.bedrock(label), image == null ? "" : image,
                action == null ? player -> { } : action));
        return this;
    }

    public PackForm info(String marker, Component label, String image) {
        this.entries.add(new Entry(marker == null ? "" : marker, Text.bedrock(label), image == null ? "" : image,
                null));
        return this;
    }

    public PackForm onClose(Consumer<Player> action) {
        this.onClose = action;
        return this;
    }

    public void send(Player player, boolean styled) {
        UUID id = player.getUniqueId();
        List<String> body = new ArrayList<>(this.content);
        List<Entry> shown = new ArrayList<>();
        SimpleForm.Builder form = SimpleForm.builder().title((styled ? this.titleMarker : "") + this.title);
        for (Entry entry : this.entries) {
            if (entry.info() && !styled) {
                body.add(entry.label());
                continue;
            }
            shown.add(entry);
            String label = (styled ? entry.marker() : "") + entry.label();
            if (entry.image().isBlank()) {
                form.button(label);
            } else {
                form.button(label, entry.image().startsWith("http") ? FormImage.Type.URL : FormImage.Type.PATH,
                        entry.image());
            }
        }
        form.content(String.join("\n\n", body))
                .validResultHandler(response -> {
                    int clicked = response.clickedButtonId();
                    if (clicked < 0 || clicked >= shown.size()) {
                        return;
                    }
                    Entry entry = shown.get(clicked);
                    onPlayer(id, target -> {
                        if (entry.info()) {
                            send(target, styled);
                        } else {
                            entry.action().accept(target);
                        }
                    });
                })
                .closedOrInvalidResultHandler(() -> {
                    if (this.onClose != null) {
                        onPlayer(id, this.onClose);
                    }
                });
        FloodgateApi.getInstance().sendForm(id, form);
    }

    private void onPlayer(UUID id, Consumer<Player> action) {
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            this.scheduler.runFor(player, () -> action.accept(player));
        }
    }
}
