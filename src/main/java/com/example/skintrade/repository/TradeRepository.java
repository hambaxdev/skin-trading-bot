package com.example.skintrade.repository;

import com.example.skintrade.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    
    /**
     * Find the most recent trades for a specific user
     * @param userId the Telegram user ID
     * @param limit the maximum number of trades to return
     * @return a list of trades ordered by creation date (newest first)
     */
    List<Trade> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find the most recent trades for a specific user
     * @param userId the Telegram user ID
     * @return a list of trades ordered by creation date (newest first)
     */
    List<Trade> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}