/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.tabs;

import gui.DatasetEvent.DatasetMethod;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.components.ColourSelecter;
import gui.tabs.CellDetailPanel.CellsListPanel.NodeData;
import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;

import utility.Utils;
import analysis.AnalysisDataset;
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.MorphologyChartFactory;
import charting.datasets.CellDatasetCreator;
import charting.datasets.NucleusDatasetCreator;
import charting.datasets.NucleusTableDatasetCreator;
import components.Cell;
import components.generic.BorderTag;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

public class CellDetailPanel extends DetailPanel implements SignalChangeListener, TreeSelectionListener {

	private static final long serialVersionUID = 1L;
	
	protected AnalysisDataset activeDataset;	
	private Cell activeCell;
	private Logger programLogger;
	
	protected CellsListPanel	cellsListPanel;		// the list of cells in the active dataset
	protected ProfilePanel	 	profilePanel; 		// the nucleus angle profile
	protected OutlinePanel 	 	outlinePanel; 		// the outline of the cell and detected objects
	protected CellStatsPanel 	cellStatsPanel;		// the stats table
	protected SegmentStatsPanel segmentStatsPanel;	// details of the individual segments

	public CellDetailPanel(Logger programLogger) {

		super(programLogger);

		try{
			this.setLayout(new GridBagLayout());

			GridBagConstraints constraints = new GridBagConstraints();
			constraints.fill = GridBagConstraints.BOTH;
			constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.gridheight = 1;
			constraints.gridwidth = 1;
			constraints.weightx = 0.3;
			constraints.weighty = 1;
			constraints.anchor = GridBagConstraints.CENTER;

			cellsListPanel = new CellsListPanel();
			this.add(cellsListPanel, constraints);

			// make the chart for each nucleus
			JPanel centrePanel = createCentrePanel();

			constraints.gridx = 1;
			constraints.gridwidth = 2;
			constraints.weightx = 1;
			this.add(centrePanel, constraints);


			outlinePanel = new OutlinePanel();
			constraints.gridx = 3;
			this.add(outlinePanel, constraints);


			this.validate();
		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Error creating cell detail panel", e);
		}

	}

	/**
	 * Create the central column panel
	 * @return
	 * @throws Exception 
	 */
	private JPanel createCentrePanel() throws Exception{
		JPanel centrePanel = new JPanel();
		centrePanel.setLayout(new BoxLayout(centrePanel, BoxLayout.Y_AXIS));
		
		
		cellStatsPanel = new CellStatsPanel();
		centrePanel.add(cellStatsPanel);
		
		profilePanel = new ProfilePanel();
		centrePanel.add(profilePanel);
		
		segmentStatsPanel = new SegmentStatsPanel();
		centrePanel.add(segmentStatsPanel);
		
		Dimension minSize = new Dimension(200, 300);
		centrePanel.setMinimumSize(minSize);
		return centrePanel;
	}
	
	
	public void update(List<AnalysisDataset> list){
		updateList(list);
	}
	
	/**
	 * Update the panel with a list of AnalysisDatasets. Data
	 * will only be displayed if the list contains one dataset.
	 * @param list the datsets
	 */
	public void updateList(final List<AnalysisDataset> list){
//		programLogger.log(Level.FINE, "Updating cell detail panel");
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				CellDetailPanel.this.list = list;
				
				if(list.size()==1){

					activeDataset = list.get(0);
					
					cellsListPanel.updateDataset(activeDataset);
					programLogger.log(Level.FINEST, "Updated cell list panel");
					updateCell(activeCell);
					programLogger.log(Level.FINEST, "Updated active cell panel");
				} else {
					
					cellsListPanel.updateDataset(null);
					programLogger.log(Level.FINEST, "Updated cell list panel");
					updateCell(null);
					
				}
			
		}});
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
				
				colour = activeDataset.getSwatch().color(segment);
