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
        writeFile file: 'Dockerfile', text: '''FROM eclipse-temurin:21-jre
ARG APP_VERSION=dev
ENV APP_VERSION=$APP_VERSION
COPY target/*.jar /app/app.jar
CMD ["sh","-c","while true; do echo version=$APP_VERSION; java -cp /app/app.jar com.mycompany.app.App; sleep 10; done"]
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
  }
  post {
    failure { echo 'Build failed' }
  }
}
