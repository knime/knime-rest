#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2025-07'

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
    node ('maven && java17') {
        def sidecars = dockerTools.createSideCarFactory()
        try {
            // sidecars for (un-)authenticated proxies
            def (proxyUser, proxyPassword, proxyStats) = ['knime-proxy', 'knime-proxy-password', 'tinyproxy.stats']
            def tinyproxy = sidecars.createSideCar(
                'docker.io/kalaksi/tinyproxy:1.6', 'tinyproxy',
                ["STAT_HOST=${proxyStats}", 'MAX_CLIENTS=500', 'ALLOWED_NETWORKS=0.0.0.0/0', 'LOG_LEVEL=Info', 'TIMEOUT=300'],
                [8888]
            ).start()
            def tinyproxyAuth = sidecars.createSideCar(
                'docker.io/kalaksi/tinyproxy:1.6', 'tinyproxyAuth',
                ["STAT_HOST=${proxyStats}", 'MAX_CLIENTS=500', 'ALLOWED_NETWORKS=0.0.0.0/0', 'LOG_LEVEL=Info', 'TIMEOUT=300', "AUTH_USER=${proxyUser}", "AUTH_PASSWORD=${proxyPassword}"],
                [8888]
            ).start()

            // expose addresses of proxies
            withEnv([
                "KNIME_TINYPROXY=http://${tinyproxy.getAddress(8888)}",
                "KNIME_TINYPROXYAUTH=http://${proxyUser}:${proxyPassword}@${tinyproxyAuth.getAddress(8888)}",
                "KNIME_TINYPROXYSTATS=http://${proxyStats}",
                "KNIME_HTTPBIN=httpbin.testing.knime.com"
            ]) {
                knimetools.defaultTychoBuild('org.knime.update.rest')

                workflowTests.runTests(
                    dependencies: [
                        repositories: [
                            'knime-conda',
                            'knime-core-columnar',
                            'knime-credentials-base',
                            'knime-distance',
                            'knime-expressions',
                            'knime-filehandling',
                            'knime-gateway',
                            'knime-jfreechart',
                            'knime-js-base',
                            'knime-js-core',
                            'knime-json',
                            'knime-kerberos',
                            'knime-python',
                            'knime-python-legacy',
                            'knime-reporting',
                            'knime-rest',
                            'knime-scripting-editor',
                            'knime-stats',
                            'knime-streaming',
                            'knime-textprocessing',
                            'knime-timeseries',
                            'knime-xml',
                        ],
                        ius: [
                        'org.knime.features.kerberos.feature.group',
                        ]
                    ]
                )
            }
        } finally {
            sidecars.close()
        }
    }

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
