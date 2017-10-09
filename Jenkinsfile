node ('nimble-jenkins-slave') {
    def app
    stage('Clone and Update') {
        git(url: 'https://github.com/nimble-platform/business-process-service.git', branch: 'master')
    }

    stage ('Build Docker Image') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh '/bin/bash -xe util.sh docker-build'
            sh 'sleep 5' // wait for image to be propagated locally
        }
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
