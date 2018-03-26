node ('nimble-jenkins-slave') {
    stage('Clone and Update') {
        git(url: 'https://github.com/nimble-platform/business-process-service.git', branch: 'k8s-integration')

        sh 'git clone https://github.com/nimble-platform/common.git ; cd common ; mvn clean install'
    }

    stage ('Build docker image') {
        sh 'mvn clean package -DskipTests'
        sh 'mvn -f catalogue-service-micro/pom.xml docker:build -DdockerImageTag=${BUILD_NUMBER} -P docker'

        sh 'sleep 5' // For the tag to populate
    }

    stage ('Push docker image') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh 'docker push nimbleplatform/business-process-service:${BUILD_NUMBER}'
        }
    }

    stage ('Deploy') {
        sh ''' sed -i 's/IMAGE_TAG/'"$BUILD_NUMBER"'/g' kubernetes/deploy.yml '''
        sh 'kubectl apply -f kubernetes/deploy.yml -n prod --validate=false'
        sh 'kubectl apply -f kubernetes/svc.yml -n prod --validate=false'
    }

    stage ('Print-deploy logs') {
        sh 'sleep 60'
        sh 'kubectl -n prod logs deploy/business-process-service -c catalogue-service'
    }
}
