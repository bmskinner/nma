package no.gui;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import no.analysis.AnalysisDataset;
import no.analysis.MorphologyAnalysis;
import no.components.NucleusBorderPoint;
import no.components.NucleusBorderSegment;
import no.components.SegmentedProfile;
import no.nuclei.Nucleus;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import utility.Utils;
import cell.Cell;
import datasets.CellDatasetCreator;
import datasets.ConsensusNucleusChartFactory;
import datasets.MorphologyChartFactory;
import datasets.NucleusDatasetCreator;
import datasets.NucleusTableDatasetCreator;
import datasets.TailDatasetCreator;

public class CellDetailPanel extends DetailPanel implements SignalChangeListener, TreeSelectionListener {

	private static final long serialVersionUID = 1L;
	
	private 	List<AnalysisDataset> list;
	protected AnalysisDataset activeDataset;	
	private Cell activeCell;
	
	protected CellsListPanel	cellsListPanel;		// the list of cells in the active dataset
	protected ProfilePanel	 	profilePanel; 		// the nucleus angle profile
	protected OutlinePanel 	 	outlinePanel; 		// the outline of the cell and detected objects
	protected CellStatsPanel 	cellStatsPanel;		// the stats table
	protected SegmentStatsPanel segmentStatsPanel;	// details of the individual segments
		
	public CellDetailPanel() {

		this.setLayout(new BorderLayout());
		
		cellsListPanel = new CellsListPanel();
		this.add(cellsListPanel, BorderLayout.WEST);
		
		// make the chart for each nucleus
		outlinePanel = new OutlinePanel();
		this.add(outlinePanel, BorderLayout.EAST);
			
		JPanel centrePanel = createCentrePanel();
		this.add(centrePanel, BorderLayout.CENTER);
		centrePanel.setMinimumSize(new Dimension(200,200));
		this.revalidate();
		
	}
	
	/**
	 * Create the central column panel
	 * @return
	 */
	private JPanel createCentrePanel(){
		JPanel centrePanel = new JPanel();
		centrePanel.setLayout(new BoxLayout(centrePanel, BoxLayout.Y_AXIS));
		
		
		cellStatsPanel = new CellStatsPanel();
		centrePanel.add(cellStatsPanel);
		
		profilePanel = new ProfilePanel();
		centrePanel.add(profilePanel);
		
		segmentStatsPanel = new SegmentStatsPanel();
		centrePanel.add(segmentStatsPanel);
		return centrePanel;
	}
	
	/**
	 * Update the panel with a list of AnalysisDatasets. Data
	 * will only be displayed if the list contains one dataset.
	 * @param list the datsets
	 */
	public void updateList(List<AnalysisDataset> list){
		this.list = list;
		
		if(list.size()==1){
			
			activeDataset = list.get(0);
			cellsListPanel.updateDataset(activeDataset);
		} else {
			
			cellsListPanel.updateDataset(null);
			updateCell(null);
		}
	}
	
	
	/**
	 * Display data for the given cell
	 * @param cell
	 */
	private void updateCell(Cell cell){
		
		cellStatsPanel.update(cell);
		outlinePanel.update(cell);
		profilePanel.update(cell);
		segmentStatsPanel.update(cell);
	}
	
	
	/**
	 * Allows for cell background to be coloured based on position in a list. Used to colour
	 * the signal stats list
	 *
	 */
	private class StatsTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			// default cell colour is white
			Color colour = Color.WHITE;

			// get the value in the first column of the row below
			if(row<table.getModel().getRowCount()-1){
				String nextRowHeader = table.getModel().getValueAt(row+1, 0).toString();

				if(nextRowHeader.equals("Signal group")){
					// we want to colour this cell preemptively
					// get the signal group from the table
					String groupString = table.getModel().getValueAt(row+1, 1).toString();
					colour = activeDataset.getSignalGroupColour(Integer.valueOf(groupString));
//					colour = ColourSelecter.getSignalColour(  Integer.valueOf(groupString)-1   ); 
				}
			}
			//Cells are by default rendered as a JLabel.
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			l.setBackground(colour);

