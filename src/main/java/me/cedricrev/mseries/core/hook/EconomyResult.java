package me.cedricrev.mseries.core.hook;

import java.math.BigDecimal;

public record EconomyResult(Status status, BigDecimal amount, BigDecimal balance, String error) {

    public enum Status {
        SUCCESS, SKIPPED, FAILED
    }

    public static EconomyResult success(BigDecimal amount, BigDecimal balance) {
        return new EconomyResult(Status.SUCCESS, amount, balance, "");
    }

    public static EconomyResult skipped() {
        return new EconomyResult(Status.SKIPPED, BigDecimal.ZERO, BigDecimal.ZERO, "");
    }

    public static EconomyResult failed(String error) {
        return new EconomyResult(Status.FAILED, BigDecimal.ZERO, BigDecimal.ZERO, error == null ? "" : error);
    }

    public boolean success() {
        return this.status == Status.SUCCESS;
    }

    public boolean failed() {
        return this.status == Status.FAILED;
    }
}
