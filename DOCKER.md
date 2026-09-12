# Docker Deployment Guide

This guide covers deploying the Accordion Chat application (backend and web application) using Docker and Docker Compose.

## Quick Start

### Using Docker Compose (Recommended)

```bash
# Copy and customize environment configuration
cp sample.env .env

# Generate a signing secret and set it as JWT_SECRET in .env
# (the backend will not start without it)
openssl rand -base64 48

# Edit .env to customize ports and settings

# Start all services (backend + webapp)
docker compose up -d

# View logs
docker compose logs -f

# Stop the services
docker compose down
```

The services will be available at:
- **Web Application**: `http://localhost:3000` (or your custom `WEBAPP_PORT`)
- **Backend API**: `http://localhost:8080` (or your custom `BACKEND_PORT`)

## Environment Configuration

The application is configured using environment variables. A `sample.env` file is provided with all available options.

### Configuration File Setup

```bash
# Create your environment file from the sample
cp sample.env .env

# Edit .env to customize your deployment
nano .env  # or use your preferred editor
```

### Key Configuration Options

#### Port Configuration
- `BACKEND_PORT`: Backend service port (default: 8080)
- `WEBAPP_PORT`: Web application port (default: 3000)
- `SERVER_PORT`: Internal backend container port (default: 8080)
- `WEBAPP_SERVER_PORT`: Internal webapp container port (default: 3000)

#### Security & CORS
- `JWT_SECRET`: **Required.** Signing secret for JWT bearer tokens, at least 32 bytes. There is
  no default — the backend fails to start when it is missing, blank, or too short. Generate one
  with `openssl rand -base64 48` and use a distinct value per deployment.
- `JWT_EXPIRATION`: Token lifetime in milliseconds (default: 86400000, i.e. 24 hours)
- `APP_CORS_ALLOWED_ORIGINS`: Comma-separated list of allowed origins
  - Development: `*` (allows all origins)
  - Production: `http://localhost:3000,https://chat.example.com`

#### Validation Rules
- `APP_MESSAGE_MAX_LENGTH`: Maximum message length in characters (default: 1000)
- `APP_USERNAME_MIN_LENGTH`: Minimum username length (default: 3)
- `APP_USERNAME_MAX_LENGTH`: Maximum username length (default: 50)
- `APP_PASSWORD_MIN_LENGTH`: Minimum password length (default: 8)

