package no.gui;

import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableColumn;

import no.analysis.AnalysisDataset;
import no.collections.CellCollection;
import no.components.NucleusBorderSegment;
import no.components.SegmentedProfile;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;

import org.jfree.data.xy.DefaultXYDataset;

import datasets.MorphologyChartFactory;
import datasets.NucleusDatasetCreator;
import datasets.NucleusTableDatasetCreator;

public class SegmentsDetailPanel extends DetailPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private ChartPanel segmentsBoxplotChartPanel; // for displaying the legnth of a given segment
	private ChartPanel segmentsProfileChartPanel; // for displaying the profiles of a given segment
	
	// 
//	private JCheckBox    normSegmentCheckBox = new JCheckBox("Normalised");	// to toggle raw or normalised segment profiles in segmentsProfileChartPanel
//	private JRadioButton rawSegmentLeftButton  = new JRadioButton("Left"); // left align raw segment profiles in segmentsProfileChartPanel
//	private JRadioButton rawSegmentRightButton = new JRadioButton("Right"); // right align raw segment profiles in segmentsProfileChartPanel
	
	private List<AnalysisDataset> list;
	
	private JPanel segmentsBoxplotPanel;// container for boxplots chart and decoration
	private JPanel segmentsProfilePanel;// container for profiles
