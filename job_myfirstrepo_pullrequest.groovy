package rscfoundation

import util.ServiceUtil

def jobGenerator(jobName) {
    def serviceName = ServiceUtil.getRscServiceName(jobName)

    return job("${jobName} Pull Request Build And Test") {

        properties {
            githubProjectUrl("https://github.gapinc.com/sourcing/${serviceName}")
        }

        scm {
            git {
                remote {
                    url("https://github.gapinc.com/sourcing/${serviceName}.git")
                    credentials('service-account')
                    name('origin')
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                }
                extensions {
                    wipeOutWorkspace()
                }
                branch('${sha1}')
            }
        }

        steps {
            gradle {
                useWrapper(true)
                makeExecutable(true)
                tasks(':myfirstrepo:clean build')
                switches('-g .')
                fromRootBuildScriptDir(true)
            }
        }

        triggers {
            githubPullRequest {
                permitAll(true)
                useGitHubHooks(true)
                extensions {
                    commitStatus {
                        context('Test/Build Pull-Request')
                        triggeredStatus('Starting testing/building pull-request...')
                        startedStatus('Testing pull-request...')
                        completedStatus('SUCCESS', 'Pull-request test/build completed!')
                        completedStatus('FAILURE', 'Pull-request test/build failed. Investigate!')
                        completedStatus('PENDING', 'Testing still in progress...')
                        completedStatus('ERROR', 'Pull-request test/build failed. Investigate!')
                    }
                    buildStatus {
                        completedStatus('SUCCESS', 'Pull-request test/build completed!')
                        completedStatus('FAILURE', 'Pull-request test/build failed. Investigate!')
                        completedStatus('ERROR', 'Pull-request test/build failed. Investigate!')
                    }
                }
            }
        }

        publishers {
            archiveJunit('myfirstrepo/build/test-results/**/*.xml')
        }

        configure { project ->
            (project / triggers / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' / gitHubAuthId).value = 'pr_auth_id'
        }

        label(ServiceUtil.PIPELINE_LABEL)
    }
}

jobGenerator('myfirstrepo')
