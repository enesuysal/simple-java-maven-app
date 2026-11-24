pipeline {
  agent any
  tools { maven 'maven-3' }
  options { timestamps() }
  stages {
    stage('Build') {
      steps { sh 'mvn -B -DskipTests clean package' }
    }
    stage('Test') {
      steps { sh 'mvn -B test' }
      post { always { junit 'target/surefire-reports/*.xml' } }
    }
    stage('Archive') {
      steps { archiveArtifacts artifacts: 'target/*.jar', fingerprint: true }
    }
    stage('Publish to Nexus') {
      steps {
        writeFile file: 'settings.xml', text: '''<settings>
  <servers>
    <server>
      <id>nexus</id>
      <username>${env.NEXUS_USER}</username>
      <password>${env.NEXUS_PASS}</password>
    </server>
  </servers>
</settings>'''
        withCredentials([usernamePassword(credentialsId: 'nexus-creds',
            usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
          sh '''mvn -B -s settings.xml -DskipTests deploy \
            -DaltDeploymentRepository=nexus::http://nexus:8081/repository/maven-snapshots/'''
        }
      }
    }
    stage('Docker Build & Push') {
      steps {
 writeFile file: 'Dockerfile', text: '''FROM eclipse-temurin:21-jdk
ARG APP_VERSION=dev
ENV APP_VERSION=$APP_VERSION
COPY Server.java /app/Server.java
CMD ["java","/app/Server.java"]
'''
        withCredentials([usernamePassword(credentialsId: 'nexus-creds',
            usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
          sh '''
            docker build --build-arg APP_VERSION=${BUILD_NUMBER} -t localhost:5001/my-app:${BUILD_NUMBER} .
            echo "$NEXUS_PASS" | docker login localhost:5001 -u "$NEXUS_USER" --password-stdin
            docker push localhost:5001/my-app:${BUILD_NUMBER}
          '''
        }
      }
    }
    stage('Deploy to Kubernetes') {
      steps {
        withCredentials([file(credentialsId: 'kubeconfig-kind', variable: 'KUBECONFIG')]) {
          sh '''
            kind load docker-image localhost:5001/my-app:${BUILD_NUMBER} --name cka
            helm upgrade --install my-app helm/my-app -n demo --create-namespace \
              --set image.tag=${BUILD_NUMBER} --rollback-on-failure --timeout 2m
            kubectl -n demo rollout status deploy/my-app --timeout=60s
          '''
        }
      }
    }
  }
  post {
    failure { echo 'Build failed' }
  }
}
