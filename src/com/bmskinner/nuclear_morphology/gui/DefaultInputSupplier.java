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
package com.bmskinner.nuclear_morphology.gui;

import java.awt.Color;
import java.io.File;

import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;

/**
 * Implements the input supplier for the default UI
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultInputSupplier implements InputSupplier {


	@Override
	public String requestString(@NonNull String message) throws RequestCancelledException {
		return requestString(message, null);
	}
	
	@Override
	public String requestString(@NonNull String message, @Nullable String existingValue) throws RequestCancelledException {
		Object s = JOptionPane.showInputDialog(null, message, message,
                JOptionPane.INFORMATION_MESSAGE, null, null, existingValue);
		
		if(s==null)
			throw new RequestCancelledException();
    	return s.toString();
	}
	
	@Override
	public double requestDouble(@NonNull String message) throws RequestCancelledException {
		return requestDouble(message, 0, -Double.MAX_VALUE, Double.MAX_VALUE, 1);
	}
	
	@Override
	public double requestDouble(@NonNull String message, double start, double min, double max, double step) throws RequestCancelledException {

		SpinnerNumberModel sModel = new SpinnerNumberModel(start, 
				min, max, step);

		JSpinner spinner = new JSpinner(sModel);

		int option = JOptionPane.showOptionDialog(null, spinner, message, 
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, null, null);

		if (option == JOptionPane.OK_OPTION)
			return (double) spinner.getModel().getValue();
		throw new RequestCancelledException();
	}
	
	@Override
	public int requestInt(@NonNull String message) throws RequestCancelledException {
		return requestInt(message, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
	}

	@Override
	public int requestInt(@NonNull String message, int start, int min, int max, int step) throws RequestCancelledException {
		SpinnerNumberModel sModel = new SpinnerNumberModel(start, 
				min, max, step);

		JSpinner spinner = new JSpinner(sModel);

		int option = JOptionPane.showOptionDialog(null, spinner, message, 
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, null, null);

		if (option == JOptionPane.OK_OPTION)
			return (int) spinner.getModel().getValue();
		throw new RequestCancelledException();
	}

	

	@Override
	public Color requestColor(@NonNull String message, @Nullable Color oldColor) throws RequestCancelledException {
		Color newColor = JColorChooser.showDialog(null, message, oldColor);
		if(newColor==null)
			throw new RequestCancelledException();
		return newColor;
	}

	@Override
	public File requestFile() throws RequestCancelledException {
		return requestFile(null);
	}

	@Override
	public File requestFile(@Nullable File defaultFolder) throws RequestCancelledException {
		File f = FileSelector.chooseFile(defaultFolder);
		if(f==null)
			throw new RequestCancelledException();
		return f;
	}
	
	@Override
	public File requestFile(@Nullable String message, @Nullable File defaultFolder, @Nullable String extension, @Nullable String extensionMessage) throws RequestCancelledException {
		FileNameExtensionFilter filter = null;
		if(extension!=null)
			filter = new FileNameExtensionFilter(extensionMessage, extension);
		File f = FileSelector.chooseFile(defaultFolder, filter, message);
		if(f==null)
			throw new RequestCancelledException();
		return f;
	}

	@Override
	public File requestFileSave(@Nullable File defaultFolder, String name, String extension)
			throws RequestCancelledException {
		FileNameExtensionFilter filter = new FileNameExtensionFilter(extension, extension);
		File f = FileSelector.chooseSaveFile(defaultFolder, filter, name+"."+extension);
		if(f==null)
			throw new RequestCancelledException();
		return f;
	}

	@Override
	public File requestFolder() throws RequestCancelledException {
		return requestFolder(null, null);
	}
	
	@Override
	public File requestFolder(@Nullable String message) throws RequestCancelledException {
		return requestFolder(message, null);
	}

	@Override
	public File requestFolder(@Nullable File defaultFolder) throws RequestCancelledException {
		return requestFolder(null, defaultFolder);
	}
	
	@Override
	public File requestFolder(@Nullable String message, @Nullable File defaultFolder) throws RequestCancelledException {
		File f = FileSelector.chooseFolder(message, defaultFolder);
		if(f==null)
			throw new RequestCancelledException();
		return f;
	}
	
	@Override
	public int requestOption(String[] options, String message) throws RequestCancelledException {
		return requestOption(options, 0, message);
	}

	@Override
	public int requestOption(String[] options, String message, String title) throws RequestCancelledException {
		return requestOption(options, 0, message, title);
	}

	@Override
	public int requestOption(String[] options, int defaultOption, String message) throws RequestCancelledException {
        return requestOption(options, defaultOption, message, message);
	}
	
	@Override
	public int requestOption(String[] options, int defaultOption, String message, String title) throws RequestCancelledException {
        
        Object result = JOptionPane.showInputDialog(null, message , title,
                                JOptionPane.QUESTION_MESSAGE, null, options, options[defaultOption]);

        if(result==null)
        	throw new RequestCancelledException();
        
        for(int i=0; i<options.length; i++)
        	if(options[i].equals(result))
        		return i;

        throw new RequestCancelledException();
        
	}
	
	@Override
	public int requestOptionAllVisible(String[] options, String message, String title)
			throws RequestCancelledException {
        return requestOptionAllVisible(options, 0, message, title);
	}
	
	@Override
	public int requestOptionAllVisible(String[] options, int defaultOption, String message, String title)
			throws RequestCancelledException {
        int result = JOptionPane.showOptionDialog(null, message, title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[defaultOption]);
        if(result<0)
        	throw new RequestCancelledException();
        return result;
	}

	@Override
	public boolean requestApproval(String message, String title) throws RequestCancelledException {
		int result = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
		if(result==2)
			throw new RequestCancelledException();
		return result==0;
	}

}
