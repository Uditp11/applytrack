# Stage 1: build the jar with the full JDK and Maven wrapper.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy only the build files first, so the dependency download is cached
# as its own layer and reused until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

COPY src src
# Tests already run in CI with "mvn verify"; the image build only packages.
RUN ./mvnw -B -q package -DskipTests

# Stage 2: run the jar on a smaller JRE-only image.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an unprivileged user instead of root.
RUN useradd --system --uid 10001 applytrack
USER applytrack

COPY --from=build /workspace/target/applytrack-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
