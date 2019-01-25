/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Etienne Studer & Donát Csikós (Gradle Inc.) - initial API and implementation and initial documentation
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 478054
 */

package org.eclipse.buildship.core.internal.launch;

import org.eclipse.buildship.core.BuildExecutionParticipant;
import org.eclipse.buildship.core.internal.CorePlugin;
import org.eclipse.buildship.core.internal.extension.BuildExecutionParticipantContribution;
import org.eclipse.buildship.core.internal.extension.InternalBuildExecutionParticipant;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.gradle.tooling.events.ProgressEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import java.io.File;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class BuildExecutionParticipants {

    private static BuildExecutionParticipants instance;

    private List<InternalBuildExecutionParticipant> contributions = Lists.newArrayList();

    // the fully-qualified name of the extension point
    private static final String EXECUTION_PARTICIPANTS_EXTENSION_ID = CorePlugin.PLUGIN_ID + ".executionparticipants";

    // the attribute specifying the plugin id to start
    private static final String EXTENSION_ATTRIBUTE_PLUGIN_ID = "id";

    private BuildExecutionParticipants() {
    }

    private final LoadingCache<String, Set<BuildExecutionParticipant>> buildExecutionCaches = 
        CacheBuilder.newBuilder().build(new CacheLoader<String, Set<BuildExecutionParticipant>>() {

            @Override
            public Set<BuildExecutionParticipant> load(String task) throws Exception {
                return contributions.stream().filter(buildParticipant -> {
                    if ( buildParticipant.getTasks().size() > 0 ) {
                        return buildParticipant.getTasks().contains(task);
                    }else {
                        return true;
                    }
                }).collect(Collectors.toSet());
            }
        }
    );

    public static void activateParticipantPlugins() {
        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(EXECUTION_PARTICIPANTS_EXTENSION_ID);
        for (IConfigurationElement element : elements) {
            String pluginId = element.getAttribute(EXTENSION_ATTRIBUTE_PLUGIN_ID);
            try {
                // start the bundle in case it is not active yet
                // for details, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=478054
                Bundle bundle = Platform.getBundle(pluginId);
                if (Bundle.ACTIVE != bundle.getState()) {
                    bundle.start(Bundle.START_TRANSIENT);
                }
            } catch (BundleException e) {
                String message = String.format("Failed to activate plugin %s referenced in extension point 'executionparticipants'.", pluginId);
                CorePlugin.logger().error(message, e);
            }
        }
    }

    public static BuildExecutionParticipants create(List<BuildExecutionParticipantContribution> buildParticipants) {
        if (instance == null) {
            instance = new BuildExecutionParticipants(InternalBuildExecutionParticipant.from(buildParticipants));
        }

        return instance;
    }

    public void postBuild(File projectDir, List<String> tasks, ProgressEvent event, IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor);
        progress.setWorkRemaining(this.contributions.size());
        try {
            tasks.stream(
            ).forEach(
                task -> {
                    Set<BuildExecutionParticipant> buildExecutionParticipant = getBuildExecutionParticipant(task);
                    buildExecutionParticipant.stream().forEach(
                        contribution -> contribution.postBuild(projectDir, task, event, monitor));
                }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void preBuild(File projectDir, List<String> tasks,IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor);
        progress.setWorkRemaining(this.contributions.size());
        try {
            tasks.stream(
            ).forEach(
                task -> {
                    Set<BuildExecutionParticipant> buildExecutionParticipant = getBuildExecutionParticipant(task);
                    buildExecutionParticipant.stream().forEach(
                        contribution -> contribution.preBuild(projectDir, task, progress)
                    );
                }
            );
        } catch (Exception e) {
        }
    }

    
    
    private BuildExecutionParticipants(List<InternalBuildExecutionParticipant> contributions) {
        this.contributions = contributions;
    }

    public Set<BuildExecutionParticipant> getBuildExecutionParticipant(String task) {
        try {
            return buildExecutionCaches.get(task);
        } catch (ExecutionException e) {
            return Collections.emptySet();
        }
    }
}
 