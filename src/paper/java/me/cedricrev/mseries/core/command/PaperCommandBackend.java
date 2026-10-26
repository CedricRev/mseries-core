package me.cedricrev.mseries.core.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;

final class PaperCommandBackend implements CommandBackend {

    private final PaperCommandManager<CommandActor> manager;

    PaperCommandBackend(JavaPlugin plugin) {
        SenderMapper<CommandSourceStack, CommandActor> mapper = SenderMapper.create(
                stack -> CommandActor.of(stack.getSender(), stack),
                actor -> (CommandSourceStack) actor.source()
        );
        this.manager = PaperCommandManager.builder(mapper)
                .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                .buildOnEnable(plugin);
    }

    @Override
    public CommandManager<CommandActor> manager() {
        return this.manager;
    }
}
