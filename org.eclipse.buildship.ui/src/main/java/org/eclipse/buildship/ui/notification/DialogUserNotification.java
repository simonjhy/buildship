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
import org.eclipse.buildship.ui.notification.ExceptionDetailsDialog.DialogController;

/**
 * Implementation of the {@link UserNotification} interface that displays all notifications in a
 * dialog.
 */
public final class DialogUserNotification implements UserNotification {

    private final AtomicReference<DialogController> dialogControllerReference = new AtomicReference<DialogController>();

    @Override
    public void errorOccurred(final String headline, final String message, final String details, final int severity, final Throwable throwable) {
        PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

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
        DialogController controller = this.dialogControllerReference.get();
        return controller == null || controller.getDialog() == null || controller.getDialog().getShell() == null || controller.getDialog().getShell().isDisposed();
    }

    private void showNewDialog(Shell shell, final String headline, final String message, final String details, final int severity, final Throwable throwable) {
        DialogController controller = ExceptionDetailsDialog.create(shell, headline, message, details, severity, throwable);
        this.dialogControllerReference.set(controller);
        controller.getDialog().setBlockOnOpen(false);
        controller.openDialog();
    }

    private void addThrowableToDialog(final Throwable throwable) {
        this.dialogControllerReference.get().addException(throwable);
    }

}
