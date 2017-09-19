#!/usr/bin/env bash

#!/usr/bin/env bash

set -e

if [ "$1" == "docker-build" ]; then

    mvn clean install -DskipTests -Drun.jvmArguments="-Dspring.profiles.active=local_dev"

elif [ "$1" == "docker-push" ]; then

    docker push nimbleplatform/frontend-service:latest

fi

