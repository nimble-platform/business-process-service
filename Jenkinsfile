node ('nimble-jenkins-slave') {

    stage('Clone and Update') {
        git(url: 'https://github.com/nimble-platform/business-process-service.git', branch: env.BRANCH_NAME)
    }

    stage('Build Dependencies') {
        sh 'rm -rf common'
        sh 'git clone https://github.com/nimble-platform/common'
        dir ('common') {
            sh 'mvn clean install'
        }
    }

    stage ('Build Java') {
        sh 'mvn install -DskipTests'
    }

    stage ('Build Docker') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh '/bin/bash -xe util.sh docker-build'
        }
    }

    // only push and update the master branch
    if (env.BRANCH_NAME == 'master') {
        stage('Push Docker') {
            withDockerRegistry([credentialsId: 'NimbleDocker']) {
                sh '/bin/bash -xe util.sh docker-push'
            }
        }

        stage('Apply to Cluster') {
            // cluster is down at the moment
            //        sh 'kubectl apply -f kubernetes/deploy.yml -n prod --validate=false'
        }
    }
}
