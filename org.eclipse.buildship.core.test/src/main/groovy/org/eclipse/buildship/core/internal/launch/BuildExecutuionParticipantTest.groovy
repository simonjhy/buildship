package org.eclipse.buildship.core.internal.launch

import org.eclipse.buildship.core.BuildExecutionParticipant
import org.eclipse.buildship.core.GradleDistribution
import org.eclipse.buildship.core.internal.CorePlugin
import org.eclipse.buildship.core.internal.extension.BuildExecutionParticipantContribution
import org.eclipse.buildship.core.internal.extension.ExtensionManager
import org.eclipse.core.runtime.IConfigurationElement
import org.eclipse.core.runtime.IContributor
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.ILaunchConfiguration
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.task.TaskFinishEvent

class BuildExecutuionParticipantTest extends BaseLaunchRequestJobTest {

    BuildExecutionParticipant buildParticipant

	File projectDir

	def setup() {
		ExtensionManager orignalManager = CorePlugin.instance.extensionManager
		CorePlugin.instance.extensionManager = new TestExtensionManager(CorePlugin.instance.extensionManager)
		orignalManager.loadBuildExecutionParticipants()
			.each { c -> CorePlugin.instance.extensionManager.buildParticipants += c }
	}

	def cleanup() {
		CorePlugin.instance.extensionManager = CorePlugin.instance.extensionManager.delegate
	}

	protected def registerBuildParticipant(BuildExecutionParticipant buildExecutionParticipant) {
		ExtensionManager manager = CorePlugin.instance.extensionManager
		int id = manager.buildParticipants.size() + 1
		manager.buildParticipants += contribution(id, buildExecutionParticipant)
		buildExecutionParticipant
	}


	private BuildExecutionParticipantContribution contribution(id, buildExecutionParticipant) {
		IConfigurationElement extension = Mock(IConfigurationElement)
		extension.createExecutableExtension('class') >> { buildExecutionParticipant }
		extension.getAttribute('id') >> "buildParticipant$id"
		extension.getAttribute('tasks') >> "foo"
		IContributor contributor = Mock(IContributor)
		contributor.getName() >> 'pluginId'
		extension.getContributor() >> contributor
		BuildExecutionParticipantContribution.from(extension)
	}


	static class TestExtensionManager {
		@Delegate ExtensionManager delegate

		List<BuildExecutionParticipantContribution> buildParticipants = []

		TestExtensionManager(ExtensionManager delegate) {
			this.delegate = delegate
		}

		List<BuildExecutionParticipantContribution> loadBuildExecutionParticipants() {
			buildParticipants
		}
	}
	
	def "Job launches a Gradle build"() {
		setup:
		projectDir = dir('project-without-build-scan') {
			file 'build.gradle', 'task foo {}'
		}
		
		BuildExecutionParticipant testExecutionParticipant =  new TestBuildExecutionParticipant("foo")
		buildParticipant = registerBuildParticipant(testExecutionParticipant)
		
		def testBuildExecutionParticipantLaunch = createLaunch(projectDir)
		def job = new RunGradleBuildLaunchRequestJob(testBuildExecutionParticipantLaunch)

		when:
        job.schedule()
        job.join()

        then:
		job.getResult().isOK()
		buildParticipant.numOfSyncs == 2
		
	}
	
	ILaunch createLaunch(File projectDir, GradleDistribution distribution = GradleDistribution.fromBuild(), tasks = ['foo'], arguments = []) {
		ILaunchConfiguration launchConfiguration = createLaunchConfiguration(projectDir, tasks, distribution, arguments)
		ILaunch launch = Mock(ILaunch)
		launch.launchConfiguration >> launchConfiguration
		launch
	}

	class TestBuildExecutionParticipant implements BuildExecutionParticipant {
		
		public TestBuildExecutionParticipant(String taskName) {
			_task = taskName;
		}
		
		String _task;
		int numOfSyncs = 0

		@Override
		public void preBuild(File projectDir, String task, IProgressMonitor monitor) {
			if ( _task.equals(task)) {
				numOfSyncs++;
			}
			
		}

		@Override
		public void postBuild(File projectDir, String task, ProgressEvent event, IProgressMonitor monitor) {
			if ( event instanceof TaskFinishEvent) {
				if ( _task.equals(task)) {
					numOfSyncs++;
				}
			}
		}
	}
	
}
