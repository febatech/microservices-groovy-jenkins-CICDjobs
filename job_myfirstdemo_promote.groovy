package rscfoundation

import util.ServiceUtil

job('RSC Foundation Promote Service Artifact') {
    parameters {
        choiceParam('service', ['myfirstrepo'], 'Choose a service to promote.')
        stringParam('version', '', 'Enter the version of the service to promote.')
    }

    scm {
        git {
            remote {
                url('https://github.gapinc.com/sourcing/myfirstrepo.git')
                credentials('github_credential')
            }
            extensions {
                wipeOutWorkspace()
            }
            branch('master')
        }
    }

    steps {
        gradle {
            useWrapper(true)
            makeExecutable(true)
            tasks('clean promoteArtifact')
            switches('-Pversion=${version} -Pservice=${service} -g .')
            fromRootBuildScriptDir(true)
        }
    }

    wrappers {
        colorizeOutput()
        credentialsBinding {
            usernamePassword('ARTIFACTORY_USERNAME', 'ARTIFACTORY_PASSWORD', 'artifactory_credential')
        }
    }

    label(ServiceUtil.PIPELINE_LABEL)
}
