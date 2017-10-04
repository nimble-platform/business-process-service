node ('nimble-jenkins-slave') {
    def app
    stage('Clone and Update') {
        // slackSend 'Started build no. ${env.BUILD_ID} of ${env.JOB_NAME}'
        git(url: 'https://github.com/nimble-platform/business-process-service.git', branch: 'master')
    }

    stage ('Build Docker Image') {
        sh '/bin/bash -xe util.sh docker-build'
    }

    stage ('Push Docker image') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh '/bin/bash -xe util.sh docker-push'
        }
    }

    stage ('Apply to Cluster') {
        sh 'kubectl apply -f kubernetes/deploy.yml -n prod --validate=false'
    }
}
