pipeline {
    agent any

    tools {
        // Matches the Java 21 / Maven environment in your custom Jenkins container
        maven 'Maven'
    }

    environment {
        SCANNER_HOME = tool 'SonarScanner'
        IMAGE_NAME = 'eventpro-app'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('1. Checkout') {
            steps {
                checkout scm
            }
        }

        stage('2. Build & Unit Tests (JUnit 5 / Mockito)') {
                    steps {
                        sh 'chmod +x mvnw'
                        // Added the -U flag to force update/re-download of plugins like surefire
                        sh './mvnw clean test-compile test jacoco:report -U'
                    }
                    post {
                        always {
                            junit 'target/surefire-reports/*.xml'
                        }
                    }
                }

        stage('3. Code Coverage (JaCoCo)') {
                    steps {
                        // Archive the JaCoCo execution data and HTML reports so you can download them from Jenkins
                        archiveArtifacts artifacts: 'target/jacoco.exec, target/site/jacoco/**/*', allowEmptyArchive: true
                    }
                }

        stage('4. SonarQube Static Analysis') {
                    steps {
                        script {
                            // Reference the SonarScanner tool configured in Jenkins Global Tools
                            def scannerHome = tool 'SonarScanner'
                            withSonarQubeEnv('SonarQube-Server') {
                                sh "${scannerHome}/bin/sonar-scanner \
                                    -Dsonar.projectKey=EventPro \
                                    -Dsonar.projectName=EventPro \
                                    -Dsonar.java.binaries=target/classes \
                                    -Dsonar.sources=src/main/java \
                                    -Dsonar.tests=src/test/java \
                                    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml"
                            }
                        }
                    }
                }

        stage('5. Quality Gate Check') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    // Pauses pipeline until SonarQube evaluates the Quality Gate rules (>=80% coverage, A rating, 0 blockers)
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('6. Package JAR Artifact') {
            steps {
                // Generates the executable Spring Boot 3.2.5 JAR artifact (skipping tests as they already passed)
                sh './mvnw package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

//        stage('7. Dockerisation') {
//                    steps {
//                        script {
//                            // Use single quotes to prevent Groovy from trying to evaluate variables prematurely
//                            sh 'docker build -f docker.jenkins -t eventpro-app:${BUILD_NUMBER} -t eventpro-app:latest .'
//                        }
//                    }
//                }
    }

    post {
        success {
            echo 'Pipeline completed successfully: EventPro is tested, analyzed, quality-approved, and dockerized!'
        }
        failure {
            echo 'Pipeline failed: Check test reports, SonarQube Quality Gate, or build logs.'
        }
    }
}
