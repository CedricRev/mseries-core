package me.cedricrev.mseries.core.hook;

import java.math.BigDecimal;
import java.util.UUID;

interface EconomyProvider {

    String api();

    String name();

    boolean enabled();

    String currency(boolean plural);

    String format(BigDecimal amount);

    BigDecimal balance(UUID player);

    boolean has(UUID player, BigDecimal amount);

    EconomyResult withdraw(UUID player, BigDecimal amount);

    EconomyResult deposit(UUID player, BigDecimal amount);
}
