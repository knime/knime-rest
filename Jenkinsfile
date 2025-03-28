#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2026-06'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        // knime-expressions -> knime-base -> knime-json
        // knime-javasnippet -> knime-json
        upstream(
            "knime-json/${env.BRANCH_NAME.replaceAll('/', '%2F')}, " +
            "knime-xml/${env.BRANCH_NAME.replaceAll('/', '%2F')}, " +
            "knime-svg/${env.BRANCH_NAME.replaceAll('/', '%2F')}"
        )
    ]),
    parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

def proxyPort = 8888
def (proxyUser, proxyPassword, proxyStats) = ['knime-proxy', 'knime-proxy-password', 'tinyproxy.stats']
def proxyConfigs = [
    [
        image: 'docker.io/kalaksi/tinyproxy:1.7',
        namePrefix: 'TINYPROXY',
        envArgs: [
            "STAT_HOST=${proxyStats}",
            'MAX_CLIENTS=500',
            'ALLOWED_NETWORKS=0.0.0.0/0',
            'LOG_LEVEL=Info',
            'TIMEOUT=300',
        ],
        ports: [proxyPort]
    ],
    [
        image: 'docker.io/kalaksi/tinyproxy:1.7',
        namePrefix: 'TINYPROXYAUTH',
        envArgs: [
            "STAT_HOST=${proxyStats}",
            'MAX_CLIENTS=500',
            'ALLOWED_NETWORKS=0.0.0.0/0',
            'LOG_LEVEL=Info',
            'TIMEOUT=300',
            "AUTH_USER=${proxyUser}",
            "AUTH_PASSWORD=${proxyPassword}",
        ],
        ports: [proxyPort]
    ]
]

try {
    node('maven && java21') {
        def sidecars = dockerTools.createSideCarFactory()
        try {
            // sidecars for (un-)authenticated proxies
            def (tinyproxy, tinyproxyAuth) = proxyConfigs.collect { cfg ->
                sidecars.createSideCar(cfg.image, cfg.namePrefix, cfg.envArgs, cfg.ports).start()
            }
            // expose addresses of proxies
            withEnv([
                "KNIME_TINYPROXY_ADDRESS=http://${tinyproxy.getAddress(proxyPort)}",
                "KNIME_TINYPROXYAUTH_ADDRESS=http://${proxyUser}:${proxyPassword}@${tinyproxyAuth.getAddress(proxyPort)}",
                "KNIME_TINYPROXYSTATS=http://${proxyStats}",
                'KNIME_HTTPBIN_ADDRESS=https://httpbin.testing.knime.com'
            ]) {
                knimetools.defaultTychoBuild(updateSiteProject: 'org.knime.update.rest', withoutNode: true)
            }
        } finally {
            sidecars.close()
        }
    }

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
                'knime-stats',
                'knime-streaming',
                'knime-textprocessing',
                'knime-timeseries',
                'knime-xml',
            ],
            ius: [
                'org.knime.features.kerberos.feature.group',
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

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar()
    }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result)
}
/* vim: set shiftwidth=4 expandtab smarttab: */