			//Return the JLabel which renders the cell.
			return l;
		}
	}
	
	/**
	 * Allows for cell background to be coloured based on position in a list. Used to colour
	 * the segment stats list
	 *
	 */
	private class SegmentTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			// default cell colour is white
			Color colour = Color.WHITE;
			
			// only apply to first row, after the first column
			if(column>0 && row==0){
				String colName = table.getColumnName(column); // will be Seg_x

				int segment = Integer.valueOf(colName.replace("Seg_", ""));

				colour = ColourSelecter.getSegmentColor(segment);
			}
			
			//Cells are by default rendered as a JLabel.
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			l.setBackground(colour);

			//Return the JLabel which renders the cell.
			return l;
		}
	}


	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		if(event.type().equals("SignalColourUpdate")){
			updateCell(activeCell);
		}

	}
	
	/**
	 * Hold the list of cells within the current active dataset
	 *
	 */
	protected class CellsListPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private JTree tree;
		
		protected CellsListPanel(){
			this.setLayout(new BorderLayout());
			
			DefaultMutableTreeNode root =
					new DefaultMutableTreeNode("Cells");
			TreeModel model = new DefaultTreeModel(root);
			tree = new JTree(model);
			tree.addTreeSelectionListener(CellDetailPanel.this);
			tree.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					
					JTree tree = (JTree) e.getSource();
					
					String name = tree.getSelectionPath().getLastPathComponent().toString();
					
					String[] bits = name.split("-");
					
					String pathName = activeDataset.getCollection().getFolder()
							+File.separator
							+bits[0]
							+File.separator
							+bits[1];

					
					// double click - remove cell
					
					if (e.getClickCount() == 2) {
						
						int result = JOptionPane.showConfirmDialog(null,

						        "Delete cell?", "Do you want to delete the cell?", JOptionPane.YES_NO_OPTION);
						
						if(result==JOptionPane.YES_OPTION){
							
							// delete the cell
							Cell cell = activeDataset.getCollection().getCell(pathName);
							activeDataset.getCollection().removeCell(cell);
							try {
								CellDetailPanel.this.fireSignalChangeEvent("MorphologyRefresh_"+activeDataset.getUUID().toString());
//								CellDetailPanel.this.log("Refreshing dataset");
//								MorphologyAnalysis.refresh(activeDataset.getCollection());

							} catch (Exception e1) {
								log("Error deleting cell: "+e1.getMessage());
							}
							
							List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
							list.add(activeDataset);
							CellDetailPanel.this.updateList(list);
							CellDetailPanel.this.fireSignalChangeEvent("UpdatePanels");
							CellDetailPanel.this.fireSignalChangeEvent("UpdatePopulationPanel");
							CellDetailPanel.this.fireSignalChangeEvent("SelectDataset_"+activeDataset.getUUID().toString());
							
						}
						
					}
				}
			});
			
			JScrollPane scrollPane = new JScrollPane(tree);
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		
		/**
		 * Trigger an update with a given dataset
		 * @param dataset
		 */
		protected void updateDataset(AnalysisDataset dataset){
			DefaultMutableTreeNode root =
					new DefaultMutableTreeNode("Cells");
			
			if(dataset!=null){
				createNodes(root, dataset);
			} 
			TreeModel model = new DefaultTreeModel(root);
			tree.setModel(model);
		}
		
		/**
		 * Create the nodes in the tree
		 * @param root the root node
		 * @param dataset the dataset to use
		 */
		private void createNodes(DefaultMutableTreeNode root, AnalysisDataset dataset){
		    
		    for(Cell cell : dataset.getCollection().getCells()){	
		    	String name = cell.getNucleus().getNameAndNumber();
		    	root.add(new DefaultMutableTreeNode(name));
		    }
		    sort(root);

		}
		
		private DefaultMutableTreeNode sort(DefaultMutableTreeNode node){
			for(int i = 0; i < node.getChildCount() - 1; i++) {
		        DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
		        String nt = child.getUserObject().toString();
		        String ntToCompare = nt.replace(".tiff", "");
		        
		        for(int j = i + 1; j <= node.getChildCount() - 1; j++) {
		            DefaultMutableTreeNode prevNode = (DefaultMutableTreeNode) node.getChildAt(j);
		            String np = prevNode.getUserObject().toString();
		            String npToCompare = np.replace(".tiff", "");

		            if(ntToCompare.compareToIgnoreCase(npToCompare) > 0) {
		                node.insert(child, j);
		                node.insert(prevNode, i);
		            }
		        }
		        if(child.getChildCount() > 0) {
		            sort(child);
		        }
			}
			return node;
			
		}
	}
	
	
	/**
	 * Show the profile for the nuclei in the given cell
	 *
	 */
	protected class ProfilePanel extends JPanel{
		
		private static final long serialVersionUID = 1L;
		private ChartPanel profileChartPanel; // holds the chart with the cell
		
		protected ProfilePanel(){
			this.setLayout(new BorderLayout());
			
			JFreeChart chart = MorphologyChartFactory.makeEmptyProfileChart();		
			profileChartPanel = MorphologyChartFactory.makeProfileChartPanel(chart); 
			this.add(profileChartPanel, BorderLayout.CENTER);
			
		}
		
		protected void update(Cell cell){

			try{

				if(cell==null){
					JFreeChart chart = MorphologyChartFactory.makeEmptyProfileChart();
					profileChartPanel.setChart(chart);

				} else {

					Nucleus nucleus = cell.getNucleus();

					XYDataset ds 	= NucleusDatasetCreator.createSegmentedProfileDataset(nucleus);
					JFreeChart chart = MorphologyChartFactory.makeIndividualNucleusProfileChart(ds, nucleus);

					profileChartPanel.setChart(chart);
				}

			} catch(Exception e){
				log("Error updating cell panel");
			}

		}

	}
	
	protected class OutlinePanel extends JPanel{

		private static final long serialVersionUID = 1L;
		
		private ChartPanel panel;
		
		protected OutlinePanel(){
			
			// make the chart for each nucleus
			this.setLayout(new BorderLayout());
			JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();

			panel = new ChartPanel(chart);
			
			this.add(panel, BorderLayout.CENTER);
			
		}
		
		protected void update(Cell cell){
			
			try{
			
			JFreeChart chart;
			if(cell==null){
				chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			} else {
				chart = MorphologyChartFactory.makeCellOutlineChart(cell, activeDataset);
			}
			panel.setChart(chart);
			}catch(Exception e){
				error("Error updating outline chart", e);
			}
		}

	}
	
	protected class CellStatsPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private JTable table; // individual cell stats
		
		private JScrollPane scrollPane;
		
		protected CellStatsPanel(){
			
			this.setLayout(new BorderLayout());
			
			scrollPane = new JScrollPane();
						
			table = new JTable(CellDatasetCreator.createCellInfoTable(null));
			table.setEnabled(false);
			
			table.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					
					JTable table = (JTable) e.getSource();
					int row = table.rowAtPoint((e.getPoint()));
					String rowName = table.getModel().getValueAt(row, 0).toString();
					
					// double click
					if (e.getClickCount() == 2) {
						
						// Look for signal group colour
						if(rowName.equals("")){
							String value = table.getModel().getValueAt(row+1, 0).toString();
							if(value.equals("Signal group")){
								
								// the group number is in the next row down
								String groupString = table.getModel().getValueAt(row+1, 1).toString();
								int signalGroup = Integer.valueOf(groupString);
								
								Color oldColour = ColourSelecter.getSignalColour( signalGroup-1 );
								
								Color newColor = JColorChooser.showDialog(
					                     CellDetailPanel.this,
					                     "Choose signal Color",
					                     oldColour);
								
								if(newColor != null){
									activeDataset.setSignalGroupColour(signalGroup, newColor);
									updateCell(activeCell);
									fireSignalChangeEvent("SignalColourUpdate");
								}
							}
						}

						// Adjust the scale
						if(rowName.equals("Scale (um/pixel)")){
							
							SpinnerNumberModel sModel 
								= new SpinnerNumberModel(activeCell.getNucleus().getScale(), 0, 100, 0.001);
							JSpinner spinner = new JSpinner(sModel);
							
							int option = JOptionPane.showOptionDialog(null, 
									spinner, 
									"Choose the new scale", 
									JOptionPane.OK_CANCEL_OPTION, 
									JOptionPane.QUESTION_MESSAGE, null, null, null);
							if (option == JOptionPane.CANCEL_OPTION) {
							    // user hit cancel
							} else if (option == JOptionPane.OK_OPTION)	{
								
								// offset by 90 because reasons?
								double scale = (Double) spinner.getModel().getValue();
								activeCell.getNucleus().setScale(scale);
								updateCell(activeCell);
								
							}
						}
						
						// Adjust the point position of tags
						Nucleus n = activeCell.getNucleus();
						if(n.hasBorderTag(rowName)){
							
							String pointType = rowName;
							
							int index = Utils.wrapIndex(n.getBorderIndex(pointType)- n.getBorderIndex(n.getReferencePoint()), n.getLength());
							
							SpinnerNumberModel sModel 
								= new SpinnerNumberModel(index, 0, n.getLength(), 1);
							JSpinner spinner = new JSpinner(sModel);
							
							int option = JOptionPane.showOptionDialog(null, 
									spinner, 
									"Choose the new "+pointType+" point", 
									JOptionPane.OK_CANCEL_OPTION, 
									JOptionPane.QUESTION_MESSAGE, null, null, null);
							if (option == JOptionPane.CANCEL_OPTION) {
							    // user hit cancel
							} else if (option == JOptionPane.OK_OPTION)	{
								
								// the value chosen by the user
								int chosenIndex = (Integer) spinner.getModel().getValue();
								
								// adjust to the actual point index
								int pointIndex = Utils.wrapIndex(chosenIndex + n.getBorderIndex(n.getReferencePoint()), n.getLength());
								
								n.addBorderTag(pointType, pointIndex);
								
								if(pointType.equals(n.getOrientationPoint())){
									if(n.hasBorderTag("intersectionPoint")){
										// only rodent sperm use the intersection point, which is equivalent to the head.
										NucleusBorderPoint newPoint = n.findOppositeBorder(n.getBorderTag(n.getOrientationPoint()));
										n.addBorderTag("intersectionPoint", n.getIndex(newPoint));
										n.addBorderTag("head", n.getIndex(newPoint));
									}
								}
								
								
								updateCell(activeCell);
								
							}
						}
							
					}

				}
			});
			
			scrollPane.setViewportView(table);
			scrollPane.setColumnHeaderView(table.getTableHeader());
			
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		protected void update(Cell cell){
			
			if(cell==null){
				table.setModel(CellDatasetCreator.createCellInfoTable(null));
			} else {
				table.setModel(CellDatasetCreator.createCellInfoTable(cell));
				table.getColumnModel().getColumn(1).setCellRenderer(new StatsTableCellRenderer());
			}
		}
	}
	
	protected class SegmentStatsPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private JTable table; // individual cell stats
		
		private JScrollPane scrollPane;
		
		protected SegmentStatsPanel(){
			
			this.setLayout(new BorderLayout());
			
			scrollPane = new JScrollPane();
						
			try {
				table = new JTable(NucleusTableDatasetCreator.createSegmentStatsTable(null));
				table.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						
						JTable table = (JTable) e.getSource();
						int row = table.rowAtPoint(e.getPoint());
						String rowName = table.getModel().getValueAt(row, 0).toString();
						
						int column = table.columnAtPoint(e.getPoint());
						String columnName = table.getModel().getColumnName(column);
						
						// double click
						if (e.getClickCount() == 2) {
							
							Nucleus n = activeCell.getNucleus();
							
							if(columnName.startsWith("Seg_")){
								
								try {
									SegmentedProfile profile = n.getAngleProfile(n.getReferencePoint());
									NucleusBorderSegment seg = profile.getSegment(columnName);
									
									if(rowName.equals("Start index")){
//										SpinnerNumberModel sModel 
//										= new SpinnerNumberModel(seg.getStartIndex(), 
//												seg.prevSegment().getStartIndex()+NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH, 
//												seg.getEndIndex()-NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH,
//												1);
										SpinnerNumberModel sModel 
										= new SpinnerNumberModel(seg.getStartIndex(), 
												0, 
												n.getLength(),
												1);
										JSpinner spinner = new JSpinner(sModel);

										int option = JOptionPane.showOptionDialog(null, 
												spinner, 
												"Choose the new segment start index", 
												JOptionPane.OK_CANCEL_OPTION, 
												JOptionPane.QUESTION_MESSAGE, null, null, null);
										if (option == JOptionPane.CANCEL_OPTION) {
											// user hit cancel
										} else if (option == JOptionPane.OK_OPTION)	{
											
											int index = (Integer) spinner.getModel().getValue();
											if(profile.update(seg, index, seg.getEndIndex())){
//											if(seg.update(index, seg.getEndIndex())){
												n.setAngleProfile(profile, n.getReferencePoint());
												updateCell(activeCell);
											} else {
												log("Updating "+seg.getStartIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
											}
											

										}
									}
									
									if(rowName.equals("End index")){
//										SpinnerNumberModel sModel 
//										= new SpinnerNumberModel(seg.getEndIndex(), 
//												seg.getStartIndex()+NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH, 
//												seg.nextSegment().getEndIndex()-NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH,
//												1);
										SpinnerNumberModel sModel 
										= new SpinnerNumberModel(seg.getEndIndex(), 
												0, 
												n.getLength(),
												1);
										JSpinner spinner = new JSpinner(sModel);

										int option = JOptionPane.showOptionDialog(null, 
												spinner, 
												"Choose the new segment end index", 
												JOptionPane.OK_CANCEL_OPTION, 
												JOptionPane.QUESTION_MESSAGE, null, null, null);
										if (option == JOptionPane.CANCEL_OPTION) {
											// user hit cancel
										} else if (option == JOptionPane.OK_OPTION)	{
											
											
											
											int index = (Integer) spinner.getModel().getValue();
											if(profile.update(seg, seg.getStartIndex(), index)){
//											if(seg.update(seg.getStartIndex(), index)){
												n.setAngleProfile(profile, n.getReferencePoint());
												updateCell(activeCell);
											} else {
												log("Updating "+seg.getEndIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
											}
											

										}
									}
									
									
									
								} catch (Exception e1) {
									error("Error getting segment", e1);
								}
							}
						}
					}
				});
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			table.setEnabled(false);
						
			scrollPane.setViewportView(table);
			scrollPane.setColumnHeaderView(table.getTableHeader());
			
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		protected void update(Cell cell){
			
			if(cell==null){
				try {
					table.setModel(NucleusTableDatasetCreator.createSegmentStatsTable(null));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					table.setModel(NucleusTableDatasetCreator.createSegmentStatsTable(cell.getNucleus()));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Enumeration<TableColumn> columns = table.getColumnModel().getColumns();

				while(columns.hasMoreElements()){
					TableColumn column = columns.nextElement();
					column.setCellRenderer(new SegmentTableCellRenderer());
				}
			}
		}
	}


	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
		// TODO Auto-generated method stub
		String name = arg0.getPath().getLastPathComponent().toString();
		
		String[] bits = name.split("-");
		
		String pathName = activeDataset.getCollection().getFolder()
				+File.separator
				+bits[0]
				+File.separator
				+bits[1];
		
		if(list.size()==1){	
			try{
				
				activeCell = activeDataset.getCollection().getCell(pathName);
				updateCell(activeCell);
			} catch (Exception e1){
				
				IJ.log("Error fetching cell: "+e1.getMessage());
				for(StackTraceElement e2 : e1.getStackTrace()){
					IJ.log(e2.toString());
				}
			}
		}
		
	}

}
