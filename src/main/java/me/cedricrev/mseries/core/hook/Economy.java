package me.cedricrev.mseries.core.hook;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public final class Economy {

    private static final String VAULT_UNLOCKED_API = "net.milkbowl.vault2.economy.Economy";

    private final Plugin plugin;
    private final boolean unlocked;
    private volatile EconomyProvider provider;

    private Economy(Plugin plugin) {
        this.plugin = plugin;
        this.unlocked = classPresent(VAULT_UNLOCKED_API);
    }

    public static Optional<Economy> vault(Plugin plugin) {
        return Hooks.of(plugin).load("Vault", () -> new Economy(plugin));
    }

    public boolean available() {
        return provider() != null;
    }

    public String name() {
        EconomyProvider current = provider();
        return current == null ? "" : current.name();
    }

    public String currency(boolean plural) {
        EconomyProvider current = provider();
        if (current == null) {
            return "";
        }
        try {
            String name = current.currency(plural);
            return name == null ? "" : name;
        } catch (RuntimeException | LinkageError e) {
            return "";
        }
    }

    public String format(BigDecimal amount) {
        EconomyProvider current = provider();
        if (current != null) {
            try {
                String formatted = current.format(amount);
                if (formatted != null) {
                    return formatted;
                }
            } catch (RuntimeException | LinkageError e) {
                warn("format", e);
            }
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    public Optional<BigDecimal> balance(UUID player) {
        EconomyProvider current = provider();
        if (current == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(current.balance(player));
        } catch (RuntimeException | LinkageError e) {
            warn("balance check", e);
            return Optional.empty();
        }
    }

    public boolean has(UUID player, BigDecimal amount) {
        if (amount.signum() <= 0) {
            return true;
        }
        EconomyProvider current = provider();
        if (current == null) {
            return false;
        }
        try {
            return current.has(player, amount);
        } catch (RuntimeException | LinkageError e) {
            warn("balance check", e);
            return false;
        }
    }

    public EconomyResult withdraw(UUID player, BigDecimal amount) {
        if (amount.signum() <= 0) {
            return EconomyResult.skipped();
        }
        EconomyProvider current = provider();
        if (current == null) {
            return EconomyResult.failed("no economy plugin");
        }
        try {
            return current.withdraw(player, amount);
        } catch (RuntimeException | LinkageError e) {
            warn("withdrawal", e);
            return EconomyResult.failed(String.valueOf(e.getMessage()));
        }
    }

    public EconomyResult deposit(UUID player, BigDecimal amount) {
        if (amount.signum() <= 0) {
            return EconomyResult.skipped();
        }
        EconomyProvider current = provider();
        if (current == null) {
            return EconomyResult.failed("no economy plugin");
        }
        try {
            return current.deposit(player, amount);
        } catch (RuntimeException | LinkageError e) {
            warn("deposit", e);
            return EconomyResult.failed(String.valueOf(e.getMessage()));
        }
    }

    private EconomyProvider provider() {
        EconomyProvider current = this.provider;
        if (current != null && enabled(current)) {
            return current;
        }
        Optional<EconomyProvider> found = Optional.empty();
        try {
            if (this.unlocked) {
                found = VaultUnlockedProvider.find(this.plugin);
            }
            if (found.isEmpty()) {
                found = LegacyVaultProvider.find(this.plugin);
            }
        } catch (RuntimeException | LinkageError e) {
            warn("lookup", e);
        }
        this.provider = found.orElse(null);
        found.ifPresent(next -> this.plugin.getLogger().info(
                "Using " + next.name() + " through the " + next.api() + " API for money"));
        return this.provider;
    }

    private static boolean enabled(EconomyProvider provider) {
        try {
            return provider.enabled();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private void warn(String action, Throwable e) {
        this.plugin.getLogger().log(Level.WARNING, "The economy plugin failed a " + action + ": " + e, e);
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, Economy.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
