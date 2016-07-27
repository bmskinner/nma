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

import charting.datasets.SignalTableCell;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import components.CellularComponent;
import components.nuclear.NuclearSignal;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class ComponentListPanel extends AbstractCellDetailPanel implements ListSelectionListener {
	
	private JList<Object> list;
	private JScrollPane   scrollPane;
	private String prevComponent = "";
	
	public ComponentListPanel(CellViewModel model) {
		super(model);
		
		this.setLayout(new BorderLayout());
		
		scrollPane = new JScrollPane();

		list = new JList<Object>();
		ListModel<Object> objectModel = createListModel();

		list.setModel(objectModel);
		list.addListSelectionListener(this);
		list.setEnabled(false);
									
		scrollPane.setViewportView(list);
		Dimension size = new Dimension(120, 200);
		scrollPane.setMinimumSize(size);
		scrollPane.setPreferredSize(size);
		
		this.add(scrollPane, BorderLayout.CENTER);
	}
	
	private ListModel<Object> createListModel(){
		DefaultListModel<Object> model = new DefaultListModel<Object>();
		
		if(this.getCellModel().hasCell()){
			
			Nucleus n = getCellModel().getCell().getNucleus();
			// Every cell has a nucleus
			SignalTableCell nucleusCell = new SignalTableCell(n.getID(), "Nucleus");
			model.addElement(nucleusCell);
			
			// Add signals present
			for(UUID i : n.getSignalCollection().getSignalGroupIDs()){
				
				if(n.getSignalCollection().hasSignal(i)){

					SignalTableCell signalCell = new SignalTableCell(i, activeDataset().getCollection().getSignalGroup(i).getGroupName());
					model.addElement(signalCell);
				}
			}
		}
		return model;
	}
	
		
	public void update(){

		finest("Updating component list for cell");
		list.removeListSelectionListener(this);
		ListModel<Object> model = createListModel();
		list.setModel(model);

		if(this.getCellModel().hasCell()){
			finest("Cell is not null");
			

			// Check if the new cell has the same component as the last
			int selectedIndex = 0;
			for(int i=0; i<model.getSize();i++){
				SignalTableCell tableCell   =  (SignalTableCell) list.getModel().getElementAt(i);
				if(tableCell.toString().equals(prevComponent)){
					selectedIndex=i;
				}
			}
			list.setSelectedIndex(selectedIndex);
			prevComponent = ((SignalTableCell) list.getModel().getElementAt(selectedIndex)).toString(); // set the new component string
//			this.getCellModel().setComponent(getSelectedComponent());
			list.setEnabled(true);
		}
		list.addListSelectionListener(this);
		
	}
	
	private CellularComponent getSelectedComponent(){
		int row = list.getSelectedIndex();
		CellularComponent c = null;
		if(row>=0){ // -1 if nothing selected
			SignalTableCell tableCell   =  (SignalTableCell) list.getModel().getElementAt(row);
			String signalGroupName = tableCell.toString();

			
			

			if(signalGroupName.equals("Nucleus")){

				c = getCellModel().getCell().getNucleus();

			} else {

				UUID signalGroup = tableCell.getID();
				
				for(NuclearSignal n : getCellModel().getCell().getNucleus().getSignalCollection().getSignals(signalGroup)){
					c = n;
				}

			}
			finest("Component selected is "+signalGroupName);
		}
		return c;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {

		finest("Component selection changed");
		CellularComponent c = getSelectedComponent();
		prevComponent = ((SignalTableCell) list.getSelectedValue()).toString(); // set the new component string
		
		this.getCellModel().setComponent(c);
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
