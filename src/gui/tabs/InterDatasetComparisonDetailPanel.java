package gui.tabs;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class InterDatasetComparisonDetailPanel extends DetailPanel {
	
	private VennDetailPanel 	vennPanel;
	private PairwiseVennDetailPanel pairwiseVennPanel;
	private WilcoxonDetailPanel wilcoxonPanel;
	private KruskalDetailPanel 	kruskalPanel;

	public InterDatasetComparisonDetailPanel(Logger programLogger) {
		super(programLogger);
		
		try {
			
			createUI();
			
		} catch (Exception e) {
			programLogger.log(Level.SEVERE, "Error creating inter-dataset panel", e);
		}
		
	}
	
	private void createUI() throws Exception {
		this.setLayout(new BorderLayout());
		JTabbedPane tabPanel = new JTabbedPane(JTabbedPane.TOP);

		vennPanel 		= new VennDetailPanel(programLogger); 
		pairwiseVennPanel = new PairwiseVennDetailPanel(programLogger);
		wilcoxonPanel 	= new WilcoxonDetailPanel(programLogger);
		kruskalPanel	= new KruskalDetailPanel(programLogger);
		

		// Add to the tabbed panel
		tabPanel.addTab("Venn", null, vennPanel, null);
		tabPanel.addTab("Pairwise Venn", null, pairwiseVennPanel, null);
		tabPanel.addTab("Wilcoxon", null, wilcoxonPanel, null);
		tabPanel.addTab("Kruskal", null, kruskalPanel, null);

		this.add(tabPanel, BorderLayout.CENTER);
	}
	
	@Override
	protected void updateDetail(){

		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try {

					vennPanel.update(getDatasets());
					programLogger.log(Level.FINEST, "Updating Venn panel");
					
					pairwiseVennPanel.update(getDatasets());
					programLogger.log(Level.FINEST, "Updating pairwise Venn panel");
					
					wilcoxonPanel.update(getDatasets());
					programLogger.log(Level.FINEST, "Updating Wilcoxon panel");
					
					kruskalPanel.update(getDatasets());
					programLogger.log(Level.FINEST, "Updating Kruskal panel");

				} catch  (Exception e){
					programLogger.log(Level.SEVERE, "Error updating inter-dataset panels", e);

				} finally {
					setUpdating(false);
				}
			}});
	}

}
