package me.cedricrev.mseries.core.command;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.BukkitCommandManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

final class LegacyCommandBackend implements CommandBackend {

    private final LegacyPaperCommandManager<CommandActor> manager;

    LegacyCommandBackend(JavaPlugin plugin) {
        SenderMapper<CommandSender, CommandActor> mapper = SenderMapper.create(
                sender -> CommandActor.of(sender, sender),
                CommandActor::sender
        );
        this.manager = new LegacyPaperCommandManager<>(plugin, ExecutionCoordinator.simpleCoordinator(), mapper);
        this.enableBrigadier(plugin);
    }

    private void enableBrigadier(JavaPlugin plugin) {
        if (this.manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            try {
                this.manager.registerBrigadier();
                return;
            } catch (BukkitCommandManager.BrigadierInitializationException e) {
                plugin.getLogger().warning("Brigadier unavailable, using standard command registration: " + e.getMessage());
            }
        }
        if (this.manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            this.manager.registerAsynchronousCompletions();
        }
    }

    @Override
    public CommandManager<CommandActor> manager() {
        return this.manager;
    }
}
