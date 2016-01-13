package gui.tabs;

import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.InterfaceEvent;
import gui.InterfaceEventListener;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.InterfaceEvent.InterfaceMethod;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class EditingDetailPanel extends DetailPanel implements SignalChangeListener, DatasetEventListener, InterfaceEventListener {
	
	private JTabbedPane tabPane;
	protected CellDetailPanel		cellDetailPanel;
	protected SegmentsEditingPanel segmentsEditingPanel;
	
	
	
	public EditingDetailPanel(Logger programLogger){
		
		super(programLogger);
		
		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane();
		this.add(tabPane, BorderLayout.CENTER);
		
		cellDetailPanel = new CellDetailPanel(programLogger);
		this.addSubPanel(cellDetailPanel);
		cellDetailPanel.addSignalChangeListener(this);
		this.addSignalChangeListener(cellDetailPanel);
		tabPane.addTab("Cells", cellDetailPanel);
		
		/*
		 * Signals come from the segment panel to this container
		 * Signals can be sent to the segment panel
		 * Events come from the panel only
		 */
		segmentsEditingPanel = new SegmentsEditingPanel(programLogger);
		segmentsEditingPanel.addSignalChangeListener(this);
		this.addSignalChangeListener(segmentsEditingPanel);
		segmentsEditingPanel.addDatasetEventListener(this);
		segmentsEditingPanel.addInterfaceEventListener(this);
		
		this.addSubPanel(segmentsEditingPanel);
		tabPane.addTab("Segmentation", segmentsEditingPanel);

		
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
		
		programLogger.log(Level.FINER, "Editing panel heard signal: "+event.type());
		if(event.sourceName().equals("CellDetailPanel") || event.sourceName().equals("SegmentsEditingPanel")){
			fireSignalChangeEvent(event.type());			
		} 
			
		cellDetailPanel.signalChangeReceived(event);
		segmentsEditingPanel.signalChangeReceived(event);

		
	}

	@Override
	public void interfaceEventReceived(InterfaceEvent event) {
		fireInterfaceEvent(event.method());
		
		if(event.method().equals(InterfaceMethod.RECACHE_CHARTS)){
			cellDetailPanel.refreshChartCache();
			cellDetailPanel.refreshTableCache();
			segmentsEditingPanel.refreshChartCache();
			segmentsEditingPanel.refreshTableCache();
		}
		
		
	}

	@Override
	public void datasetEventReceived(DatasetEvent event) {
		
		if(event.hasSecondaryDataset()){
			fireDatasetEvent(event.method(), event.getDatasets(), event.secondaryDataset());
		} else {
			fireDatasetEvent(event.method(), event.getDatasets());
		}
	}

}
