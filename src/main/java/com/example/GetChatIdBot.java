package com.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetChatIdBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(GetChatIdBot.class);
    private String name;
    private String token;

    public GetChatIdBot() {
        this.name = System.getenv("BOT_NAME");
        this.token = System.getenv("BOT_TOKEN");
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String messageText = "Your chat ID is: " + chatId;
            sendTextMessage(chatId, messageText);
        }
    }

    public void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send message", e);
        }
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new GetChatIdBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
