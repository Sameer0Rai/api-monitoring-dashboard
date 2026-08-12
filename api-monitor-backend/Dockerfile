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

# Explicit memory bounds, not JVM defaults - Java 17 is container-aware and will size
# the heap off available RAM, but leaves metaspace/thread-stacks/native memory
# effectively uncapped. On a full-size server that's fine; on a memory-constrained
# free-tier host (512MB) it lets total RSS creep past the container's limit over time,
# which gets the process SIGKILLed by the host's OOM killer (exit code 137) rather than
# failing predictably inside the JVM. SerialGC also has far lower memory overhead than
# the default G1GC, which matters more than its lower throughput at this scale.
ENTRYPOINT ["java", "-Xmx300m", "-XX:MaxMetaspaceSize=128m", "-Xss256k", "-XX:+UseSerialGC", "-jar", "app.jar"]
