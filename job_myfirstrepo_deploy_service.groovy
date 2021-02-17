package rscfoundation

import util.ServiceUtil

def jobGenerator(jobName) {
    def serviceName = ServiceUtil.getRscServiceName(jobName)

    return job("${jobName} Deploy service") {

        label(ServiceUtil.PIPELINE_LABEL)

        parameters {
            choiceParam('service', ['myfirstrepo'], 'Select a service.')
            choiceParam('environment', [ 'qa','release','production'], 'Select an environment.')
            stringParam('captcha', '', 'Enter the captcha to deploy to production.')
            stringParam('artifactId', '', 'Artifact id to deploy.')
            booleanParam('useAutoscaler', false, 'Enable PCF Autoscaling for this service.')
        }

        scm {
            git {
                remote {
                    url("https://github.gapinc.com/sourcing/${serviceName}.git")
                    credentials('github_credential')
                }
                extensions {
                    wipeOutWorkspace()
                }
                branch('master')
            }
        }
        steps {
            conditionalSteps {
                condition {
                    and {
                        stringsMatch('${environment}', 'production', false)
                    } {
                        and {
                            stringsMatch('${captcha}', 'production', false)
                        }
                    }

                }

                steps {
                    gradle {
                        useWrapper(true)
                        makeExecutable(true)
                        tasks('clean')
                        tasks('deployZeroDowntime')
                        switches('-PartifactId=${artifactId} -PappEnvironment=${environment} -Pservice=${service} -PuseAutoscaler=${useAutoscaler} -g .')
                        fromRootBuildScriptDir(true)
                    }
                }
            }

            conditionalSteps {
                condition {
                    not {
                        stringsMatch('${environment}', 'production', false)
                    }
                }

                steps {
                    gradle {
                        useWrapper(true)
                        makeExecutable(true)
                        tasks('clean')
                        tasks('deployZeroDowntime')
                        switches('-PartifactId=${artifactId} -PappEnvironment=${environment} -Pservice=${service} -PuseAutoscaler=${useAutoscaler} -g .')
                        fromRootBuildScriptDir(true)
                    }
                }
            }

            conditionalSteps {
                condition {
                    and {
                        stringsMatch('${environment}', 'production', false)
                    } {
                        and {
                            not { stringsMatch('${captcha}', 'production', false) }
                        }
                    }

                }

                steps {
                    gradle {
                        useWrapper(true)
                        makeExecutable(true)
                        tasks('deployFailed')
                        switches('-g .')
                        fromRootBuildScriptDir(true)
                    }
                }
            }
        }

        wrappers {
            colorizeOutput()
            credentialsBinding {
                usernamePassword('CLOUD_FOUNDRY_USERNAME', 'CLOUD_FOUNDRY_PASSWORD', 'cf-dev-credential')
                usernamePassword('GITHUB_USERNAME', 'GITHUB_PASSWORD', 'github_token')
                usernamePassword('', 'ENCRYPTION_PASSWORD', 'jasypt_credential')
            }
        }

        configure { root ->
            root / 'properties' / 'jenkins.plugins.office365connector.WebhookJobProperty'(plugin: 'Office-365-Connector@4.4') / 'webhooks' / 'jenkins.plugins.office365connector.Webhook' {
                url ServiceUtil.OFFICE_365_WEBHOOK_URL
                startNotification false
                notifySuccess true
                notifyAborted true
                notifyNotBuilt true
                notifyUnstable true
                notifyFailure true
                notifyBackToNormal true
                notifyRepeatedFailure true
                timeout '30000'
            }
        }
    }
}

jobGenerator('RSC Sharepoint Integrator')
