/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.buildship.core.internal.launch;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.buildship.core.BuildExecutionParticipant;
import org.eclipse.buildship.core.internal.extension.BuildExecutionParticipantContribution;
import org.eclipse.buildship.core.internal.extension.InternalBuildExecutionParticipant;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

public class BuildExecutionParticipants {

    private static BuildExecutionParticipants instance;

    private List<InternalBuildExecutionParticipant> contributions = Lists.newArrayList();

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

    public static BuildExecutionParticipants create(List<BuildExecutionParticipantContribution> buildParticipants) {
        if (instance == null) {
            instance = new BuildExecutionParticipants(InternalBuildExecutionParticipant.from(buildParticipants));
        }

        return instance;
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
 