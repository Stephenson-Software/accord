# accordion-prototype

**Accordion Chat MVP** - A self-hosted real-time chat application with Spring Boot WebSocket backend and LibGDX frontend.

## Overview

Accordion is a Discord-like self-hosted chat application designed for simplicity and extensibility. This MVP demonstrates core chat functionality with **multiple chat rooms/channels**, password-based authentication, and real-time messaging.

**Technology Stack:**
- **Backend**: Spring Boot 3.x, WebSocket (STOMP), H2 Database, Spring Data JPA
- **Web Frontend**: Spring Boot, Thymeleaf, SockJS, STOMP.js (browser-based)
- **Desktop Frontend**: LibGDX, Java WebSocket client, Scene2D UI

## Features

- ✅ **Multiple chat rooms/channels** - Create and switch between different channels
- ✅ Single chat room (global) - Backwards compatible default channel
- ✅ Password-based authentication (Spring Security, BCrypt-hashed passwords, JWT bearer tokens)
- ✅ Authenticated WebSocket sessions (STOMP `CONNECT` frames require a valid JWT)
- ✅ Real-time message broadcasting via WebSocket
- ✅ **Message persistence in H2 database (survives container restarts)**
- ✅ Message history on login (channel-specific)
- ✅ Timestamp for each message
- ✅ User join/leave notifications
- ✅ **Browser-based web interface** (accessible from any device)
- ✅ Cross-platform desktop support (LibGDX)
- ✅ Docker containerization for easy deployment

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+ (for backend and webapp)
- Gradle 7+ (included via wrapper for LibGDX desktop frontend)
- Docker & Docker Compose (for containerized deployment)

### Running the Application

**Option 1: Using Docker Compose (Recommended)**

```bash
# Copy sample environment file and customize if needed
cp sample.env .env

# Generate a signing secret and set it as JWT_SECRET in .env
# (the backend will not start without it)
openssl rand -base64 48

# Edit .env to configure ports and other settings

# Start all services (backend + web app)
docker compose up -d

# Access the web application
open http://localhost:3000

# For network access from other devices:
# 1. Set DOCKER_HOST_IP to your machine's IP in .env
# 2. Restart: docker compose down && docker compose up -d
# 3. Access from other devices: http://YOUR_IP:3000
```

**Option 2: Run services manually**

```bash
# Terminal 1 - Start backend (JWT_SECRET is required; startup fails without it)
export JWT_SECRET="$(openssl rand -base64 48)"
cd backend
mvn spring-boot:run

# Terminal 2 - Start web application
cd webapp
mvn spring-boot:run

# Access at http://localhost:3000

# Optional: Terminal 3 - Start LibGDX desktop client
cd frontend
./gradlew desktop:run
```

**Option 3: Run backend + multiple LibGDX clients**

```bash
# Terminal 1 - Start backend (JWT_SECRET is required; startup fails without it)
export JWT_SECRET="$(openssl rand -base64 48)"
cd backend && mvn spring-boot:run

# Terminal 2+ - Start as many LibGDX clients as you want
cd frontend && ./gradlew desktop:run
```

### Using the Chat Application

**Web Application (Browser):**
1. Navigate to `http://localhost:3000`
2. Click "Register" to switch the form to registration mode on a first visit
3. Enter your username (minimum 3 characters, alphanumeric + underscore) and a password (minimum 8 characters, containing an uppercase letter, a lowercase letter, and a digit)
4. Submit the form — the returned JWT is used for the REST calls and the WebSocket handshake that follow
5. **Select a channel from the sidebar** or create a new one with "+ New Channel"
6. Type your message and click "Send" or press Enter
7. **Switch between channels** by clicking on channel names in the sidebar
8. Open in multiple browser tabs to test real-time messaging across channels

**LibGDX Desktop Client:**

> **Note:** The desktop client predates the JWT authentication described below and has not been updated for it. `ChatWebSocketClient` sends a STOMP `CONNECT` frame with no `Authorization` header, which `WebSocketAuthInterceptor` rejects, so the client cannot connect to the current backend. Use the web application until desktop parity lands.

