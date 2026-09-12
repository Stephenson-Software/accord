# Accordion Chat MVP

## Overview
A self-hosted real-time chat application with a Spring Boot WebSocket backend, a Thymeleaf web application, and a LibGDX desktop frontend. This MVP demonstrates core chat functionality across multiple channels, secured with password-based authentication.

## Architecture

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.x
- **Communication**: WebSocket (STOMP protocol)
- **Database**: H2 (in-memory for MVP, file-based option available)
- **Persistence**: Spring Data JPA
- **Port**: 8080

#### Components:
1. **WebSocket Configuration**: Configures STOMP endpoints and message broker
2. **Domain Models**:
   - `User`: Represents chat users (id, username, password, joinedAt)
   - `Channel`: Represents chat rooms (id, name, description, createdAt, createdBy)
   - `ChatMessage`: Represents messages (id, username, content, timestamp, channelId)
   - `TypingIndicator`: Transient typing-state payload (username, channelId, typing)
3. **Repositories**: JPA repositories for data persistence
4. **Controllers**:
   - `UserController`: REST API for user registration, login, and username availability
   - `ChannelController`: REST API for listing and creating channels
   - `ChatController`: WebSocket message handler
   - `MessageRestController`: REST API for message history (`/api/messages`); declared alongside `ChatController` in the same file
5. **Services**: Business logic for user, channel, and message management
6. **Security**: Spring Security with BCrypt password hashing and JWT bearer tokens
   - `SecurityConfig`: stateless filter chain; permits `/api/users/register`, `/api/users/login`, `/ws/**`, and `/h2-console/**`, and requires authentication for everything else
   - `JwtUtil`: token issuing and validation
   - `JwtAuthenticationFilter`: reads the `Authorization: Bearer` header on REST requests
   - `WebSocketAuthInterceptor`: authenticates STOMP `CONNECT` frames, rejecting those without a valid token
   - `CustomUserDetailsService`: loads users for authentication

### Frontend (LibGDX)
- **Framework**: LibGDX (cross-platform game/UI framework)
- **WebSocket Client**: Java WebSocket client
- **Screens**:
   - `LoginScreen`: Username entry
   - `ChatScreen`: Message display and input
- **UI Components**: Scene2D for UI elements

### Communication Flow
```
Client (Web / LibGDX) <--> WebSocket <--> Spring Boot Server <--> H2 Database

1. User submits credentials → POST /api/users/register or /api/users/login → Server returns a JWT
2. Client connects to WebSocket → /ws endpoint → STOMP CONNECT carries Authorization: Bearer <token>
3. User sends message → /app/chat.send/{channelId} → Server processes → /topic/messages/{channelId}
4. All clients subscribed to that channel topic receive the message
```

## Features

### Current MVP Features
- ✅ Multiple chat rooms/channels, with a default channel for backwards compatibility
- ✅ Password-based authentication (BCrypt hashing, JWT bearer tokens)
- ✅ Authenticated WebSocket sessions (STOMP `CONNECT` requires a valid JWT)
- ✅ Real-time message broadcasting via WebSocket
- ✅ Message persistence in H2 database
- ✅ Channel-specific message history
- ✅ Timestamp for each message
- ✅ User join notifications
- ✅ Typing indicators (web application)
- ✅ Browser-based web application (Thymeleaf, SockJS, STOMP.js)
- ✅ Simple LibGDX UI with message list and input field

### MVP Limitations
- No authorization roles or permissions — every authenticated user can read and post in every channel
- No user avatars
- No private messages
- No message editing/deletion
- No file uploads
- No emoji support
- No user list or online/offline presence
- In-memory H2 database when run from source (Docker Compose uses a persistent volume)
- The LibGDX desktop client predates JWT authentication and cannot connect to the current backend

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven 3.6+ (for backend)
- Gradle 7+ (for frontend)

### Backend Setup

1. **Navigate to backend directory**:
   ```bash
   cd backend
   ```

2. **Build the project**:
   ```bash
   mvn clean install
   ```

3. **Run the server**:
   ```bash
   # Required: the backend refuses to start without a JWT signing secret of at least 32 bytes
   export JWT_SECRET="$(openssl rand -base64 48)"
   mvn spring-boot:run
   ```
   
   The server will start on `http://localhost:8080`

4. **Access H2 Console** (optional):
   - URL: `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:chatdb`
   - Username: `sa`
   - Password: (leave empty)

### Frontend Setup

1. **Navigate to frontend directory**:
   ```bash
   cd frontend
   ```

2. **Build the project**:
   ```bash
   ./gradlew desktop:dist
   ```

