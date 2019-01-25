package org.eclipse.buildship.core.internal.extension

import org.eclipse.buildship.core.BuildExecutionParticipant
import org.eclipse.buildship.core.internal.test.fixtures.WorkspaceSpecification
import org.eclipse.core.runtime.IConfigurationElement
import org.eclipse.core.runtime.IContributor

class InternalBuildExecutionParticipantTest extends WorkspaceSpecification {

    def "Can handle zero build execution participant"() {
        expect:
        InternalBuildExecutionParticipant.from([]).empty
    }

    def "Can handle basic build execution participant"() {
        when:
        List<InternalBuildExecutionParticipant> participants = InternalBuildExecutionParticipant.from([participant()])

        then:
        participants.size() == 1
        participants[0].id == 'id'
    }

    def "Ignores build execution participant with undefined ID"() {
        expect:
        InternalBuildExecutionParticipant.from([participant(null)]).empty
    }

    def "Omits build execution participant with duplicate ID"() {
        expect:
        InternalBuildExecutionParticipant.from([participant(), participant()]).size() == 1
    }

    def "Omits uninstantiable build execution participant" () {
        expect:
        InternalBuildExecutionParticipant.from([uninstantiableBuildExecution()]).empty
    }

    def "extract build execction participants"() {
		setup:
		BuildExecutionParticipantContribution b1 = participant('b1')
		BuildExecutionParticipantContribution b2 = participant('b2')
		
		when:
		List<InternalBuildExecutionParticipant> buildParticipants = InternalBuildExecutionParticipant.from([b1, b2])

		then:
		assertBuildExecutionParticipants(buildParticipants, 'b1', 'b2')
    }

	def "check duplicate build execction participants"() {
		setup:
		BuildExecutionParticipantContribution b1 = participant('b1')
		BuildExecutionParticipantContribution b2 = participant('b1')
		
		when:
		List<InternalBuildExecutionParticipant> buildParticipants = InternalBuildExecutionParticipant.from([b1, b2])

		then:
		assertBuildExecutionParticipants(buildParticipants, 'b1')
	}

	def "check invalid build execction participants"() {
		setup:
		BuildExecutionParticipantContribution b1 = participant('b1')
		BuildExecutionParticipantContribution b2 = participant(null)
		
		when:
		List<InternalBuildExecutionParticipant> buildParticipants = InternalBuildExecutionParticipant.from([b1, b2])

		then:
		assertBuildExecutionParticipants(buildParticipants, 'b1')
	}
			
    private void assertBuildExecutionParticipants(List<InternalBuildExecutionParticipant> buildExecutionParticipants, String... ids) {
        assert buildExecutionParticipants.collect { it.id } == ids
    }

    private BuildExecutionParticipantContribution participant(id = 'id', filterTasks = []) {
        IConfigurationElement extension = Mock(IConfigurationElement)
        extension.createExecutableExtension('class') >> Mock(BuildExecutionParticipant)
        extension.getAttribute('id') >> id
        extension.getAttribute('tasks') >> filterTasks.join(',')
        IContributor contributor = Mock(IContributor)
        contributor.getName() >> 'pluginId'
        extension.getContributor() >> contributor
        BuildExecutionParticipantContribution.from(extension)
    }

    private BuildExecutionParticipantContribution uninstantiableBuildExecution() {
        IConfigurationElement extension = Mock(IConfigurationElement)
        extension.createExecutableExtension('class') >> { throw new Exception() }
        extension.getAttribute('id') >> 'id'
        IContributor contributor = Mock(IContributor)
        contributor.getName() >> 'pluginId'
        extension.getContributor() >> contributor
        BuildExecutionParticipantContribution.from(extension)
    }
}
