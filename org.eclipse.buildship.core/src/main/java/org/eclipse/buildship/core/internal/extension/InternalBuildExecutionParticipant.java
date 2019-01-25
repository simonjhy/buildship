/*
 * Copyright (c) 2018 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.buildship.core.internal.extension;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.buildship.core.BuildExecutionParticipant;
import org.eclipse.buildship.core.internal.GradlePluginsRuntimeException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.gradle.tooling.events.ProgressEvent;

import com.google.common.collect.Sets;

/**
 * @author Simon Jiang
 */

public final class InternalBuildExecutionParticipant implements BuildExecutionParticipant {

    private final BuildExecutionParticipant buildExecutionParticipant;
    private final BuildExecutionParticipantContribution contribution;

    private InternalBuildExecutionParticipant(BuildExecutionParticipantContribution contribution) {
        this.buildExecutionParticipant = createBuildExecutionParticipant(contribution);
        this.contribution = contribution;
    }

    private static BuildExecutionParticipant createBuildExecutionParticipant(BuildExecutionParticipantContribution contribution) {
        try {
            return contribution.createBuildExecutionParticipant();
        } catch (CoreException e) {
            throw new GradlePluginsRuntimeException(e);
        }
    }

    @Override
    public void preBuild(File projectDir, String task, IProgressMonitor monitor) {
        this.buildExecutionParticipant.preBuild(projectDir, task, monitor);
    }

    @Override
    public void postBuild(File projectDir, String task, ProgressEvent event,IProgressMonitor monitor) {
        this.buildExecutionParticipant.postBuild(projectDir, task, event, monitor);
    }

    public String getId() {
        return this.contribution.getId();
    }

    public String getContributorPluginId() {
        return this.contribution.getContributorPluginId();
    }

    public List<String> getTasks() {
        return this.contribution.getTasks();
    }
    
    public static List<InternalBuildExecutionParticipant> from(List<BuildExecutionParticipantContribution> buildExecutionParticipants) {
        buildExecutionParticipants = new ArrayList<>(buildExecutionParticipants);

        filterInvalidBuildExecutionParticipants(buildExecutionParticipants);
        filterDuplicateIds(buildExecutionParticipants);
        return buildExecutionParticipants.stream().map(c -> new InternalBuildExecutionParticipant(c)).collect(Collectors.toList());
    }

    private static void filterInvalidBuildExecutionParticipants(List<BuildExecutionParticipantContribution> buildContributions) {
        Iterator<BuildExecutionParticipantContribution> it = buildContributions.iterator();
        while (it.hasNext()) {
            BuildExecutionParticipantContribution buildContribution = it.next();
            if (buildContribution.getId() == null) {
                it.remove();
            } else {
                try {
                    buildContribution.createBuildExecutionParticipant();
                } catch (Exception e) {
                    it.remove();
                }
            }
        }
    }

    private static void filterDuplicateIds(List<BuildExecutionParticipantContribution> buildContributions) {
        Set<String> ids = Sets.newHashSet();
        Iterator<BuildExecutionParticipantContribution> it = buildContributions.iterator();
        while (it.hasNext()) {
            BuildExecutionParticipantContribution buildContribution = it.next();
            String id = buildContribution.getId();
            if (ids.contains(id)) {
                it.remove();
            } else {
                ids.add(id);
            }
        }
    }
}