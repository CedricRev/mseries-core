package me.cedricrev.mseries.core.hook;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

final class VaultUnlockedProvider implements EconomyProvider {

    private final Economy economy;
    private final String caller;

    private VaultUnlockedProvider(Economy economy, String caller) {
        this.economy = economy;
        this.caller = caller;
    }

    static Optional<EconomyProvider> find(Plugin plugin) {
        RegisteredServiceProvider<Economy> registration =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return Optional.empty();
        }
        return Optional.of(new VaultUnlockedProvider(registration.getProvider(), plugin.getName()));
    }

    @Override
    public String api() {
        return "VaultUnlocked";
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
        return plural ? this.economy.defaultCurrencyNamePlural(this.caller)
                : this.economy.defaultCurrencyNameSingular(this.caller);
    }

    @Override
    public String format(BigDecimal amount) {
        return this.economy.format(this.caller, amount);
    }

    @Override
    public BigDecimal balance(UUID player) {
        return this.economy.getBalance(this.caller, player);
    }

    @Override
    public boolean has(UUID player, BigDecimal amount) {
        return this.economy.has(this.caller, player, amount);
    }

    @Override
    public EconomyResult withdraw(UUID player, BigDecimal amount) {
        return result(this.economy.withdraw(this.caller, player, amount));
    }

    @Override
    public EconomyResult deposit(UUID player, BigDecimal amount) {
        return result(this.economy.deposit(this.caller, player, amount));
    }

    private static EconomyResult result(EconomyResponse response) {
        if (response == null) {
            return EconomyResult.failed("no response");
        }
        return response.transactionSuccess()
                ? EconomyResult.success(response.amount, response.balance)
                : EconomyResult.failed(response.errorMessage);
    }
}
