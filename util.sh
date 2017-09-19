#!/usr/bin/env bash

#!/usr/bin/env bash

set -e

if [ "$1" == "docker-build" ]; then

    mvn clean install -DskipTests
    mvn -f -P docker

elif [ "$1" == "docker-push" ]; then

    docker push nimbleplatform/frontend-service:latest

fi

