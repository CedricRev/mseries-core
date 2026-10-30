package me.cedricrev.mseries.core.menu;

import java.util.Map;
import org.bukkit.entity.Player;

interface FormSubmitter {

    void submit(Player player, Menu menu, Map<String, String> values);
}
