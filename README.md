# SmartScheduler
# SmartScheduler

A Spring Boot application that intelligently parses user input into calendar events using AI, with Telegram integration.

## Features

- **AI-Powered Event Parsing**: Uses Spring AI to extract event details from natural language text
- **Telegram Bot Integration**: Interact with the scheduler via Telegram
- **Event Management**: Create and manage events with automatic date/time parsing
- **Error Handling**: User-friendly error messages and exception handling
- **Retry Logic**: Automatic retry mechanism for transient AI service failures

## Tech Stack

- **Java** with Spring Boot
- **Spring AI** for natural language processing
- **Telegram Bot API** integration
- **Maven** for dependency management
- **Lombok** for code generation


## Setup

1. Clone the repository:
```bash
git clone https://github.com/VladSmt/SmartScheduler.git
```

2. Configure AI and Telegram credentials in `application.properties`

3. Build with Maven:
```bash
mvn clean build
```

4. Run the application:
```bash
mvn spring-boot:run
```

## Usage

Send event descriptions to the Telegram bot

The AI service will parse the input and create calendar events automatically.