//	private JComboBox<String> segmentSelectionBox; // choose which segments to compare
	
	private SegmentStatsPanel segmentStatsPanel;
	
	
	public SegmentsDetailPanel() {
			
		this.setLayout(new BorderLayout());
		
		JPanel panel = new JPanel(new GridBagLayout());
		
		Dimension minimumChartSize = new Dimension(100, 100);
		segmentsProfilePanel  = createSegmentProfilePanel();
		segmentsProfilePanel.setMinimumSize(minimumChartSize);
		
		segmentsBoxplotPanel = createSegmentBoxplotsPanel();
		segmentsBoxplotPanel.setMinimumSize(minimumChartSize);
		
		segmentStatsPanel = new SegmentStatsPanel();
		segmentStatsPanel.setMinimumSize(minimumChartSize);
		
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.weightx = 1;
		constraints.weighty = 0.5;
		constraints.anchor = GridBagConstraints.CENTER;
		
		panel.add(segmentStatsPanel, constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.weighty = 1;
		panel.add(segmentsProfilePanel, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridheight = 2;
		constraints.gridwidth = 1;
		panel.add(segmentsBoxplotPanel, constraints);
		
		this.add(panel, BorderLayout.CENTER);
		
		
	}
	
	private JPanel createSegmentProfilePanel(){
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		
		JFreeChart profileChart = MorphologyChartFactory.makeEmptyProfileChart();
		segmentsProfileChartPanel= MorphologyChartFactory.makeProfileChartPanel(profileChart);
		
		segmentsProfileChartPanel.setMinimumSize(minimumChartSize);
		segmentsProfileChartPanel.setPreferredSize(preferredChartSize);
		segmentsProfileChartPanel.setMinimumDrawWidth( 0 );
		segmentsProfileChartPanel.setMinimumDrawHeight( 0 );
		panel.add(segmentsProfileChartPanel, BorderLayout.CENTER);
		
		
		// checkbox to select raw or normalised profiles
//		normSegmentCheckBox.setSelected(true);
//		normSegmentCheckBox.setEnabled(false);
//		normSegmentCheckBox.setActionCommand("NormalisedSegmentProfile");
//		normSegmentCheckBox.addActionListener(this);
//		
//		// make buttons to select raw profiles
//		rawSegmentLeftButton.setSelected(true);
//		rawSegmentLeftButton.setActionCommand("LeftAlignSegmentProfile");
//		rawSegmentRightButton.setActionCommand("RightAlignSegmentProfile");
//		rawSegmentLeftButton.addActionListener(this);
//		rawSegmentRightButton.addActionListener(this);
//		rawSegmentLeftButton.setEnabled(false);
//		rawSegmentRightButton.setEnabled(false);
//		
//
//		//Group the radio buttons.
//		final ButtonGroup alignGroup = new ButtonGroup();
//		alignGroup.add(rawSegmentLeftButton);
//		alignGroup.add(rawSegmentRightButton);
//		
//		JPanel alignPanel = new JPanel();
//		alignPanel.setLayout(new BoxLayout(alignPanel, BoxLayout.X_AXIS));
//
//		alignPanel.add(normSegmentCheckBox);
//		alignPanel.add(rawSegmentLeftButton);
//		alignPanel.add(rawSegmentRightButton);
//		panel.add(alignPanel, BorderLayout.NORTH);
		return panel;
	}
	
	private JPanel createSegmentBoxplotsPanel(){
		JPanel panel = new JPanel(); // main container in tab

		panel.setLayout(new BorderLayout());
		
		JFreeChart boxplot = MorphologyChartFactory.makeEmptyBoxplot();

		
		segmentsBoxplotChartPanel = new ChartPanel(boxplot);
		panel.add(segmentsBoxplotChartPanel, BorderLayout.CENTER);
		
//		segmentSelectionBox = new JComboBox<String>();
//		segmentSelectionBox.setActionCommand("SegmentBoxplotChoice");
//		segmentSelectionBox.addActionListener(this);
//		panel.add(segmentSelectionBox, BorderLayout.NORTH);

		return panel;
	}
	
	public void update(List<AnalysisDataset> list){
		this.list = list;
		
		if(list!=null && !list.isEmpty()){
			updateSegmentsBoxplot(list); // get segname from panel
			updateSegmentsProfile(list, null, false, false); // get segname from panel
			segmentStatsPanel.update(list.get(0));
		}
	}
	
	private void updateSegmentsBoxplot(List<AnalysisDataset> list){
		try{
			BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createSegmentVariabillityDataset(list);
			JFreeChart boxplotChart = MorphologyChartFactory.makeSegmentBoxplot(ds, list);
			segmentsBoxplotChartPanel.setChart(boxplotChart);
		} catch (Exception e){
			IJ.log("Error updating segments boxplot: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
			JFreeChart boxplotChart = MorphologyChartFactory.makeEmptyBoxplot();
			segmentsBoxplotChartPanel.setChart(boxplotChart);
			
		}
	}
	
	private void updateSegmentsProfile(List<AnalysisDataset> list, String segName, boolean normalised, boolean rightAlign){
		
		DefaultXYDataset ds = null;
		try {
			if(normalised){
				ds = NucleusDatasetCreator.createMultiProfileSegmentDataset(list, segName);
			} else {
				ds = NucleusDatasetCreator.createRawMultiProfileSegmentDataset(list, segName, rightAlign);
			}

			JFreeChart chart = null;
			if(normalised){
				chart = MorphologyChartFactory.makeProfileChart(ds, 100);
			} else {
				int length = 100;
				for(AnalysisDataset d : list){
					if(   (int) d.getCollection().getMedianArrayLength()>length){
						length = (int) d.getCollection().getMedianArrayLength();
					}
				}
				chart = MorphologyChartFactory.makeProfileChart(ds, length);
			}								
			segmentsProfileChartPanel.setChart(chart);


		} catch (Exception e) {
			error("Error in plotting segment profile", e);
		} 
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
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
	
	protected class SegmentStatsPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private JTable table; // individual cell stats
		
		private JScrollPane scrollPane;
		private AnalysisDataset activeDataset;
		
		protected SegmentStatsPanel(){
			
			this.setLayout(new BorderLayout());
			
			scrollPane = new JScrollPane();
						
			try {
				table = new JTable(NucleusTableDatasetCreator.createMedianProfileSegmentStatsTable(null));
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
							
							CellCollection collection = activeDataset.getCollection();
							if(columnName.startsWith("Seg_")){
								
								try {
									SegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(collection.getReferencePoint());
									NucleusBorderSegment seg = profile.getSegment(columnName);
									
//									if(rowName.equals("Start index")){
//										SpinnerNumberModel sModel 
//										= new SpinnerNumberModel(seg.getStartIndex(), 
//												0, 
//												profile.size(),
//												1);
//										JSpinner spinner = new JSpinner(sModel);
//
//										int option = JOptionPane.showOptionDialog(null, 
//												spinner, 
//												"Choose the new segment start index", 
//												JOptionPane.OK_CANCEL_OPTION, 
//												JOptionPane.QUESTION_MESSAGE, null, null, null);
//										if (option == JOptionPane.CANCEL_OPTION) {
//											// user hit cancel
//										} else if (option == JOptionPane.OK_OPTION)	{
//											
//											int index = (Integer) spinner.getModel().getValue();
//											if(profile.update(seg, index, seg.getEndIndex())){
////											if(seg.update(index, seg.getEndIndex())){
//												collection.getProfileCollection().addSegments(collection.getReferencePoint(), profile.getSegments());
//												update(activeDataset);
//											} else {
//												log("Updating "+seg.getStartIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
//											}
//											
//
//										}
//									}
									
//									if(rowName.equals("End index")){
////										SpinnerNumberModel sModel 
////										= new SpinnerNumberModel(seg.getEndIndex(), 
////												seg.getStartIndex()+NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH, 
////												seg.nextSegment().getEndIndex()-NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH,
////												1);
//										SpinnerNumberModel sModel 
//										= new SpinnerNumberModel(seg.getEndIndex(), 
//												0, 
//												profile.size(),
//												1);
//										JSpinner spinner = new JSpinner(sModel);
//
//										int option = JOptionPane.showOptionDialog(null, 
//												spinner, 
//												"Choose the new segment end index", 
//												JOptionPane.OK_CANCEL_OPTION, 
//												JOptionPane.QUESTION_MESSAGE, null, null, null);
//										if (option == JOptionPane.CANCEL_OPTION) {
//											// user hit cancel
//										} else if (option == JOptionPane.OK_OPTION)	{
//											
//											
//											
//											int index = (Integer) spinner.getModel().getValue();
//											if(profile.update(seg, seg.getStartIndex(), index)){
//												collection.getProfileCollection().addSegments(collection.getReferencePoint(), profile.getSegments());
//												update(activeDataset);
//											} else {
//												log("Updating "+seg.getEndIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
//											}
//											
//
//										}
//									}
									
									
									
								} catch (Exception e1) {
									error("Error getting segment", e1);
								}
							}
						}
					}
				});
			} catch (Exception e) {
				error("Error in segment table", e);
			}
			table.setEnabled(false);
						
			scrollPane.setViewportView(table);
			scrollPane.setColumnHeaderView(table.getTableHeader());
			
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		protected void update(AnalysisDataset dataset){
			
			if(dataset==null){
				try {
					table.setModel(NucleusTableDatasetCreator.createMedianProfileSegmentStatsTable(null));
				} catch (Exception e) {
					error("Error updating segment stats panel", e);
				}
			} else {
				activeDataset = dataset;
				try {
					table.setModel(NucleusTableDatasetCreator.createMedianProfileSegmentStatsTable(dataset));
				} catch (Exception e) {
					error("Error updating segment stats panel", e);
				}

				Enumeration<TableColumn> columns = table.getColumnModel().getColumns();

				while(columns.hasMoreElements()){
					TableColumn column = columns.nextElement();
					column.setCellRenderer(new SegmentTableCellRenderer());
				}
			}
		}
	}
}
