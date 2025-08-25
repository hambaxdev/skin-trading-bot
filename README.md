# Skin Trade Bot

A Telegram bot that calculates profit/loss between multiple trading platforms for CS:GO skins and stores trade history in a PostgreSQL database.

## Features

- Calculate profit/loss between different trading platforms
- Store trade history in a database
- View recent trade history
- Support for multiple platforms with different fee rates

## Supported Platforms

| Platform | Fee Rate |
|----------|----------|
| Steam    | 15%      |
| Float    | 10%      |
| CSM      | 5%       |
| CSMM     | 2%       |
| CSMAR    | 1%       |

## Commands

- `/start` - Show greeting and usage instructions
- `/trade site=price site=price ...` - Calculate best/worst price, profit, and percentage
  - Example: `/trade steam=100 csm=95 float=90`
- `/fees` - Show fee rates for all supported platforms
- `/history` - Show your last 10 saved trades
- `/help` - Show usage instructions

## Requirements

- Java 17
- Maven
- Docker and Docker Compose (for containerized deployment)

## Configuration

The application uses the following environment variables:

- `BOT_TOKEN` - Telegram Bot API token
- `BOT_USERNAME` - Telegram Bot username

## Building and Running

### Using Docker Compose

1. Create a `.env` file in the project root based on the `.env.template`:

```bash
# Copy the template
cp .env.template .env

# Edit the .env file with your actual values
# Replace your_bot_token and your_bot_username with your actual Telegram bot credentials
```

2. Build and run the application:

```bash
docker-compose up -d
```

### Manual Build

1. Build the application:

```bash
mvn clean package
```

2. Run the application:

```bash
java -jar target/skin-trade-bot-0.0.1-SNAPSHOT.jar
```

## Development

### Prerequisites

- Java 17
- Maven
- PostgreSQL

### Setup

1. Clone the repository
2. Set up a PostgreSQL database
3. Configure application.properties with your database and bot credentials
4. Run the application

## License

This project is licensed under the MIT License - see the LICENSE file for details.
