package me.cedricrev.mseries.core.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import me.cedricrev.mseries.core.scheduler.Scheduler;
import me.cedricrev.mseries.core.text.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class ChestRenderer implements Renderer, Listener {

    private static final int PAGE_SIZE = 45;
    private static final int PREVIOUS_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int CONFIRM_BODY_SLOT = 4;
    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_NO_SLOT = 15;

    private final Scheduler scheduler;

    ChestRenderer(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void open(Player player, Menu menu) {
        open(player, () -> menu, menu, 0);
    }

    void open(Player player, Supplier<Menu> source, Menu menu, long refreshTicks) {
        MenuHolder holder = new MenuHolder(source, menu);
        Inventory inventory = Bukkit.createInventory(holder, size(menu), Text.legacy(menu.title()));
        holder.inventory(inventory);
        fill(holder);
        player.openInventory(inventory);
        if (refreshTicks > 0) {
            holder.refresh(this.scheduler.runForTimer(player, () -> refresh(player, holder), refreshTicks, refreshTicks));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0 || event.getRawSlot() >= holder.getInventory().getSize()) {
            return;
        }
        Button button = holder.clickable().get(event.getRawSlot());
        if (button != null) {
            button.action().ifPresent(action -> this.scheduler.runFor(player, () -> action.accept(player)));
        } else if (event.getRawSlot() == PREVIOUS_SLOT && holder.page() > 0) {
            holder.page(holder.page() - 1);
            fill(holder);
        } else if (event.getRawSlot() == NEXT_SLOT && hasNextPage(holder)) {
            holder.page(holder.page() + 1);
            fill(holder);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder holder
                && event.getPlayer() instanceof Player player) {
            holder.stopRefresh();
            holder.menu().onClose().ifPresent(action -> action.accept(player));
        }
    }

    private void refresh(Player player, MenuHolder holder) {
        if (!player.isOnline() || player.getOpenInventory().getTopInventory().getHolder() != holder) {
            holder.stopRefresh();
            return;
        }
        holder.menu(holder.source().get());
        fill(holder);
    }

    private void fill(MenuHolder holder) {
        Inventory inventory = holder.getInventory();
        Menu menu = holder.menu();
        inventory.clear();
        holder.clickable().clear();
        if (menu.kind() == Menu.Kind.CONFIRM) {
            menu.body().ifPresent(body -> inventory.setItem(CONFIRM_BODY_SLOT, item(Material.PAPER, menu.title(),
                    List.of(body))));
            place(holder, CONFIRM_YES_SLOT, menu.buttons().get(0));
            place(holder, CONFIRM_NO_SLOT, menu.buttons().get(1));
        } else {
            List<Button> loose = new ArrayList<>();
            for (Button button : menu.buttons()) {
                if (button.slot() >= 0 && button.slot() < inventory.getSize()) {
                    place(holder, button.slot(), button);
                } else {
                    loose.add(button);
                }
            }
            if (paged(menu)) {
                int from = holder.page() * PAGE_SIZE;
                for (int i = from; i < Math.min(loose.size(), from + PAGE_SIZE); i++) {
                    place(holder, i - from, loose.get(i));
                }
                if (holder.page() > 0) {
                    inventory.setItem(PREVIOUS_SLOT, item(Material.ARROW, menu.previousPage(), List.of()));
                }
                if (hasNextPage(holder)) {
                    inventory.setItem(NEXT_SLOT, item(Material.ARROW, menu.nextPage(), List.of()));
                }
            } else {
                int slot = 0;
                for (Button button : loose) {
                    while (slot < inventory.getSize() && inventory.getItem(slot) != null) {
                        slot++;
                    }
                    if (slot >= inventory.getSize()) {
                        break;
                    }
                    place(holder, slot, button);
                }
            }
        }
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void place(MenuHolder holder, int slot, Button button) {
        ItemStack stack = button.item().map(Supplier::get).orElseGet(() -> new ItemStack(button.icon()));
        holder.getInventory().setItem(slot, decorate(stack, button.label(), button.lore()));
        if (button.action().isPresent()) {
            holder.clickable().put(slot, button);
        }
    }

    private static boolean paged(Menu menu) {
        long loose = menu.buttons().stream().filter(button -> button.slot() < 0).count();
        boolean placed = menu.buttons().stream().anyMatch(button -> button.slot() >= 0);
        return !placed && loose > 54;
    }

    private static boolean hasNextPage(MenuHolder holder) {
        return paged(holder.menu()) && (holder.page() + 1) * PAGE_SIZE < holder.menu().buttons().size();
    }

    private static int size(Menu menu) {
        if (menu.kind() == Menu.Kind.CONFIRM) {
            return 27;
        }
        if (menu.rows() > 0) {
            return menu.rows() * 9;
        }
        if (paged(menu)) {
            return 54;
        }
        int highest = menu.buttons().stream().mapToInt(Button::slot).max().orElse(-1);
        int needed = Math.max(highest + 1, menu.buttons().size());
        int rows = Math.max(1, Math.min(6, (needed + 8) / 9));
        return rows * 9;
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        return decorate(new ItemStack(material), name, lore);
    }

    private static ItemStack decorate(ItemStack stack, Component name, List<Component> lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName("§r" + Text.legacy(name));
        List<String> lines = new ArrayList<>();
        for (Component line : lore) {
            for (String part : Text.legacy(line).split("\n", -1)) {
                lines.add("§r" + part);
            }
        }
        meta.setLore(lines);
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }
}
