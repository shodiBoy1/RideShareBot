package com.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class RideShareBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(RideShareBot.class);

    private String name;
    private String token;
    private final String channelUsername = "@ride_share_de";
    private final String botUsername = "@TakeWithBot";

    private ThreadLocal<Update> updateEvent = new ThreadLocal<>();
    private Map<Long, String> userStates = new HashMap<>();
    private Map<Long, RideDetails> rideDetailsMap = new HashMap<>();

    public RideShareBot() {
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
    public void clearWebhook() {
        try {
            execute(new DeleteWebhook());
        } catch (TelegramApiException e) {
            logger.warn("No webhook to clear or failed to clear webhook: " + e.getMessage());
        }
    }

    @Override
    public final void onUpdateReceived(Update updateEvent) {
        this.updateEvent.set(updateEvent);
        onUpdateEventReceived(this.updateEvent.get());
    }

    public void onUpdateEventReceived(Update updateEvent) {
        if (updateEvent.hasMessage() && updateEvent.getMessage().hasText()) {
            handleIncomingMessage(updateEvent);
        } else if (updateEvent.hasCallbackQuery()) {
            handleCallbackQuery(updateEvent);
        }
    }

    private void handleIncomingMessage(Update update) {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if (messageText.equals("/start")) {
            sendWelcomeMessage(chatId);
        } else if (messageText.equals("/disclaimer")) {
            sendDisclaimer(chatId);
        } else {
            handleUserResponse(chatId, messageText);
        }
    }


    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "start_publication":
                sendTextMessageAsync(chatId, "Do you need a ride or offer a ride?", getRideOptions());
                userStates.put(chatId, "ride_option");
                break;
            case "go_to_channel":
                sendTextMessageAsync(chatId, "Redirecting to the channel...");
                openChannel(chatId);
                break;
            case "need_ride":
                RideDetails passengerDetails = new RideDetails();
                passengerDetails.setType("passenger");
                rideDetailsMap.put(chatId, passengerDetails);
                askForRideDetails(chatId);
                userStates.put(chatId, "need_ride_from_city");
                break;
            case "offer_ride":
                RideDetails driverDetails = new RideDetails();
                driverDetails.setType("driver");
                rideDetailsMap.put(chatId, driverDetails);
                askForOfferDetails(chatId);
                userStates.put(chatId, "offer_ride_from_city");
                break;
        }
    }

    private void handleUserResponse(Long chatId, String messageText) {
        String state = userStates.get(chatId);
        if (state == null) return;

        RideDetails details = rideDetailsMap.getOrDefault(chatId, new RideDetails());

        try {
            switch (state) {
                case "ride_option":
                    if (messageText.equalsIgnoreCase("need a ride")) {
                        askForRideDetails(chatId);
                        userStates.put(chatId, "need_ride_from_city");
                    } else if (messageText.equalsIgnoreCase("offer a ride")) {
                        askForOfferDetails(chatId);
                        userStates.put(chatId, "offer_ride_from_city");
                    }
                    break;
                case "need_ride_from_city":
                    details.setCityA(messageText);
                    sendTextMessageAsync(chatId, "To which city are you going?");
                    userStates.put(chatId, "need_ride_to_city");
                    break;
                case "need_ride_to_city":
                    details.setCityB(messageText);
                    sendTextMessageAsync(chatId, "How many persons?");
                    userStates.put(chatId, "need_ride_persons");
                    break;
                case "need_ride_persons":
                    int numberOfPersons = Integer.parseInt(messageText);
                    details.setNumberOfPersons(numberOfPersons);
                    sendTextMessageAsync(chatId, "What is your contact information?");
                    userStates.put(chatId, "need_ride_contact_info");
                    break;
                case "need_ride_contact_info":
                    if (isValidContactInfo(messageText)) {
                        details.setContactInfo(messageText);
                        sendTextMessageAsync(chatId, "Thank you! Your ride request will be posted in the channel.");
                        sendRideDetailsToChannel(details);
                        userStates.remove(chatId);
                        rideDetailsMap.put(chatId, details);
                    } else {
                        sendTextMessageAsync(chatId, "Please provide a valid contact (Telegram username starting with '@' or phone number).");
                    }
                    break;
                case "offer_ride_from_city":
                    details.setCityA(messageText);
                    sendTextMessageAsync(chatId, "To which city are you going?");
                    userStates.put(chatId, "offer_ride_to_city");
                    break;
                case "offer_ride_to_city":
                    details.setCityB(messageText);
                    sendTextMessageAsync(chatId, "What is the date of the ride?");
                    userStates.put(chatId, "offer_ride_date");
                    break;
                case "offer_ride_date":
                    details.setDateTime(messageText);
                    sendTextMessageAsync(chatId, "What time will you start the ride?");
                    userStates.put(chatId, "offer_ride_time");
                    break;
                case "offer_ride_time":
                    details.setDateTime(details.getDateTime() + " " + messageText);
                    sendTextMessageAsync(chatId, "How many persons can you take?");
                    userStates.put(chatId, "offer_ride_persons");
                    break;
                case "offer_ride_persons":
                    int offerRidePersons = Integer.parseInt(messageText);
                    details.setNumberOfPersons(offerRidePersons);
                    sendTextMessageAsync(chatId, "What is the price per person?");
                    userStates.put(chatId, "offer_ride_price");
                    break;
                case "offer_ride_price":
                    double price = Double.parseDouble(messageText);
                    details.setPricePerPerson(price);
                    sendTextMessageAsync(chatId, "What is the car model?");
                    userStates.put(chatId, "offer_ride_car_model");
                    break;
                case "offer_ride_car_model":
                    details.setCarModel(messageText);
                    sendTextMessageAsync(chatId, "What is your contact information? (Please provide your Telegram @username or phone number)");
                    userStates.put(chatId, "offer_ride_contact_info");
                    break;
                case "offer_ride_contact_info":
                    if (isValidContactInfo(messageText)) {
                        details.setContactInfo(messageText);
                        sendTextMessageAsync(chatId, "Thank you for your offer! It's posted in our channel.");
                        sendRideDetailsToChannel(details);
                        userStates.remove(chatId);
                        rideDetailsMap.put(chatId, details);
                    } else {
                        sendTextMessageAsync(chatId, "Please provide a valid contact (Telegram username starting with '@' or phone number).");
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            sendTextMessageAsync(chatId, "Please enter a valid number.");
        }

        rideDetailsMap.put(chatId, details);
    }

    private void sendWelcomeMessage(Long chatId) {
        String welcomeText = "Welcome to RideShare Bot! You can use this bot to find or offer rides. Please note: By using this bot, you agree to our terms. We are not responsible for any actions or incidents that occur. Use at your own risk. Type /disclaimer for more info.";
        sendTextMessageAsync(chatId, welcomeText, getStartOptions());
    }

    private Map<String, String> getStartOptions() {
        Map<String, String> options = new HashMap<>();
        options.put("Find a ride", "go_to_channel");
        options.put("Give a publication", "start_publication");
        return options;
    }

    private Map<String, String> getRideOptions() {
        Map<String, String> options = new HashMap<>();
        options.put("Need a ride", "need_ride");
        options.put("Offer a ride", "offer_ride");
        return options;
    }

    private void askForRideDetails(Long chatId) {
        String text = "From which city are you going?";
        sendTextMessageAsync(chatId, text);
    }

    private void askForOfferDetails(Long chatId) {
        String text = "From which city are you going?";
        sendTextMessageAsync(chatId, text);
    }

    private void sendRideDetailsToChannel(RideDetails details) {
        String messageText;
        if (details.getType().equals("driver")) {
            String driver = "🕵🏼 I am a #Driver";
            String fromCity = "🏢 From: #";
            String toCity = "🏠 To: #";
            String departureDate = "📅 Departure Date: ";
            String departureTime = "⏰ Departure Time: ";
            String numberOfPersons = "🙋🏻‍♂️ Number of Persons: ";
            String price = "💵 Price: ";
            String carModel = "🚙 Car Model: ";
            String contactInfo = "📱 Contact Info: ";

            messageText = driver + "\n" +
                    fromCity + details.getCityA() + "\n" +
                    toCity + details.getCityB() + "\n" +
                    departureDate + details.getDateTime().split(" ")[0] + "\n" +
                    departureTime + details.getDateTime().split(" ")[1] + "\n" +
                    numberOfPersons + details.getNumberOfPersons() + "\n" +
                    price + details.getPricePerPerson() + " euro\n" +
                    carModel + details.getCarModel() + "\n" +
                    contactInfo + details.getContactInfo() + "\n" +
                    botUsername;
        } else {
            String passenger = "🙋🏻‍♂️ I am a #Passenger";
            String fromCity = "🏢 From: #";
            String toCity = "🏠 To: #";
            String numberOfPersons = "🙋🏻‍♂️ Number of Persons: ";
            String contactInfo = "📱 Contact Info: ";

            messageText = passenger + "\n" +
                    fromCity + details.getCityA() + "\n" +
                    toCity + details.getCityB() + "\n" +
                    numberOfPersons + details.getNumberOfPersons() + "\n" +
                    contactInfo + details.getContactInfo() + "\n" +
                    botUsername;
        }

        sendTextMessageToChannel(messageText);
    }

    private void sendTextMessageToChannel(String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(channelUsername);
            message.setText(new String(text.getBytes(), StandardCharsets.UTF_8));
            execute(message);
            logger.info("Sent message to channel: {}", message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send message to channel", e);
        }
    }

    private void openChannel(Long chatId) {
        try {
            String url = "https://t.me/" + channelUsername.substring(1);
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Go to Channel");
            button.setUrl(url);
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(Collections.singletonList(button));
            markup.setKeyboard(keyboard);

            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Click the button below to go to the channel:");
            message.setReplyMarkup(markup);

            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send channel link", e);
        }
    }

    public void sendTextMessageAsync(Long chatId, String text) {
        try {
            SendMessage message = createMessage(chatId.toString(), text);
            execute(message);
            logger.info("Sent message: {}", message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send message", e);
        }
    }

    public void sendTextMessageAsync(Long chatId, String text, Map<String, String> buttons) {
        try {
            SendMessage message = createMessage(chatId.toString(), text, buttons);
            execute(message);
            logger.info("Sent message: {}", message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send message", e);
        }
    }

    public SendMessage createMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setText(new String(text.getBytes(), StandardCharsets.UTF_8));
        message.setParseMode("markdown");
        message.setChatId(chatId);
        return message;
    }

    public SendMessage createMessage(String chatId, String text, Map<String, String> buttons) {
        SendMessage message = createMessage(chatId, text);
        if (buttons != null && !buttons.isEmpty())
            attachButtons(message, buttons);
        return message;
    }

    private void attachButtons(SendMessage message, Map<String, String> buttons) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Map.Entry<String, String> entry : buttons.entrySet()) {
            String buttonName = entry.getKey();
            String buttonValue = entry.getValue();

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(new String(buttonName.getBytes(), StandardCharsets.UTF_8));
            button.setCallbackData(buttonValue);

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
    }

    private boolean isValidContactInfo(String contactInfo) {
        return contactInfo.startsWith("@") || contactInfo.matches("\\+?[0-9]{10,13}");
    }

    private void sendDisclaimer(Long chatId) {
        String disclaimerText = "Disclaimer & Privacy Policy:\n\n" +
                "1. RideShare Bot acts as a platform to help people find or offer rides. We do not verify the identity of users or validate the information provided by them.\n" +
                "2. You use this bot at your own risk. We are not responsible for any agreements made between users, incidents, accidents, or disputes that may arise.\n" +
                "3. Your personal information (such as contact info) is shared only with users who engage with your ride posts. We do not store or process this data for any other purpose.\n" +
                "4. Please ensure you follow your local laws and regulations regarding ride-sharing.\n\n" +
                "By using this bot, you agree to the terms outlined above.";
        sendTextMessageAsync(chatId, disclaimerText);
    }


}
