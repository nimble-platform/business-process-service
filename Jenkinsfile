node('nimble-jenkins-slave') {

    stage('Clone and Update') {
        git(url: 'https://github.com/nimble-platform/business-process-service.git', branch: env.BRANCH_NAME)
    }

    stage('Build Dependencies') {
        sh 'rm -rf common'
        sh 'git clone https://github.com/nimble-platform/common'
        dir('common') {
            sh 'mvn clean install'
        }
    }

    stage('Build Java') {
        sh 'mvn clean package -DskipTests'
    }

    stage('Build Docker') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh '/bin/bash -xe util.sh docker-build'
        }
    }

    if (env.BRANCH_NAME == 'master') {
        stage('Deploy') {
            sh 'ssh nimble "cd /data/nimble_setup/ && sudo ./run-prod.sh restart-single business-process-service"'
        }
    }

    // Kubernetes is disabled for now
//    if (env.BRANCH_NAME == 'master') {
//        stage('Push Docker') {
//            withDockerRegistry([credentialsId: 'NimbleDocker']) {
//                sh '/bin/bash -xe util.sh docker-push'
//            }
//        }
//
//        stage('Apply to Cluster') {
//            sh 'kubectl apply -f kubernetes/deploy.yml -n prod --validate=false'
//        }
//    }
}
