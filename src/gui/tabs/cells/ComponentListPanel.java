package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.UUID;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.options.ChartOptions;
import charting.options.TableOptions;
import components.CellularComponent;
import components.nuclear.NuclearSignal;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class ComponentListPanel extends AbstractCellDetailPanel implements ListSelectionListener {
	
	private JList<ComponentListCell> list;
	private JScrollPane   scrollPane;
	private String prevComponent = "";
	
	public ComponentListPanel(CellViewModel model) {
		super(model);
		
		this.setLayout(new BorderLayout());
		
		scrollPane = new JScrollPane();

		list = new JList<ComponentListCell>();
		ListModel<ComponentListCell> objectModel = createListModel();

		list.setModel(objectModel);
		list.addListSelectionListener(this);
		list.setEnabled(false);
									
		scrollPane.setViewportView(list);
		Dimension size = new Dimension(120, 200);
		scrollPane.setMinimumSize(size);
		scrollPane.setPreferredSize(size);
		
		this.add(scrollPane, BorderLayout.CENTER);
	}
	
	/**
	 * Create a list with all the components in the active cell
	 * @return
	 */
	private ListModel<ComponentListCell> createListModel(){
		DefaultListModel<ComponentListCell> model = new DefaultListModel<ComponentListCell>();
		
		if(this.getCellModel().hasCell()){
			
			Nucleus n = getCellModel().getCell().getNucleus();
			// Every cell has a nucleus
			ComponentListCell nucleusCell = new ComponentListCell("Nucleus", n);
			
			model.addElement(nucleusCell);
			
			// Add signals present
			for(UUID i : n.getSignalCollection().getSignalGroupIDs()){
				
				if(n.getSignalCollection().hasSignal(i)){

					NuclearSignal signal = n.getSignalCollection().getSignals(i).get(0);
					ComponentListCell signalCell = new ComponentListCell( activeDataset().getCollection().getSignalGroup(i).getGroupName(), signal);
					model.addElement(signalCell);
				}
			}
		}
		return model;
	}
	
		
	public void update(){

		finest("Updating component list for cell");
		list.removeListSelectionListener(this);
		ListModel<ComponentListCell> model = createListModel();
		list.setModel(model);

		if(this.getCellModel().hasCell()){
			finest("Cell is not null");
			

			// Check if the new cell has the same component as the last
			int selectedIndex = 0;
			for(int i=0; i<model.getSize();i++){
				ComponentListCell tableCell   =  (ComponentListCell) list.getModel().getElementAt(i);
				if(tableCell.toString().equals(prevComponent)){
					selectedIndex=i;
				}
			}
			list.setSelectedIndex(selectedIndex);
			prevComponent = ((ComponentListCell) list.getModel().getElementAt(selectedIndex)).toString(); // set the new component string
			this.getCellModel().setComponent(getSelectedComponent());
			list.setEnabled(true);
		} else {
			list.setEnabled(false);
		}
		list.addListSelectionListener(this);
		
	}
	
	private ComponentListCell getSelectedRow(){
		ComponentListCell tableCell   = null;
		int row = list.getSelectedIndex();
		finer("Selected component row "+row);
		if(row>=0){ // -1 if nothing selected
			tableCell   =  (ComponentListCell) list.getModel().getElementAt(row);
		}
		return tableCell;
	}
	
	private CellularComponent getSelectedComponent(){
		int row = list.getSelectedIndex();
		CellularComponent c = null;
		if(row>=0){ // -1 if nothing selected
			ComponentListCell tableCell   = getSelectedRow();
			c = tableCell.getComponent();
		}
		return c;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {

		finest("Component selection changed");
		ComponentListCell cell = getSelectedRow();
		if(cell != null){
			prevComponent = cell.toString(); // set the new component string
			CellularComponent c = cell.getComponent();
			this.getCellModel().setComponent(c);
		}
				
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception {
		return null;
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}

}
