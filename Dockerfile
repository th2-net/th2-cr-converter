FROM gradle:7.6-jdk11 AS build
ARG release_version
COPY ./ .
RUN gradle clean build dockerPrepare -Prelease_version=${Prelease_version}

FROM eclipse-temurin:11-alpine
COPY --from=build /home/app /home/app
WORKDIR /home/app

ENTRYPOINT ["java", "-Dlog4j2.configurationFile=file:/var/th2/config/log4j2.properties", "-jar", "/home/main/layers/application.jar"]