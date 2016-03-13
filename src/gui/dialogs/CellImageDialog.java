/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.dialogs;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.border.EmptyBorder;

import components.Cell;
import gui.LoadingIconDialog;
import gui.components.AnnotatedNucleusPanel;

/**
 * View a cell annotated onto its original source image
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellImageDialog extends LoadingIconDialog {
	
	private AnnotatedNucleusPanel panel;

	public CellImageDialog(Cell cell) {
		super();
		
		if(!cell.getNucleus().getSourceFile().exists()){
			log(Level.WARNING, "Cannot load image: source file not present");
			this.dispose();
		} else {


			this.panel = new AnnotatedNucleusPanel(programLogger);

			this.setLayout(new BorderLayout());
			this.add(panel, BorderLayout.CENTER);
			this.setTitle(cell.getNucleus().getNameAndNumber());

			this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			this.setLocationRelativeTo(null); // centre on screen


			try{
				panel.updateCell(cell);
			} catch(Exception e){
				logError("Error making dialog", e);
			}
			this.setModal(false);
			this.pack();
			this.setVisible(true);
		}
	}

}
