package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.Cell;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;

@SuppressWarnings("serial")
public class CellsListPanel extends AbstractCellDetailPanel implements TreeSelectionListener {
		
	private JTree tree;
	
	public CellsListPanel(CellViewModel model) {
		super(model);
		this.setLayout(new BorderLayout());
		
		DefaultMutableTreeNode root =
				new DefaultMutableTreeNode(new NodeData("Cells", null));
		TreeModel treeModel = new DefaultTreeModel(root);
		tree = new JTree(treeModel);
		tree.addTreeSelectionListener(this);
		
		tree.setEnabled(false);
		JScrollPane scrollPane = new JScrollPane(tree);
		Dimension size = new Dimension(120, 200);
		scrollPane.setMinimumSize(size);
		scrollPane.setPreferredSize(size);
		
		this.add(scrollPane, BorderLayout.CENTER);
	}
	
	
	/**
	 * Trigger an update with a given dataset
	 * @param dataset
	 */
	@Override
	protected void updateSingle() {
		DefaultMutableTreeNode root =
				new DefaultMutableTreeNode(new NodeData("Cells", null));
		

		createNodes(root, activeDataset());
		tree.setEnabled(true);

		TreeModel model = new DefaultTreeModel(root);
		
		tree.removeTreeSelectionListener(this);
		tree.setModel(model);
		
		// If a cell is still active in view, select it in the list
		if(this.getCellModel().hasCell()){
			DefaultMutableTreeNode node = getNode(this.getCellModel().getCell());
			
			if(node!=null){
				TreePath path = new TreePath(node.getPath());

				tree.setSelectionPath(path);
			}
		}
		
		// Replace the listener
		tree.addTreeSelectionListener(this);
	}
	
	@Override
	protected void updateMultiple(){
		updateNull();
	}

	@Override
	protected void updateNull(){
		DefaultMutableTreeNode root =
				new DefaultMutableTreeNode(new NodeData("Cells", null));


		tree.setEnabled(false);

		TreeModel model = new DefaultTreeModel(root);

		tree.removeTreeSelectionListener(this);
		tree.setModel(model);
		tree.addTreeSelectionListener(this);
	}
	
	/**
	 * Create the nodes in the tree
	 * @param root the root node
	 * @param dataset the dataset to use
	 */
	private void createNodes(DefaultMutableTreeNode root, IAnalysisDataset dataset){
	    
		List<ICell> cells = new ArrayList<ICell>(dataset.getCollection().getCells());
		Collections.sort(cells);
		
	    for(ICell cell : cells){	

	    	String name = cell.getNucleus().getNameAndNumber();
	    	UUID id = cell.getId();

	    	root.add(new DefaultMutableTreeNode( new NodeData(name, id)));
	    }

	}
	
	private DefaultMutableTreeNode getNode(ICell cell){
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
			
		for(int i = 0; i < root.getChildCount() - 1; i++) {
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) root.getChildAt(i);
			NodeData data = (NodeData) n.getUserObject();
			if(data.getID().equals(cell.getId())){
				return n;
			}
		}
		return null;
	}
	
	public class NodeData {
		private String name;
		private UUID id;
		private String imageName;
		private int nucleusNumber;
		
		
		public NodeData(String name, UUID id) {
			this.name = name;
			this.id = id;
			if(!name.equals("Cells")){
				String[] array = name.split("\\.\\w+-"); // remove file extension and dash, leaving filename and nucleus number
				this.imageName = array[0];
				this.nucleusNumber = Integer.valueOf(array[1]);
			}
			
		}
		public String getName() {
			return name;
		}
		public UUID getID() {
			return id;
		}
		public String toString() {
//			return name;
			if(name.equals("Cells")){
				return name;
			}
			NumberFormat df = DecimalFormat.getInstance();
			df.setMaximumFractionDigits(0);
			df.setMinimumIntegerDigits(2);
			return imageName+"-"+df.format(nucleusNumber);
		}
	}
	
	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
		finest("Cell list selection changed");
		
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) arg0.getPath().getLastPathComponent();
		NodeData data = (NodeData) node.getUserObject();

		UUID cellID = data.getID();

		if(this.isSingleDataset()){	
			try{
				
				if(cellID!=null){ // only null for root
					this.getCellModel().setCell( activeDataset().getCollection().getCell(cellID) );
					finer("Updating selected cell to "+this.getCellModel().getCell().getNucleus().getNameAndNumber());
					
				} else {
					this.getCellModel().setCell(null);
					finer("Null cell selected");

				}				
				
			} catch (Exception e1){
				warn("Error fetching cell");
				log(Level.FINE, "Error fetching cell", e1);
			}
		} else {
			finer("Not a single dataset");
		}
		
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return null;
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
		return null;
	}


	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

}
