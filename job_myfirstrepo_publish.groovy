package rscfoundation

import config.DeployConfig
import util.ServiceUtil

def jobGenerator(jobName) {
    def serviceName = ServiceUtil.getRscServiceName(jobName)

    return job("${jobName} Publish") {
        scm {
            git {
                remote {
                    url("https://github.gapinc.com/sourcing/${serviceName}.git")
                    credentials('github_credential')
                }
                branch('master')
                extensions {
                    wipeOutWorkspace()
                }
            }
        }

        steps {
            gradle {
                useWrapper(true)
                makeExecutable(true)
                tasks('clean uploadArchivesForJenkins')
                switches('-Pversion=${BUILD_NUMBER} --continue -g .')
                fromRootBuildScriptDir(true)
            }
        }

        publishers {
            downstreamParameterized {
                trigger('RSC Foundation Deploy to Environment') {
                    condition('SUCCESS')
                    parameters {
                        predefinedProp('service', serviceName)
                        predefinedProp('version', '${BUILD_NUMBER}')
                        predefinedProp('rsc_env_name', 'ci')
                        predefinedProp('rsc_server',DeployConfig.RSC_CI_SERVER[0])
                    }
                }
            }
        }

        wrappers {
            colorizeOutput()
            credentialsBinding {
                usernamePassword('ARTIFACTORY_USERNAME', 'ARTIFACTORY_PASSWORD', 'artifactory_credential')
            }
        }


        configure { root ->
            root / 'properties' / 'jenkins.plugins.office365connector.WebhookJobProperty'(plugin: 'Office-365-Connector@4.4') / 'webhooks' / 'jenkins.plugins.office365connector.Webhook' {
                url ServiceUtil.OFFICE_365_WEBHOOK_URL
                startNotification false
                notifySuccess false
                notifyAborted true
                notifyNotBuilt true
                notifyUnstable true
                notifyFailure true
                notifyBackToNormal true
                notifyRepeatedFailure true
                timeout '30000'
            }
        }

        label(ServiceUtil.PIPELINE_LABEL)
    }
}

jobGenerator('myfirstrepo')

