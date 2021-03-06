/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.as.ui.bot.itests.reddeer.util;

import org.jboss.ide.eclipse.as.reddeer.server.editor.AbstractEditorWithBrowser;
import org.eclipse.reddeer.common.condition.AbstractWaitCondition;

/**
 * Waits until the editor with browser contains the specified text. 
 * 
 * @author Lucia Jelinkova
 *
 */
public class EditorWithBrowserContainsTextCondition extends AbstractWaitCondition {

	private String text;
	
	private String actualText;

	private AbstractEditorWithBrowser editor;

	public EditorWithBrowserContainsTextCondition(AbstractEditorWithBrowser editor, String text) {
		this.text = text;
		this.editor = editor;
	}

	@Override
	public boolean test() {
		actualText = editor.getText();
		return actualText.contains(text);
	}

	@Override
	public String description() {
		return "Page should contain text: " + text + ", but contains: " + actualText;
	}
}
