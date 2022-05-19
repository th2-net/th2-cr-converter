FROM gradle:7.4.2-jdk11 AS build
ARG release_version
COPY ./ .
RUN gradle --no-daemon clean build -Prelease_version=${release_version}

RUN mkdir /home/app
RUN cp ./build/libs/*.jar /home/app/application.jar

FROM adoptopenjdk/openjdk11:alpine
COPY --from=build /home/app /home/app
WORKDIR /home/app

ENTRYPOINT ["java", "-Dlog4j2.configurationFile=file:/var/th2/config/log4j2.properties", "-jar", "/home/app/application.jar"]