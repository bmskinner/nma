package com.bmskinner.nuclear_morphology.gui.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.SettingsDialog;
import com.bmskinner.nuclear_morphology.gui.main.MainWindow;

/**
 * Allow a random subset of cells to be extracted as a child of the given dataset.
 * @author ben
 * @since 1.13.8
 *
 */
public class ExtractRandomCellsAction extends SingleDatasetResultAction {

	private static final String PROGRESS_LBL = "Extract cells";

	public ExtractRandomCellsAction(IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(dataset, PROGRESS_LBL, acceptor, eh);
		this.setProgressBarIndeterminate();
	}

	@Override
	public void run() {
		ExtractNucleiSetupDialog dialog = new ExtractNucleiSetupDialog();

		if (dialog.isReadyToRun()) {
			List<ICell> cells = new ArrayList<>(dataset.getCollection().getCells());
			
			Collections.shuffle(cells);
			
			List<ICell> subList = cells.subList(0, dialog.getCellCount());

			ICellCollection c = new VirtualCellCollection(dataset, "Random_selection");
			for(ICell cell : subList){
				c.addCell(cell);
			}

			 if (c.hasCells()) {

	                try {
	                    dataset.getCollection().getProfileManager().copyCollectionOffsets(c);
	                } catch (ProfileException e) {
	                    warn("Error copying collection offsets");
	                    stack("Error in offsetting", e);
	                }

	                dataset.addChildCollection(c);

	                // attach the clusters to their parent collection
	                IAnalysisDataset d = dataset.getChildDataset(c.getID());
	                d.setRoot(false);

	                // set shared counts
	                c.setSharedCount(dataset.getCollection(), c.size());
	                dataset.getCollection().setSharedCount(c, c.size());
	                
	                getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
	            }

		} else {
			fine("User cancelled operation");
		}
		cancel();
	}



	private class ExtractNucleiSetupDialog extends SettingsDialog implements ActionListener {
		
		private JSpinner spinner;
		public ExtractNucleiSetupDialog() {
	        super(true);

	        this.setTitle("Extract cells options");
	        setSize(450, 300);
	        this.setLocationRelativeTo(null);
	        createGUI();
	        // this.pack();
	        this.setVisible(true);
	    }
		
		public int getCellCount(){
			return (int) spinner.getModel().getValue();
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		
		private void createGUI() {

	        setLayout(new BorderLayout());

	        JPanel panel = new JPanel();
	        GridBagLayout layout = new GridBagLayout();
	        panel.setLayout(layout);

	        List<JLabel> labels = new ArrayList<JLabel>();
	        List<Component> fields = new ArrayList<Component>();

	        spinner = new JSpinner( new SpinnerNumberModel(1, 1, dataset.getCollection().getNucleusCount(), 1));
	        labels.add(new JLabel("Number of cells"));
	        fields.add(spinner);


	        this.addLabelTextRows(labels, fields, layout, panel);

	        JPanel header = new JPanel(new FlowLayout());
	        header.add(new JLabel("Extract random cells from the dataset"));

	        this.add(header, BorderLayout.NORTH);
	        this.add(panel, BorderLayout.CENTER);

	        this.add(createFooter(), BorderLayout.SOUTH);

	    }
	}

}
