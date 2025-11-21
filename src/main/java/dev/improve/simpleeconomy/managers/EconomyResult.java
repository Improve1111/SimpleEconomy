package dev.improve.simpleeconomy.managers;

public record EconomyResult(EconomyStatus status, double resultingBalance) {

    public static EconomyResult invalidAmount() {
        return new EconomyResult(EconomyStatus.INVALID_AMOUNT, Double.NaN);
    }

    public boolean success() {
        return status == EconomyStatus.SUCCESS;
    }
}

