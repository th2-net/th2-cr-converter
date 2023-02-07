FROM gradle:7.6-jdk17 AS build
ARG release_version
COPY ./ .
RUN gradle --no-daemon clean installBootDist -Prelease_version=${release_version}

RUN mkdir /home/app
RUN cp -r ./build/install/th2-cr-converter-boot/ /home/app/

FROM eclipse-temurin:17-alpine
COPY --from=build /home/app /home/app
WORKDIR /home/app

ENTRYPOINT ["/home/app/th2-cr-converter-boot/bin/th2-cr-converter"]