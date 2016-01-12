package gui.tabs;

import gui.SignalChangeEvent;
import gui.SignalChangeListener;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class EditingDetailPanel extends DetailPanel implements SignalChangeListener {
	
	private JTabbedPane tabPane;
	protected CellDetailPanel		cellDetailPanel;
	protected SegmentsEditingPanel segmentsEditingPanel;
	
	public EditingDetailPanel(Logger programLogger){
		
		super(programLogger);
		
		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane();
		this.add(tabPane, BorderLayout.CENTER);
		
		cellDetailPanel = new CellDetailPanel(programLogger);
		cellDetailPanel.addSignalChangeListener(this);
		tabPane.addTab("Cells", cellDetailPanel);
		
		segmentsEditingPanel = new SegmentsEditingPanel(programLogger);
		segmentsEditingPanel.addSignalChangeListener(this);
		tabPane.addTab("Median segments", segmentsEditingPanel);

		
	}
	
	@Override
	public void updateDetail(){

		programLogger.log(Level.FINE, "Updating editing detail panel");
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if(getDatasets()!=null && !getDatasets().isEmpty()){
					
					if(isSingleDataset()){
						
						cellDetailPanel.setEnabled(true);
						segmentsEditingPanel.setEnabled(true);
						
						cellDetailPanel.update(getDatasets()); 
						programLogger.log(Level.FINEST, "Updated segments boxplot panel");

						segmentsEditingPanel.update(getDatasets()); 
						programLogger.log(Level.FINEST, "Updated segments histogram panel");
					} else {
						cellDetailPanel.setEnabled(false);
						segmentsEditingPanel.setEnabled(false);
					}
				}
				setUpdating(false);
			}
		});
	}

	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		
		if(event.sourceName().equals("CellDetailPanel") || event.sourceName().equals("SegmentsEditingPanel")){
			fireSignalChangeEvent(event.type());			
		} 
			
		cellDetailPanel.signalChangeReceived(event);
		segmentsEditingPanel.signalChangeReceived(event);

		
	}

}
