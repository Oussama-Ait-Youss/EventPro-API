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
                        // Grant execution rights to Maven wrapper
                        sh 'chmod +x mvnw'
                        // Compiles code and runs unit tests, generating JaCoCo execution data
                        sh './mvnw clean test-compile test jacoco:report'
                    }
                    post {
                        always {
                            // Archives JUnit test reports in Jenkins
                            junit 'target/surefire-reports/*.xml'
                        }
                    }
                }

        stage('3. Code Coverage (JaCoCo)') {
            steps {
                // Publishes JaCoCo coverage report graphs in Jenkins dashboard
                jacoco(
                    execPattern: 'target/jacoco.exec',
                    classPattern: 'target/classes',
                    sourcePattern: 'src/main/java',
                    minimumBranchCoverage: '80',
                    minimumLineCoverage: '80'
                )
            }
        }

        stage('4. SonarQube Static Analysis') {
            steps {
                withSonarQubeEnv('SonarQube-Server') {
                    // Runs SonarScanner to analyze code smells, bugs, duplications, and coverage
                    sh './mvnw sonar:sonar \
                        -Dsonar.projectKey=EventPro \
                        -Dsonar.projectName=EventPro \
                        -Dsonar.java.binaries=target/classes \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
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

        stage('7. Dockerisation') {
            steps {
                // Builds versioned Docker image using the Docker CLI mounted from the host
                script {
                    sh "docker build -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} -t ${env.IMAGE_NAME}:latest ."
                }
            }
        }
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