1. **Frontend window will open** (800x600)
2. **Enter your username** (minimum 3 characters)
3. **Click "Login"** to enter the chat room
4. **Type your message** and click "Send" or press Enter
5. **Open multiple clients** to test real-time messaging

**Backend API:**
- Backend runs on `http://localhost:8080`
- WebSocket endpoint: `ws://localhost:8080/ws`
- Authentication: every endpoint except those marked *public* below requires an
  `Authorization: Bearer <token>` header. Tokens are issued by the register and
  login endpoints. See [MVP.md](MVP.md) for request and response shapes.
- REST API endpoints:
  - `POST /api/users/register` - Register a new user, returns a JWT (*public*)
  - `POST /api/users/login` - Log in, returns a JWT (*public*)
  - `GET /api/users/check/{username}` - Report whether a username is taken
  - `GET /api/channels` - List all channels
  - `POST /api/channels` - Create a new channel
  - `GET /api/channels/{id}` - Get channel details
  - `GET /api/messages?limit={n}&channelId={id}` - Get recent messages, optionally scoped to a channel
- STOMP destinations (application prefix `/app`, broker prefix `/topic`):
  - Send `/app/chat.send` → broadcast on `/topic/messages` (default channel)
  - Send `/app/chat.send/{channelId}` → broadcast on `/topic/messages/{channelId}`
  - Send `/app/chat.join` → broadcast on `/topic/messages` (default channel)
  - Send `/app/chat.join/{channelId}` → broadcast on `/topic/messages/{channelId}`
  - Send `/app/chat.typing/{channelId}` → broadcast on `/topic/typing/{channelId}`

## Docker Deployment

### Prerequisites for Docker

- Docker 20.10+ 
- Docker Compose 2.0+

### Configuration

The application uses environment variables for configuration. A `sample.env` file is provided with all available options:

```bash
# Copy and customize the environment file
cp sample.env .env

# Edit .env to configure:
# - The required JWT signing secret (JWT_SECRET)
# - Port numbers (BACKEND_PORT, WEBAPP_PORT)
# - CORS settings (APP_CORS_ALLOWED_ORIGINS)
# - Database configuration
# - Validation rules (message length, username and password constraints)
# - And more...
```

**Key configuration options:**

- `JWT_SECRET`: **Required.** JWT signing secret, at least 32 bytes, with no default — the
  backend fails to start when it is missing, blank, or too short. Generate one with
  `openssl rand -base64 48` and use a distinct value per deployment.