#### Usage Reporting
- `USAGE_REPORTING_ENABLED`: Whether the backend reports that it is in use to the
  [trace](https://github.com/Stephenson-Software/trace) usage service (default: `true`).
  It sends one `startup` event when ready (program name `accordion` and its version only) and
  one `channel-created` event per channel created (program name only) — nothing about users,
  messages, channel names or the host. Set to `false` to turn it off; the backend logs one
  INFO line at every start saying whether it is on. See the README's "Usage reporting" section.

#### Database Configuration (H2)
- `SPRING_DATASOURCE_URL`: Database connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `SPRING_JPA_HIBERNATE_DDL_AUTO`: Schema generation strategy

#### Backend Connection (for webapp)

**Important**: The webapp uses TWO sets of backend URLs:

1. **Server-side URLs** (webapp server → backend server, internal Docker network):
   - `ACCORDION_BACKEND_URL`: Backend API base URL (default: `http://backend:8080`)
   - `ACCORDION_BACKEND_WS_URL`: Backend WebSocket URL (default: `http://backend:8080/ws`)
   - Uses Docker service name `backend` for internal communication

2. **Client-side URLs** (browser → backend server, must be publicly accessible):
   - `ACCORDION_BACKEND_CLIENT_URL`: Backend API base URL for browser (default: `http://localhost:8080`)
   - `ACCORDION_BACKEND_CLIENT_WS_URL`: Backend WebSocket URL for browser (default: `http://localhost:8080/ws`)
   - **Must use `localhost` or your public domain** - browsers cannot resolve Docker service names

### Example: Custom Ports

To run on custom ports, edit your `.env` file:

```bash
# Custom port configuration
BACKEND_PORT=9090           # Host port for backend
SERVER_PORT=9090            # Container port for backend
WEBAPP_PORT=4000            # Host port for webapp
WEBAPP_SERVER_PORT=4000     # Container port for webapp

# Server-side backend URLs (webapp server → backend, internal Docker network)
ACCORDION_BACKEND_URL=http://backend:9090
ACCORDION_BACKEND_WS_URL=http://backend:9090/ws

# Client-side backend URLs (browser → backend, must use localhost or public domain)
ACCORDION_BACKEND_CLIENT_URL=http://localhost:9090
ACCORDION_BACKEND_CLIENT_WS_URL=http://localhost:9090/ws
```

**Important Notes:**
- `BACKEND_PORT` and `SERVER_PORT` should typically be the same for simplicity
- `WEBAPP_PORT` and `WEBAPP_SERVER_PORT` should typically be the same
- When changing `SERVER_PORT`, update BOTH server-side AND client-side backend URLs
- Server-side URLs use Docker service name `backend`
- **Client-side URLs must use `localhost` or your public domain** (not `backend`)

Then start the services:

```bash
docker compose up -d
```

Access:
- Web app: `http://localhost:4000`
- Backend API: `http://localhost:9090`

## Docker Compose Configuration

The `compose.yml` file provides a complete configuration for running both services:

- **Port Mapping**: Backend accessible on `http://localhost:8080`
- **Health Checks**: Automatic health monitoring
- **Environment Variables**: Configurable via environment section
- **Restart Policy**: Automatically restarts unless stopped manually
- **Network**: Isolated bridge network for future service expansion

## Manual Docker Commands

### Building the Image

```bash
docker build -t accordion-backend:latest .
```

### Running the Container

`JWT_SECRET` is required; the container exits at startup without it. Generating a fresh secret on
each run, as below, invalidates every previously issued token — supply a stable value from your
own secret store when tokens need to survive a restart.

```bash
docker run -d \
  -p 8080:8080 \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  --name accordion-backend \
  accordion-backend:latest
```

### Viewing Logs

```bash
# Follow logs
docker logs -f accordion-backend

# View last 100 lines
docker logs --tail 100 accordion-backend
```

### Stopping and Removing

```bash
# Stop the container
docker stop accordion-backend

# Remove the container
docker rm accordion-backend

# Remove the image
docker rmi accordion-backend:latest
```

## Environment Variables

Configure the application using environment variables:

### CORS Configuration

```bash
docker run -d \
  -p 8080:8080 \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  -e APP_CORS_ALLOWED_ORIGINS="https://yourdomain.com,https://app.yourdomain.com" \
  --name accordion-backend \
  accordion-backend:latest
```

### Validation Configuration

```bash
docker run -d \
  -p 8080:8080 \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  -e APP_USERNAME_MIN_LENGTH=5 \
  -e APP_USERNAME_MAX_LENGTH=30 \
  -e APP_PASSWORD_MIN_LENGTH=12 \
  -e APP_MESSAGE_MAX_LENGTH=500 \
  --name accordion-backend \
  accordion-backend:latest
```

### All Available Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Server port |
| `JWT_SECRET` | *(none — required)* | JWT signing secret, at least 32 bytes; startup fails without it |
| `JWT_EXPIRATION` | `86400000` | JWT lifetime in milliseconds (24 hours) |
| `APP_CORS_ALLOWED_ORIGINS` | `*` | Allowed CORS origins (use specific domains in production) |
| `APP_USERNAME_MIN_LENGTH` | `3` | Minimum username length |
| `APP_USERNAME_MAX_LENGTH` | `50` | Maximum username length |
| `APP_PASSWORD_MIN_LENGTH` | `8` | Minimum password length |
| `APP_MESSAGE_MAX_LENGTH` | `1000` | Maximum message content length |
| `SPRING_JPA_SHOW_SQL` | `false` | Show SQL queries in logs |
| `SPRING_H2_CONSOLE_ENABLED` | `true` | Enable H2 console |

## Health Checks

The container includes automatic health checks that ping the `/ws` endpoint every 30 seconds. The container is considered healthy when this endpoint responds successfully.

`/ws` is used because it is one of the few endpoints Spring Security permits anonymously; every `/api/**` endpoint other than registration and login answers an unauthenticated probe with `401`, which would leave the container permanently unhealthy.

Check health status:

```bash
docker inspect --format='{{.State.Health.Status}}' accordion-backend
```

## Production Deployment

### Security Recommendations

1. **Use a Unique JWT Secret**: Generate `JWT_SECRET` with `openssl rand -base64 48`, use a
   distinct value per deployment, and keep it out of version control. Rotating the secret
   invalidates every issued token, so users must log in again.

2. **Set Specific CORS Origins**:
   ```yaml
   environment:
     - APP_CORS_ALLOWED_ORIGINS=https://yourdomain.com
   ```

3. **Use Secrets for Sensitive Data**: For production databases, use Docker secrets or environment files.

4. **Run Behind Reverse Proxy**: Use Nginx or Traefik for SSL termination and load balancing.

5. **Resource Limits**: Add resource constraints to prevent resource exhaustion:
   ```yaml
   services:
     backend:
       deploy:
         resources:
           limits:
             cpus: '1.0'
             memory: 512M
           reservations:
             cpus: '0.5'
             memory: 256M
   ```

### Using with Persistent Database

For production, replace H2 with PostgreSQL or MySQL:

```yaml
services:
  backend:
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/accordiondb
      - SPRING_DATASOURCE_USERNAME=accordion
      - SPRING_DATASOURCE_PASSWORD=secure_password
      - SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
    depends_on:
      - postgres
  
  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=accordiondb
      - POSTGRES_USER=accordion
      - POSTGRES_PASSWORD=secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

## Networking

### Connecting Frontend to Dockerized Backend

When running the frontend on the host machine and backend in Docker:

```bash
# Frontend connects to backend
./gradlew desktop:run -Daccordion.websocket.url=ws://localhost:8080/ws
```

### Running Multiple Services

The compose configuration uses a bridge network (`accordion-network`) that allows for future service expansion:

```yaml
services:
  backend:
    networks:
      - accordion-network
  
  # Add more services here
  # frontend-web:
  #   networks:
  #     - accordion-network
```

## Troubleshooting

### Webapp Cannot Connect to Backend

If the web application shows "Could not connect to server" errors:

**Root Cause**: The webapp uses JavaScript in the browser to connect to the backend. Browsers cannot resolve Docker service names like `backend`.

**Solution**: Ensure you're using the client-side URLs (not the server-side URLs):

1. **Check your `.env` file has BOTH URL sets**:
   ```bash
   # Server-side URLs (webapp server → backend, internal Docker network)
   ACCORDION_BACKEND_URL=http://backend:8080
   ACCORDION_BACKEND_WS_URL=http://backend:8080/ws
   
   # Client-side URLs (browser → backend, must use localhost or public domain)
   DOCKER_HOST_IP=localhost
   ACCORDION_BACKEND_CLIENT_URL=http://localhost:8080
   ACCORDION_BACKEND_CLIENT_WS_URL=http://localhost:8080/ws
   ```

2. **If using custom ports**, update the client URLs:
   ```bash
   # Example with custom port 9090
   BACKEND_PORT=9090
   SERVER_PORT=9090
   DOCKER_HOST_IP=localhost
   ACCORDION_BACKEND_CLIENT_URL=http://${DOCKER_HOST_IP}:9090
   ACCORDION_BACKEND_CLIENT_WS_URL=http://${DOCKER_HOST_IP}:9090/ws
   ```

3. **For production with a public domain**:
   ```bash
   DOCKER_HOST_IP=yourdomain.com
   ACCORDION_BACKEND_CLIENT_URL=https://${DOCKER_HOST_IP}
   ACCORDION_BACKEND_CLIENT_WS_URL=https://${DOCKER_HOST_IP}/ws
   ```

4. **Rebuild and restart**:
   ```bash
   docker compose down
   docker compose up -d --build
   ```

### Accessing from Other Machines on Your Network

To access the web application from other devices on your local network (e.g., phones, tablets, or other computers):

1. **Find your host machine's IP address**:
   ```bash
   # Linux/Mac
   ip addr show | grep "inet " | grep -v 127.0.0.1
   # or
   ifconfig | grep "inet " | grep -v 127.0.0.1
   
   # Windows
   ipconfig | findstr IPv4
   ```
   
   Example output: `192.168.1.100`

2. **Update your `.env` file with the host IP**:
   ```bash
   DOCKER_HOST_IP=192.168.1.100
   # URLs will automatically resolve using the DOCKER_HOST_IP variable:
   # ACCORDION_BACKEND_CLIENT_URL=http://${DOCKER_HOST_IP}:8080
   # ACCORDION_BACKEND_CLIENT_WS_URL=http://${DOCKER_HOST_IP}:8080/ws
   ```

3. **Update CORS to allow network access**:
   ```bash
   # Allow all origins (development only)
   APP_CORS_ALLOWED_ORIGINS=*
   
   # Or specify your network devices (more secure)
   APP_CORS_ALLOWED_ORIGINS=http://192.168.1.100:3000,http://192.168.1.101:3000
   ```

4. **Rebuild and restart**:
   ```bash
   docker compose down
   docker compose up -d --build
   ```

5. **Access from other devices**:
   - Open browser on any device on your network
   - Navigate to: `http://192.168.1.100:3000` (use your actual IP)
   - Login and chat!

**Important Notes**:
- Ensure your firewall allows incoming connections on the configured ports (default: 3000, 8080)
- The `DOCKER_HOST_IP` variable makes it easy to configure network access without manually updating multiple URLs
- For production deployments, always use specific CORS origins instead of `*`

### Backend Service Hangs or Fails to Start After Changing Ports

If you change ports and the backend service hangs or the health check fails:

1. **Ensure port consistency**: When changing `SERVER_PORT`, also update the backend URLs:
   ```bash
   # In .env file
   SERVER_PORT=9090
   ACCORDION_BACKEND_URL=http://backend:9090
   ACCORDION_BACKEND_WS_URL=http://backend:9090/ws
   ACCORDION_BACKEND_CLIENT_URL=http://localhost:9090
   ACCORDION_BACKEND_CLIENT_WS_URL=http://localhost:9090/ws
   ```

2. **Use matching internal/external ports for simplicity**:
   ```bash
   # Recommended configuration
   BACKEND_PORT=9090
   SERVER_PORT=9090
   ```

3. **Rebuild containers after changing configuration**:
   ```bash
   docker compose down
   docker compose up -d --build
   ```

4. **Check health check status**:
   ```bash
   docker compose ps
   # Look for "health: starting" or "unhealthy" status
   ```

### Container Won't Start

```bash
# Check logs
docker compose logs backend

# Check container status
docker compose ps
```

### Port Already in Use

```bash
# Change BACKEND_PORT in .env file
BACKEND_PORT=8081

# Or specify directly
docker compose up -d
```

### Health Check Failing

```bash
# Increase start period if application takes longer to start
healthcheck:
  start_period: 60s
```

Point the probe at a publicly reachable endpoint. `/ws` is permitted anonymously, whereas every
`/api/**` endpoint other than `/api/users/register` and `/api/users/login` returns `401` to an
unauthenticated probe and can never report healthy.

### Backend Container Exits Immediately

Check the logs for a missing or unusable signing secret:

```bash
docker compose logs backend
```

`JWT_SECRET` is required and has no default. A missing value fails placeholder resolution, and a
blank or shorter-than-32-byte value is rejected at startup. Set it in `.env` and recreate the
service with `docker compose up -d`.

### Build Issues

```bash
# Clean build
docker compose build --no-cache

# Build with verbose output
docker compose build --progress=plain
```

## Development Workflow

### Live Development with Docker

For development, mount the source code as a volume:

```bash
# Build and run with hot reload (requires spring-boot-devtools)
docker run -d \
  -p 8080:8080 \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  -v $(pwd)/backend/src:/app/src \
  --name accordion-backend-dev \
  accordion-backend:latest
```

### Debugging

Run the container with debug port exposed:

```bash
docker run -d \
  -p 8080:8080 \
  -p 5005:5005 \
  -e JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
  --name accordion-backend \
  accordion-backend:latest
```

Connect your IDE debugger to `localhost:5005`.

## Image Optimization

The Dockerfile uses multi-stage builds to create a minimal runtime image:

- **Build Stage**: Uses `maven:3.9-eclipse-temurin-17-alpine` (~400MB)
- **Runtime Stage**: Uses `eclipse-temurin:17-jre-alpine` (~170MB)
- **Final Image Size**: ~250MB (includes application)

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Build Docker Image
  run: docker build -t accordion-backend:${{ github.sha }} .

- name: Push to Registry
  run: |
    docker tag accordion-backend:${{ github.sha }} ghcr.io/username/accordion-backend:latest
    docker push ghcr.io/username/accordion-backend:latest
```

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
- [Spring Boot with Docker](https://spring.io/guides/gs/spring-boot-docker/)
