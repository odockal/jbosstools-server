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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.core.Is;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.as.ui.bot.itests.reddeer.Runtime;
import org.jboss.tools.as.ui.bot.itests.reddeer.RuntimeMatcher;
import org.jboss.tools.as.ui.bot.itests.reddeer.ui.RuntimeDetectionPreferencePage;
import org.jboss.tools.as.ui.bot.itests.reddeer.ui.SearchingForRuntimesDialog;

/**
 * Common scenario for runtime detection tests.
 * 
 * It adds the runtime's installation folder to the runtime detection,
 * checks if it is correctly recognized and created and then remove
 * added runtime's installation folder.
 * 
 * 
 *   
 * @author Lucia Jelinkova
 * @author Petr Suchy
 * @author Radoslav Rabara
 */
public class DetectRuntimeTemplate extends RuntimeDetectionUtility {
	
	public static List<Runtime> detectRuntime(String path) {
		assertTrue("Path " + path + " doesn't exists", new File(path).exists());
		SearchingForRuntimesDialog searchingForRuntimesDialog = addPath(path);
		
		List<Runtime> runtimes = searchingForRuntimesDialog.getRuntimes();
		searchingForRuntimesDialog.ok();
		WorkbenchPreferenceDialog preferences = new WorkbenchPreferenceDialog();
		preferences.ok();
		return runtimes;
	}
	
	public static List<Runtime> detectRuntime(String path, List<Runtime> expected) {
		assertTrue("Path " + path + " doesn't exists", new File(path).exists());
		SearchingForRuntimesDialog searchingForRuntimesDialog = addPath(path);
		
		List<Runtime> runtimes = searchingForRuntimesDialog.getRuntimes();
		
		searchingForRuntimesDialog.ok();
		
		WorkbenchPreferenceDialog preferences = new WorkbenchPreferenceDialog();
		preferences.ok();
		
		assertCountOfRuntimes(searchingForRuntimesDialog, expected, runtimes, path);
		assertThatExpectedRuntimesArePresent(expected, runtimes);
		return runtimes;
	}

	
	public static void removePath(String requiredPath) {
		WorkbenchPreferenceDialog preferenceDialog = new WorkbenchPreferenceDialog();
		preferenceDialog.open();
		RuntimeDetectionPreferencePage runtimeDetectionPage = new RuntimeDetectionPreferencePage(preferenceDialog);
		preferenceDialog.select(runtimeDetectionPage);
						
		List<String> allPaths = runtimeDetectionPage.getAllPaths();
		assertTrue("Expected is presence of path " + requiredPath + " but there are:\n"
				+ Arrays.toString(allPaths.toArray()), allPaths.contains(requiredPath));
		
		runtimeDetectionPage.removeAllPaths();
		
		allPaths = runtimeDetectionPage.getAllPaths();
		
		preferenceDialog.ok();
		
		assertThat("Not all paths were removed. There are " + Arrays.toString(allPaths.toArray()), allPaths.size(), Is.is(0));
	}

	
	private static void assertCountOfRuntimes(SearchingForRuntimesDialog dialog,
			List<Runtime> expected,
			List<Runtime> runtimes, String path) {
		int size = runtimes.size();
		int expectedSize = expected.size();
		if(size > 0) {
			assertThat("Expected " + expectedSize + " but there were " + size
					+ ":\nExpected runtimes: "+Arrays.toString(expected.toArray())
					+ "\nBut there were " + Arrays.toString(runtimes.toArray()), size, is(expectedSize));
		} else {
			dialog.cancel();
			fail("No runtime detected in folder: " + path);
		}
	}
	
	private static void assertThatExpectedRuntimesArePresent(List<Runtime> expected, List<Runtime> runtimes) {
		for (Runtime runtime : expected){
			assertThat(runtimes, hasItem(new RuntimeMatcher(runtime)));
		}
	}
}
