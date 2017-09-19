#!/usr/bin/env bash

#!/usr/bin/env bash

set -e

if [ "$1" == "docker-build" ]; then

    mvn clean install -DskipTests
    mvn docker:build -P docker

elif [ "$1" == "docker-push" ]; then

    docker push nimbleplatform/business-process-service:latest

fi

