# Zincoid

Personal website backend, built with Spring Boot 4.1 + MyBatis-Plus.

## Tech Stack

- **Java 21** / Spring Boot 4.1
- **MyBatis-Plus** + MySQL
- **JWT** authentication
- **Flexmark** Markdown rendering

## Quick Start

```bash
# 1. Create the database
# 2. Edit src/main/resources/application-dev.yml with your database credentials
# 3. Package and run
./mvnw package -DskipTests
java -jar target/Zincoid-springboot-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Server starts at `http://localhost:8080`.

> The frontend is a separate Vue 3 project — see `Zincoid-vue`.
