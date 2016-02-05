/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
import gui.InterfaceEvent.InterfaceMethod;
import gui.RotationMode;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.components.FixedAspectRatioChartPanel;
import gui.components.ColourSelecter;
import gui.components.DraggableOverlayChartPanel;
import gui.components.ExportableTable;
import gui.components.ShapeOverlay;
import gui.components.ShapeOverlayObject;
import gui.components.panels.ProfileTypeOptionsPanel;
import gui.components.panels.RotationSelectionSettingsPanel;
import gui.dialogs.AngleWindowSizeExplorer;
import gui.dialogs.CellImageDialog;
import gui.tabs.CellDetailPanel.CellsListPanel.NodeData;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;

import utility.Constants;
import utility.Utils;
import analysis.AnalysisDataset;
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.MorphologyChartFactory;
import charting.charts.OutlineChartFactory;
import charting.datasets.CellDatasetCreator;
import charting.datasets.NucleusDatasetCreator;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.AbstractCellularComponent;
import components.Cell;
import components.CellularComponent;
import components.generic.BorderTag;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.BorderPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class CellDetailPanel extends DetailPanel implements SignalChangeListener, TreeSelectionListener {
	
	private Cell activeCell = null;
	
	protected CellsListPanel	cellsListPanel;		// the list of cells in the active dataset
	protected ProfilePanel	 	profilePanel; 		// the nucleus angle profile
	protected OutlinePanel 	 	outlinePanel; 		// the outline of the cell and detected objects
	protected CellStatsPanel 	cellStatsPanel;		// the stats table
	protected SignalListPanel 	signalListPanel;	// choose which background image to display

	public CellDetailPanel(Logger programLogger) {

		super(programLogger);

		try{
			this.setLayout(new GridBagLayout());

			GridBagConstraints constraints = new GridBagConstraints();
			constraints.fill = GridBagConstraints.BOTH;
			constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.gridheight = 2;
			constraints.gridwidth = 1;
			constraints.weightx = 0.5;
			constraints.weighty = 0.6;
			constraints.anchor = GridBagConstraints.CENTER;

			cellsListPanel = new CellsListPanel();
			cellsListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			this.add(cellsListPanel, constraints);
			
			constraints.gridx = 0;
			constraints.gridy = 2;
			constraints.gridheight = 2;
			constraints.gridwidth = 1;
			constraints.weightx = 0.5;
			constraints.weighty = 0.4;
			signalListPanel = new SignalListPanel();
			signalListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			this.add(signalListPanel, constraints);

			// make the chart for each nucleus
			JPanel centrePanel = createCentrePanel();

			constraints.gridx = 1;
			constraints.gridy = 0;
			constraints.gridwidth = 2;
			constraints.gridheight = 4;
			constraints.weightx = 0.9;
			constraints.weighty = 1;
			this.add(centrePanel, constraints);


			outlinePanel = new OutlinePanel();
			outlinePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			constraints.gridx = 3;
			constraints.gridy = 0;
			constraints.weightx = 0.7;
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
		centrePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		
		cellStatsPanel = new CellStatsPanel();
		centrePanel.add(cellStatsPanel);
		
		profilePanel = new ProfilePanel();
		centrePanel.add(profilePanel);

		Dimension minSize = new Dimension(200, 300);
		centrePanel.setMinimumSize(minSize);
		return centrePanel;
	}
	
//	@Override
//	public void updateDetail(){
//		updateList(getDatasets());
//		setUpdating(false);
//	}
	
	protected void updateSingle() throws Exception {
		cellsListPanel.updateDataset( activeDataset()  );
		programLogger.log(Level.FINEST, "Updated cell list panel");
		updateCell(activeCell);
		programLogger.log(Level.FINEST, "Updated active cell panel");
	}
	
	protected void updateMultiple() throws Exception {
		updateNull();
	}
	
	protected void updateNull() throws Exception {
		cellsListPanel.updateDataset(null);
		programLogger.log(Level.FINEST, "Updated cell list panel");
		updateCell(null);
	}
		
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	/**
	 * Display data for the given cell
	 * @param cell
	 */
	private void updateCell(Cell cell){
		
		signalListPanel.update(cell);
		cellStatsPanel.update(cell);
		profilePanel.update(cell);
		outlinePanel.update(cell);
		
		
	}
	
	
	private void updateSegmentIndex(boolean start, int index, NucleusBorderSegment seg, Nucleus n, SegmentedProfile profile) throws Exception{
		
		int startPos = start ? seg.getStartIndex() : seg.getEndIndex();
		int newStart = start ? index : seg.getStartIndex();
		int newEnd = start ? seg.getEndIndex() : index;
		
		int rawOldIndex =  n.getOffsetBorderIndex(BorderTag.REFERENCE_POINT, startPos);

						
		if(profile.update(seg, newStart, newEnd)){
			n.setProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT, profile);
			
			/* Check the border tags - if they overlap the old index
			 * replace them. 
			 */
			int rawIndex = n.getOffsetBorderIndex(BorderTag.REFERENCE_POINT, index);
			programLogger.log(Level.FINEST, "Testing border tags");
			programLogger.log(Level.FINEST, "Updating to index "+index+" from reference point");
			programLogger.log(Level.FINEST, "Raw old border point is index "+rawOldIndex);
			programLogger.log(Level.FINEST, "Raw new border point is index "+rawIndex);
			
			if(n.hasBorderTag(rawOldIndex)){						
				BorderTag tagToUpdate = n.getBorderTag(rawOldIndex);
				programLogger.log(Level.FINE, "Updating tag "+tagToUpdate);
				n.setBorderTag(tagToUpdate, rawIndex);	
				
				// Update intersection point if needed
				if(tagToUpdate.equals(BorderTag.ORIENTATION_POINT)){
					n.setBorderTag(BorderTag.INTERSECTION_POINT, n.getBorderIndex(n.findOppositeBorder(n.getBorderTag(BorderTag.ORIENTATION_POINT))));
				}
				
			} else {
				programLogger.log(Level.FINEST, "No border tag at index "+rawOldIndex+" from reference point");
//				programLogger.log(Level.FINEST, n.dumpInfo(Nucleus.ALL_POINTS));
			}

			updateCell(activeCell);
			fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
		} else {
			programLogger.log(Level.INFO, "Updating "+seg.getStartIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
		}
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
					colour = activeDataset().getSignalGroupColour(Integer.valueOf(groupString));
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
							Cell cell = activeDataset().getCollection().getCell(cellID);
							try {
								activeDataset().getCollection().removeCell(cell);
							} catch (Exception e2) {
								programLogger.log(Level.SEVERE, "Error removing cell from collection", e);
							}
							node.removeFromParent();
							DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
							model.reload();
							
							List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
							list.add(activeDataset());
							
							try {
								CellDetailPanel.this.fireDatasetEvent(DatasetMethod.RECALCULATE_CACHE, list);
								CellDetailPanel.this.fireDatasetEvent(DatasetMethod.REFRESH_MORPHOLOGY, list);

							} catch (Exception e1) {
								programLogger.log(Level.SEVERE, "Error deleting cell", e1);
							}
							
							try {
								CellDetailPanel.this.updateSingle();
							} catch (Exception e1) {
								programLogger.log(Level.SEVERE, "Error updating cell", e1);
							}
							CellDetailPanel.this.fireSignalChangeEvent("UpdatePanels");
							CellDetailPanel.this.fireSignalChangeEvent("UpdatePopulationPanel");
							CellDetailPanel.this.fireDatasetEvent(DatasetMethod.SELECT_DATASETS, list);

						}
						
					}
				}
			});
			
			tree.setEnabled(false);
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
				tree.setEnabled(true);
			} else {
				TreeModel model = new DefaultTreeModel(root);
				tree.setModel(model);
				tree.setEnabled(false);
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
		        
		        for(int j = i + 1; j <= node.getChildCount() - 1; j++) {
		            DefaultMutableTreeNode prevNode = (DefaultMutableTreeNode) node.getChildAt(j);
		            String np = prevNode.getUserObject().toString();

		            if(nt.compareToIgnoreCase(np) > 0) {
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
//				return name;
				if(name.equals("Cells")){
					return name;
				}
				NumberFormat df = DecimalFormat.getInstance();
//				df.setRoundingMode(RoundingMode.FLOOR);
				df.setMaximumFractionDigits(0);
				df.setMinimumIntegerDigits(2);
				return imageName+"-"+df.format(nucleusNumber);
			}
		}
	}
	
	
	/**
	 * Show the profile for the nuclei in the given cell
	 *
	 */
	protected class ProfilePanel extends JPanel implements SignalChangeListener, ActionListener {
		
		private DraggableOverlayChartPanel profileChartPanel; // holds the chart with the cell
		private ProfileTypeOptionsPanel profileOptions  = new ProfileTypeOptionsPanel();
		
//		private JButton windowSizeButton = new JButton("Window sizes");
		
		protected ProfilePanel(){

			this.setLayout(new BorderLayout());
			
			JFreeChart chart = MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR);	
			
			profileChartPanel = new DraggableOverlayChartPanel(chart, null, false); 
			profileChartPanel.addSignalChangeListener(this);
			this.add(profileChartPanel, BorderLayout.CENTER);
			
			JPanel header = new JPanel(new FlowLayout());
			header.add(profileOptions);
			profileOptions.addActionListener(this);
			
//			windowSizeButton.addActionListener(this);
//			header.add(windowSizeButton);
			
			this.add(header, BorderLayout.NORTH);
			
		}
		
		protected void update(Cell cell){

			try{
				
				ProfileType type = profileOptions.getSelected();

				if(cell==null){
					JFreeChart chart = MorphologyChartFactory.makeEmptyProfileChart(type);
					profileChartPanel.setChart(chart);
					profileOptions.setEnabled(false);

				} else {

					profileOptions.setEnabled(true);
					Nucleus nucleus = cell.getNucleus();
					
					ChartOptions options = new ChartOptionsBuilder()
							.setSwatch(activeDataset().getSwatch())
							.setProfileType(type)
							.build();

					JFreeChart chart = MorphologyChartFactory.makeIndividualNucleusProfileChart(nucleus, options);

					profileChartPanel.setChart(chart, nucleus.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT), false);
					
				}

			} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error updating cell panel", e);
				JFreeChart chart = MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR);
				profileChartPanel.setChart(chart);
				profileOptions.setEnabled(false);
			}

		}

		@Override
		public void signalChangeReceived(SignalChangeEvent event) {
			if(event.type().contains("UpdateSegment")){

				try{
//					
					String[] array = event.type().split("\\|");
					int selectedSegMidpoint = Integer.valueOf(array[1]);
					String index = array[2];
					int indexValue = Integer.valueOf(index);

					Nucleus n = activeCell.getNucleus();
					SegmentedProfile profile = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT);
					
					/*
					 * The numbering of segments is adjusted for profile charts, so we can't rely on 
					 * the segment name stored in the profile.
					 * 
					 * Get the name via the midpoint index of the segment that was selected. 
					 */
					NucleusBorderSegment seg = profile.getSegmentContaining(selectedSegMidpoint);
//					NucleusBorderSegment seg = profile.getSegment(segName);

					updateSegmentIndex(true, indexValue, seg, n, profile);
				} catch(Exception e){
					programLogger.log(Level.SEVERE, "Error updating segment", e);
				}

			}
			
			
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			update(activeCell);
			