- `JWT_EXPIRATION`: JWT lifetime in milliseconds (default: 86400000, i.e. 24 hours)
- `BACKEND_PORT`: Backend service port (default: 8080)
- `WEBAPP_PORT`: Web application port (default: 3000)
- `APP_CORS_ALLOWED_ORIGINS`: CORS allowed origins (default: `*` for development)
- `APP_MESSAGE_MAX_LENGTH`: Maximum message length (default: 1000)
- `APP_USERNAME_MIN_LENGTH`: Minimum username length (default: 3)
- `APP_USERNAME_MAX_LENGTH`: Maximum username length (default: 50)
- `APP_PASSWORD_MIN_LENGTH`: Minimum password length (default: 8)
- `USAGE_REPORTING_ENABLED`: Whether the backend reports that it is in use to the
  [trace](https://github.com/Stephenson-Software/trace) usage service (default: `true`; see
  [Usage reporting](#usage-reporting))

See `sample.env` for the complete list of configurable options.

### Running with Docker Compose

The easiest way to run the backend in a container:

```bash
# Build and start the backend
docker compose up -d

# View logs
docker compose logs -f backend

# Stop the service
docker compose down
```

The services will be available at:
- **Web Application**: `http://localhost:3000` (browser-based UI)
- **Backend API**: `http://localhost:8080` (REST & WebSocket)

**Note:** If you customized ports in `.env`, use those port numbers instead.

### Building Docker Images Manually

**Backend:**
```bash
# Build the backend image
docker build -t accordion-backend:latest -f Dockerfile .

# Run the container
docker run -d \
  -p 8080:8080 \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  --name accordion-backend \
  accordion-backend:latest
```

**Web Application:**
```bash
# Build the webapp image
docker build -t accordion-webapp:latest -f Dockerfile.webapp .

# Run the container (requires backend to be running)
docker run -d \
  -p 3000:3000 \
  -e ACCORDION_BACKEND_URL=http://backend:8080 \
  -e ACCORDION_BACKEND_WS_URL=ws://backend:8080/ws \
  --link accordion-backend:backend \
  --name accordion-webapp \
  accordion-webapp:latest
```

**View logs:**
```bash
docker logs -f accordion-backend
docker logs -f accordion-webapp
```

**Stop and remove:**
```bash
docker stop accordion-backend accordion-webapp
docker rm accordion-backend
```

### Environment Variables

Configure the application using environment variables:

```bash
docker run -d \
  -p 8080:8080 \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  -e APP_CORS_ALLOWED_ORIGINS="https://yourdomain.com" \
  -e APP_USERNAME_MIN_LENGTH=5 \
  -e APP_MESSAGE_MAX_LENGTH=500 \
  --name accordion-backend \
  accordion-backend:latest
```

See `compose.yml` for all available environment variables.

### Accessing H2 Console (Optional)

View the database contents:
- URL: `http://localhost:8080/h2-console`
- JDBC URL: When using Docker Compose with persistence (default): `jdbc:h2:file:/app/data/chatdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- JDBC URL: When using in-memory mode: `jdbc:h2:mem:chatdb`
- Username: `sa`
- Password: (leave empty)
- Note: The JDBC URL (including parameters) should match your `SPRING_DATASOURCE_URL` environment variable.

### Database Persistence

By default, **when running via Docker Compose**, the application uses a **file-based H2 database with Docker volume persistence**. This means your chat messages and channels are preserved across container restarts.

**Note:** When running the backend directly from source (without Docker), it defaults to in-memory H2 as configured in `backend/src/main/resources/application.properties`.

**Default Configuration (with Docker Compose):**
- Database is stored in a Docker volume named `h2_data`
- Data persists when containers are stopped and restarted
- Database files are stored at `/app/data/chatdb` inside the container
- Schema management uses `update` mode by default (convenient for development)
  - **For production**: Consider using `validate` mode with proper database migrations (Flyway/Liquibase)
  - Override in `.env`: `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`

**To use in-memory database instead (data lost on restart):**

Edit your `.env` file:
```bash
SPRING_DATASOURCE_URL=jdbc:h2:mem:chatdb
SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop
```

Then restart:
```bash
docker compose down && docker compose up -d
```

**To clear persisted data:**
```bash
# Stop containers and remove the volume
docker compose down -v

# Restart fresh
docker compose up -d
```

## Project Structure

```
accordion-prototype/
├── MVP.md                          # Detailed MVP documentation
├── README.md                       # This file
├── Dockerfile                      # Docker image for backend
├── Dockerfile.webapp               # Docker image for web application
├── compose.yml                     # Docker Compose configuration
├── .dockerignore                   # Docker build exclusions
├── backend/                        # Spring Boot backend
│   ├── pom.xml                    # Maven configuration
│   └── src/main/java/com/accordion/
│       ├── AccordionApplication.java
│       ├── config/                # WebSocket, Spring Security, and data seeding configuration
│       ├── controller/            # REST and WebSocket controllers
│       ├── dto/                   # Request/response payloads (login, register, auth)
│       ├── model/                 # JPA entities
│       ├── repository/            # Data repositories
│       ├── security/              # JWT issuing/validation and authentication filters
│       ├── service/               # Business logic
│       ├── trace/                 # Vendored trace-client-java (usage reporting)
│       └── util/                  # Validation utilities
├── webapp/                         # Spring Boot web application
│   ├── pom.xml                    # Maven configuration
│   └── src/
│       ├── main/
│       │   ├── java/com/accordion/webapp/
│       │   │   ├── AccordionWebApplication.java
│       │   │   └── controller/    # Web controllers
│       │   └── resources/
│       │       ├── templates/     # Thymeleaf HTML templates
│       │       │   ├── index.html # Login page
│       │       │   └── chat.html  # Chat interface
│       │       └── application.properties
│       └── test/                  # JUnit 5 test suite (mirrors the main tree)
└── frontend/                       # LibGDX frontend (desktop)
    ├── build.gradle               # Root Gradle config
    ├── core/                      # Shared code
    │   └── src/com/accordion/
    │       ├── AccordionGame.java
    │       ├── config/            # Configuration
    │       ├── screen/            # Login & Chat screens
    │       └── websocket/         # WebSocket client
    └── desktop/                   # Desktop launcher
        └── src/com/accordion/desktop/
            └── DesktopLauncher.java
```

## Building from Source

### Backend

```bash
cd backend
mvn clean install
```

### Frontend

```bash
cd frontend
./gradlew build
```

## Configuration

### Backend (`backend/src/main/resources/application.properties`)

```properties
server.port=8080
spring.datasource.url=jdbc:h2:mem:chatdb
spring.h2.console.enabled=true
```

### Usage reporting

The backend reports that it is in use to the
[trace](https://github.com/Stephenson-Software/trace) usage service at
`https://trace.danielstephenson.dev`, so it is known how many Accordion servers are running
and on which versions. It sends exactly two kinds of event:

- `startup`, once the backend is ready — the program name (`accordion`) and its version only
- `channel-created`, each time a channel is created through the API — the program name only

Nothing about users, messages, channel names, the host or its address is ever sent. The send
happens on its own daemon thread, never throws and never blocks startup; a trace server that is
unreachable is a dropped report, not an error. Reporting is on by default and the backend logs
one INFO line at every start saying whether it is on:

```
Usage reporting is on: accordion sends a startup event (program name and version only) and a channel-created event (program name only) to https://trace.danielstephenson.dev. Turn it off with USAGE_REPORTING_ENABLED=false (usage-reporting.enabled).
```

To turn it off, set `USAGE_REPORTING_ENABLED=false` in `.env` (Docker Compose) or in the
backend's environment, or set `usage-reporting.enabled=false` in
`backend/src/main/resources/application.properties`. `USAGE_REPORTING_ENDPOINT` points it at
a different trace server. Tests never report.

### Frontend

The desktop client's WebSocket endpoint comes from `AppConfig.getWebSocketUrl()`,
which defaults to `ws://localhost:8080/ws`. To connect to a remote server, override
it with a system property rather than editing the source:

```bash
./gradlew desktop:run -Daccordion.websocket.url=ws://yourserver.com:8080/ws
```

## Development Roadmap

See [MVP.md](MVP.md) for the complete roadmap and architecture details.

### Completed
- ✅ Multiple chat rooms/channels
- ✅ Channel creation and management
- ✅ Channel-specific message history
- ✅ Password-based authentication with JWT (Spring Security)
- ✅ Typing indicators (web application)

### Next Steps
- Desktop client parity with the web application (JWT authentication, channels, typing indicators)
- Private direct messages
- User online/offline status
- Message search and pagination
- PostgreSQL/MySQL support for production
- File/image sharing

## Documentation

- **[MVP.md](MVP.md)** - Complete MVP documentation including:
  - Architecture details
  - API documentation
  - Setup instructions
  - Feature roadmap
  - Troubleshooting guide

## Contributing

This is an MVP prototype. Contributions are welcome for:
- Bug fixes
- Performance improvements
- Documentation updates
- Feature implementations from the roadmap

## License
This project is licensed under the **Stephenson Software Non-Commercial License (Stephenson-NC)**.  
© 2025 Daniel McCoy Stephenson. All rights reserved.  

You may use, modify, and share this software for **non-commercial purposes only**.  
Commercial use is prohibited without explicit written permission from the copyright holder.  

Full license text: [Stephenson-NC License](https://github.com/Stephenson-Software/stephenson-nc-license)  
SPDX Identifier: `Stephenson-NC`

## Support

For issues or questions, please open an issue on GitHub.

