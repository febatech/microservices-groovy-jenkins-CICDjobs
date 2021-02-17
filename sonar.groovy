package rscfoundation

import util.ServiceUtil

def jobGenerator(jobName) {
    def serviceName = ServiceUtil.getRscServiceName(jobName)

    return job("${jobName} Sonar") {
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
                tasks('sonar')
                switches('-Pversion=${BUILD_NUMBER} -PsonarAnalysisMode=analysis -g .')
                fromRootBuildScriptDir(true)
            }
        }

        publishers {
            publishHtml {
                report('build/sonar/issues-report/') {
                    reportName('Sonar Report')
                    keepAll()
                    allowMissing()
                    reportFiles('issues-report.html')
                }
            }
            downstreamParameterized {
                trigger("${jobName} Publish") {
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
