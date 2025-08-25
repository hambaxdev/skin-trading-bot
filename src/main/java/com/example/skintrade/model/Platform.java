package com.example.skintrade.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum Platform {
    STEAM("steam", new BigDecimal("0.15")),
    FLOAT("float", new BigDecimal("0.10")),
    CSM("csm", new BigDecimal("0.05")),
    CSMM("csmm", new BigDecimal("0.02")),
    CSMAR("csmar", new BigDecimal("0.01"));

    private final String code;
    private final BigDecimal feeRate;

    private static final Map<String, Platform> BY_CODE = 
            Arrays.stream(values())
                  .collect(Collectors.toMap(Platform::getCode, Function.identity()));

    Platform(String code, BigDecimal feeRate) {
        this.code = code;
        this.feeRate = feeRate;
    }

    public static Platform fromCode(String code) {
        Platform platform = BY_CODE.get(code.toLowerCase());
        if (platform == null) {
            throw new IllegalArgumentException("Unknown platform: " + code);
        }
        return platform;
    }

    public static boolean isValidPlatform(String code) {
        return BY_CODE.containsKey(code.toLowerCase());
    }

    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(feeRate);
    }

    public BigDecimal calculateNetAmount(BigDecimal amount) {
        return amount.subtract(calculateFee(amount));
    }
}