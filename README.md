# Concurrent Framework — Building a Concurrent Web Server from Scratch

A minimalist web framework built in pure Java without external dependencies, implementing a concurrent HTTP server with support for annotations, dependency injection through reflection, and graceful shutdown.

---

## Architecture

### Project Structure

```
src/main/java/org/example/
├── MicroSpringboot.java              → Entry point: scans annotations and bootstraps
├── HttpServer.java                   → Concurrent HTTP server, request routing
├── ConcurrentRequest.java            → Thread pool manager (ExecutorService)
├── Anotaciones/
│   ├── RestController.java          → Marks a class as a web component
│   ├── GetMapping.java              → Maps methods to HTTP GET routes
│   └── RequestParam.java            → Injects query string parameters
└── Controller/
    ├── HelloController.java         → Example: GET /hello route
    └── GreetingController.java      → Example: GET /greeting with parameters
```

### Execution Flow

```
MicroSpringboot.main(args)
    
1. Scans classpath looking for @RestController classes
2. Automatically registers methods marked with @GetMapping
3. Instantiates ConcurrentRequest(10) - pool of 10 threads
4. Starts HttpServer on port 35000
    
For each HTTP request:
    → HttpServer.accept(Socket)
    → ConcurrentRequest.executor.execute(handleRequest)
    → Available thread processes the request
    → Extracts parameters from query string
    → Invokes corresponding method via reflection
    → Returns HTML response
```

---

## Requirements

- **Java 21** or higher
- **Maven 3.9** or higher
- **Docker** (optional, for containerization)

Verify installation:
```bash
java -version
mvn -version
docker --version
```

---

## Installation and Execution

### 1. Clone/Download the repository
```bash
git clone <repository-url>
cd concurrentFramework
```

### 2. Compile the project
```bash
mvn clean compile
```

### 3. Run locally
```bash
mvn compile exec:java -Dexec.mainClass="org.example.MicroSpringboot"
```

The server will be available at `http://localhost:35000`

### 4. With Docker

**Build image:**
```bash
docker-compose build
```

**Run:**
```bash
docker-compose up
```

Access: `http://localhost:35000`

---

## Framework Usage

### Create a Web Controller

Use `@RestController` and `@GetMapping`:

```java
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Concurrent Framework!";
    }
}
```

### With Query String Parameters

```java
@RestController
public class SearchController {

    @GetMapping("/search")
    public String search(
        @RequestParam(value = "q", defaultValue = "docs") String query
    ) {
        return "Searching for: " + query;
    }
}
```

Usage:
```bash
curl "http://localhost:35000/search?q=java"
```

---

## Key Features

### 1. **Efficient Concurrency**

The framework uses a **fixed thread pool (10 threads)** managed by `ConcurrentRequest`:

```java
ExecutorService executor = Executors.newFixedThreadPool(10);
```

The main server accepts connections and delegates them to the executor for parallel processing:

```java
while (!concurrentRequest.isShuttingDown()) {
    Socket client = serverSocket.accept();
    concurrentRequest.executor.execute(() -> handleRequest(client));
}
```

### 2. ** Shutdown**

The framework supports shutdown without losing in-progress requests.

**Shutdown endpoint:**
```bash
curl http://localhost:35000/shutdown
```

**Behavior:**
1. Immediately rejects new connections
2. Waits up to 10 seconds for active requests to complete
3. Closes the server without interruptions

---

##  How Reflection Works

The framework uses Java Reflection to:

1. **Scan the classpath** for classes with `@RestController`
2. **Dynamically register** methods with `@GetMapping`
3. **Inject parameters** from the query string using `@RequestParam`
4. **Invoke methods** at runtime

Example internally:

```java
// Scanning
Class<?> controllerClass = Class.forName("org.example.HelloController");
if (controllerClass.isAnnotationPresent(RestController.class)) {
    Object instance = controllerClass.getDeclaredConstructor().newInstance();
    
    for (Method method : controllerClass.getMethods()) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            // Registers the route and method
            String path = method.getAnnotation(GetMapping.class).value();
            controllerMethods.put(path, method);
        }
    }
}

// At request time: m.invoke(instance, args);
```

---

## Docker

### Dockerfile

The project uses **multi-stage build** to optimize size:

```dockerfile
# Stage 1: Builder
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve
COPY src src
RUN mvn clean package -DskipTests

# Stage 2: Runtime (JRE only)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/classes ./classes
ENV PORT=35000
EXPOSE 35000
ENTRYPOINT ["java", "-cp", "classes", "org.example.MicroSpringboot"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  framework:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: concurrent-framework
    ports:
      - "35000:35000"
    environment:
      - PORT=35000
    restart: unless-stopped
```

**Start:**
```bash
docker-compose up
```

---

## Usage Examples

### Simple Request
```bash
curl http://localhost:35000/hello
```

### With Parameters
```bash
curl "http://localhost:35000/greeting?name=John"
```

### Server Shutdown
```bash
curl http://localhost:35000/shutdown
```

---
### docker deployment
add a tag
<img width="961" height="38" alt="image" src="https://github.com/user-attachments/assets/4b9e03ba-dff5-4034-b1c9-3f16bf456bab" />
verify
<img width="880" height="24" alt="image" src="https://github.com/user-attachments/assets/30ae636a-f8d2-4b0e-85e7-7fe2cd88d29b" />
look in docker hub
<img width="1887" height="516" alt="image" src="https://github.com/user-attachments/assets/0a6dc456-bdd4-402b-8b61-df6b26461f78" />


## Author

Juan Feñipe Rangel Rodriguez
