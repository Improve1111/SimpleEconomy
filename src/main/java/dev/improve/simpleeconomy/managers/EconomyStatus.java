package dev.improve.simpleeconomy.managers;

public enum EconomyStatus {
    SUCCESS,
    NEGATIVE_AMOUNT,
    INVALID_AMOUNT,
    INSUFFICIENT_FUNDS,
    EXCEEDS_MAX_BALANCE,
    BELOW_MIN_BALANCE,
    SAME_ACCOUNT,
    DATABASE_ERROR
}

