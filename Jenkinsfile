node ('nimble-jenkins-slave') {
    def app
    stage('Clone and Update') {
        git(url: 'https://github.com/nimble-platform/business-process-service.git', branch: 'master')
    }

    stage('Build Dependencies') {
        sh 'rm -rf common'
        sh 'git clone https://github.com/nimble-platform/common'
        dir ('common') {
            sh 'mvn clean install'
        }
    }

    stage ('Build Java') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh '/bin/bash -xe util.sh java-build'
        }
    }

    stage ('Build Docker') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh '/bin/bash -xe util.sh docker-build'
        }
    }

    stage ('Push Docker image') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh '/bin/bash -xe util.sh docker-push'
        }
    }

    stage ('Apply to Cluster') {
        // cluster is down at the moment
//        sh 'kubectl apply -f kubernetes/deploy.yml -n prod --validate=false'
    }
}
