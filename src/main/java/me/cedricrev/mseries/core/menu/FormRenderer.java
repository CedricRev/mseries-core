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
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

final class FormRenderer implements Renderer {

    private final Scheduler scheduler;

    FormRenderer(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    static boolean isBedrock(UUID player) {
        FloodgateApi api = FloodgateApi.getInstance();
        return api != null && api.isFloodgatePlayer(player);
    }

    @Override
    public void open(Player player, Menu menu) {
        UUID id = player.getUniqueId();
        if (menu.kind() == Menu.Kind.CONFIRM) {
            Button yes = menu.buttons().get(0);
            Button no = menu.buttons().get(1);
            ModalForm.Builder form = ModalForm.builder()
                    .title(Text.bedrock(menu.title()))
                    .content(menu.body().map(Text::bedrock).orElse(""))
                    .button1(Text.bedrock(yes.label()))
                    .button2(Text.bedrock(no.label()))
                    .validResultHandler(response -> run(id, response.clickedFirst() ? yes : no))
                    .closedResultHandler(() -> closed(id, menu));
            FloodgateApi.getInstance().sendForm(id, form);
            return;
        }
        List<String> content = new ArrayList<>();
        menu.body().map(Text::bedrock).ifPresent(content::add);
        List<Button> actions = new ArrayList<>();
        SimpleForm.Builder form = SimpleForm.builder().title(Text.bedrock(menu.title()));
        for (Button button : menu.buttons()) {
            if (button.chestOnly()) {
                continue;
            }
            if (button.action().isEmpty()) {
                content.add(describe(button));
                continue;
            }
            actions.add(button);
            String label = describe(button);
            if (button.image().isPresent()) {
                String image = button.image().get();
                form.button(label, image.startsWith("http") ? FormImage.Type.URL : FormImage.Type.PATH, image);
            } else {
                form.button(label);
            }
        }
        form.content(String.join("\n\n", content))
                .validResultHandler(response -> {
                    int clicked = response.clickedButtonId();
                    if (clicked >= 0 && clicked < actions.size()) {
                        run(id, actions.get(clicked));
                    }
                })
                .closedResultHandler(() -> closed(id, menu));
        FloodgateApi.getInstance().sendForm(id, form);
    }

    private static String describe(Button button) {
        StringBuilder text = new StringBuilder(Text.bedrock(button.label()));
        for (Component line : button.lore()) {
            text.append("\n§r").append(Text.bedrock(line));
        }
        return text.toString();
    }

    private void run(UUID id, Button button) {
        button.action().ifPresent(action -> onPlayer(id, action));
    }

    private void closed(UUID id, Menu menu) {
        menu.onClose().ifPresent(action -> onPlayer(id, action));
    }

    private void onPlayer(UUID id, Consumer<Player> action) {
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            this.scheduler.runFor(player, () -> action.accept(player));
        }
    }
}
