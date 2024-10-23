# RideShareBot

RideShareBot is a Telegram bot that helps users find or offer rides by connecting passengers and drivers through Telegram channels. The bot automatically directs posts to country-specific channels based on the user's location and details.

## Features

- **Find or Offer a Ride**: Users can either offer a ride or search for one.
- **Country-based Ride Posts**: Rides are posted in country-specific channels based on the user's origin city.(will come later)
- **Privacy & Disclaimer**: The bot includes a disclaimer to inform users that they use the bot at their own risk.
- **Contact Information Validation**: Users are required to submit valid contact information (either a phone number or a Telegram username starting with `@`).
- **Simple & Easy to Use**: Designed to be user-friendly for both drivers and passengers.

## Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/shodiBoy1/RideShareBot.git
   cd RideShareBot
   ```
2. **Set up environment variables: Create a .env file in the root of the project and add your bot token and bot name**:
   ```env
   BOT_NAME=YourBotName
   BOT_TOKEN=YourTelegramBotToken
   ```
3. **Install dependencies: Use Maven to install dependencies**:
   ```bash
   mvn clean install
   ```
4. Run the bot locally: After building the project, you can run the bot:
   ```bash
   java -jar target/rideshare-bot-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

**Usage**:
- Start the bot: Send /start to the bot in Telegram to begin using it.
- Disclaimer: Send /disclaimer to see the terms and conditions.
- Offer or Find a Ride: Follow the prompts to either offer or find a ride. The bot will automatically post the ride details in the appropriate country-based Telegram channel.

**Contributing**:
- Contributions are welcome! Please feel free to submit a pull request or open an issue.

**License**:
- This project is licensed under the MIT License - see the LICENSE file for details.

**Disclaimer**:
- By using this bot, you agree that RideShareBot and its developers are not responsible for any agreements, incidents, or disputes that occur. The bot is a platform to help users find or offer rides, and all activities are at the user's own risk.
