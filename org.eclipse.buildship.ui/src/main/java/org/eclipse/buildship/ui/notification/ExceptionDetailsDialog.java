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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.buildship.ui.UiPlugin;
import org.eclipse.buildship.ui.i18n.UiMessages;
import org.eclipse.buildship.ui.util.font.FontUtils;

/**
 * Custom {@link Dialog} implementation showing an exception and its stacktrace.
 */
public final class ExceptionDetailsDialog extends Dialog {

    private static final int COPY_EXCEPTION_BUTTON_ID = 25;

    private final DialogController controller;

    private Image image;
    private Button detailsButton;
    private Composite stackTraceAreaControl;
    private Label singleErrorMessageLabel;
    private TableViewer multiErrorExceptionList;
    private Label singleErrorDetailsLabel;
    private Label multiErrorMessageLabel;
    private Clipboard clipboard;
    private Text stacktraceAreaText;
    private Composite singleErrorContainer;
    private Composite multiErrorContainer;
    private StackLayout stackLayout;

    private ExceptionDetailsDialog(DialogController controller, Shell shell, int severity) {
        super(new SameShellProvider(shell));
        this.controller = Preconditions.checkNotNull(controller);
        this.image = getIconForSeverity(severity, shell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite dialogArea = (Composite) super.createDialogArea(parent);
        dialogArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // dialog image
        ((GridLayout) dialogArea.getLayout()).numColumns = 2;
        Label imageLabel = new Label(dialogArea, 0);
        this.image.setBackground(imageLabel.getBackground());
        imageLabel.setImage(this.image);
        imageLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_BEGINNING));

        // composite to include all text widgets
        Composite textArea = new Composite(dialogArea, SWT.NONE);
        GridLayout textAreaLayout = new GridLayout(1, false);
        textAreaLayout.verticalSpacing = FontUtils.getFontHeightInPixels(parent.getFont());
        textAreaLayout.marginWidth = textAreaLayout.marginHeight = 0;
        textArea.setLayout(textAreaLayout);
        GridData textAreaLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        textAreaLayoutData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
        textArea.setLayoutData(textAreaLayoutData);

        Composite stackLayoutContainer = new Composite(textArea, SWT.NONE);
        this.stackLayout = new StackLayout();
        stackLayoutContainer.setLayout(this.stackLayout);
        stackLayoutContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        // single error container
        this.singleErrorContainer = new Composite(stackLayoutContainer, SWT.NONE);
        GridLayout singleErrorContainerLayout = new GridLayout(1, false);
        singleErrorContainerLayout.marginWidth = singleErrorContainerLayout.marginHeight = 0;
        this.singleErrorContainer.setLayout(singleErrorContainerLayout);
        this.stackLayout.topControl = this.singleErrorContainer;

        // single error label
        this.singleErrorMessageLabel = new Label(this.singleErrorContainer, SWT.WRAP);
        GridData messageLabelGridData = new GridData();
        messageLabelGridData.verticalAlignment = SWT.TOP;
        messageLabelGridData.grabExcessHorizontalSpace = true;
        this.singleErrorMessageLabel.setLayoutData(messageLabelGridData);

        // single error details
        this.singleErrorDetailsLabel = new Label(this.singleErrorContainer, SWT.WRAP);
        GridData detailsLabelGridData = new GridData();
        detailsLabelGridData.verticalAlignment = SWT.TOP;
        detailsLabelGridData.grabExcessHorizontalSpace = true;
        this.singleErrorDetailsLabel.setLayoutData(detailsLabelGridData);

        // multi error container
        this.multiErrorContainer = new Composite(stackLayoutContainer, SWT.NONE);
        GridLayout multiErrorContainerLayout = new GridLayout(1, false);
        multiErrorContainerLayout.marginWidth = multiErrorContainerLayout.marginHeight = 0;
        this.multiErrorContainer.setLayout(multiErrorContainerLayout);

