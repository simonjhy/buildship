/*
 * Copyright (c) 2018 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.buildship.core;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Simon Jiang
 * @since 3.0
 */
public interface BuildExecutionParticipant {

    void preAction(File projectDir, String task,IProgressMonitor monitor);

    void postAction(File projectDir, String task, IProgressMonitor monitor);
}
