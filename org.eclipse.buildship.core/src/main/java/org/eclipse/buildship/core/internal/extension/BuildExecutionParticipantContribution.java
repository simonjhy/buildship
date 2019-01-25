/*
 * Copyright (c) 2018 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.buildship.core.internal.extension;

import java.util.Collections;
import java.util.List;

import org.eclipse.buildship.core.BuildExecutionParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * Describes a valid or invalid contributed project configurator.
 *
 * @author Donat Csikos
 */
public final class BuildExecutionParticipantContribution {

    private final IConfigurationElement extension;
    private final String id;
    private final List<String> tasks;

    private BuildExecutionParticipant buildExecutionParticipant;

    private BuildExecutionParticipantContribution(IConfigurationElement extension, String id, List<String> filterTasks) {
        this.extension = extension;
        this.id = id;
        this.tasks = filterTasks;
    }

    public BuildExecutionParticipant createBuildExecutionParticipant() throws CoreException {
        if (this.buildExecutionParticipant == null) {
            this.buildExecutionParticipant = BuildExecutionParticipant.class.cast(this.extension.createExecutableExtension("class"));
        }

        return this.buildExecutionParticipant;
    }

    public String getId() {
        return this.id;
    }

    public List<String> getTasks() {
        return this.tasks;
    }

    static BuildExecutionParticipantContribution from(IConfigurationElement extension) {
        String id = extension.getAttribute("id");

        Splitter splitter = Splitter.on(',').trimResults().omitEmptyStrings();
        String filterTasksString = extension.getAttribute("tasks");
        List<String> filterTasks = filterTasksString == null
                ? Collections.emptyList()
                : Lists.newArrayList(splitter.split(filterTasksString));

        return new BuildExecutionParticipantContribution(extension, id, filterTasks);
    }

    public static BuildExecutionParticipantContribution from(BuildExecutionParticipantContribution contribuion, List<String> filterTasks) {
        return new BuildExecutionParticipantContribution(contribuion.extension, contribuion.id, filterTasks);
    }

    @Override
    public String toString() {
        return "ProjectConfiguratorContribution [id=" + getId() + ", filterTasks=" + this.tasks + "]";
    }
}