3. **Run the application**:
   ```bash
   ./gradlew desktop:run
   ```

### Quick Start (Both Services)

From the root directory:

```bash
# Terminal 1 - Start backend
cd backend && mvn spring-boot:run

# Terminal 2 - Start frontend
cd frontend && ./gradlew desktop:run
```

## Configuration

### Backend Configuration (`application.properties`)
```properties
# Server
server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:mem:chatdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# CORS Configuration (set to specific origins in production)
app.cors.allowed-origins=*

# Message Content Validation
app.message.max-length=1000

# Username Validation
app.username.max-length=50
app.username.min-length=3

# Password Validation
app.password.min-length=8

# JWT Configuration
# The placeholder carries no default, so an absent JWT_SECRET is unresolvable and
# startup fails. JwtUtil additionally rejects a blank or shorter-than-32-byte secret.
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000
```

**Security Note**: For production deployment, replace `app.cors.allowed-origins=*` with specific trusted domains (e.g., `https://yourdomain.com`).

**Startup Note**: The backend will not start unless `JWT_SECRET` is set to a value of at least 32 bytes. Export one before running `mvn spring-boot:run`.

### Frontend Configuration
- **WebSocket endpoint**: Configurable via `AppConfig.getWebSocketUrl()`
  - Default: `ws://localhost:8080/ws`
  - Override with system property: `-Daccordion.websocket.url=ws://yourserver.com:8080/ws`
- **Window size**: 800x600
- **Title**: "Accordion Chat"
- **Username constraints**: 3-50 characters, alphanumeric and underscore only
- **Message length**: Maximum 1000 characters

## Project Structure

```
accordion-prototype/
├── MVP.md                          # This file
├── README.md                       # Project overview
├── TICKETS.md                      # Development tickets and status
├── DOCKER.md                       # Docker deployment guide
├── backend/                        # Spring Boot backend
│   ├── pom.xml                    # Maven configuration
│   └── src/
│       ├── main/
│       │   ├── java/com/accordion/
│       │   │   ├── AccordionApplication.java
│       │   │   ├── config/
│       │   │   │   ├── DataInitializer.java
│       │   │   │   ├── SecurityConfig.java
│       │   │   │   └── WebSocketConfig.java
│       │   │   ├── controller/
│       │   │   │   ├── ChannelController.java
│       │   │   │   ├── ChatController.java
│       │   │   │   └── UserController.java
│       │   │   ├── dto/
│       │   │   │   ├── AuthResponse.java
│       │   │   │   ├── LoginRequest.java
│       │   │   │   └── RegisterRequest.java
│       │   │   ├── model/
│       │   │   │   ├── Channel.java
│       │   │   │   ├── ChatMessage.java
│       │   │   │   ├── TypingIndicator.java
│       │   │   │   └── User.java
│       │   │   ├── repository/
│       │   │   │   ├── ChannelRepository.java
│       │   │   │   ├── ChatMessageRepository.java
│       │   │   │   └── UserRepository.java
│       │   │   ├── security/
│       │   │   │   ├── CustomUserDetailsService.java
│       │   │   │   ├── JwtAuthenticationFilter.java
│       │   │   │   ├── JwtUtil.java
│       │   │   │   └── WebSocketAuthInterceptor.java
│       │   │   ├── service/
│       │   │   │   ├── ChannelService.java
│       │   │   │   ├── ChatService.java
│       │   │   │   ├── UsageReportingService.java
│       │   │   │   └── UserService.java
│       │   │   ├── trace/
│       │   │   │   └── TraceClient.java   # vendored trace-client-java
│       │   │   └── util/
│       │   │       └── ValidationUtils.java
│       │   └── resources/
│       │       └── application.properties
│       └── test/                  # JUnit 5 test suite (mirrors the main tree)
├── webapp/                         # Spring Boot web application
│   ├── pom.xml                    # Maven configuration
│   └── src/
│       ├── main/
│       │   ├── java/com/accordion/webapp/
│       │   │   ├── AccordionWebApplication.java
│       │   │   └── controller/
│       │   │       └── ChatController.java
│       │   └── resources/
│       │       ├── templates/
│       │       │   ├── index.html # Login and registration page
│       │       │   └── chat.html  # Chat interface
│       │       └── application.properties
│       └── test/                  # JUnit 5 test suite (mirrors the main tree)
└── frontend/                       # LibGDX frontend
    ├── build.gradle               # Root Gradle config
    ├── settings.gradle
    ├── core/                      # Shared code
    │   └── src/com/accordion/
    │       ├── AccordionGame.java
    │       ├── config/
    │       │   └── AppConfig.java
    │       ├── screen/
    │       │   ├── LoginScreen.java
    │       │   └── ChatScreen.java
    │       └── websocket/
    │           └── ChatWebSocketClient.java
    └── desktop/                   # Desktop launcher
        └── src/com/accordion/desktop/
            └── DesktopLauncher.java
```

