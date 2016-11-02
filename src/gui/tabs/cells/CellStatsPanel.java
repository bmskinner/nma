package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableModel;

import charting.datasets.AbstractDatasetCreator;
import charting.datasets.AnalysisDatasetTableCreator;
import charting.datasets.CellTableDatasetCreator;
import charting.datasets.SignalTableCell;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import components.nuclei.Nucleus;
import gui.DatasetEvent;
import gui.GlobalOptions;
import gui.components.ExportableTable;
import gui.dialogs.CellImageDialog;

@SuppressWarnings("serial")
public class CellStatsPanel extends AbstractCellDetailPanel {
	
	private ExportableTable table; // individual cell stats
	
	private JScrollPane scrollPane;
	
	private JButton scaleButton;
	private JButton sourceButton;
	
	
	public CellStatsPanel(CellViewModel model) {
		super(model);
		this.setLayout(new BorderLayout());
		
		scrollPane = new JScrollPane();

		TableModel tableModel = AnalysisDatasetTableCreator.createBlankTable();		
		
		table = new ExportableTable(tableModel);
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
							
							changeSignalGroupColour(row);

						}
					}						
				}

			}
		});
		
		scrollPane.setViewportView(table);
		scrollPane.setColumnHeaderView(table.getTableHeader());
		
		this.add(scrollPane, BorderLayout.CENTER);
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
		
		this.setEnabled(false);
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		scaleButton.setEnabled(b);
		sourceButton.setEnabled(b);
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
		
		scaleButton = new JButton("Change scale");
		scaleButton.addActionListener( e -> { updateScale();}  );
		
		sourceButton = new JButton("Show source image");
		sourceButton.addActionListener( e -> { showCellImage();}  );
		
		panel.add(scaleButton);
		panel.add(sourceButton);
		
		return panel;
	}
		
	private void showCellImage(){
		new CellImageDialog( this.getCellModel().getCell());
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

			update();
			fireSignalChangeEvent("SignalColourUpdate");
		}
	}
	
	private void updateScale(){
		SpinnerNumberModel sModel 
		= new SpinnerNumberModel(this.getCellModel().getCell().getNucleus().getScale(), 1, 100000, 1);
		JSpinner spinner = new JSpinner(sModel);


		int option = JOptionPane.showOptionDialog(null, 
				spinner, 
				"Choose the new scale: pixels per micron", 
				JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (option == JOptionPane.OK_OPTION) {
			
			//TODO: merged datasets - apply to merge source cells only or all cells

			Object[] options = { "Apply to all cells" , "Apply to only this cell", };
			int applyAllOption = JOptionPane.showOptionDialog(null, "Apply this scale to all cells in the dataset?", "Apply to all?",

					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

					null, options, options[1]);

			double scale = (Double) spinner.getModel().getValue();

			if(scale>0){ // don't allow a scale to cause divide by zero errors
				if(applyAllOption==0){ // button at index 1

					finest("Updating scale for all cells");
					for(Nucleus n : activeDataset().getCollection().getNuclei()){
						n.setScale(scale);
					}
					if(activeDataset().getCollection().hasConsensusNucleus()){
						activeDataset().getCollection().getConsensusNucleus().setScale(scale);
					}
					

				} else {
					finest("Updating scale for single cell");
					this.getCellModel().getCell().getNucleus().setScale(scale);

				}
				finest("Refreshing cache");
				this.refreshTableCache();
				fireDatasetEvent(DatasetEvent.REFRESH_CACHE, getDatasets());

				
			} else {
				warn("Cannot set a scale to zero");
			}
		}
	}
	
	@Override
	public synchronized void refreshTableCache(){
		finest("Preparing to refresh table cache");
		clearTableCache();
		finest("Updating tables after clear");
		this.update();
	}
	
	public synchronized void update(){
		
		if(this.isMultipleDatasets() || ! this.hasDatasets()){
			table.setModel(AbstractDatasetCreator.createBlankTable());
			return;
		}
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setCell(this.getCellModel().getCell())
			.setScale(GlobalOptions.getInstance().getScale())
			.setTarget(table)
			.setRenderer(TableOptions.FIRST_COLUMN, new StatsTableCellRenderer())
			.build();

		try{
			
			setTable(options);

		} catch(Exception e){
			warn("Error updating cell stats table");
			fine("Error updating cell stats table", e);
		}
	}
	
	@Override
	public void setChartsAndTablesLoading(){
		
		table.setModel(AbstractDatasetCreator.createLoadingTable());
	}
	
	@Override
	protected void updateSingle() {
		update();
	}



	@Override
	protected void updateMultiple() {
		updateNull();
	}



	@Override
	protected void updateNull() {
		table.setModel(AbstractDatasetCreator.createBlankTable());
		
	}



	@Override
	protected TableModel createPanelTableType(TableOptions options){
		
		if(getCellModel().hasCell()){
			return new CellTableDatasetCreator(getCellModel().getCell()).createCellInfoTable(options);
		} else {
			return CellTableDatasetCreator.createBlankTable();
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
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setBackground(colour);

			//Return the JLabel which renders the cell.
			return this;
		}
	}

}

