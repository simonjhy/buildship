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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.buildship.core.GradlePluginsRuntimeException;
import org.eclipse.buildship.ui.UiPlugin;
import org.eclipse.buildship.ui.i18n.UiMessages;
import org.eclipse.buildship.ui.util.font.FontUtils;

/**
 * Custom {@link Dialog} implementation showing an exception and its stacktrace.
 */
public final class ExceptionDetailsDialog extends Dialog {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$
    private static final int COPY_EXCEPTION_BUTTON_ID = 25;

    private final Image image;
    private final String title;
    private final String message;
    private final String details;
    private final Collection<Throwable> throwables;

    private Button detailsButton;
    private Control stackTraceAreaControl;
    private Label singleErrorMessageLabel;
    private TableViewer multiErrorExceptionList;
    private Label singleErrorDetailsLabel;
    private Label multiErrorMessageLabel;
    private Clipboard clipboard;
    private Text stacktraceAreaText;
    private Composite singleErrorContainer;
    private Composite multiErrorContainer;
    private StackLayout stackLayout;

    public ExceptionDetailsDialog(Shell shell, String title, String message, String details, int severity, Throwable throwable) {
        super(new SameShellProvider(shell));

        this.image = getIconForSeverity(severity, shell);
        this.title = Preconditions.checkNotNull(title);
        this.message = Preconditions.checkNotNull(message);
        this.details = Preconditions.checkNotNull(details);
        this.throwables = new ArrayList<Throwable>(Arrays.asList(throwable));

        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
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
                swtImageKey = SWT.ICON_WARNING;
                UiPlugin.logger().error("Can't find image for severity: " + severity);
        }

        return shell.getDisplay().getSystemImage(swtImageKey);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        // set dialog box title
        shell.setText(this.title);
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
        multiErrorExceptionListGridData.verticalAlignment = SWT.TOP;
        multiErrorExceptionListGridData.grabExcessHorizontalSpace = true;
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
                if (ExceptionDetailsDialog.this.stacktraceAreaText != null && ExceptionDetailsDialog.this.stacktraceAreaText.isDisposed()) {
                    ISelection selection = event.getSelection();
                    String stackTraces = collectStackTraces(collectSelectedExceptions(selection));
                    ExceptionDetailsDialog.this.stacktraceAreaText.setText(stackTraces);
                }
            }
        });

        // set clipboard
        this.clipboard = new Clipboard(getShell().getDisplay());

        // after widget creation update the contents
        updateWidgetText();
        updatePresentationMode();

        return dialogArea;
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
            toggleStacktraceArea();
        } else if (id == COPY_EXCEPTION_BUTTON_ID) {
            copyErrorToClipboard();
        } else {
            super.buttonPressed(id);
        }
    }

    /**
     * Adds the target exception to the dialog.
     *
     * @param exception the new exception to display.
     */
    public void addException(final Throwable exception) {
        Display display = PlatformUI.getWorkbench().getDisplay();
        Thread displayThread = display.getThread();

        if (Thread.currentThread().equals(displayThread)) {
            addExceptionInUiThread(exception);
        } else {
           throw new GradlePluginsRuntimeException("This method must be called from the UI thread");
        }
    }

    private void addExceptionInUiThread(Throwable exception) {
        // the exception list always manipulated from the UI thread, therefore no synchronization
        // is necessary
        this.throwables.add(exception);
        if (getContents() != null && !getContents().isDisposed()) {
            updateWidgetText();
            updatePresentationMode();
            relayoutShell();
        }
    }

    private Collection<Throwable> collectSelectedExceptions(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            @SuppressWarnings("unchecked")
            Collection<Throwable> selectedExceptions = FluentIterable.from(((IStructuredSelection) selection).toList()).filter(Throwable.class).toList();
            if (!selectedExceptions.isEmpty()) {
                return selectedExceptions;
            }
        }
        // if nothing is selected, then return all available exceptions
        return this.throwables;
    }

    private void updatePresentationMode() {
        if (this.throwables.size() > 1) {
            this.stackLayout.topControl = this.multiErrorContainer;
            getShell().setText(UiMessages.Dialog_Title_Multiple_Error);
        } else {
            this.stackLayout.topControl = this.singleErrorContainer;
            getShell().setText(this.title);
        }
    }

    private void copyErrorToClipboard() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.message);
        sb.append(LINE_SEPARATOR);
        sb.append(this.details);
        sb.append(LINE_SEPARATOR);
        sb.append(collectStackTraces(this.throwables));

        this.clipboard.setContents(new String[] { sb.toString() }, new Transfer[] { TextTransfer.getInstance() });
    }

    private void toggleStacktraceArea() {
        // show/hide stacktrace
        if (this.stackTraceAreaControl == null) {
            this.stackTraceAreaControl = createStacktraceArea((Composite) getContents());
            this.detailsButton.setText(IDialogConstants.HIDE_DETAILS_LABEL);
        } else {
            this.stackTraceAreaControl.dispose();
            this.stackTraceAreaControl = null;
            this.detailsButton.setText(IDialogConstants.SHOW_DETAILS_LABEL);
        }

        relayoutShell();
    }

    private Control createStacktraceArea(Composite parent) {
        // create the stacktrace container area
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout containerLayout = new GridLayout();
        containerLayout.marginHeight = containerLayout.marginWidth = 0;
        container.setLayout(containerLayout);

        this.stacktraceAreaText = new Text(container, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        this.stacktraceAreaText.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.stacktraceAreaText.setText(collectStackTraces(collectSelectedExceptions(this.multiErrorExceptionList.getSelection())));

        return container;
    }

    private void relayoutShell() {
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

    private void updateWidgetText() {
        this.singleErrorMessageLabel.setText(this.message);
        this.singleErrorDetailsLabel.setText(this.details);
        this.multiErrorMessageLabel.setText(this.message);
        this.multiErrorExceptionList.setInput(this.throwables);
    }

    private static String collectStackTraces(Collection<Throwable> throwables) {
        Writer writer = new StringWriter(1024);
        PrintWriter printWriter = new PrintWriter(writer);
        for (Throwable throwable : throwables) {
            throwable.printStackTrace(printWriter);
            printWriter.write(LINE_SEPARATOR);
        }
        return writer.toString();
    }

}
