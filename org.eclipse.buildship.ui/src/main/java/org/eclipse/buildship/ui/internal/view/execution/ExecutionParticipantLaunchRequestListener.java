/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.buildship.ui.internal.view.execution;

import java.util.Set;

import org.eclipse.buildship.core.BuildExecutionParticipant;
import org.eclipse.buildship.core.internal.CorePlugin;
import org.eclipse.buildship.core.internal.configuration.RunConfiguration;
import org.eclipse.buildship.core.internal.console.ProcessDescription;
import org.eclipse.buildship.core.internal.event.Event;
import org.eclipse.buildship.core.internal.event.EventListener;
import org.eclipse.buildship.core.internal.launch.BuildExecutionParticipants;
import org.eclipse.buildship.core.internal.launch.ExecuteLaunchRequestEvent;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.PlatformUI;

public final class ExecutionParticipantLaunchRequestListener implements EventListener {

    @Override
    public void onEvent(Event event) {
        if (event instanceof ExecuteLaunchRequestEvent) {
            handleExecutParticipantLaunchRequest((ExecuteLaunchRequestEvent) event);
        }
    }

    private void handleExecutParticipantLaunchRequest(final ExecuteLaunchRequestEvent event) {
        BuildExecutionParticipants buildParticipants = BuildExecutionParticipants.create(CorePlugin.extensionManager().loadBuildExecutionParticipants());

        RunConfiguration runConfig = event.getProcessDescription().getRunConfig();
        if (runConfig.getTasks().size() > 0) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    ProcessDescription processDescription = event.getProcessDescription();

                    Job executParticipantJob = processDescription.getJob();

                    executParticipantJob.addJobChangeListener(new JobChangeAdapter() {
                        @Override
                        public void done(IJobChangeEvent event) {
                            for( String task : runConfig.getTasks() ) {
                                Set<BuildExecutionParticipant> buildExecutionParticipant = buildParticipants.getBuildExecutionParticipant(task);

                                buildExecutionParticipant.stream().forEach(
                                    buildParticipant -> buildParticipant.postAction(runConfig.getProjectConfiguration().getProjectDir(), task, new NullProgressMonitor())
                                );
                            }
                        }
                    });
                }
            });
        }
    }
}
