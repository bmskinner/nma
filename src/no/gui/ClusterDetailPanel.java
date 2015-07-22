package no.gui;


import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import no.analysis.AnalysisDataset;

public class ClusterDetailPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	public static final String SOURCE_COMPONENT = "ClusterDetailPanel"; 
	private JPanel clustersPanel;
	private List<Object> listeners = new ArrayList<Object>();

	public ClusterDetailPanel() {
		
		clustersPanel = new JPanel();

		clustersPanel.setLayout(new BoxLayout(clustersPanel, BoxLayout.Y_AXIS));
		this.add(clustersPanel, BorderLayout.CENTER);

	}
	
	public void update(List<AnalysisDataset> list){
		
		if(list.size()==1){
			AnalysisDataset dataset = list.get(0);
			
			clustersPanel = new JPanel();
			clustersPanel.setLayout(new BoxLayout(clustersPanel, BoxLayout.Y_AXIS));
			
			if(!dataset.hasClusters()){ // only allow clustering once per population

				JButton btnNewClusterAnalysis = new JButton("Cluster population");
				btnNewClusterAnalysis.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent arg0) {
						fireSignalChangeEvent("NewClusterAnalysis");

					}
				});
				clustersPanel.add(btnNewClusterAnalysis);
				
			} else { // clusters present, show the tree if available
				JTextArea label = new JTextArea(dataset.getClusterTree());
				label.setLineWrap(true);
				
				JScrollPane treeView = new JScrollPane(label);
				clustersPanel.add(treeView);
			}
			
			
		} else {
			clustersPanel = new JPanel();
			clustersPanel.setLayout(new BoxLayout(clustersPanel, BoxLayout.Y_AXIS));
		}
		
	}
	
	public synchronized void addSignalChangeListener( SignalChangeListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
        listeners.remove( l );
    }
	
    private synchronized void fireSignalChangeEvent(String message) {
    	
        SignalChangeEvent event = new SignalChangeEvent( this, message, SOURCE_COMPONENT);
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
        }
    }

}
