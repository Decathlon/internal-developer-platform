FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /usr/app

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre-jammy
WORKDIR /opt

ENV SERVER_PORT=8080 \
    PORT=8080

RUN groupadd -r -g 1001 idp-core-group && \
    useradd -r -u 1001 -g idp-core-group -s /bin/false idp-core-user

COPY --from=build /usr/app/target/idp-core*.jar /opt/app.jar

USER 1001:1001

HEALTHCHECK --interval=30s --timeout=5s --start-period=25s --retries=3 \
    CMD wget -q --spider "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["java", "-jar", "/opt/app.jar"]
