# ---- Build stage -----------------------------------------------------------
# Builds the jar with Maven inside the container so the image is reproducible
# without requiring Maven or a matching JDK on the host.
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy the POM first and download dependencies into a separate layer so that
# `docker build` only re-downloads dependencies when pom.xml actually changes,
# not on every source-code edit.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests


# ---- Runtime stage ----------------------------------------------------------
# A JRE (not a full JDK) is enough to run the jar, and Alpine keeps the final
# image small.
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S monitor && adduser -S monitor -G monitor
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

RUN chown -R monitor:monitor /app
USER monitor

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
