package com.bmskinner.nuclear_morphology.gui;

import java.awt.Color;
import java.awt.Component;
import java.io.File;

import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.main.InputSupplier;
import com.bmskinner.nuclear_morphology.main.InputSupplier.RequestCancelledException;

/**
 * Implements the input supplier for the default UI
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultInputSupplier implements InputSupplier {


	@Override
	public String requestString(String message) throws RequestCancelledException {
		return requestString(message, null);
	}
	
	@Override
	public String requestString(String message, String existingValue) throws RequestCancelledException {
		Object s = JOptionPane.showInputDialog(null, message, message,
                JOptionPane.INFORMATION_MESSAGE, null, null, existingValue);
		
		if(s==null)
			throw new RequestCancelledException();
    	return s.toString();
	}

	@Override
	public double requestDouble(String message, double start, double min, double max, double step) throws RequestCancelledException {

		SpinnerNumberModel sModel = new SpinnerNumberModel(start, 
				min, max, step);

		JSpinner spinner = new JSpinner(sModel);

		int option = JOptionPane.showOptionDialog(null, spinner, message, 
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, null, null);

		if (option == JOptionPane.OK_OPTION) {
			return (double) spinner.getModel().getValue();
		} 
		throw new RequestCancelledException();
	}

	@Override
	public int requestInt(String messsage) throws RequestCancelledException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double requestDouble(String message) throws RequestCancelledException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Color requestColor(String message, @Nullable Color oldColor) throws RequestCancelledException {
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
	
	
	
	

}
