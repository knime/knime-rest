#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
	// provide a list of upstream jobs which should trigger a rebuild of this job
	pipelineTriggers([
		upstream('knime-json/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
		upstream('knime-xml/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
		upstream('knime-expressions/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
		upstream('knime-javasnippet/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
		upstream('knime-svg/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
	]),
	buildDiscarder(logRotator(numToKeepStr: '5')),
	disableConcurrentBuilds()
])

try {
	knimetools.defaultTychoBuild('org.knime.update.rest')

	workflowTests.runTests(
		dependencies: [
		  repositories: [
            'knime-rest', 'knime-xml', 'knime-json', 'knime-filehandling', 'knime-stats', 'knime-timeseries',
            'knime-textprocessing', 'knime-reporting'
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

/* vim: set ts=4: */