//				colour = ColourSelecter.getOptimisedColor(segment);
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
					new DefaultMutableTreeNode(new NodeData("Cells", null));
			TreeModel model = new DefaultTreeModel(root);
			tree = new JTree(model);
			tree.addTreeSelectionListener(CellDetailPanel.this);
			tree.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					
					JTree tree = (JTree) e.getSource();
					
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
					NodeData data = (NodeData) node.getUserObject();

					UUID cellID = data.getID();

					// double click - remove cell
					
					if (e.getClickCount() == 2) {
						
						Object[] options = { "Don't delete cell" , "Delete cell", };
						int result = JOptionPane.showOptionDialog(null, "Delete this cell?", "Confirm delete",

						        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

						        null, options, options[0]);

						if(result==1){ // button at index 1

							
							// delete the cell
							Cell cell = activeDataset.getCollection().getCell(cellID);
							activeDataset.getCollection().removeCell(cell);
							node.removeFromParent();
							DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
							model.reload();
							
							List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
							list.add(activeDataset);
							
							try {
								CellDetailPanel.this.fireDatasetEvent(DatasetMethod.RECALCULATE_CACHE, list);
								CellDetailPanel.this.fireDatasetEvent(DatasetMethod.REFRESH_MORPHOLOGY, list);

							} catch (Exception e1) {
								programLogger.log(Level.SEVERE, "Error deleting cell", e1);
							}
							
							CellDetailPanel.this.updateList(list);
							CellDetailPanel.this.fireSignalChangeEvent("UpdatePanels");
							CellDetailPanel.this.fireSignalChangeEvent("UpdatePopulationPanel");
							CellDetailPanel.this.fireDatasetEvent(DatasetMethod.SELECT_DATASETS, list);

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
					new DefaultMutableTreeNode(new NodeData("Cells", null));
			
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
//		    	String name = cell.getCellId().toString();
		    	String name = cell.getNucleus().getNameAndNumber();
		    	UUID id = cell.getId();
//		    	root.add(new DefaultMutableTreeNode(name));
		    	root.add(new DefaultMutableTreeNode( new NodeData(name, id)));
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
		
		public class NodeData {
			private String name;
			private UUID id;
			public NodeData(String name, UUID id) {
				this.name = name;
				this.id = id;
			}
			public String getName() {
				return name;
			}
			public UUID getID() {
				return id;
			}
			public String toString() {
				return name;
			}
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
					JFreeChart chart = MorphologyChartFactory.makeIndividualNucleusProfileChart(ds, nucleus, activeDataset.getSwatch());

					profileChartPanel.setChart(chart);
					
//					nucleus.getAngleProfile(BorderTag.REFERENCE_POINT).fastFourierTransform();
				}

			} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error updating cell panel", e);
			}

		}

	}
	
	protected class OutlinePanel extends JPanel{

		private static final long serialVersionUID = 1L;
		
		private ChartPanel panel;
		
		@SuppressWarnings("serial")
		protected OutlinePanel(){
			
			// make the chart for each nucleus
			this.setLayout(new BorderLayout());
			JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();

			panel = new ChartPanel(chart){
				
				
				@Override
				public void restoreAutoBounds() {
					XYPlot plot = (XYPlot) this.getChart().getPlot();
					
					double chartWidth = this.getWidth();
					double chartHeight = this.getHeight();
					double aspectRatio = chartWidth / chartHeight;
					
					// start with impossible values
					double xMin = chartWidth;
					double yMin = chartHeight;
//					
					double xMax = 0;
					double yMax = 0;
					
					// get the max and min values of the chart
					for(int i = 0; i<plot.getDatasetCount();i++){
						XYDataset dataset = plot.getDataset(i);
						
						xMax = DatasetUtilities.findMaximumDomainValue(dataset).doubleValue() > xMax
								? DatasetUtilities.findMaximumDomainValue(dataset).doubleValue()
								: xMax;
						
						xMin = DatasetUtilities.findMinimumDomainValue(dataset).doubleValue() < xMin
								? DatasetUtilities.findMinimumDomainValue(dataset).doubleValue()
								: xMin;
								
						yMax = DatasetUtilities.findMaximumRangeValue(dataset).doubleValue() > yMax
								? DatasetUtilities.findMaximumRangeValue(dataset).doubleValue()
								: yMax;
						
						yMin = DatasetUtilities.findMinimumRangeValue(dataset).doubleValue() < yMin
								? DatasetUtilities.findMinimumRangeValue(dataset).doubleValue()
								: yMin;
					}
					

					// find the ranges they cover
					double xRange = xMax - xMin;
					double yRange = yMax - yMin;
					
//					double aspectRatio = xRange / yRange;

					double newXRange = xRange;
					double newYRange = yRange;

					// test the aspect ratio
//					IJ.log("Old range: "+xMax+"-"+xMin+", "+yMax+"-"+yMin);
					if( (xRange / yRange) > aspectRatio){
						// width is not enough
//						IJ.log("Too narrow: "+xRange+", "+yRange+":  aspect ratio "+aspectRatio);
						newXRange = xRange * 1.1;
						newYRange = newXRange / aspectRatio;
					} else {
						// height is not enough
//						IJ.log("Too short: "+xRange+", "+yRange+":  aspect ratio "+aspectRatio);
						newYRange = yRange * 1.1; // add some extra x space
						newXRange = newYRange * aspectRatio; // get the new Y range
					}
					

					// with the new ranges, find the best min and max values to use
					double xDiff = (newXRange - xRange)/2;
					double yDiff = (newYRange - yRange)/2;

					xMin -= xDiff;
					xMax += xDiff;
					yMin -= yDiff;
					yMax += yDiff;
//					IJ.log("New range: "+xMax+"-"+xMin+", "+yMax+"-"+yMin);

					plot.getRangeAxis().setRange(yMin, yMax);
					plot.getDomainAxis().setRange(xMin, xMax);				
				} 
				
//				@Override
//				//override the default zoom to keep aspect ratio
//				public void zoom(java.awt.geom.Rectangle2D selection){
//					
//					Rectangle2D.Double newSelection = null;
//					if(selection.getWidth()>selection.getHeight()){
//						newSelection = new Rectangle2D.Double(selection.getX(), 
//								selection.getY(), 
//								selection.getWidth(), 
//								selection.getWidth());					
//					} else {
//						newSelection = new Rectangle2D.Double(selection.getX(), 
//								selection.getY(), 
//								selection.getHeight(), 
//								selection.getHeight());		
//					}
//					super.zoom(newSelection);
//				}
			};
			
			panel.addComponentListener(new ComponentAdapter() {
    			@Override
    			public void componentResized(ComponentEvent e) {
    				panel.restoreAutoBounds();
    			}
    		});
			

			
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
				if(cell!=null){
					panel.restoreAutoBounds();
				}
			} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error updating outline chart", e);
			}
		}

	}
	
	protected class CellStatsPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private JTable table; // individual cell stats
		
		private JScrollPane scrollPane;
		
		protected CellStatsPanel() throws Exception{
			
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
								
								Object[] options = { "Apply to all cells" , "Apply to only this cell", };
								int applyAllOption = JOptionPane.showOptionDialog(null, "Apply this scale to all cells in the dataset?", "Apply to all?",

								        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

								        null, options, options[1]);

								
								
//								int applyAllOption = JOptionPane.showConfirmDialog(null,
//
//								        "Apply this scale to all cells in the dataset?", "Apply to all?", JOptionPane.YES_NO_OPTION);
								
								
								double scale = (Double) spinner.getModel().getValue();
								if(applyAllOption==0){ // button at index 1
//								if(applyAllOption==JOptionPane.YES_OPTION){
									
									for(Nucleus n : activeDataset.getCollection().getNuclei()){
										n.setScale(scale);
									}
									
								} else {
									activeCell.getNucleus().setScale(scale);

								}
								updateCell(activeCell);
							}
						}
						
						// Adjust the point position of tags
						Nucleus n = activeCell.getNucleus();
						BorderTag tag = activeDataset.getCollection().getNucleusType().getTagFromName(rowName);
						if(n.hasBorderTag(tag)){
							
							String pointType = rowName;
							
							
							int index = Utils.wrapIndex(n.getBorderIndex(tag)- n.getBorderIndex(BorderTag.REFERENCE_POINT), n.getLength());
							
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
								
								int existingIndex = n.getBorderIndex(tag);
								
								// adjust to the actual point index
								int pointIndex = Utils.wrapIndex(chosenIndex + n.getBorderIndex(BorderTag.REFERENCE_POINT), n.getLength());
								
								// find the amount the index is changing by
								int difference = pointIndex - existingIndex;
								
								// TODO: update segment boundaries 
								try {
									
									SegmentedProfile profile = n.getAngleProfile(tag);
									NucleusBorderSegment seg = profile.getSegment("Seg_0");
									// this updates the correct direction, but the wrong end of the segment
									seg.lengthenStart(-difference);
									
									n.setAngleProfile(profile, tag);
									
								} catch(Exception e1){
									programLogger.log(Level.SEVERE, "Error updating cell profile", e1);
								}
								
								// Update the border tag index
								n.setBorderTag(tag, pointIndex);
								
								if(tag.equals(BorderTag.ORIENTATION_POINT)){
									if(n.hasBorderTag(BorderTag.INTERSECTION_POINT)){
										// only rodent sperm use the intersection point, which is equivalent to the head.
										NucleusBorderPoint newPoint = n.findOppositeBorder(n.getPoint(BorderTag.ORIENTATION_POINT));
										n.setBorderTag(BorderTag.INTERSECTION_POINT, n.getIndex(newPoint));
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
			
			try{

				if(cell==null){
					table.setModel(CellDatasetCreator.createCellInfoTable(null));
				} else {
					table.setModel(CellDatasetCreator.createCellInfoTable(cell));
					table.getColumnModel().getColumn(1).setCellRenderer(new StatsTableCellRenderer());
				}
			} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error updating cell", e);
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
									SegmentedProfile profile = n.getAngleProfile(BorderTag.REFERENCE_POINT);
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
												n.setAngleProfile(profile, BorderTag.REFERENCE_POINT);
												updateCell(activeCell);
											} else {
												programLogger.log(Level.INFO, "Updating "+seg.getStartIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
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
												n.setAngleProfile(profile, BorderTag.REFERENCE_POINT);
												updateCell(activeCell);
											} else {
												programLogger.log(Level.INFO, "Updating "+seg.getEndIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
											}
											

										}
									}
									
									
									
								} catch (Exception e1) {
									programLogger.log(Level.SEVERE, "Error getting segment", e1);
								}
							}
						}
					}
				});
			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error in segment stats", e);
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
					programLogger.log(Level.SEVERE, "Error updating segment stats", e);
				}
			} else {
				try {
					table.setModel(NucleusTableDatasetCreator.createSegmentStatsTable(cell.getNucleus()));
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error updating segment stats", e);
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

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) arg0.getPath().getLastPathComponent();
		NodeData data = (NodeData) node.getUserObject();

		UUID cellID = data.getID();

		if(list.size()==1){	
			try{
				
				activeCell = activeDataset.getCollection().getCell(cellID);
				updateCell(activeCell);
			} catch (Exception e1){
				programLogger.log(Level.SEVERE, "Error fetching cell", e1);
			}
		}
		
	}

}
