package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;

import analysis.AnalysisDataset;
import components.Cell;
import gui.LoadingIconDialog;
import gui.components.AnnotatedNucleusPanel;

@SuppressWarnings("serial")
public class ManualCellCurator extends LoadingIconDialog {
	
	private JButton keepButton = new JButton("Keep");
	private JButton rejectButton = new JButton("Reject");
	
	private AnnotatedNucleusPanel panel; 
	
	private AnalysisDataset dataset = null;
	
	private List<UUID> idsToKeep = new ArrayList<UUID>();
	private int cellIndex = 0;

	
	public ManualCellCurator(Logger programLogger, AnalysisDataset dataset){
		super(programLogger);
		
		this.dataset = dataset;
		this.setLocationRelativeTo(null);
		
		createUI();
		
		this.pack();
		this.setModal(true);
		this.setVisible(true);
	}
	
	public List<UUID> getIDsToKeep(){
		return this.idsToKeep;
	}
	
	private void createUI(){
		this.setLayout(new BorderLayout());
		this.setSize(500,500);
		this.panel = new AnnotatedNucleusPanel(programLogger);
		this.add(panel, BorderLayout.CENTER);
		updateCell(dataset.getCollection().getCells().get(cellIndex));
		
		JPanel buttonPanel = makeButtonPanel();
		this.add(buttonPanel, BorderLayout.SOUTH);
		
	}
	
	private void updateCell(Cell cell){
		try {
			int totalCells = dataset.getCollection().size();
			int cellNumber = cellIndex+1;
			this.setTitle(cell.getNucleus().getNameAndNumber()+": Cell "+cellNumber+" of "+totalCells);
			panel.updateCell(cell);

		} catch (Exception e) {
			programLogger.log(Level.SEVERE, "Error updating cell", e);
		}
	}
	
	private JPanel makeButtonPanel(){
		JPanel panel = new JPanel(new FlowLayout());
		
		panel.add(keepButton);
		panel.add(rejectButton);

		keepButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				if(cellIndex==dataset.getCollection().size()-1){
					// last cell
					setVisible(false);
				} else {

					programLogger.log(Level.FINEST, "Keeping cell");
					idsToKeep.add(dataset.getCollection().getCells().get(cellIndex++).getId());
					updateCell(dataset.getCollection().getCells().get(cellIndex));
				}

			}
		});
		
		rejectButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(cellIndex==dataset.getCollection().size()-1){
					// last cell
					setVisible(false);
				} else {
					programLogger.log(Level.FINEST, "Rejecting cell");
					updateCell(dataset.getCollection().getCells().get(++cellIndex));
				}

			}
		});
		
		
		return panel;
	}

}
