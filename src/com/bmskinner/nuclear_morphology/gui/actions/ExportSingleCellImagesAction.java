package com.bmskinner.nuclear_morphology.gui.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nuclear_morphology.io.CellImageExportMethod;

/**
 * Export each nucleus to a separate image
 * @author ben
 *
 */
public class ExportSingleCellImagesAction extends MultiDatasetResultAction {

	private static final String PROGRESS_LBL = "Exporting single cells";

	public ExportSingleCellImagesAction(@NonNull List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
		super(datasets, PROGRESS_LBL, acceptor, eh);
	}

	@Override
	public void run() {
		setProgressBarIndeterminate();
		
		IAnalysisMethod m = new CellImageSetupDialog(datasets).getMethod();
		
		int nNuclei = datasets.stream().mapToInt(d->d.getCollection().getNucleusCount()).sum();
		
		worker = new DefaultAnalysisWorker(m, nNuclei);
		worker.addPropertyChangeListener(this);
		ThreadManager.getInstance().submit(worker);
	}
	
	@SuppressWarnings("serial")
    private class CellImageSetupDialog extends SubAnalysisSetupDialog {

        private static final String DIALOG_TITLE = "Cell image export options";
        
        HashOptions o = new DefaultOptions();

        public CellImageSetupDialog(final @NonNull List<IAnalysisDataset> datasets) {
            this(datasets, DIALOG_TITLE);
        }
        
        /**
         * Constructor that does not make panel visible
         * @param dataset the dataset
         * @param title
         */
        protected CellImageSetupDialog(final @NonNull List<IAnalysisDataset> datasets, final String title) {
            super(datasets, title);
            setDefaults();
            createUI();
            packAndDisplay();
        }


        @Override
        public IAnalysisMethod getMethod() {
        	return new CellImageExportMethod(datasets, o);
        }

        @Override
        protected void createUI() {

        	getContentPane().add(createHeader(), BorderLayout.NORTH);
        	getContentPane().add(createFooter(), BorderLayout.SOUTH);

            JPanel optionsPanel = new JPanel();
            GridBagLayout layout = new GridBagLayout();
            optionsPanel.setLayout(layout);
            
            List<JLabel> labels = new ArrayList<>();
            List<Component> fields = new ArrayList<>();
            
            JCheckBox maskBox = new JCheckBox();
            maskBox.setSelected(o.getBoolean(CellImageExportMethod.MASK_BACKGROUND));
            maskBox.addActionListener(e -> o.setBoolean(CellImageExportMethod.MASK_BACKGROUND, maskBox.isSelected()));
            
            labels.add(new JLabel("Mask background"));
            fields.add(maskBox);

            this.addLabelTextRows(labels, fields, layout, optionsPanel);
            

            getContentPane().add(optionsPanel, BorderLayout.CENTER);
        }

		@Override
		protected void setDefaults() {
			o.setBoolean(CellImageExportMethod.MASK_BACKGROUND, CellImageExportMethod.DEAULT_MASK_BACKGROUND);
		}

		@Override
		public HashOptions getOptions() {
			return o;
		}
    }
	

}
