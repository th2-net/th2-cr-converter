FROM gradle:7.6-jdk17 AS build
ARG release_version
COPY ./ .
RUN gradle clean build dockerPrepare -Prelease_version=${Prelease_version}

FROM eclipse-temurin:17-alpine
WORKDIR /home
COPY --from=build /home/gradle/build/docker .

ENTRYPOINT ["java", "-Dlog4j2.configurationFile=file:/var/th2/config/log4j2.properties", "-jar", "/home/main/layers/application.jar"]