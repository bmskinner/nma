package com.bmskinner.nuclear_morphology.core;

import java.awt.Color;
import java.io.File;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface to allow different user interaction interfaces. Provides methods
 * to request user input.
 * @author bms41
 * @since 1.14.0
 *
 */
public interface InputSupplier {
	
	/**
	 * Request a string input
	 * @param message the message to provide to the user
	 * @return
	 * @throws RequestCancelledException
	 */
	String requestString(@NonNull String message) throws RequestCancelledException;
	
	/**
	 * Request a string input
	 * @param message the message to provide to the user
	 * @param existingValue the current value for the input
	 * @return
	 * @throws RequestCancelledException
	 */
	String requestString(@NonNull String message, @Nullable String existingValue) throws RequestCancelledException;

	/**
	 * Request an int input
	 * @param messsage the message to provide to the user
	 * @return
	 * @throws RequestCancelledException
	 */
	int requestInt(@NonNull String messsage) throws RequestCancelledException;
	
	/**
	 * Request an int input
	 * @param message the message to provide to the user
	 * @param start the initial value (if using a spinner)
	 * @param min the min value (if using a spinner)
	 * @param max the max value (if using a spinner)
	 * @param step the step size  (if using a spinner)
	 * @return
	 * @throws RequestCancelledException
	 */
	int requestInt(@NonNull String messsage, int start, int min, int max, int step) throws RequestCancelledException;

	/**
	 * Request a double input
	 * @param message the message to provide to the user
	 * @return
	 * @throws RequestCancelledException
	 */
	double requestDouble(@NonNull String message) throws RequestCancelledException;
	
	/**
	 * Request a double input
	 * @param message the message to provide to the user
	 * @param start the initial value (if using a spinner)
	 * @param min the min value (if using a spinner)
	 * @param max the max value (if using a spinner)
	 * @param step the step size  (if using a spinner)
	 * @return
	 * @throws RequestCancelledException
	 */
	double requestDouble(@NonNull String message, double start, double min, double max, double step) throws RequestCancelledException;
	
	/**
	 * Request a colour input. It is the responsibility of implementing classes to return a valid Color
	 * object.
	 * @param message the message to provide to the user
	 * @param oldColor the optional old colour to provide
	 * @return the new colour
	 * @throws RequestCancelledException
	 */
	Color requestColor(@NonNull String message, @Nullable Color oldColor) throws RequestCancelledException;
	
	
	/**
	 * Request a file input.
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFile() throws RequestCancelledException;
	
	/**
	 * Request a folder input.
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFolder() throws RequestCancelledException;
	
	/**
	 * Request a folder input.
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFolder(@Nullable File defaultFolder) throws RequestCancelledException;
	
	/**
	 * Request a file input.
	 * @param defaultFolder the default folder
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFile(@Nullable File defaultFolder) throws RequestCancelledException;
	
	/**
	 * Request a file to save to, with the default name and extension.
	 * @param defaultFolder the default folder
	 * 
	 * @return
	 * @throws RequestCancelledException
	 */
	File requestFileSave(@Nullable File defaultFolder, String name, String extension) throws RequestCancelledException;
	
	/**
	 * Exception thwown when the user cancels the input request
	 * @author bms41
	 * @since 1.14.0
	 *
	 */
	public class RequestCancelledException extends Exception {	}

}
