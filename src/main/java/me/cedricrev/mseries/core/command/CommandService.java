package me.cedricrev.mseries.core.command;

import java.lang.reflect.InvocationTargetException;
import me.cedricrev.mseries.core.platform.Platform;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotations.AnnotationParser;

public final class CommandService {

    private static final String PAPER_BACKEND = "PaperCommandBackend";

    private final CommandManager<CommandActor> manager;
    private final AnnotationParser<CommandActor> annotations;

    private CommandService(CommandManager<CommandActor> manager) {
        this.manager = manager;
        this.annotations = new AnnotationParser<>(manager, CommandActor.class);
    }

    public static CommandService create(JavaPlugin plugin) {
        CommandBackend backend = Platform.current().hasPaperCommands()
                ? loadPaperBackend(plugin)
                : new LegacyCommandBackend(plugin);
        return new CommandService(backend.manager());
    }

    private static CommandBackend loadPaperBackend(JavaPlugin plugin) {
        String name = CommandService.class.getPackageName() + "." + PAPER_BACKEND;
        try {
            Class<? extends CommandBackend> type = Class.forName(name, true, CommandService.class.getClassLoader())
                    .asSubclass(CommandBackend.class);
            return type.getDeclaredConstructor(JavaPlugin.class).newInstance(plugin);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Cannot create Paper command manager", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot load Paper command backend " + name, e);
        }
    }

    public CommandService register(Object... containers) {
        for (Object container : containers) {
            this.annotations.parse(container);
        }
        return this;
    }

    public CommandManager<CommandActor> manager() {
        return this.manager;
    }
}
