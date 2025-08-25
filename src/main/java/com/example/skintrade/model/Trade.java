package com.example.skintrade.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "trades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username")
    private String username;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trade_prices", joinColumns = @JoinColumn(name = "trade_id"))
    @MapKeyColumn(name = "platform")
    @Column(name = "price")
    private Map<String, BigDecimal> prices;

    @Column(name = "best_platform")
    private String bestPlatform;

    @Column(name = "best_price")
    private BigDecimal bestPrice;

    @Column(name = "worst_platform")
    private String worstPlatform;

    @Column(name = "worst_price")
    private BigDecimal worstPrice;

    @Column(name = "profit")
    private BigDecimal profit;

    @Column(name = "profit_percentage")
    private BigDecimal profitPercentage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