        // multi error label
        this.multiErrorMessageLabel = new Label(this.multiErrorContainer, SWT.WRAP);
        this.multiErrorMessageLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        // multi error messages displayed in a list viewer
        GridData multiErrorExceptionListGridData = new GridData();
        multiErrorExceptionListGridData.horizontalAlignment = SWT.FILL;
        multiErrorExceptionListGridData.verticalAlignment = SWT.FILL;
        multiErrorExceptionListGridData.grabExcessHorizontalSpace = true;
        multiErrorExceptionListGridData.grabExcessVerticalSpace = true;
        multiErrorExceptionListGridData.widthHint = 800;
        this.multiErrorExceptionList = new TableViewer(this.multiErrorContainer, SWT.MULTI);
        this.multiErrorExceptionList.getControl().setLayoutData(multiErrorExceptionListGridData);
        this.multiErrorExceptionList.setContentProvider(new ArrayContentProvider());
        this.multiErrorExceptionList.setLabelProvider(new LabelProvider() {

            @Override
            public String getText(Object element) {
                if (element instanceof Throwable) {
                    return ((Throwable) element).getMessage();
                } else {
                    return "";
                }
            }
        });
        this.multiErrorExceptionList.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                ExceptionDetailsDialog.this.controller.updateStacktraceArea();
            }
        });

        // set clipboard
        this.clipboard = new Clipboard(getShell().getDisplay());

        // after widget creation update the contents
        this.controller.updateMessages();
        this.controller.updatePresentatationMode();

        return dialogArea;
    }

    private Image getIconForSeverity(int severity, Shell shell) {
        int swtImageKey;
        switch (severity) {
            case IStatus.OK:
            case IStatus.INFO:
                swtImageKey = SWT.ICON_INFORMATION;
                break;
            case IStatus.WARNING:
            case IStatus.CANCEL:
                swtImageKey = SWT.ICON_WARNING;
                break;
            case IStatus.ERROR:
                swtImageKey = SWT.ICON_ERROR;
                break;
            default:
                // display a warning image if if unknown severity is requested
                swtImageKey = SWT.ICON_WARNING;
                UiPlugin.logger().error("Can't find image for severity: " + severity);
        }

        return shell.getDisplay().getSystemImage(swtImageKey);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button copyExceptionButton = createButton(parent, COPY_EXCEPTION_BUTTON_ID, "", false);
        copyExceptionButton.setToolTipText(UiMessages.Button_CopyFailuresToClipboard_Tooltip);

        copyExceptionButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_COPY));
        this.detailsButton = createButton(parent, IDialogConstants.DETAILS_ID, IDialogConstants.SHOW_DETAILS_LABEL, false);
        Button okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        okButton.setFocus();
    }

    @Override
    protected void setButtonLayoutData(Button button) {
        if (button.getData() != null && button.getData().equals(COPY_EXCEPTION_BUTTON_ID)) {
            // do not set a width hint for the copy error button, like it is done in the super
            // implementation
            GridDataFactory.swtDefaults().applyTo(button);
            return;
        }
        super.setButtonLayoutData(button);
    }

    @Override
    protected void initializeBounds() {
        // do not make columns equal width so that we can have a smaller 'copy failure' button
        Composite buttonBar = (Composite) getButtonBar();
        GridLayout layout = (GridLayout) buttonBar.getLayout();
        layout.makeColumnsEqualWidth = false;
        super.initializeBounds();
    }

    @Override
    protected void buttonPressed(int id) {
        if (id == IDialogConstants.DETAILS_ID) {
            this.controller.toggleStacktraceArea();
        } else if (id == COPY_EXCEPTION_BUTTON_ID) {
            this.controller.copyStacktracesToClipboard();
        } else {
            super.buttonPressed(id);
        }
    }

    void relayoutShell() {
        // compute the new window size
        Point oldSize = getContents().getSize();
        Point newSize = getContents().computeSize(SWT.DEFAULT, SWT.DEFAULT);

        Point oldWindowSize = getShell().getSize();
        Point newWindowSize = new Point(oldWindowSize.x, oldWindowSize.y + (newSize.y - oldSize.y));

        // crop new window size to screen
        Point windowLocation = getShell().getLocation();
        Rectangle screenArea = getContents().getDisplay().getClientArea();
        if (newWindowSize.y > screenArea.height - (windowLocation.y - screenArea.y)) {
            newWindowSize.y = screenArea.height - (windowLocation.y - screenArea.y);
        }

        getShell().setSize(newWindowSize);
        ((Composite) getContents()).layout();
    }

    @Override
    public boolean close() {
        if (this.clipboard != null) {
            this.clipboard.dispose();
            this.clipboard = null;
        }
        return super.close();
    }

    void updateMessageText(String message) {
        if (widgetAccessible(this.singleErrorMessageLabel)) {
            this.singleErrorMessageLabel.setText(message);
        }
        if (widgetAccessible(this.multiErrorMessageLabel)) {
            this.multiErrorMessageLabel.setText(message);
        }
    }

    void updateDetailsText(String details) {
        if (widgetAccessible(this.singleErrorContainer)) {
            this.singleErrorDetailsLabel.setText(details);
        }
    }

    void updateDisplayedThrowables(Collection<Throwable> throwables) {
        if (this.multiErrorExceptionList != null && widgetAccessible(this.multiErrorExceptionList.getControl())) {
            this.multiErrorExceptionList.setInput(throwables);
        }
    }

    void updateStacktraceAreText(String stacktrace) {
        if (widgetAccessible(this.stackTraceAreaControl)) {
            this.stacktraceAreaText.setText(stacktrace);
        }
    }

    void showSingleException(String title) {
        if (this.stackLayout != null && widgetAccessible(getShell())) {
            this.stackLayout.topControl = this.singleErrorContainer;
            getShell().setText(title);
            this.singleErrorContainer.getParent().layout();
        }
    }

    void showMultiException(String title) {
        if (this.stackLayout != null && widgetAccessible(getShell())) {
            this.stackLayout.topControl = this.multiErrorContainer;
            getShell().setText(title);
            this.multiErrorContainer.getParent().layout();
        }
    }

    private boolean widgetAccessible(Widget widget) {
        return widget != null && !widget.isDisposed();
    }

    boolean isStacktraceAreaVisible() {
        return this.stackTraceAreaControl != null;
    }

    void showStacktraceArea() {
        // create the stacktrace container area
        this.stackTraceAreaControl = new Composite((Composite) getContents(), SWT.NONE);
        this.stackTraceAreaControl.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout containerLayout = new GridLayout();
        containerLayout.marginHeight = containerLayout.marginWidth = 0;
        this.stackTraceAreaControl.setLayout(containerLayout);

        //  the text inside the stacktrace area
        this.stacktraceAreaText = new Text(this.stackTraceAreaControl, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        this.stacktraceAreaText.setLayoutData(new GridData(GridData.FILL_BOTH));

        // update
        this.detailsButton.setText(IDialogConstants.HIDE_DETAILS_LABEL);
    }

    void hideStacktraceArea() {
        this.stackTraceAreaControl.dispose();
        this.stackTraceAreaControl = null;
        this.detailsButton.setText(IDialogConstants.SHOW_DETAILS_LABEL);
    }

    Collection<Throwable> getSelectedExceptionsFromList() {
        if (this.multiErrorExceptionList != null && widgetAccessible(this.multiErrorExceptionList.getControl())) {
            ISelection selection = this.multiErrorExceptionList.getSelection();
            if (selection instanceof IStructuredSelection) {
                @SuppressWarnings("unchecked")
                Collection<Throwable> selectedExceptions = FluentIterable.from(((IStructuredSelection) selection).toList()).filter(Throwable.class).toList();
                if (!selectedExceptions.isEmpty()) {
                    return selectedExceptions;
                }
            }
        }
        // if nothing is selected, then return all available exceptions
        return Collections.emptyList();
    }

    void setClipboardContent(String content) {
        if (this.clipboard != null && !this.clipboard.isDisposed()) {
            this.clipboard.setContents(new String[] { content }, new Transfer[] { TextTransfer.getInstance() });
        }
    }

    public static DialogController create(Shell shell, String title, String message, String details, int severity, Throwable throwable) {
        return new DialogController(shell, title, message, details, severity, throwable);
    }

    /**
     * Controller object to create {@link ExceptionDetailsDialog} instances and assign new
     * exceptions to it.
     * <p/>
     * This class should be accessed only form the UI thread.
     */
    public static final class DialogController {

        private static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

        private final String title;
        private final String message;
        private final String details;
        private final ArrayList<Throwable> throwables;

        private final ExceptionDetailsDialog dialog;

        private DialogController(Shell shell, String title, String message, String details, int severity, Throwable throwable) {
            this.title = Preconditions.checkNotNull(title);
            this.message = Preconditions.checkNotNull(message);
            this.details = Preconditions.checkNotNull(details);
            this.throwables = new ArrayList<Throwable>(Arrays.asList(throwable));
            this.dialog = new ExceptionDetailsDialog(this, shell, severity);
        }

        /**
         * Returns the contained dialog object.
         *
         * @return the contained dialog
         */
        public Dialog getDialog() {
            return this.dialog;
        }

        /**
         * Opens the dialog.
         */
        public void openDialog() {
            this.dialog.open();
        }

        /**
         * Adds the target exception to the dialog.
         *
         * @param exception the new exception to display.
         */
        public void addException(final Throwable exception) {
            // save the new exceptions
            this.throwables.add(exception);

            if (this.dialog.getContents() != null) {
                // update the the widget content
                updateMessages();

                // update the presentation mode
                updatePresentatationMode();
            }
        }

        private void updatePresentatationMode() {
            if (this.throwables.size() > 1) {
                this.dialog.showMultiException(UiMessages.Dialog_Title_Multiple_Error);
            } else {
                this.dialog.showSingleException(this.title);
            }
        }

        private void updateStacktraceArea() {
            Collection<Throwable> selectedExceptions = this.dialog.getSelectedExceptionsFromList();
            if (selectedExceptions.isEmpty()) {
                selectedExceptions = this.throwables;
            }
            this.dialog.updateStacktraceAreText(collectStackTraces(selectedExceptions));
        }

        private String collectStackTraces(Collection<Throwable> throwables) {
            Writer writer = new StringWriter(1024);
            PrintWriter printWriter = new PrintWriter(writer);
            for (Throwable throwable : throwables) {
                throwable.printStackTrace(printWriter);
                printWriter.write(LINE_SEPARATOR);
            }
            return writer.toString();
        }

        private void toggleStacktraceArea() {
            if (this.dialog.isStacktraceAreaVisible()) {
                this.dialog.hideStacktraceArea();
            } else {
                this.dialog.showStacktraceArea();
                updateStacktraceArea();
            }
            this.dialog.relayoutShell();
        }

        private void copyStacktracesToClipboard() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.message);
            sb.append(LINE_SEPARATOR);
            sb.append(this.details);
            sb.append(LINE_SEPARATOR);
            sb.append(collectStackTraces(this.throwables));
            this.dialog.setClipboardContent(sb.toString());
        }

        private void updateMessages() {
            this.dialog.updateMessageText(this.message);
            this.dialog.updateDetailsText(this.details);
            this.dialog.updateDisplayedThrowables(this.throwables);
        }
    }

}
