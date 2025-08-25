package com.example.skintrade.bot;

import com.example.skintrade.config.BotConfig;
import com.example.skintrade.model.Platform;
import com.example.skintrade.model.Trade;
import com.example.skintrade.service.TradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SkinTradeBot extends TelegramLongPollingBot {

    private static final String COMMAND_START = "/start";
    private static final String COMMAND_HELP = "/help";
    private static final String COMMAND_TRADE = "/trade";
    private static final String COMMAND_HISTORY = "/history";

    private static final Pattern TRADE_PATTERN = Pattern.compile("([a-zA-Z]+)=([0-9]+(\\.[0-9]+)?)");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BotConfig config;
    private final TradeService tradeService;

    public SkinTradeBot(BotConfig config, TradeService tradeService) {
        super(config.getToken());
        this.config = config;
        this.tradeService = tradeService;
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getFrom().getUserName();

        if (username == null) {
            username = update.getMessage().getFrom().getFirstName();
        }

        try {
            if (messageText.startsWith(COMMAND_START)) {
                sendStartMessage(chatId, update.getMessage().getFrom().getFirstName());
            } else if (messageText.startsWith(COMMAND_HELP)) {
                sendHelpMessage(chatId);
            } else if (messageText.startsWith(COMMAND_TRADE)) {
                processTrade(chatId, username, messageText);
            } else if (messageText.startsWith(COMMAND_HISTORY)) {
                sendTradeHistory(chatId, update.getMessage().getFrom().getId());
            } else {
                sendMessage(chatId, "Unknown command. Type /help for available commands.");
            }
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            sendMessage(chatId, "Error: " + e.getMessage());
        }
    }

    private void sendStartMessage(Long chatId, String firstName) {
        String message = "Hello, " + firstName + "! 👋\n\n" +
                "Welcome to the CS:GO Skin Trade Bot. I can help you calculate profit/loss between different trading platforms.\n\n" +
                "Type /help to see available commands or use the buttons below.";
        sendMessage(chatId, message, true);
    }

    private void sendHelpMessage(Long chatId) {
        String message = "Available commands:\n\n" +
                "/start - Show greeting and usage instructions\n" +
                "/trade site=price site=price ... - Calculate best/worst price, profit, and percentage\n" +
                "  Example: /trade steam=100 csm=95 float=90\n" +
                "  Supported platforms: steam, float, csm, csmm, csmar\n" +
                "/history - Show your last 10 saved trades\n" +
                "/help - Show this help message\n\n" +
                "You can also use the buttons below for quick access to commands.";
        sendMessage(chatId, message, true);
    }

    private void processTrade(Long chatId, String username, String messageText) {
        // Extract platform=price pairs
        String tradeParams = messageText.substring(COMMAND_TRADE.length()).trim();

        if (tradeParams.isEmpty()) {
            sendMessage(chatId, "Please provide at least two platform=price pairs.\n" +
                    "Example: /trade steam=100 csm=95 float=90");
            return;
        }

        Map<String, BigDecimal> platformPrices = new HashMap<>();
        Matcher matcher = TRADE_PATTERN.matcher(tradeParams);

        while (matcher.find()) {
            String platform = matcher.group(1).toLowerCase();
            BigDecimal price = new BigDecimal(matcher.group(2));
            platformPrices.put(platform, price);
        }

        if (platformPrices.size() < 2) {
            sendMessage(chatId, "Please provide at least two valid platform=price pairs.\n" +
                    "Example: /trade steam=100 csm=95 float=90");
            return;
        }

        try {
            // Calculate and save trade
            Trade trade = tradeService.calculateAndSaveTrade(
                    chatId, 
                    username, 
                    platformPrices
            );

            // Format and send result
            String result = formatTradeResult(trade);
            sendMessage(chatId, result);

        } catch (IllegalArgumentException e) {
            sendMessage(chatId, "Error: " + e.getMessage() + "\n" +
                    "Supported platforms: steam, float, csm, csmm, csmar");
        }
    }

    private String formatTradeResult(Trade trade) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Trade Analysis\n\n");

        // Platform prices
        sb.append("Prices:\n");
        for (Map.Entry<String, BigDecimal> entry : trade.getPrices().entrySet()) {
            String platformName = entry.getKey();
            BigDecimal price = entry.getValue();
            BigDecimal netAmount = Platform.fromCode(platformName).calculateNetAmount(price);
            BigDecimal fee = Platform.fromCode(platformName).calculateFee(price);

            sb.append(String.format("• %s: %.2f (fee: %.2f, net: %.2f)\n", 
                    platformName, price, fee, netAmount));
        }

        sb.append("\n");

        // Best platform
        sb.append(String.format("Best platform: %s (%.2f)\n", 
                trade.getBestPlatform(), 
                trade.getBestPrice()));

        // Worst platform
        sb.append(String.format("Worst platform: %s (%.2f)\n", 
                trade.getWorstPlatform(), 
                trade.getWorstPrice()));

        // Profit and percentage
        sb.append(String.format("\nProfit: %.2f (%.2f%%)", 
                trade.getProfit(), trade.getProfitPercentage()));

        return sb.toString();
    }

    private void sendTradeHistory(Long chatId, Long userId) {
        try {
            List<Trade> trades = tradeService.getRecentTrades(userId);

            if (trades == null || trades.isEmpty()) {
                sendMessage(chatId, "You don't have any saved trades yet.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📜 Your last ").append(Math.min(trades.size(), 10)).append(" trades:\n\n");

            for (int i = 0; i < trades.size(); i++) {
                try {
                    Trade trade = trades.get(i);
                    sb.append(i + 1).append(". ");

                    // Format creation date safely
                    if (trade.getCreatedAt() != null) {
                        sb.append(trade.getCreatedAt().format(DATE_FORMATTER));
                    } else {
                        sb.append("Unknown date");
                    }
                    sb.append("\n");

                    // Platforms - handle null prices map
                    if (trade.getPrices() != null && !trade.getPrices().isEmpty()) {
                        List<String> platforms = trade.getPrices().entrySet().stream()
                                .map(e -> String.format("%s=%.2f", e.getKey(), e.getValue()))
                                .collect(Collectors.toList());
                        sb.append("   Platforms: ").append(String.join(", ", platforms)).append("\n");
                    } else {
                        sb.append("   Platforms: None\n");
                    }

                    // Profit - handle null values
                    if (trade.getProfit() != null && trade.getProfitPercentage() != null) {
                        sb.append(String.format("   Profit: %.2f (%.2f%%)\n\n", 
                                trade.getProfit(), trade.getProfitPercentage()));
                    } else {
                        sb.append("   Profit: Unknown\n\n");
                    }
                } catch (Exception e) {
                    log.error("Error formatting trade: {}", e.getMessage(), e);
                    sb.append("   [Error displaying this trade]\n\n");
                }
            }

            sendMessage(chatId, sb.toString());
        } catch (Exception e) {
            log.error("Error retrieving trade history: {}", e.getMessage(), e);
            sendMessage(chatId, "Error retrieving your trade history. Please try again later.");
        }
    }

    private void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, false);
    }

    private void sendMessage(Long chatId, String text, boolean withKeyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        if (withKeyboard) {
            message.setReplyMarkup(createCommandsKeyboard());
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage(), e);
        }
    }

    private ReplyKeyboardMarkup createCommandsKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        keyboardMarkup.setSelective(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // First row with start and help commands
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(COMMAND_START));
        row1.add(new KeyboardButton(COMMAND_HELP));
        keyboard.add(row1);

        // Second row with trade and history commands
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(COMMAND_TRADE + " steam=100 csm=95 float=90"));
        row2.add(new KeyboardButton(COMMAND_HISTORY));
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}
