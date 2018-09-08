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
import com.bmskinner.nuclear_morphology.io.Io.Importer;

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
	public File requestFile(@Nullable File defaultFolder, @Nullable String extension, @Nullable String extensionMessage) throws RequestCancelledException {
		FileNameExtensionFilter filter = null;
		if(extension!=null)
			filter = new FileNameExtensionFilter(extensionMessage, extension);
		File f = FileSelector.chooseFile(defaultFolder, filter);
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
	public int requestOption(String[] options, int defaultOption, String message) throws RequestCancelledException {
        int result = JOptionPane.showOptionDialog(null, message, message,
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[defaultOption]);
        if(result<0)
        	throw new RequestCancelledException();
        return result;
        
	}
	
	
	
	

}
