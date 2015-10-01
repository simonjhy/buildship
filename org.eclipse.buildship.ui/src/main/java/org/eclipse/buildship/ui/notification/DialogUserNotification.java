/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Etienne Studer & Donát Csikós (Gradle Inc.) - initial API and implementation and initial documentation
 */

package org.eclipse.buildship.ui.notification;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import org.eclipse.buildship.core.notification.UserNotification;

/**
 * Implementation of the {@link UserNotification} interface that displays all notifications in a
 * dialog.
 */
public final class DialogUserNotification implements UserNotification {

    private final AtomicReference<ExceptionDetailsDialog> dialogReference = new AtomicReference<ExceptionDetailsDialog>();

    @Override
    public void errorOccurred(final String headline, final String message, final String details, final int severity, final Throwable throwable) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

            @Override
            public void run() {
                Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
                if (noDialogVisible()) {
                    showNewDialog(shell, headline, message, details, severity, throwable);
                } else {
                    addThrowableToDialog(throwable);
                }
            }
        });
    }

    private boolean noDialogVisible() {
        ExceptionDetailsDialog dialog = this.dialogReference.get();
        return dialog == null || dialog.getShell() == null || dialog.getShell().isDisposed();
    }

    private void showNewDialog(Shell shell, final String headline, final String message, final String details, final int severity, final Throwable throwable) {
        ExceptionDetailsDialog dialog = new ExceptionDetailsDialog(shell, headline, message, details, severity, throwable);
        this.dialogReference.set(dialog);
        dialog.open();
    }

    private void addThrowableToDialog(final Throwable throwable) {
        this.dialogReference.get().addException(throwable);
    }

}
