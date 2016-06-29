package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.datasets.CellDatasetCreator;
import charting.datasets.SignalTableCell;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import components.AbstractCellularComponent;
import components.Cell;
import components.generic.BorderTag;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import gui.DatasetEvent.DatasetMethod;
import gui.components.ColourSelecter;
import gui.components.ExportableTable;
import gui.components.panels.MeasurementUnitSettingsPanel;
import gui.dialogs.CellImageDialog;

@SuppressWarnings("serial")
public class CellStatsPanel extends AbstractCellDetailPanel {
	
	private ExportableTable table; // individual cell stats
	
	private JScrollPane scrollPane;
	
	public CellStatsPanel() {
		
		this.setLayout(new BorderLayout());
		
		scrollPane = new JScrollPane();
		
		TableOptions options = new TableOptionsBuilder()
				.setCell(null)
				.build();
		
		TableModel model;
		try {
			model = getTable(options);
		} catch (Exception e1) {
			warn("Error creating cell stats table model");
			log(Level.FINE, "Error creating cell stats table model", e1);
			model = null;
		}
					
		
		table = new ExportableTable(model);
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
					if(rowName.equals("Scale (pixels/um)")){
						
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
				
				SegmentedProfile profile = n.getProfile(ProfileType.ANGLE, tag);
				NucleusBorderSegment seg = profile.getSegment("Seg_0");
				// this updates the correct direction, but the wrong end of the segment
				seg.lengthenStart(-difference);
				
				n.setProfile(ProfileType.ANGLE, tag, profile);
				
			} catch(Exception e1){
				log(Level.SEVERE, "Error updating cell profile", e1);
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
			
			
			update(activeCell);
			
		}
	}
	
	private void showCellImage(){
		new CellImageDialog( activeCell);
	}
	
	private void changeSignalGroupColour(int row){
		// the group number is in the next row down
		String groupString = table.getModel().getValueAt(row+1, 1).toString();
		UUID signalGroup = UUID.fromString(groupString);
		
		Color oldColour = activeDataset().getCollection().getSignalGroup(signalGroup).getGroupColour();
		
		Color newColor = JColorChooser.showDialog(
				CellStatsPanel.this,
                 "Choose signal Color",
                 oldColour);
		
		if(newColor != null){
            activeDataset().getCollection().getSignalGroup(signalGroup).setGroupColour(newColor);//.setSignalGroupColour(signalGroup, newColor);

			update(activeCell);
			fireSignalChangeEvent("SignalColourUpdate");
		}
	}
	
	private void updateScale(){
		SpinnerNumberModel sModel 
		= new SpinnerNumberModel(activeCell.getNucleus().getScale(), 1, 100000, 1);
		JSpinner spinner = new JSpinner(sModel);


		int option = JOptionPane.showOptionDialog(null, 
				spinner, 
				"Choose the new scale: pixels per micron", 
				JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (option == JOptionPane.CANCEL_OPTION) {
			// user hit cancel
		} else if (option == JOptionPane.OK_OPTION)	{
			
			//TODO: merged datasets - apply to merge source cells only or all cells

			Object[] options = { "Apply to all cells" , "Apply to only this cell", };
			int applyAllOption = JOptionPane.showOptionDialog(null, "Apply this scale to all cells in the dataset?", "Apply to all?",

					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

					null, options, options[1]);

			double scale = (Double) spinner.getModel().getValue();

			if(scale>0){ // don't allow a scale to cause divide by zero errors
				if(applyAllOption==0){ // button at index 1

					for(Nucleus n : activeDataset().getCollection().getNuclei()){
						n.setScale(scale);
					}
					activeDataset().getCollection().getConsensusNucleus().setScale(scale);
					

				} else {
					activeCell.getNucleus().setScale(scale);

				}
				fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());
//				update(activeCell);
				
			} else {
				warn("Cannot set a scale to zero");
			}
		}
	}
	
	public void update(Cell cell){
		super.update(cell);
		
		TableOptions options = new TableOptionsBuilder()
				.setDatasets(getDatasets())
				.setCell(activeCell)
				.setScale(MeasurementUnitSettingsPanel.getInstance().getSelected())
				.build();
		
		try{
			
			TableModel model = getTable(options);
			table.setModel(model);
			
			if(cell!=null){
				table.getColumnModel().getColumn(1).setCellRenderer(  new StatsTableCellRenderer() );
			}
		} catch(Exception e){
			warn("Error updating cell stats table");
			log(Level.FINE, "Error updating cell stats table", e);
		}
	}
	
	@Override
	protected void updateSingle() {
		activeCell = null;
		update(activeCell);
	}



	@Override
	protected void updateMultiple() {
		updateNull();
	}



	@Override
	protected void updateNull() {
		activeCell = null;
		update(activeCell);
		
	}



	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception {
		return CellDatasetCreator.getInstance().createCellInfoTable(options);
	}



	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
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
			if(row < table.getModel().getRowCount()-1){
				
				int nextRow = row+1;
				String nextRowHeader = table.getModel().getValueAt(nextRow, 0).toString();

				if(nextRowHeader.equals("Signal group")){
					// we want to colour this cell preemptively
					// get the signal group from the table
					
					SignalTableCell tableCell = (SignalTableCell) table.getModel().getValueAt(nextRow, 1);
					
                    colour = tableCell.getColor();

				}
			}
			//Cells are by default rendered as a JLabel.
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			l.setBackground(colour);

			//Return the JLabel which renders the cell.
			return l;
		}
	}

}