## API Documentation

### Authentication

`SecurityConfig` permits `/api/users/register`, `/api/users/login`, `/ws/**`, and
`/h2-console/**` anonymously and requires authentication for every other request.
Authenticated REST calls carry the token issued by register or login:

```
Authorization: Bearer <token>
```

Tokens are signed with HS256 using `jwt.secret` and expire after `jwt.expiration`
milliseconds (default `86400000`, i.e. 24 hours).

### REST Endpoints

#### Register
- **POST** `/api/users/register` — *public*
- **Body**: `{ "username": "string", "password": "string" }`
- **Response** `200`: `{ "token": "string", "username": "string", "userId": 1 }`
- **Response** `400`: `{ "error": "string" }` when the username is missing or invalid, the password is missing or fails complexity validation, or the username is already taken

#### Login
- **POST** `/api/users/login` — *public*
- **Body**: `{ "username": "string", "password": "string" }`
- **Response** `200`: `{ "token": "string", "username": "string", "userId": 1 }`
- **Response** `400`: `{ "error": "Username and password are required" }`
- **Response** `401`: `{ "error": "Invalid username or password" }`

#### Check Username Availability
- **GET** `/api/users/check/{username}` — *authentication required*
- **Response** `200`: `true` when the username is already taken, `false` when it is free
- **Response** `400`: `false` when the username fails format validation

#### List Channels
- **GET** `/api/channels` — *authentication required*
- **Response** `200`: Array of Channel objects

#### Get Channel
- **GET** `/api/channels/{id}` — *authentication required*
- **Response** `200`: A Channel object
- **Response** `404`: when no channel has that id

#### Create Channel
- **POST** `/api/channels` — *authentication required*
- **Body**: `{ "name": "string", "description": "string", "createdBy": "string" }`
- **Response** `201`: The created Channel object
- **Response** `400`: `{ "error": "string" }` when the name is missing, falls outside the configured length range (`app.channel.name-min-length`/`app.channel.name-max-length`, defaulting to 3 and 50), or contains anything other than letters, numbers, hyphens, and underscores; when the description exceeds 500 characters; or when `createdBy` is missing

#### Get Messages
- **GET** `/api/messages` — *authentication required*
- **Query Params**: `limit` (optional, default: 50, clamped to 1-500), `channelId` (optional; all channels when omitted)
- **Response** `200`: Array of ChatMessage objects

### WebSocket Endpoints

#### Connect
- **Endpoint**: `/ws` — *public handshake*, SockJS enabled
- **Protocol**: STOMP over WebSocket
- **Headers**: the STOMP `CONNECT` frame must carry `Authorization: Bearer <token>`; `WebSocketAuthInterceptor` rejects the connection otherwise
- **Prefixes**: application destinations are prefixed `/app`, broker destinations `/topic`

#### Send Message (default channel)
- **Destination**: `/app/chat.send`
- **Payload**: `{ "username": "string", "content": "string" }`
- **Broadcasts to**: `/topic/messages`
- Any `channelId` in the payload is ignored; the message is always stored in the default channel.

#### Send Message (specific channel)
- **Destination**: `/app/chat.send/{channelId}`
- **Payload**: `{ "username": "string", "content": "string" }`
- **Broadcasts to**: `/topic/messages/{channelId}`

#### Join (default channel)
- **Destination**: `/app/chat.join`
- **Payload**: `{ "username": "string" }`
- **Broadcasts to**: `/topic/messages` — a message from `System` reading `<username> has joined the chat`

#### Join (specific channel)
- **Destination**: `/app/chat.join/{channelId}`
- **Payload**: `{ "username": "string" }`
- **Broadcasts to**: `/topic/messages/{channelId}` — a message from `System` reading `<username> has joined the chat`

#### Typing Indicator
- **Destination**: `/app/chat.typing/{channelId}`
- **Payload**: `{ "username": "string", "typing": true }` — `typing` defaults to `true` when omitted
- **Broadcasts to**: `/topic/typing/{channelId}`
- **Receives**: `{ "username": "string", "channelId": 1, "typing": true }`

