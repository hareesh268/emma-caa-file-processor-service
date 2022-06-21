#!/usr/bin/env groovy
@Library("com.optum.jenkins.pipeline.library@v0.3.5") _
import com.optum.jenkins.pipeline.library.artifactory.Artifactory

//Docker related parameter name and value
String dockerHost = 'docker.repo1.uhc.com'
String dockerNamespace = 'emma'
String dockerRepository = 'emma-caa-file-processor-service'
String dockerImageName = "$dockerHost/$dockerNamespace/$dockerRepository"
String dockerCredentialId = '00942104-29fa-4854-b84a-5b621a0ed48e'
String dockerTagPrefix = new Date().format('yyyy-MM-dd')
String microserviceName = "fileProcessorService"

// Kubernetes information
String kubernetesClusterIPAddress = "10.202.2.232"

// Approval information
String failureNotifyList = 'DWMP_Dev@ds.uhc.com'
String successNotifyList = 'DWMP_Dev@ds.uhc.com'
String approverList = "skhatr1,npatel76,mpeke,sjain195,jratnak1,nmoham31,praut4"

def APP_NAME='emma-caa-file-processor-service'
EmailNotifyListToDL = 'DWMP_Dev@ds.uhc.com'
EmailNotifyListToApprover1 = 'DWMP_Dev@ds.uhc.com'
EmailNotifyListToApprover2 = 'DWMP_Dev@ds.uhc.com'

approver1 = 'emma_caa_jenkins_level1_approver_prod'
approver2 = 'emma_caa_jenkins_level2_approver_prod'

def start_ts = node {sh(script: "date --utc +%Y%m%d_%H%M%S",returnStdout: true).trim().toString()}
String user = ''
String user_id = ''
String user_email = '' 

node {
		wrap([$class: 'BuildUser']) {
			 user = "${BUILD_USER}"
			 user_id = "${BUILD_USER_ID}"
			 user_email = "${BUILD_USER_EMAIL}"
		}
	}
	
def regions = [
        "dev"      : [
                "k8s_credentials": "9a6861dd-a20d-412e-b094-f9498ecbb9b1",
                "k8s_cluster"    : kubernetesClusterIPAddress,
                "k8s_namespace"  : "emma-dev",
                "isProduction"   : false,
                "tag"            : "dev"
        ],
        "test"     : [
                "k8s_credentials"     : "d801a511-0683-46ed-bac3-339e9b28908e",
                "k8s_cluster"         : kubernetesClusterIPAddress,
                "k8s_namespace"       : "emma-test",
                "isProduction"        : false,
                "ApproverList"        : approverList,
                "ApprovalWaitTimeUnit": "HOURS",
                "ApprovalWaitTime"    : 3,
                "tag"                 : "test"
        ],
        "stage_ctc": [
                "k8s_credentials"     : "e1ff42dd-34ea-4ccc-8ed4-8f8949816325",
                "k8s_cluster"         : "10.202.2.252",
                "k8s_namespace"       : "emma-stage",
                "isProduction"        : false,
                "ApproverList"        : approverList,
                "ApprovalWaitTimeUnit": "HOURS",
                "ApprovalWaitTime"    : 3,
                "tag"                 : "stage_ctc"
        ],
        "stage_elr": [
                "k8s_credentials"     : "d7410614-4c3a-4f75-8eee-48e671ef0bd9",
                "k8s_cluster"         : "10.49.2.252",
                "k8s_namespace"       : "emma-stage",
                "isProduction"        : false,
                "ApproverList"        : approverList,
                "ApprovalWaitTimeUnit": "HOURS",
                "ApprovalWaitTime"    : 3,
                "tag"                 : "stage_elr"
        ],
        "prod_ctc" : [
                "k8s_credentials"     : "dffd8b24-7313-47fa-a7b2-f634906a0a86",
                "k8s_cluster"         : "10.202.2.252",
                "k8s_namespace"       : "emma-prod",
                "isProduction"        : true,
                "ApproverList"        : approverList,
                "ApprovalWaitTimeUnit": "HOURS",
                "ApprovalWaitTime"    : 3,
                "tag"                 : "prod_ctc"
        ],
        "prod_elr" : [
                "k8s_credentials"     : "2db1b336-ed12-4452-9395-0ed62752c533",
                "k8s_cluster"         : "10.49.2.252",
                "k8s_namespace"       : "emma-prod",
                "isProduction"        : true,
                "ApproverList"        : approverList,
                "ApprovalWaitTimeUnit": "HOURS",
                "ApprovalWaitTime"    : 3,
                "tag"                 : "prod_elr"
        ]


]

