#!/usr/bin/env bash

set -e

if [ "$1" == "java-build" ]; then

    mvn clean install -DskipTests

elif [ "$1" == "docker-build" ]; then

    mvn docker:build -P docker

elif [ "$1" == "docker-push" ]; then

    docker push nimbleplatform/business-process-service:latest

fi

