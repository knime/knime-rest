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

	/*
	workflowTests.runTests(
		// The name of the feature that pulls in all required dependencies for running workflow tests.
		// You can also provide a list here but separating it into one feature makes it clearer and allows
		// pulling in independant plug-ins, too.
		"org.knime.features.ap-repository-template.testing.feature.group",
		// with or without assertions
		false,
		// a list of upstream jobs to look for when getting dependencies
		// the following jobs are included by default and do *not* need to be included:
		// knime-tp, knime-shared, knime-core, knime-base, knime-workbench, knime-expressions, knime-js-core, knime-svg, knime-product
		["knime-r", "knime-python"],
		// optional list of test configurations
		testConfigurations
	)

	stage('Sonarqube analysis') {
		env.lastStage = env.STAGE_NAME
		// passing the test configuration is optional but must be done when they are
		// used above in the workflow tests
		workflowTests.runSonar(testConfigurations)
	}
	*/
 } catch (ex) {
	 currentBuild.result = 'FAILURE'
	 throw ex
 } finally {
	 notifications.notifyBuild(currentBuild.result);
 }

/* vim: set ts=4: */
