/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.core;

import java.awt.Color;
import java.io.File;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.gui.DefaultInputSupplier;

/**
 * Interface to allow different user interaction interfaces. Provides methods to
 * request user input.
 * 
 * @author bms41
 * @since 1.14.0
 *
 */
public interface InputSupplier {

	static InputSupplier getDefault() {
		return new DefaultInputSupplier();
	}

	/**
	 * Request a string input
	 * 
	 * @param message the message to provide to the user
	 * @return
	 * @throws RequestCancelledException
	 */
	String requestString(@NonNull String message) throws RequestCancelledException;

	/**
	 * Request a string input
	 * 
	 * @param message       the message to provide to the user
	 * @param existingValue the current value for the input
	 * @return
	 * @throws RequestCancelledException
	 */
	String requestString(@NonNull String message, @Nullable String existingValue)
			throws RequestCancelledException;

	/**
	 * Request an int input
	 * 
	 * @param messsage the message to provide to the user
	 * @return
	 * @throws RequestCancelledException
	 */
	int requestInt(@NonNull String messsage) throws RequestCancelledException;

	/**
	 * Request an int input
	 * 
	 * @param message the message to provide to the user
	 * @param start   the initial value (if using a spinner)
	 * @param min     the min value (if using a spinner)
	 * @param max     the max value (if using a spinner)
	 * @param step    the step size (if using a spinner)
	 * @return
	 * @throws RequestCancelledException
	 */
	int requestInt(@NonNull String messsage, int start, int min, int max, int step)
			throws RequestCancelledException;

	/**
	 * Request a double input
	 * 
	 * @param message the message to provide to the user
	 * @return
	 * @throws RequestCancelledException
	 */
	double requestDouble(@NonNull String message) throws RequestCancelledException;

	/**
	 * Request a double input
	 * 
	 * @param message the message to provide to the user
	 * @param start   the initial value (if using a spinner)
	 * @param min     the min value (if using a spinner)
	 * @param max     the max value (if using a spinner)
	 * @param step    the step size (if using a spinner)
	 * @return
	 * @throws RequestCancelledException
	 */
	double requestDouble(@NonNull String message, double start, double min, double max, double step)
			throws RequestCancelledException;

	/**
	 * Request a colour input. It is the responsibility of implementing classes to
	 * return a valid Color object.
	 * 
	 * @param message  the message to provide to the user
	 * @param oldColor the optional old colour to provide
	 * @return the new colour
	 * @throws RequestCancelledException
	 */
	Color requestColor(@NonNull String message, @Nullable Color oldColor)
			throws RequestCancelledException;

	/**
	 * Request a file input.
	 * 
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFile() throws RequestCancelledException;

	/**
	 * Request a folder input. The starting directory will be the default system
	 * folder.
	 * 
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFolder() throws RequestCancelledException;

	/**
	 * Request a folder input.
	 * 
	 * @param defaultFolder the default folder
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFolder(@Nullable File defaultFolder) throws RequestCancelledException;

	/**
	 * Request a folder input.
	 * 
	 * @param message the title bar message
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFolder(@Nullable String message) throws RequestCancelledException;

	/**
	 * Request a folder input.
	 * 
	 * @param message       the title bar message
	 * @param defaultFolder the default folder
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFolder(@Nullable String message, @Nullable File defaultFolder)
			throws RequestCancelledException;

	/**
	 * Request a file input.
	 * 
	 * @param defaultFolder the default folder
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFile(@Nullable File defaultFolder) throws RequestCancelledException;

	/**
	 * Request a file input with an extension filter
	 * 
	 * @param defaultFolder    the default folder
	 * @param extension        the file extension filter
	 * @param extensionMessage the textual description of the file extension
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFile(@Nullable String message, @Nullable File defaultFolder,
			@Nullable String extension,
			@Nullable String extensionMessage) throws RequestCancelledException;

	/**
	 * Request a file to save to, with the default name and extension.
	 * 
	 * @param defaultFolder the default folder
	 * @param name          the file name
	 * @param extension     the file extension, with no '.'
	 * @return the selected file
	 * @throws RequestCancelledException
	 */
	File requestFileSave(@Nullable File defaultFolder, String name, String extension)
			throws RequestCancelledException;

	/**
	 * Request the user to choose between a set of options. The default option will
	 * be the first item in the options provided.
	 * 
	 * @param options the options to choose between
	 * @param message the message to provide to the user
	 * @return the chosen option
	 */
	int requestOption(Object[] options, String message) throws RequestCancelledException;

	/**
	 * Request the user to choose between a set of options. The default option will
	 * be the first item in the options provided.
	 * 
	 * @param options the options to choose between
	 * @param message the message to provide to the user
	 * @param title   a title to provide on dialog boxes
	 * @return the chosen option
	 */
	int requestOption(Object[] options, String message, String title)
			throws RequestCancelledException;

	/**
	 * Request the user to choose between a set of options
	 * 
	 * @param options       the options to choose between
	 * @param defaultOption the index of the default option
	 * @param message       the message to provide to the user
	 * @param title         a title to provide on dialog boxes
	 * @return the chosen option
	 */
	int requestOption(Object[] options, int defaultOption, String message, String title)
			throws RequestCancelledException;

	/**
	 * Request the user to choose between a set of options
	 * 
	 * @param options       the options to choose between
	 * @param defaultOption the index of the default option
	 * @param message       the message to provide to the user
	 * @return the chosen option
	 */
	int requestOption(Object[] options, int defaultOption, String message)
			throws RequestCancelledException;

	/**
	 * Request the user to choose between a set of options. All the options will be
	 * on screen. The default option will be the first item in the options provided.
	 * 
	 * @param options the options to choose between
	 * @param message the message to provide to the user
	 * @param title   a title to provide on dialog boxes
	 * @return the chosen option
	 */
	int requestOptionAllVisible(Object[] options, String message, String title)
			throws RequestCancelledException;

	/**
	 * Request the user to choose between a set of options. All the options will be
	 * on screen.
	 * 
	 * @param options       the options to choose between
	 * @param defaultOption the index of the default option
	 * @param message       the message to provide to the user
	 * @param title         a title to provide on dialog boxes
	 * @return the chosen option
	 */
	int requestOptionAllVisible(Object[] options, int defaultOption, String message, String title)
			throws RequestCancelledException;

	/**
	 * Request the user to approve an action. Responds yes (true), no (false) or
	 * excepts on cancel.
	 * 
	 * @param message the message to provide to the user
	 * @param title   a title to provide on dialog boxes
	 * @return
	 * @throws RequestCancelledException
	 */
	boolean requestApproval(String message, String title) throws RequestCancelledException;

	/**
	 * Test the given file is suitable as a save option. Checks nulls and requests
	 * confirmation of overwriting any existing file
	 * 
	 * @param file the file to check
	 * @return true if the file should be written to, false otherwise
	 * @throws RequestCancelledException if the user cancels
	 */
	boolean fileIsOKForSave(File file) throws RequestCancelledException;

	/**
	 * Exception thrown when the user cancels the input request
	 * 
	 * @author bms41
	 * @since 1.14.0
	 *
	 */
	public class RequestCancelledException extends Exception {
		private static final long serialVersionUID = 1L;
	}

}
