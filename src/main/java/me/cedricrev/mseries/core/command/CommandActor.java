package me.cedricrev.mseries.core.command;

import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public final class CommandActor {

    private final CommandSender sender;
    private final Object source;

    private CommandActor(CommandSender sender, Object source) {
        this.sender = sender;
        this.source = source;
    }

    static CommandActor of(CommandSender sender, Object source) {
        return new CommandActor(sender, source);
    }

    public CommandSender sender() {
        return this.sender;
    }

    public Object source() {
        return this.source;
    }

    public Optional<Player> player() {
        return this.sender instanceof Player player ? Optional.of(player) : Optional.empty();
    }

    public boolean isPlayer() {
        return this.sender instanceof Player;
    }

    public boolean isConsole() {
        return this.sender instanceof ConsoleCommandSender;
    }

    public boolean hasPermission(String permission) {
        return this.sender.hasPermission(permission);
    }

    public void reply(String message) {
        this.sender.sendMessage(message);
    }
}