#### Subscribe to Messages
- **Destinations**: `/topic/messages` (default channel) and `/topic/messages/{channelId}`
- **Receives**: `{ "id": 1, "username": "string", "content": "string", "timestamp": "2024-01-01T12:00:00", "channelId": 1 }`

## Development Roadmap

### Phase 1: MVP (Current)
- [x] Basic project structure
- [x] Spring Boot WebSocket backend
- [x] LibGDX frontend with basic UI
- [x] Single chat room
- [x] Username login
- [x] Real-time messaging
- [x] H2 database integration
- [x] Message persistence and history

### Phase 2: Enhanced Features
- [x] Multiple chat rooms/channels
- [x] User authentication (password-based)
- [ ] Private direct messages
- [ ] User online/offline status
- [x] Typing indicators (web application; not yet in the desktop client)
- [x] Message timestamps in UI (web application; relative times and date separators still outstanding)
- [ ] User list panel

### Phase 3: Advanced Features
- [ ] File/image uploads
- [ ] Emoji picker and reactions
- [ ] Message search
- [ ] User profiles and avatars
- [ ] User roles and permissions
- [ ] Message editing and deletion
- [ ] Voice channels (stretch goal)

### Phase 4: Production Ready
- [ ] PostgreSQL/MySQL database option
- [x] Docker containerization
- [ ] User registration with email
- [ ] Password reset functionality
- [ ] Rate limiting and security
- [ ] Mobile support (Android/iOS via LibGDX)
- [ ] Comprehensive logging and monitoring
- [ ] Backup and restore functionality

## Testing

### Backend Testing

The backend carries the largest automated test suite (JUnit 5, Spring Boot Test,
Mockito, and `spring-security-test`):

```bash
cd backend
mvn test
```

### Web Application Testing

The `webapp` module has a `@WebMvcTest` slice suite covering `ChatController`'s view
names and the backend URLs it exposes to the browser:

```bash
cd webapp
mvn test
```

Packaging additionally verifies the Thymeleaf templates and the executable jar:

```bash
cd webapp
mvn clean package
```

### Frontend Testing

The LibGDX modules have no test sources either — `./gradlew test` reports
`NO-SOURCE` and executes nothing. Use the build task as a compile check:

```bash
cd frontend
./gradlew build
```

### Continuous Integration

`.github/workflows/ci.yml` runs three jobs on every pull request: **Backend Build and
Test** and **Webapp Build and Test**, which execute those modules' suites, and
**Frontend Build**, which compiles the LibGDX modules without running any test
(`./gradlew test` is `NO-SOURCE` there). No job exercises `compose.yml` or the
Dockerfiles, so changes to those must be verified locally.

### Manual Testing Checklist
1. Start backend server with `JWT_SECRET` exported
2. Verify H2 console access
3. Start the web application
4. Register an account (test validation: username 3-50 chars, alphanumeric + underscore; password min 8 chars with uppercase, lowercase, and a digit)
5. Log out and log back in with the same credentials
6. Confirm an unauthenticated `GET /api/messages` returns `401`
7. Send a test message
8. Test message length limit (1000 characters)
9. Test invalid username characters (should be rejected)
10. Create a channel and switch to it
11. Open a second browser tab and verify messages appear in both
12. Verify the typing indicator appears in the other tab and clears when typing stops
13. Reload the page and verify channel message history loads
14. Test connection error handling (disconnect server)

## Security

### MVP Security Features
- ✅ **Authentication**: Spring Security with BCrypt-hashed passwords and stateless JWT bearer tokens
  - Password: minimum 8 characters, requiring an uppercase letter, a lowercase letter, and a digit
  - Tokens signed with HS256; `JWT_SECRET` is mandatory and has no shipped default, so startup fails fast without it
- ✅ **Authenticated WebSocket Sessions**: STOMP `CONNECT` frames without a valid bearer token are rejected
- ✅ **Input Validation**: Username and message content validation
  - Username: 3-50 characters, alphanumeric and underscore only
  - Message: Maximum 1000 characters
- ✅ **CORS Configuration**: Configurable via environment variables
- ✅ **Database Constraints**: Length limits enforced at database level
- ✅ **Request Limits**: Message history API limited to 500 messages max
- ✅ **Error Handling**: Proper exception handling with user-friendly messages
- ✅ **Logging**: Structured logging with java.util.logging