//			if(e.getSource()==windowSizeButton){
//				new AngleWindowSizeExplorer(activeDataset(), programLogger);
//			}
			
		}

	}
	
	protected class OutlinePanel extends JPanel implements ActionListener{
		
		private RotationSelectionSettingsPanel rotationPanel;

		private JCheckBox showHookHump = new JCheckBox("Show hook and hump ROIs");
		private FixedAspectRatioChartPanel panel;
		
//		boolean drawPointOverlay = false; // debugging
//		private ShapeOverlay overlay = new ShapeOverlay();
		
		protected OutlinePanel(){
			
			// make the chart for each nucleus
			this.setLayout(new BorderLayout());
			JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();

			
			JPanel settingsPanel = new JPanel(new FlowLayout());
			
			rotationPanel = new RotationSelectionSettingsPanel();
			rotationPanel.setEnabled(false);
			rotationPanel.addActionListener(this);
			
			settingsPanel.add(rotationPanel);
			settingsPanel.add(createHookHumpPanel());
			
			this.add(settingsPanel, BorderLayout.NORTH);
			
			panel = new FixedAspectRatioChartPanel(chart);
//			panel.addOverlay(overlay);
			panel.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					panel.restoreAutoBounds();
				}
			});
						
			this.add(panel, BorderLayout.CENTER);
			
		}
		
		private JPanel createHookHumpPanel(){
			JPanel panel = new JPanel();
			panel.add(showHookHump);
			showHookHump.setEnabled(false);
			showHookHump.addActionListener(this);
			return panel;
			
		}
		
