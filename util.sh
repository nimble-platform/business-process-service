#!/usr/bin/env bash


set -e

if [ "$1" == "java-build" ]; then

    mvn clean install -DskipTests

elif [ "$1" == "docker-build" ]; then

    mvn docker:build -P docker

elif [ "$1" == "docker-build-staging" ]; then

    mvn docker:build -P docker -DdockerImageTag=staging

elif [ "$1" == "docker-build-efack8s" ]; then

    mvn docker:build -P docker -DdockerImageTag=newk8snimble

elif [ "$1" == "docker-push" ]; then

    mvn docker:push -P docker

elif [ "$1" == "all" ]; then

    mvn clean install -DskipTests
    mvn docker:build docker:push -P docker

fi

