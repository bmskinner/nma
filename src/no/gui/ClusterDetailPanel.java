package no.gui;


import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import no.analysis.AnalysisDataset;

public class ClusterDetailPanel extends DetailPanel {

	private static final long serialVersionUID = 1L;
	
	private JButton 	clusterButton	= new JButton("Cluster population");
	private JLabel		statusLabel 	= new JLabel("No clusters present", SwingConstants.CENTER);
	private JPanel		statusPanel		= new JPanel(new BorderLayout());
	private JTextArea 	tree			= new JTextArea();
	private JScrollPane treeView;
	
	private JTable		mergeSources;
	
	private JPanel		clusterPanel	= new JPanel(new BorderLayout());
	private JPanel		mergePanel		= new JPanel(new BorderLayout());

	public ClusterDetailPanel() {
		
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		clusterPanel = makeClusterPanel();
		mergePanel 	 = makeMergeSourcesPanel();
		this.add(mergePanel);
		this.add(clusterPanel);

	}
	
	/**
	 * This panel shows the status of the dataset, 
	 * and holds the clustering button
	 * @return
	 */
	private JPanel makeStatusPanel(){
		
		JPanel panel = new JPanel(new BorderLayout());
		clusterButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				fireSignalChangeEvent("NewClusterAnalysis");

			}
		});
		clusterButton.setVisible(false);
		
		panel.add(clusterButton, BorderLayout.SOUTH);
		panel.add(statusLabel, BorderLayout.CENTER);
				
		return panel;
	}
	
	private JPanel makeClusterPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		
		treeView = new JScrollPane(tree);
		panel.add(treeView, BorderLayout.CENTER);
		treeView.setVisible(false);
			
		statusPanel = makeStatusPanel();
		panel.add(statusPanel, BorderLayout.NORTH);
		
		return panel;
	}
	
	private JPanel makeMergeSourcesPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		mergeSources = new JTable(makeBlankTable());
		mergeSources.setEnabled(false);
		
		panel.add(mergeSources, BorderLayout.CENTER);
		panel.add(mergeSources.getTableHeader(), BorderLayout.NORTH);
		return panel;
	}
	
	private DefaultTableModel makeBlankTable(){
		DefaultTableModel model = new DefaultTableModel();

		Vector<Object> names 	= new Vector<Object>();
		Vector<Object> nuclei 	= new Vector<Object>();

		names.add("No merge sources");
		nuclei.add("");


		model.addColumn("Merge source", names);
		model.addColumn("Nuclei", nuclei);
		return model;
	}
	
	public void update(List<AnalysisDataset> list){
		
		clusterButton.setVisible(false);
		
		if(list.size()==1){
			AnalysisDataset dataset = list.get(0);
			
			// If no clusters are present, show the button
			if(!dataset.hasClusters()){
				
				statusLabel.setText("Dataset contains no clusters");
				clusterButton.setVisible(true);
				tree.setText("");
				treeView.setVisible(false);
				
			} else {
				 // clusters present, show the tree if available
				// Show a line indicating clusters are present anyway
				statusLabel.setText("Dataset contains "+dataset.getClusterIDs().size()+" clusters");
				
				String treeString = dataset.getClusterTree();
				if(treeString!=null){
					tree.setText(treeString);
					tree.setLineWrap(true);
					treeView.revalidate();
					treeView.setVisible(true);
				}
				

			}
			if(dataset.hasMergeSources()){
				
				DefaultTableModel model = new DefaultTableModel();

				Vector<Object> names 	= new Vector<Object>();
				Vector<Object> nuclei 	= new Vector<Object>();

				for( UUID id : dataset.getMergeSources()){
					AnalysisDataset mergeSource = dataset.getMergeSource(id);
					names.add(mergeSource.getName());
					nuclei.add(mergeSource.getCollection().getNucleusCount());
				}
				model.addColumn("Merge source", names);
				model.addColumn("Nuclei", nuclei);

				mergeSources.setModel(model);
				
			} else {
				mergeSources.setModel(makeBlankTable());
			}
		} else { // more than one dataset selected
			statusLabel.setText("Multiple datasets selected");
			clusterButton.setVisible(false);
			tree.setText("");
			treeView.setVisible(false);
			
			mergeSources.setModel(makeBlankTable());
		}
		
	}
}