### Security Limitations (MVP)
- ⚠️ **No Authorization**: All authenticated users can see and post in all channels; there are no roles
- ⚠️ **Unverified Message Authorship**: The `username` in a STOMP payload is validated for format but is not checked against the authenticated principal, so a client may post under another name
- ⚠️ **No Encryption**: Messages sent in plain text over WebSocket
- ⚠️ **No Rate Limiting**: No protection against spam or DoS, including credential brute-forcing
- ⚠️ **CORS Default**: Allows all origins by default (must configure for production)
- ⚠️ **H2 Console Exposed**: `/h2-console/**` is permitted anonymously and frame options are disabled; both must be turned off in production
- ⚠️ **No Password Reset**: A forgotten password cannot be recovered

### Production Security Recommendations
1. **Enable HTTPS/WSS**: Use TLS for all connections
2. **Configure CORS**: Set `app.cors.allowed-origins` to specific trusted domains
3. **Harden Authentication**: Add password reset and consider OAuth alongside the shipped password/JWT flow
4. **Add Authorization**: Implement role-based access control, and bind message authorship to the authenticated principal
5. **Disable the H2 Console**: Set `spring.h2.console.enabled=false` and restore frame options
6. **Enable Rate Limiting**: Prevent abuse and DoS attacks
7. **Input Sanitization**: Additional validation for XSS prevention
8. **Security Headers**: Add security headers (CSP, HSTS, etc.)
9. **Audit Logging**: Log security-relevant events
10. **Regular Updates**: Keep dependencies up to date
11. **Security Scanning**: Run regular vulnerability scans

## Troubleshooting

### Backend Issues

**Port 8080 already in use**:
```bash
# Find process using port 8080
lsof -i :8080
# Kill the process
kill -9 <PID>
```

**Backend exits at startup with a `JWT_SECRET` placeholder error**:
- Expected behavior — `jwt.secret` is declared as a bare `${JWT_SECRET}` placeholder with no default, so no secret ships with the application
- Export a value of at least 32 bytes before starting: `export JWT_SECRET="$(openssl rand -base64 48)"`
- A blank or shorter-than-32-byte value produces an `IllegalStateException` from `JwtUtil` instead, naming the required length

**Database connection errors**:
- Check H2 console configuration
- Verify JDBC URL matches application.properties
- Ensure H2 dependency is included

### Frontend Issues

**WebSocket connection failed**:
- Verify backend is running on localhost:8080
- Check firewall settings
- Review browser console for CORS errors

**LibGDX won't start**:
- Verify Java version (17+)
- Check Gradle wrapper permissions: `chmod +x gradlew`
- Clear Gradle cache: `./gradlew clean`

### Common Issues

**Messages not appearing**:
- Check WebSocket connection status
- Verify STOMP subscription to /topic/messages
- Check browser/application console for errors

**Login not working**:
- Verify the backend API is accessible and returns a token:
  `curl -X POST http://localhost:8080/api/users/login -H 'Content-Type: application/json' -d '{"username":"alice","password":"Passw0rd"}'`
- A `401` means the credentials are wrong; a `400` means a field is missing
- Register first if the account does not exist yet — `POST /api/users/register` with the same body shape
- Check the network tab for the API response

**Requests fail with 401 after logging in**:
- Confirm the client sends `Authorization: Bearer <token>` on every call except register and login
- Tokens expire after `jwt.expiration` milliseconds (24 hours by default); log in again to obtain a fresh one
- A STOMP `CONNECT` without that header is rejected by `WebSocketAuthInterceptor`, which closes the WebSocket

## Technology Stack

### Backend
- **Java 17+**: Programming language
- **Spring Boot 3.x**: Application framework
- **Spring WebSocket**: Real-time communication
- **Spring Data JPA**: Data persistence
- **H2 Database**: In-memory database
- **Maven**: Build tool
- **Lombok** (optional): Reduce boilerplate code

### Frontend
- **Java 17+**: Programming language
- **LibGDX**: Cross-platform UI framework
- **Scene2D**: UI toolkit
- **Java-WebSocket**: WebSocket client library
- **Gradle**: Build tool

## Performance Considerations

### MVP Performance Profile
- **Expected Users**: 5-10 concurrent users
- **Message Throughput**: ~100 messages/minute
- **Database**: In-memory (fast, but not persistent)
- **Memory Usage**: ~200MB backend, ~150MB frontend

### Scalability Notes
For production use beyond MVP:
1. Switch to persistent database (PostgreSQL/MySQL)
2. Implement connection pooling
3. Add Redis for session management
4. Implement load balancing for multiple servers
5. Add message pagination/lazy loading
6. Optimize database queries with indexes

## Contributing

This is an MVP prototype. Contributions welcome for:
- Bug fixes
- Performance improvements
- Documentation updates
- Feature implementations from roadmap

## License

[Specify license here]

## Contact

[Project maintainer contact information]
