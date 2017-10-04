/*
 * Copyright (c) 2017 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.buildship.ui.editor;

import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

/**
 * Gradle Editor entry point
 *
 * @author Christophe Moine
 */
public class GradleEditor extends TextEditor {
    @Override
    protected void initializeEditor() {
        setSourceViewerConfiguration(new GradleTextViewerConfiguration());
        super.initializeEditor();
    }

    /*
     * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#isTabsToSpacesConversionEnabled()
     * @since 3.7
     */
    @Override
    protected boolean isTabsToSpacesConversionEnabled() {
        // Can't use our own preference store because JDT disables this functionality in its preferences.
        return EditorsUI.getPreferenceStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
    }
}
