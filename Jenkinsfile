#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2025-12'

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

        def (proxyUser, proxyPassword, proxyStats) = ['knime-proxy', 'knime-proxy-password', 'tinyproxy.stats']
        def proxyConfigs = [
            [
                image: 'docker.io/kalaksi/tinyproxy:1.6',
                namePrefix: 'tinyproxy',
                envArgs: [
                    "STAT_HOST=${proxyStats}",
                    'MAX_CLIENTS=500',
                    'ALLOWED_NETWORKS=0.0.0.0/0',
                    'LOG_LEVEL=Info',
                    'TIMEOUT=300',
                ],
                ports: [8888]
            ],
            [
                image: 'docker.io/kalaksi/tinyproxy:1.6',
                namePrefix: 'tinyproxyAuth',
                envArgs: [
                    "STAT_HOST=${proxyStats}",
                    'MAX_CLIENTS=500',
                    'ALLOWED_NETWORKS=0.0.0.0/0',
                    'LOG_LEVEL=Info',
                    'TIMEOUT=300',
                    "AUTH_USER=${proxyUser}",
                    "AUTH_PASSWORD=${proxyPassword}",
                ],
                ports: [8888]
            ]
        ]

        try {
            // sidecars for (un-)authenticated proxies
            def (tinyproxy, tinyproxyAuth) = proxyConfigs.collect { cfg ->
                sidecars.createSideCar(cfg.image, cfg.namePrefix, cfg.envArgs, cfg.ports).start()
            }

            // expose addresses of proxies
            withEnv([
                "KNIME_TINYPROXY=http://${tinyproxy.getAddress(8888)}",
                "KNIME_TINYPROXYAUTH=http://${proxyUser}:${proxyPassword}@${tinyproxyAuth.getAddress(8888)}",
                "KNIME_TINYPROXYSTATS=http://${proxyStats}",
                "KNIME_HTTPBIN=httpbin.testing.knime.com"
            ]) {
                knimetools.defaultTychoBuild(updateSiteProject: 'org.knime.update.rest')

                workflowTests.runTests(
                    dependencies: [
                        repositories: [
                            'knime-conda',
                            'knime-conda-channels',
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
                            'org.knime.features.kerberos.feature.group'
                        ]
                    ],
                    sidecarContainers: proxyConfigs.collect { cfg ->
                        [
                            image: cfg.image,
                            namePrefix: cfg.namePrefix,
                            port: cfg.ports[0],
                            envArgs: cfg.envArgs
                        ]
                    }
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
