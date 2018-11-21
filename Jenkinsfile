node('nimble-jenkins-slave') {

    stage('Clone and Update') {
        git(url: 'https://github.com/nimble-platform/business-process-service.git', branch: env.BRANCH_NAME)
    }

    stage('Build Dependencies') {
        sh 'rm -rf common'
        sh 'git clone https://github.com/nimble-platform/common'
        dir('common') {
            sh 'git checkout ' + env.BRANCH_NAME
            sh 'mvn clean install'
        }
    }

    stage('Build Java') {
        sh 'mvn clean package -DskipTests'
    }

    if (env.BRANCH_NAME == 'staging') {
        stage('Build Docker') {
            sh '/bin/bash -xe util.sh docker-build-staging'
        }

        stage('Push Docker') {
            sh 'docker push nimbleplatform/business-process-service:staging'
        }

        stage('Deploy') {
            sh 'ssh staging "cd /srv/nimble-staging/ && ./run-staging.sh restart-single business-process-service"'
        }
    } else {
        stage('Build Docker') {
            sh '/bin/bash -xe util.sh docker-build'
        }
    }

    if (env.BRANCH_NAME == 'master') {

        stage('Push Docker') {
            sh 'mvn docker:build -P docker'
        }

        stage('Push Docker') {
            sh 'mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version' // fetch dependencies
            sh 'docker push nimbleplatform/business-process-service:$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v \'\\[\')'
            sh 'docker push nimbleplatform/business-process-service:latest'
        }

        stage('Deploy MVP') {
            sh 'ssh nimble "cd /data/deployment_setup/prod/ && sudo ./run-prod.sh restart-single business-process-service"'
        }

        stage('Deploy FMP') {
            sh 'ssh fmp-prod "cd /srv/nimble-fmp/ && ./run-fmp-prod.sh restart-single business-process-service"'
        }
    }
}
