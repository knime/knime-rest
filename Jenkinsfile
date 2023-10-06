#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2023-12'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        // knime-expressions -> knime-base -> knime-json
        // knime-javasnippet -> knime-json
        upstream("knime-json/${env.BRANCH_NAME.replaceAll('/', '%2F')}" +
            ", knime-xml/${env.BRANCH_NAME.replaceAll('/', '%2F')}" +
            ", knime-svg/${env.BRANCH_NAME.replaceAll('/', '%2F')}")
    ]),
    parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
    knimetools.defaultTychoBuild('org.knime.update.rest')

    workflowTests.runTests(
        dependencies: [
            repositories: [
                'knime-gateway',
                'knime-credentials-base',
                'knime-rest',
                'knime-xml',
                'knime-json',
                'knime-filehandling',
                'knime-stats',
                'knime-timeseries',
                'knime-textprocessing',
                'knime-reporting',
                'knime-jfreechart',
                'knime-distance',
                'knime-kerberos',
                'knime-js-core',
                'knime-js-base',
                'knime-expressions',
                'knime-python-legacy',
                'knime-core-columnar',
                'knime-conda'
            ]
        ]
    )

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar()
    }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */
