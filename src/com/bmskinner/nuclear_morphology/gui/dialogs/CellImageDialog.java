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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.components.AnnotatedNucleusPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * View a cell annotated onto its original source image
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellImageDialog extends LoadingIconDialog {
	
	private static final Logger LOGGER = Logger.getLogger(CellImageDialog.class.getName());

    private AnnotatedNucleusPanel panel;

    public CellImageDialog(ICell cell) {
        super();

        // Assume cytoplasm==RGB for now
        if (cell.hasCytoplasm()) {

            if (!cell.getCytoplasm().getSourceFile().exists()) {
                LOGGER.warning("Cannot load image: source file not present");
                this.dispose();
            }

        }

        if (cell.hasNucleus()) {
            for(Nucleus n : cell.getNuclei()){
                if (!n.getSourceFile().exists()) {
                    LOGGER.warning("Cannot load image: source file not present");
                    this.dispose();
                }
            }
            
        }

        this.panel = new AnnotatedNucleusPanel();

        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.CENTER);
        this.setTitle(cell.getPrimaryNucleus().getNameAndNumber());

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        try {
            panel.showCell(cell);

        } catch (Exception e) {
            LOGGER.warning("Cannot make cell image dialog");
            LOGGER.log(Loggable.STACK, "Error making dialog", e);
        }
        this.setModal(false);
        this.pack();
        this.centerOnScreen();
        this.setVisible(true);
    }

}
