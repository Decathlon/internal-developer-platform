FROM eclipse-temurin:25-jre-alpine

ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-Dotel.java.global-autoconfigure.enabled=true"

RUN apk add --no-cache shadow && \
    groupadd -r idp-core-group && \
    useradd --no-log-init -r -g idp-core-group idp-core-user && \
    apk del shadow

USER idp-core-user:idp-core-group

WORKDIR /opt
COPY ./target/idp-core*.jar /opt/app.jar

ENTRYPOINT ["java", "-jar", "/opt/app.jar"]
