node('nimble-jenkins-slave') {

    // -----------------------------------------------
    // --------------- Staging Branch ----------------
    // -----------------------------------------------
    if (env.BRANCH_NAME == 'staging') {

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

        stage('Run Tests') {
            sh '#!/bin/bash \n source /var/jenkins_home/test_staging.sh && mvn clean test'
        }

        stage('Build Java') {
            sh 'mvn clean package -DskipTests'
        }

        stage('Build Docker') {
            sh '/bin/bash -xe util.sh docker-build-staging'
        }

        stage('Push Docker') {
            sh 'docker push nimbleplatform/business-process-service:staging'
        }

        stage('Deploy') {
            sh 'ssh staging "cd /srv/nimble-staging/ && ./run-staging.sh restart-single business-process-service"'
        }
    }

    // -----------------------------------------------
    // ---------------- Master Branch ----------------
    // -----------------------------------------------
    if (env.BRANCH_NAME == 'master') {

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
    }

    // -----------------------------------------------
    // ---------------- Release Tags -----------------
    // -----------------------------------------------
    if (env.TAG_NAME ==~ /^\d+.\d+.\d+$/) {

        stage('Clone and Update') {
            git(url: 'https://github.com/nimble-platform/business-process-service.git', branch: 'master')
        }

        stage('Build Dependencies') {
            sh 'rm -rf common'
            sh 'git clone https://github.com/nimble-platform/common'
            dir('common') {
                sh 'git checkout master'
                sh 'mvn clean install'
            }
        }

        stage('Set version') {
            sh 'mvn versions:set -DnewVersion=' + env.TAG_NAME
        }

        stage('Build Java') {
            sh 'mvn clean package -DskipTests'
        }

        stage('Build Docker') {
            sh 'mvn docker:build -P docker'
        }

        stage('Push Docker') {
            sh 'docker push nimbleplatform/business-process-service:' + env.TAG_NAME
            sh 'docker push nimbleplatform/business-process-service:latest'
        }

        stage('Deploy MVP') {
            sh 'ssh nimble "cd /data/deployment_setup/prod/ && sudo ./run-prod.sh restart-single business-process-service"'
        }

        stage('Deploy FMP') {
            sh 'ssh fmp-prod "cd /srv/nimble-fmp/ && ./run-fmp-prod.sh restart-single business-process-service"'
        }

        stage('Deploy Efactory') {
            sh 'ssh efac-prod "cd /srv/nimble-efac/ && ./run-efac-prod.sh restart-single business-process-service"'
        }
    }
}