// last_run_stage is updated as each stage runs. If the build fails, last_run_stage will hold the name of the failing stage
String last_run_stage = "No stage has started"
String proceedToBuild = true
String region = ""

pipeline {
    parameters {
        choice(choices: 'no\nyes', description: 'Do you want to deploy the specific Docker Build?', name: 'deploySpeicifDockerBuild')
        text(name: 'dockerBuild', defaultValue: '', description: 'Enter Docker Build')
        choice(choices: 'no\nyes', description: 'Do you want to perform a Fortify scan?', name: 'fortifyscan')
        choice(choices: 'no\nyes', description: 'Do you want to perform a SonarQube scan?', name: 'sonarscan')
        choice(choices: 'no\nyes', description: 'Do you want to deploy in Dev?', name: 'dev')
        choice(choices: 'no\nyes', description: 'Do you want to deploy in Test?', name: 'test')
        choice(choices: 'no\nyes', description: 'Do you want to deploy in Stage CTC Data Center?', name: 'stage_ctc')
        choice(choices: 'no\nyes', description: 'Do you want to deploy in Stage ELR Data Center?', name: 'stage_elr')
        choice(choices: 'no\nyes', description: 'Do you want to deploy in Prod CTC Data Center?', name: 'prod_ctc')
        choice(choices: 'no\nyes', description: 'Do you want to deploy in Prod ELR Data Center?', name: 'prod_elr')

    }

    agent {
        label 'docker-kitchensink-slave'
    }

    environment {
        DOCKER_VERSION = '17.06.1'
        KUBECTL_VERSION = '1.15.3'
        JAVA_VERSION = '11.0'
        MAVEN_VERSION = '3.3.9'
        FORTIFYSONAR_SCAN = "/home/jenkins/workspace/EMMAFolder/${dockerRepository}"
        FORTIFY_VERSION = 'Fortify_SCA_and_Apps_19.2.0'
        DOCKER_IMAGE_TAG = "${params.dockerBuild}"
		SN_USER = '700001946'
    }

    // TODO: Need to build stages for running javadoc stage, stashing it, and publishing to github pages
    // TODO: Why are we not running jacoco during build time? Would this run it twice?
    stages {
        stage('Maven Build') {
            when {
                expression { params.deploySpeicifDockerBuild == 'no' }
            }
            steps {
                glMavenBuild mavenGoals: 'clean install -U -e -f pom.xml',
                        mavenOpts: '-Dmaven.test.skip=true',
                        mavenVersion: env.MAVEN_VERSION,
                        javaVersion: env.JAVA_VERSION,
                        isDebugMode: true
            }
        }
        stage("Sonar and Fortify Scans") {
            parallel {

                //Sonar and Fortify Scans
                //Verify 'scarUploadToken' and 'scarProjectName' are available in http://scar.uhc.com/

                stage("Fortify Scan") {

                    when {
                        expression {
                            params.fortifyscan == 'yes'
                        }
                    }

                    steps {
                        echo "************  Fortify Scan Step Started  ************"

                        script { last_run_stage = "Sonar and Fortify Scans" }

                        glFortifyScan scarProjectVersion: "26994",
                                fortifyBuildName: "${dockerRepository}",
                                scarProjectName: "EID_UHGWM110-025517",
                                scarUploadToken: "436197ec-d165-4157-b846-c0f5b2f6c000",
                                isGenerateDevWorkbook: true,
                                sourceDirectory: "${env.FORTIFYSONAR_SCAN}/src/main",
                                criticalThreshold: 100,
                                highThreshold: 100,
                                mediumThreshold: 100,
                                lowThreshold: 100,
                                uploadToScar: true,
                                scarCredentialsId: "${dockerCredentialId}",
                                fortifyTranslateExclusions: "-exclude 'src/test/**/*' -exclude 'target/**/*' -exclude 'src/main/resources/**/*.properties' -exclude 'src/main/resources/**/*.yml'",
                                template: "Developer Workbook"

                        echo "************  Fortify Scan Ended  ************"
                    }
                }

                stage("Sonar Scan") {
                    when {
                        expression {
                            params.sonarscan == 'yes'
                        }
                    }

                    steps {
                        echo "****Sonar Step Started****"
                        script { last_run_stage = "Sonar and Fortify Scans" }

                        glSonarMavenScan gitUserCredentialsId: 'EMMA_GIT_CRED', branchName: params.BRANCH_TO_BUILD

                        echo "****Sonar Step Ended****"
                    }
                }
            }
        }

        stage('Build and Push Docker Image') {
            when {
                expression {
                    return (params.dev == 'yes') ||
                            (params.test == 'yes') ||
                            (params.stage_ctc == 'yes') ||
                            (params.stage_elr == 'yes') ||
                            (params.prod_elr == 'yes') ||
                            (params.prod_ctc == 'yes')
                }
                expression { params.deploySpeicifDockerBuild == 'no' }
            }
            steps {
                script {
                    last_run_stage = env.STAGE_NAME
                    // Replace all characters not allowed in tag because branch name might contain those characters
                    def dockerImageTag = "$dockerTagPrefix-build-num-${env.BUILD_NUMBER}-${env.GIT_LOCAL_BRANCH}"
                    dockerImageTag = dockerImageTag.replaceAll("\\/", "_")
                    echo "***** DockerImageTag ${dockerImageTag}   *****"
                    DOCKER_IMAGE_TAG = dockerImageTag
                    echo "***** DOCKER_IMAGE_TAG ${DOCKER_IMAGE_TAG}   *****"
                }

                echo "************   ${last_run_stage} started  ************ "

                glDockerImageBuildPush credentialsId: "$dockerCredentialId",
                        image: "$dockerImageName:${DOCKER_IMAGE_TAG}",
                        containerRegistry: "$dockerHost",
                        dockerVersion: "${env.DOCKER_VERSION}"

                echo "************   ${last_run_stage} completed ************ "
            }
        }
        stage('Deploy to the Dev Environment') {
            when {
                expression { params.dev == 'yes' }
            }
            steps {
                script {
                    last_run_stage = env.STAGE_NAME
                    region = "dev"
                }

                echo "************   ${last_run_stage} started  ************ "

                echo "************  Promote docker image to Artifactory ${last_run_stage} started  ************ "
                glArtifactoryDockerPromote credentialsId: "$dockerCredentialId",
                        destArtifactoryRepo: Artifactory.DEFAULT_DOCKER_ARTIFACTORY_REPOSITORY,
                        sourceDockerRepo: "$dockerNamespace/$dockerRepository",
                        sourceTag: "${DOCKER_IMAGE_TAG}",
                        destTag: "${regions[region].tag}"
                echo "************   promotion completed ************ "

                echo "************   Deploying Docker Image  ${DOCKER_IMAGE_TAG} into ${region}  ************ "


                // Change the CONTAINER_TAG word in K8s deployment file to current docker image tag being used in this build before deploying
                sh "sed -i \'s,CONTAINER_TAG,${regions[region].tag},g\' ${env.WORKSPACE}/k8deployment/${region}/deployment.yaml"
                glKubernetesApply credentials: "${regions[region].k8s_credentials}",
                        cluster: "${regions[region].k8s_cluster}",
                        namespace: "${regions[region].k8s_namespace}",
                        external: true,
                        yamls: ["k8deployment/$region/deployment.yaml", "k8deployment/$region/service.yaml", "k8deployment/$region/networkpolicy.yaml"],
                        deleteIfExists: true, wait: true, delayDelete: 30, times: 40,
                        env: "NonProd",
                        isProduction: false

                echo "************   ${last_run_stage} completed  ************ "
            }
        }
        stage('Test Deployment Approval Alert') {
            when {
                expression { params.test == 'yes' }
            }
            steps {
                script {
                    last_run_stage = env.STAGE_NAME
                    region = "test"
                }

                echo "************   ${last_run_stage} started  ************ "

                emailext body: "Approval Needed for deployment of CAA File Processor Microservice to $region " +
                        "environment. Approval link: ${BUILD_URL}input/",
                        subject: "Approval Needed for CAA File Processor Microservice Deployment to $region",
                        to: "shardul_khatri@optum.com;rajeev_gupta@optum.com",
                        from: "noreply@optum.com"

                script {
                    try {
                        glApproval message: "Approve deployment to $region?",
                                submitter: regions[region].ApproverList,
                                time: regions[region].ApprovalWaitTime,
                                unit: regions[region].ApprovalWaitTimeUnit,
                                defaultValue: "Approval Comment",
                                sendEmailOnTimeout: "true",
                                timeoutEmailTo: "skhatr1"
                    } catch (e) {
                        currentBuild.result = 'SUCCESS'
                        proceedToBuild = false
                        return
                    }
                }

                echo "************   ${last_run_stage} completed  ************ "
            }
        }
        stage('Deploy to the Test Environment') {
            when {
                expression { params.test == 'yes' }
                expression { return proceedToBuild }
            }
            steps {
                script {
                    last_run_stage = env.STAGE_NAME
                    region = "test"
                }

                echo "************   ${last_run_stage} started  ************ "
                echo "************   Deploying Docker Image  ${DOCKER_IMAGE_TAG} into ${region}  ************ "


                // Change the CONTAINER_TAG word in K8s deployment file to current docker image tag being used in this build before deploying
                sh "sed -i \'s,CONTAINER_TAG,${DOCKER_IMAGE_TAG},g\' ${env.WORKSPACE}/k8deployment/${region}/deployment.yaml"
                glKubernetesApply credentials: "${regions[region].k8s_credentials}",
                        cluster: "${regions[region].k8s_cluster}",
                        namespace: "${regions[region].k8s_namespace}",
                        external: true,
                        yamls: ["k8deployment/$region/deployment.yaml", "k8deployment/$region/service.yaml", "k8deployment/$region/networkpolicy.yaml"],
                        deleteIfExists: true, wait: true, delayDelete: 30, times: 40,
                        env: "NonProd",
                        isProduction: false

                echo "************   Stage ${last_run_stage} completed  ************ "
            }
        }

        stage('Stage Deployment Approval Alert') {
            when {
                expression { return (params.stage_elr == 'yes') || (params.stage_ctc == 'yes') }
            }
            steps {
                script {
                    last_run_stage = env.STAGE_NAME
                    region = "stage_ctc"

                    if (params.stage_elr == 'yes') {
                        region = "stage_elr"
                    }

                }

                echo "************   ${last_run_stage} started  ************ "

                emailext body: "Approval Needed for deployment of CAA File Processor Microservice to $region " +
                        "environment. Approval link: ${BUILD_URL}input/",
                        subject: "Approval Needed for CAA File Processor Microservice Deployment to $region",
                        to: "shardul_khatri@optum.com",
                        from: "noreply@optum.com"

                script {
                    try {
                        glApproval message: "Approve deployment to $region?",
                                submitter: regions[region].ApproverList,
                                time: regions[region].ApprovalWaitTime,
                                unit: regions[region].ApprovalWaitTimeUnit,
                                defaultValue: "Approval Comment",
                                sendEmailOnTimeout: "true",
                                timeoutEmailTo: "skhatr1"
                    } catch (e) {
                        currentBuild.result = 'SUCCESS'
                        proceedToBuild = false
                        return
                    }
                }

                echo "************   ${last_run_stage} completed  ************ "
            }
        }
        stage('Deploy to the Stage CTC Environment') {
            when {
                expression { params.stage_ctc == 'yes' }
                expression { return proceedToBuild }
            }
            steps {
                script {
                    last_run_stage = env.STAGE_NAME
                    region = "stage_ctc"
                }

                echo "************   ${last_run_stage} started  ************ "

                echo "************  Promote docker image to Artifactory ${last_run_stage} started  ************ "
                glArtifactoryDockerPromote credentialsId: "$dockerCredentialId",
                        destArtifactoryRepo: Artifactory.DEFAULT_DOCKER_ARTIFACTORY_REPOSITORY,
                        sourceDockerRepo: "$dockerNamespace/$dockerRepository",
                        sourceTag: "dev",
                        destTag: "${regions[region].tag}"
                echo "************   promotion completed ************ "

                echo "************   Deploying Docker Image  ${DOCKER_IMAGE_TAG} into ${region}  ************ "


                // Change the CONTAINER_TAG word in K8s deployment file to current docker image tag being used in this build before deploying
                sh "sed -i \'s,CONTAINER_TAG,${regions[region].tag},g\' ${env.WORKSPACE}/k8deployment/${region}/deployment.yaml"
                glKubernetesApply credentials: "${regions[region].k8s_credentials}",
                        cluster: "${regions[region].k8s_cluster}",
                        namespace: "${regions[region].k8s_namespace}",
                        external: true,
                        yamls: ["k8deployment/${region}/deployment.yaml", "k8deployment/${region}/service.yaml", "k8deployment/${region}/networkpolicy.yaml"],
                        deleteIfExists: true, wait: true, delayDelete: 30, times: 40,
                        env: "NonProd",
                        isProduction: false

                echo "************   Stage ${last_run_stage} completed  ************ "
            }
        }

        stage('Deploy to the Stage ELR Environment') {
            when {
                expression { params.stage_elr == 'yes' }
                expression { return proceedToBuild }
            }
            steps {
                script {
                    last_run_stage = env.STAGE_NAME
                    region = "stage_elr"
                }

                echo "************   ${last_run_stage} started  ************ "

                echo "************  Promote docker image to Artifactory ${last_run_stage} started  ************ "
                glArtifactoryDockerPromote credentialsId: "$dockerCredentialId",
                        destArtifactoryRepo: Artifactory.DEFAULT_DOCKER_ARTIFACTORY_REPOSITORY,
                        sourceDockerRepo: "$dockerNamespace/$dockerRepository",
                        sourceTag: "dev",
                        destTag: "${regions[region].tag}"
                echo "************   promotion completed ************ "

                echo "************   Deploying Docker Image  ${DOCKER_IMAGE_TAG} into ${region}  ************ "


                // Change the CONTAINER_TAG word in K8s deployment file to current docker image tag being used in this build before deploying
                sh "sed -i \'s,CONTAINER_TAG,${regions[region].tag},g\' ${env.WORKSPACE}/k8deployment/${region}/deployment.yaml"
                glKubernetesApply credentials: "${regions[region].k8s_credentials}",
                        cluster: "${regions[region].k8s_cluster}",
                        namespace: "${regions[region].k8s_namespace}",
                        external: true,
                        yamls: ["k8deployment/${region}/deployment.yaml", "k8deployment/${region}/service.yaml", "k8deployment/${region}/networkpolicy.yaml"],
                        deleteIfExists: true, wait: true, delayDelete: 30, times: 40,
                        env: "NonProd",
                        isProduction: false

                echo "************   Stage ${last_run_stage} completed  ************ "
            }
        }

		/*stage('Service Now ticket verification') {
		   when {
                expression { params.prod_ctc == 'yes' }
                expression { return proceedToBuild }
            }
			steps {		
				script{
					try {			
						glServiceNowTicketInput message: 'Provide ServiceNow Ticket number ' , submitter: approver1 
						glServiceNowTicketVerify credentials : "${env.SN_USER}", ticket: "${env.SN_TICKET}" , isChangeWindow : 'true'          

						CHANGE_TICKET_NUMBER = "${env.SN_TICKET}"
						echo "${CHANGE_TICKET_NUMBER}"
					} catch (e) {
							currentBuild.result = 'SUCCESS'
							proceedToBuild = false
							return
						}
				 } 
			}      
		} 
		
		stage('Level-1 Approval'){
            when {
                expression { params.prod_ctc == 'yes' }
                expression { return proceedToBuild }
            }		
		 steps{
				emailext body: "Hello Team, \n\n Please provide Level-1 approval in jenkins to start the deployment for the below input.\n Level-1 approver should have access to 'emma_caa_jenkins_level1_approver_prod' windows group in secure. \n\n App Name			: ${APP_NAME} \n App Env 			: Prod \n\n Verified Change ticket 		: ${CHANGE_TICKET_NUMBER} \n Submitted by			: ${user} \n Build URL			: ${BUILD_URL} \n\n Thanks and Regards, \n EMMA-CAA Team", subject: "Approval-$JOB_NAME-$BUILD_NUMBER", from: 'jenkins-emma-caa@optum.com', to: "$EmailNotifyListToDL,$user_email"
				
				script{
					try {
						glApproval message: 'Approve deployment to production? ' , 
						submitter: approver1 , 
						duplicateApproverCheck: false , 
						submitterParameter: 'approver',
						sendEmailOnTimeout: "true",
						timeoutEmailTo: "${user_email}"				

						APPROVER1 = "${env.APPROVERS}"
						echo "${APPROVER1}"
					} catch (e) {
							currentBuild.result = 'SUCCESS'
							proceedToBuild = false
							return
						}
				 } 
			}
		}
		
		stage('Level-2 Approval'){
            when {
                expression { params.prod_ctc == 'yes' }
                expression { return proceedToBuild }
            }		
		 steps{
				emailext body: "Hello Team, \n\n Please provide Level-2 approval in jenkins to start the deployment for the below input.\n Level-2 approver should have access to 'emma_caa_jenkins_level2_approver_prod' windows group in secure.  \n\n App Name			: ${APP_NAME} \n App Env 			: Prod \n\n Verified Change ticket 		: ${CHANGE_TICKET_NUMBER} \n Submitted by			: ${user} \n Build URL			: ${BUILD_URL} \n\n Thanks and Regards, \n EMMA-CAA Team", subject: "Approval-$JOB_NAME-$BUILD_NUMBER", from: 'jenkins-emma-caa@optum.com', to: "${EmailNotifyListToDL},${user_email}"
							
				script{
					try {
						glApproval message: 'Approve deployment to production? ' , 
							submitter: approver2 , 
							duplicateApproverCheck: true , 
							submitterParameter: 'approver',
							sendEmailOnTimeout: "true",
							timeoutEmailTo: "${user_email}"
							
						  APPROVER2 = "${env.APPROVERS}"
						  echo "${APPROVER2}"
					 } catch (e) {
							currentBuild.result = 'SUCCESS'
							proceedToBuild = false
							return
					}
				}
			}
		}*/
		
        stage('Deploy to the Prod CTC Environment') {
            when {
                expression { params.prod_ctc == 'yes' }
                expression { return proceedToBuild }
            }
            steps {
                script {
                    last_run_stage = env.STAGE_NAME
                    region = "prod_ctc"
                }

                echo "************   ${last_run_stage} started  ************ "

                echo "************  Promote docker image to Artifactory ${last_run_stage} started  ************ "
                glArtifactoryDockerPromote credentialsId: "$dockerCredentialId",
                        destArtifactoryRepo: Artifactory.DEFAULT_DOCKER_ARTIFACTORY_PROMOTION_REPOSITORY,
                        sourceDockerRepo: "$dockerNamespace/$dockerRepository",
                        sourceTag: "stage_ctc",
                        destTag: "${regions[region].tag}"
                echo "************   promotion completed ************ "

                echo "************   Deploying Docker Image  ${DOCKER_IMAGE_TAG} into ${region}  ************ "


                // Change the CONTAINER_TAG word in K8s deployment file to current docker image tag being used in this build before deploying
                sh "sed -i \'s,CONTAINER_TAG,${regions[region].tag},g\' ${env.WORKSPACE}/k8deployment/${region}/deployment.yaml"
                glKubernetesApply credentials: "${regions[region].k8s_credentials}",
                        cluster: "${regions[region].k8s_cluster}",
                        namespace: "${regions[region].k8s_namespace}",
                        external: true,
                        yamls: ["k8deployment/${region}/deployment.yaml", "k8deployment/${region}/service.yaml", "k8deployment/${region}/networkpolicy.yaml"],
                        deleteIfExists: true, wait: true, delayDelete: 30, times: 40,
                        env: "Prod",
                        isProduction: true

                echo "************   Stage ${last_run_stage} completed  ************ "
            }
        }

        stage('Deploy to the Prod ELR Environment') {
            when {
                expression { params.prod_elr == 'yes' }
                expression { return proceedToBuild }
            }
            steps {
                script {
                    last_run_stage = env.STAGE_NAME
                    region = "prod_elr"
                }

                echo "************   ${last_run_stage} started  ************ "
                echo "************  Promote docker image to Artifactory ${last_run_stage} started  ************ "
                glArtifactoryDockerPromote credentialsId: "$dockerCredentialId",
                        destArtifactoryRepo: Artifactory.DEFAULT_DOCKER_ARTIFACTORY_PROMOTION_REPOSITORY,
                        sourceDockerRepo: "$dockerNamespace/$dockerRepository",
                        sourceTag: "stage_elr",
                        destTag: "${regions[region].tag}"
                echo "************   promotion completed ************ "

                echo "************   Deploying Docker Image  ${DOCKER_IMAGE_TAG} into ${region}  ************ "


                // Change the CONTAINER_TAG word in K8s deployment file to current docker image tag being used in this build before deploying
                sh "sed -i \'s,CONTAINER_TAG,${regions[region].tag},g\' ${env.WORKSPACE}/k8deployment/${region}/deployment.yaml"
                glKubernetesApply credentials: "${regions[region].k8s_credentials}",
                        cluster: "${regions[region].k8s_cluster}",
                        namespace: "${regions[region].k8s_namespace}",
                        external: true,
                        yamls: ["k8deployment/${region}/deployment.yaml", "k8deployment/${region}/service.yaml", "k8deployment/${region}/networkpolicy.yaml"],
                        deleteIfExists: true, wait: true, delayDelete: 30, times: 40,
                        env: "Prod",
                        isProduction: true

                echo "************   Stage ${last_run_stage} completed  ************ "
            }
        }
    }
    post {
        failure {
            echo "Failure"
            script { echo "Build Failure for $JOB_NAME at step: $last_run_stage" }
            emailext body: "Build Failure for $JOB_NAME at step: $last_run_stage. Build Info: ${DOCKER_IMAGE_TAG}",
                    subject: "Build Failure for $JOB_NAME build ${DOCKER_IMAGE_TAG}",
                    to: failureNotifyList
        }
        success {
            echo "Success"
            script { echo "Build Successful for $JOB_NAME" }
            emailext body: "Build Successful for $JOB_NAME. Build Info: $BUILD_URL",
                    subject: "Build Successful for $JOB_NAME build ${DOCKER_IMAGE_TAG}",
                    to: successNotifyList
        }
    }
}
