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
        ],
        sidecarContainers: [
            [ image: "docker.io/kalaksi/tinyproxy:1.6", namePrefix: "TINYPROXYAUTH", port: 8888, envArgs: ["STAT_HOST=tinyproxy.stats", "MAX_CLIENTS=500", "ALLOWED_NETWORKS=0.0.0.0/0", "LOG_LEVEL=Info", "TIMEOUT=300", "AUTH_USER='knime-proxy'", "AUTH_PASSWORD='knime-proxy-password'"]
            ],
            [ image: "docker.io/kalaksi/tinyproxy:1.6", namePrefix: "TINYPROXY", port: 8888, envArgs: ["STAT_HOST=tinyproxy.stats", "MAX_CLIENTS=500", "ALLOWED_NETWORKS=0.0.0.0/0", "LOG_LEVEL=Info", "TIMEOUT=300"]
            ],
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
