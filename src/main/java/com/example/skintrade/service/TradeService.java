package com.example.skintrade.service;

import com.example.skintrade.model.Platform;
import com.example.skintrade.model.Trade;
import com.example.skintrade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final TradeRepository tradeRepository;
    
    /**
     * Calculate profit/loss between multiple trading platforms and save the trade
     * 
     * @param userId Telegram user ID
     * @param username Telegram username
     * @param platformPrices Map of platform codes to prices
     * @return The calculated trade with profit/loss information
     */
    @Transactional
    public Trade calculateAndSaveTrade(Long userId, String username, Map<String, BigDecimal> platformPrices) {
        // Validate platforms
        validatePlatforms(platformPrices.keySet());
        
        // Calculate best and worst prices
        Map.Entry<String, BigDecimal> bestEntry = null;
        Map.Entry<String, BigDecimal> worstEntry = null;
        
        for (Map.Entry<String, BigDecimal> entry : platformPrices.entrySet()) {
            String platformCode = entry.getKey();
            BigDecimal price = entry.getValue();
            BigDecimal netAmount = Platform.fromCode(platformCode).calculateNetAmount(price);
            
            if (bestEntry == null || netAmount.compareTo(Platform.fromCode(bestEntry.getKey()).calculateNetAmount(bestEntry.getValue())) > 0) {
                bestEntry = entry;
            }
            
            if (worstEntry == null || netAmount.compareTo(Platform.fromCode(worstEntry.getKey()).calculateNetAmount(worstEntry.getValue())) < 0) {
                worstEntry = entry;
            }
        }
        
        // Calculate profit and percentage
        BigDecimal bestNetAmount = Platform.fromCode(bestEntry.getKey()).calculateNetAmount(bestEntry.getValue());
        BigDecimal worstNetAmount = Platform.fromCode(worstEntry.getKey()).calculateNetAmount(worstEntry.getValue());
        BigDecimal profit = bestNetAmount.subtract(worstNetAmount);
        
        // Calculate profit percentage (profit / worst price * 100)
        BigDecimal profitPercentage = profit.multiply(BigDecimal.valueOf(100))
                .divide(worstNetAmount, 2, RoundingMode.HALF_UP);
        
        // Create and save trade
        Trade trade = Trade.builder()
                .userId(userId)
                .username(username)
                .prices(new HashMap<>(platformPrices))
                .bestPlatform(bestEntry.getKey())
                .bestPrice(bestEntry.getValue())
                .worstPlatform(worstEntry.getKey())
                .worstPrice(worstEntry.getValue())
                .profit(profit)
                .profitPercentage(profitPercentage)
                .build();
        
        return tradeRepository.save(trade);
    }
    
    /**
     * Get the most recent trades for a user
     * 
     * @param userId Telegram user ID
     * @return List of the 10 most recent trades
     */
    public List<Trade> getRecentTrades(Long userId, int limit) {
        return tradeRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
    }

    public List<Trade> getRecentTrades(Long userId) {
        return getRecentTrades(userId, 10); // по умолчанию последние 10
    }
    
    /**
     * Validate that all platform codes are valid
     * 
     * @param platformCodes Set of platform codes to validate
     * @throws IllegalArgumentException if any platform code is invalid
     */
    private void validatePlatforms(Set<String> platformCodes) {
        List<String> invalidPlatforms = platformCodes.stream()
                .filter(code -> !Platform.isValidPlatform(code))
                .collect(Collectors.toList());
        
        if (!invalidPlatforms.isEmpty()) {
            throw new IllegalArgumentException("Invalid platforms: " + String.join(", ", invalidPlatforms));
        }
    }
}