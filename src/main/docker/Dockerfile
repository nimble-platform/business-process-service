FROM openjdk:8
MAINTAINER Salzburg Research <nimble-srfg@salzburgresearch.at>
VOLUME /tmp
ARG finalName
ENV JAR '/'$finalName
ARG port
ADD $finalName $JAR
RUN touch $JAR
ENV PORT 8085
EXPOSE $PORT

RUN env

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar", "/business-process-service.jar"]