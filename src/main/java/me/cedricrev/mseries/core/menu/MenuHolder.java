package me.cedricrev.mseries.core.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import me.cedricrev.mseries.core.scheduler.Task;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class MenuHolder implements InventoryHolder {

    private final Supplier<Menu> source;
    private final Map<Integer, Button> clickable = new HashMap<>();
    private Menu menu;
    private Inventory inventory;
    private int page;
    private Task refresh;

    MenuHolder(Supplier<Menu> source, Menu menu) {
        this.source = source;
        this.menu = menu;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    Supplier<Menu> source() {
        return this.source;
    }

    Menu menu() {
        return this.menu;
    }

    void menu(Menu menu) {
        this.menu = menu;
    }

    Map<Integer, Button> clickable() {
        return this.clickable;
    }

    int page() {
        return this.page;
    }

    void page(int page) {
        this.page = page;
    }

    void refresh(Task refresh) {
        this.refresh = refresh;
    }

    void stopRefresh() {
        if (this.refresh != null) {
            this.refresh.cancel();
            this.refresh = null;
        }
    }
}
