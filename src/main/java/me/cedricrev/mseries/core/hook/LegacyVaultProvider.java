package me.cedricrev.mseries.core.hook;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

final class LegacyVaultProvider implements EconomyProvider {

    private final Plugin plugin;
    private final Economy economy;

    private LegacyVaultProvider(Plugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    static Optional<EconomyProvider> find(Plugin plugin) {
        RegisteredServiceProvider<Economy> registration =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return Optional.empty();
        }
        return Optional.of(new LegacyVaultProvider(plugin, registration.getProvider()));
    }

    @Override
    public String api() {
        return "Vault";
    }

    @Override
    public String name() {
        return this.economy.getName();
    }

    @Override
    public boolean enabled() {
        return this.economy.isEnabled();
    }

    @Override
    public String currency(boolean plural) {
        return plural ? this.economy.currencyNamePlural() : this.economy.currencyNameSingular();
    }

    @Override
    public String format(BigDecimal amount) {
        return this.economy.format(amount.doubleValue());
    }

    @Override
    public BigDecimal balance(UUID player) {
        return BigDecimal.valueOf(this.economy.getBalance(offline(player)));
    }

    @Override
    public boolean has(UUID player, BigDecimal amount) {
        return this.economy.has(offline(player), amount.doubleValue());
    }

    @Override
    public EconomyResult withdraw(UUID player, BigDecimal amount) {
        return result(this.economy.withdrawPlayer(offline(player), amount.doubleValue()));
    }

    @Override
    public EconomyResult deposit(UUID player, BigDecimal amount) {
        return result(this.economy.depositPlayer(offline(player), amount.doubleValue()));
    }

    private OfflinePlayer offline(UUID player) {
        return this.plugin.getServer().getOfflinePlayer(player);
    }

    private static EconomyResult result(EconomyResponse response) {
        if (response == null) {
            return EconomyResult.failed("no response");
        }
        return response.transactionSuccess()
                ? EconomyResult.success(BigDecimal.valueOf(response.amount), BigDecimal.valueOf(response.balance))
                : EconomyResult.failed(response.errorMessage);
    }
}
