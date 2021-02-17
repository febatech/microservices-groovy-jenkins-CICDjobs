package rscfoundation

import util.ServiceUtil

def jobGenerator(jobName){
    def serviceName = ServiceUtil.getRscServiceName(jobName)

    return job("${jobName} Build And Test"){
        wrappers {
            colorizeOutput()
        }

        scm {
            git {
                remote {
                    url("https://github.gapinc.com/sourcing/${serviceName}.git")
                    credentials('github_credential')
                }
                branch("master")
                extensions {
                    wipeOutWorkspace()
                }
            }
        }

        steps {
            gradle {
                useWrapper(true)
                makeExecutable(true)
                tasks(':myfirstrepo:test')
                switches('-g .')
                fromRootBuildScriptDir(true)
            }
        }

        triggers {
            githubPush()
        }

        publishers {
            archiveJunit('myfirstrepo/build/test-results/**/*.xml')
            downstreamParameterized {
                trigger("${jobName} Sonar") {
                    condition('SUCCESS')
                    parameters {
                        gitRevision()
                    }
                }
            }

        }

        configure { root ->
            root / 'properties' / 'jenkins.plugins.office365connector.WebhookJobProperty' (plugin:'Office-365-Connector@4.4') / 'webhooks' / 'jenkins.plugins.office365connector.Webhook' {
                url ServiceUtil.OFFICE_365_WEBHOOK_URL
                startNotification true
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

jobGenerator('RSC Foundation')