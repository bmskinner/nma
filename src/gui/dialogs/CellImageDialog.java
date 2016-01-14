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

	public CellImageDialog(Logger programLogger, Cell cell) {
		super(programLogger);
		
		if(!cell.getNucleus().getSourceFile().exists()){
			programLogger.log(Level.WARNING, "Cannot load image: source file not present");
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
				programLogger.log(Level.SEVERE, "Error making dialog", e);
			}
			this.setModal(false);
			this.pack();
			this.setVisible(true);
		}
	}

}