//		public void drawCellBackgroundImage(File f, int channel){
//			panel.drawImageAsAnnotation(f, channel);
//		}
				
		protected void update(Cell cell){

			RotationMode rotateMode = rotationPanel.getSelected();
			boolean showHook = showHookHump.isSelected();
			
//			panel.setCell(cell);
			
//			panel.removeOverlay(overlay);
			
			try{
				JFreeChart chart;
				if(cell==null){
					rotationPanel.setEnabled(false);
					showHookHump.setEnabled(false);
					chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
				} else {
					
					CellularComponent component = signalListPanel.getActiveComponent();
					
					if(activeDataset().getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
						showHookHump.setEnabled(true);
					} else {
						showHookHump.setEnabled(false);
					}
					
					rotationPanel.setEnabled(true);
					
					chart = OutlineChartFactory.makeCellOutlineChart(cell, activeDataset(), rotateMode, showHook, component);
				}
				
				panel.setChart(chart);
//				panel.clearShapeAnnotations();
//				panel.setChart(chart);
//				if(rotateMode.equals(RotationMode.ACTUAL)){
//					panel.drawNucleusImageAsAnnotation();
//				} else {
//					panel.clearShapeAnnotations();
//				}
				
				
				
				if(cell!=null){
					panel.restoreAutoBounds();
										
//					overlay.clearShapes();
//
//					for(BorderPoint p : cell.getNucleus().getBorderList()){
//						Shape s = new Ellipse2D.Double(p.getX(), p.getY(), 1d, 1d);
//						ShapeOverlayObject ov = new ShapeOverlayObject(s);
//						ov.setVisible(drawPointOverlay);
//						overlay.addShape(ov);
//					}
//					panel.addOverlay(overlay);
				}
				
			} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error updating outline chart", e);
				JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
				panel.setChart(chart);
				rotationPanel.setEnabled(false);
				showHookHump.setEnabled(false);
			}
		}

		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			update(activeCell);
			
		}

	}
	
	protected class CellStatsPanel extends JPanel {
		
		private ExportableTable table; // individual cell stats
		
		private JScrollPane scrollPane;
		
		protected CellStatsPanel() throws Exception{
			
			this.setLayout(new BorderLayout());
			
			scrollPane = new JScrollPane();
						
			table = new ExportableTable(CellDatasetCreator.createCellInfoTable(null));
			table.setEnabled(false);
			
			table.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					
					JTable table = (JTable) e.getSource();
					int row = table.rowAtPoint((e.getPoint()));
					String rowName = table.getModel().getValueAt(row, 0).toString();
					
					// double click
					if (e.getClickCount() == 2) {
						
						if(rowName.equals("Source image")){
							showCellImage();
						}
						
						// Look for signal group colour
						if(rowName.equals("")){
							String value = table.getModel().getValueAt(row+1, 0).toString();
							if(value.equals("Signal group")){
								
								changeSignalGroupColour(row);

							}
						}

						// Adjust the scale
						if(rowName.equals("Scale (um/pixel)")){
							
							updateScale();
						}
						
						// Adjust the point position of tags
						Nucleus n = activeCell.getNucleus();
						BorderTag tag = activeDataset().getCollection().getNucleusType().getTagFromName(rowName);
						if(n.hasBorderTag(tag)){
							
							updateBorderTagIndex(n, tag);
							
						}
							
					}

				}
			});
			
			scrollPane.setViewportView(table);
			scrollPane.setColumnHeaderView(table.getTableHeader());
			
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		private void updateBorderTagIndex(Nucleus n, BorderTag tag){
//			String pointType = rowName;
			
			
			int index = AbstractCellularComponent.wrapIndex(n.getBorderIndex(tag)- n.getBorderIndex(BorderTag.REFERENCE_POINT), n.getBorderLength());
			
			SpinnerNumberModel sModel 
				= new SpinnerNumberModel(index, 0, n.getBorderLength(), 1);
			JSpinner spinner = new JSpinner(sModel);
			
			int option = JOptionPane.showOptionDialog(null, 
					spinner, 
					"Choose the new "+tag.toString(), 
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.QUESTION_MESSAGE, null, null, null);
			if (option == JOptionPane.CANCEL_OPTION) {
			    // user hit cancel
			} else if (option == JOptionPane.OK_OPTION)	{
				
				// the value chosen by the user
				int chosenIndex = (Integer) spinner.getModel().getValue();
				
				int existingIndex = n.getBorderIndex(tag);
				
				// adjust to the actual point index
				int pointIndex = AbstractCellularComponent.wrapIndex(chosenIndex + n.getBorderIndex(BorderTag.REFERENCE_POINT), n.getBorderLength());
				
				// find the amount the index is changing by
				int difference = pointIndex - existingIndex;
				
				// TODO: update segment boundaries 
				try {
					
					SegmentedProfile profile = n.getProfile(ProfileType.REGULAR, tag);
					NucleusBorderSegment seg = profile.getSegment("Seg_0");
					// this updates the correct direction, but the wrong end of the segment
					seg.lengthenStart(-difference);
					
					n.setProfile(ProfileType.REGULAR, tag, profile);
					
				} catch(Exception e1){
					programLogger.log(Level.SEVERE, "Error updating cell profile", e1);
				}
				
				// Update the border tag index
				n.setBorderTag(tag, pointIndex);
				
				if(tag.equals(BorderTag.ORIENTATION_POINT)){
					if(n.hasBorderTag(BorderTag.INTERSECTION_POINT)){
						// only rodent sperm use the intersection point, which is equivalent to the head.
						BorderPoint newPoint = n.findOppositeBorder(n.getBorderPoint(BorderTag.ORIENTATION_POINT));
						n.setBorderTag(BorderTag.INTERSECTION_POINT, n.getBorderIndex(newPoint));
					}
				}
				
				
				updateCell(activeCell);
				
			}
		}
		
		private void showCellImage(){
			new CellImageDialog(programLogger, activeCell);
		}
		
		private void changeSignalGroupColour(int row){
			// the group number is in the next row down
			String groupString = table.getModel().getValueAt(row+1, 1).toString();
			int signalGroup = Integer.valueOf(groupString);
			
			Color oldColour = ColourSelecter.getSignalColour( signalGroup-1 );
			
			Color newColor = JColorChooser.showDialog(
                     CellDetailPanel.this,
                     "Choose signal Color",
                     oldColour);
			
			if(newColor != null){
				activeDataset().setSignalGroupColour(signalGroup, newColor);
				updateCell(activeCell);
				fireSignalChangeEvent("SignalColourUpdate");
			}
		}
		
		private void updateScale(){
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

				double scale = (Double) spinner.getModel().getValue();

				if(scale>0){ // don't allow a scale to cause divide by zero errors
					if(applyAllOption==0){ // button at index 1
						//								if(applyAllOption==JOptionPane.YES_OPTION){

						for(Nucleus n : activeDataset().getCollection().getNuclei()){
							n.setScale(scale);
						}

					} else {
						activeCell.getNucleus().setScale(scale);

					}
					updateCell(activeCell);
				} else {
					programLogger.log(Level.WARNING, "Cannot set a scale to zero");
				}
			}
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
	
	protected class SignalListPanel extends JPanel implements ListSelectionListener {
		
		private static final long serialVersionUID = 1L;
		
		private CellularComponent activeComponent; 
		
		private JList<String> signalList;
		private JScrollPane scrollPane;
		
		protected SignalListPanel(){
			
			this.setLayout(new BorderLayout());
			
			scrollPane = new JScrollPane();
						
			try {
				
				
				signalList = new JList<String>();
				DefaultListModel<String> model = new DefaultListModel<String>();

				signalList.setModel(model);
				signalList.addListSelectionListener(this);
				signalList.setEnabled(false);
				
			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error in segment stats", e);
			}
						
			scrollPane.setViewportView(signalList);
			
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		public CellularComponent getActiveComponent(){
			return this.activeComponent;
		}
						
		protected void update(Cell cell){

			if(cell!=null){
				
				activeComponent = activeCell.getNucleus();
				
				try {
					DefaultListModel<String> model = new DefaultListModel<String>();
					model.addElement("Nucleus");
					for(int i : activeCell.getNucleus().getSignalGroups()){
						if(activeCell.getNucleus().hasSignal(i)){
							model.addElement(activeCell.getNucleus().getSignalCollection().getSignalGroupName(i));
						}
					}
					signalList.setModel(model);
					signalList.setSelectedIndex(0);
					signalList.setEnabled(true);

				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error updating signal list", e);
				}
			} else {
				try {
					DefaultListModel<String> model = new DefaultListModel<String>();

					signalList.setModel(model);
					signalList.setEnabled(false);
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error updating signal list", e);
				}
			}
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {

//			if selected value is a valid file reference, load the file and update the
			// nucleus annotation
			int row = signalList.getSelectedIndex();
			
			if(row>=0){ // -1 if nothing selected
				String signalGroupName = signalList.getModel().getElementAt(row);

				if(signalGroupName.equals("Nucleus")){
					
					activeComponent = activeCell.getNucleus();
					
//					File file =  activeCell.getNucleus().getSourceFile();
//					if(file.exists()){
//
//						outlinePanel.drawCellBackgroundImage(file, Constants.COUNTERSTAIN);
//
//					}

				} else {

					int signalGroup = activeCell.getNucleus().getSignalCollection().getSignalGroup(signalGroupName);
//					File file       = activeCell.getNucleus().getSignalCollection().getSourceFile(signalGroup);
//					int channel     = activeCell.getNucleus().getSignalCollection().getSignalChannel(signalGroup);
//					int stack       = Constants.rgbToStack(channel);

					for(NuclearSignal n : activeCell.getNucleus().getSignalCollection().getSignals(signalGroup)){
						activeComponent = n;
					}
					
//					if(file.exists()){
//
//						// find the channel of the signal
//						outlinePanel.drawCellBackgroundImage(file, stack);
//
//
//					}
				}
				outlinePanel.update(activeCell);
			}
			
		}
	}


	@Override
	public void valueChanged(TreeSelectionEvent arg0) {

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) arg0.getPath().getLastPathComponent();
		NodeData data = (NodeData) node.getUserObject();

		UUID cellID = data.getID();

		if(getDatasets().size()==1){	
			try{
				
				activeCell = activeDataset().getCollection().getCell(cellID);
				updateCell(activeCell);
			} catch (Exception e1){
				programLogger.log(Level.SEVERE, "Error fetching cell", e1);
			}
		}
		
	}

}
