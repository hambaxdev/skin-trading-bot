package com.example.skintrade.service;

import com.example.skintrade.model.Platform;
import com.example.skintrade.model.Trade;
import com.example.skintrade.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TradeService tradeService;

    private Long userId;
    private String username;
    private Map<String, BigDecimal> platformPrices;

    @BeforeEach
    void setUp() {
        userId = 123456789L;
        username = "testuser";
        platformPrices = new HashMap<>();
        platformPrices.put("steam", new BigDecimal("100.00"));
        platformPrices.put("csm", new BigDecimal("95.00"));
        platformPrices.put("float", new BigDecimal("90.00"));
    }

    @Test
    void calculateAndSaveTrade_shouldCalculateCorrectly() {
        // Given
        Trade expectedTrade = Trade.builder()
                .userId(userId)
                .username(username)
                .prices(platformPrices)
                .bestPlatform("csmar")
                .bestPrice(new BigDecimal("85.00"))
                .worstPlatform("steam")
                .worstPrice(new BigDecimal("100.00"))
                .profit(new BigDecimal("0.15"))
                .profitPercentage(new BigDecimal("0.18"))
                .build();

        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Add csmar with the lowest price but highest net amount due to lowest fee
        platformPrices.put("csmar", new BigDecimal("85.00"));

        // When
        Trade result = tradeService.calculateAndSaveTrade(userId, username, platformPrices);

        // Then
        verify(tradeRepository, times(1)).save(any(Trade.class));

        // Calculate expected values
        BigDecimal steamNet = Platform.STEAM.calculateNetAmount(new BigDecimal("100.00"));
        BigDecimal csmarNet = Platform.CSMAR.calculateNetAmount(new BigDecimal("85.00"));
        BigDecimal expectedProfit = csmarNet.subtract(steamNet);
        BigDecimal expectedPercentage = expectedProfit.multiply(BigDecimal.valueOf(100))
                .divide(steamNet, 2, RoundingMode.HALF_UP);

        // Verify calculations
        assertEquals(userId, result.getUserId());
        assertEquals(username, result.getUsername());
        assertEquals(platformPrices, result.getPrices());
        assertEquals("csmar", result.getBestPlatform());
        assertEquals(new BigDecimal("85.00"), result.getBestPrice());
        assertEquals("steam", result.getWorstPlatform());
        assertEquals(new BigDecimal("100.00"), result.getWorstPrice());
        
        // Compare with calculated values
        assertEquals(0, expectedProfit.compareTo(result.getProfit()));
        assertEquals(0, expectedPercentage.compareTo(result.getProfitPercentage()));
    }

    @Test
    void calculateAndSaveTrade_shouldThrowExceptionForInvalidPlatform() {
        // Given
        platformPrices.put("invalid", new BigDecimal("80.00"));

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tradeService.calculateAndSaveTrade(userId, username, platformPrices);
        });

        assertTrue(exception.getMessage().contains("Invalid platforms: invalid"));
        verify(tradeRepository, never()).save(any(Trade.class));
    }
}