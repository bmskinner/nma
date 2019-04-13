package com.bmskinner.nuclear_morphology.gui.actions;

import java.awt.BorderLayout;
import java.util.concurrent.ExecutionException;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.classification.ProfileTsneMethod.TsneResult;
import com.bmskinner.nuclear_morphology.charting.charts.ScatterChartFactory;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.ImageThumbnailGenerator;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusterTreeDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.HierarchicalTreeSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.TsneSetupDialog;
import com.bmskinner.nuclear_morphology.gui.events.ChartOptionsRenderedEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.jujutsu.utils.MatrixOps;

/**
 * Run tSNE on datasets
 * @author bms41
 * @since 1.16.0
 *
 */
public class RunTsneAction  extends SingleDatasetResultAction
implements EventListener {

	private static final String PROGRESS_BAR_LABEL = "Running t-SNE";

	public RunTsneAction(IAnalysisDataset dataset,@NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor, eh);
	}

	@Override
	public void run() {

		SubAnalysisSetupDialog tsneSetup = new TsneSetupDialog(dataset);

		if (tsneSetup.isReadyToRun()) { // if dialog was cancelled, skip
			IAnalysisMethod m = tsneSetup.getMethod();
			worker = new DefaultAnalysisWorker(m);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);

		} else {
			this.cancel();
		}
		tsneSetup.dispose();
	}
	
    @Override
    public void finished() {

        try {
        	IAnalysisResult r = worker.get();
//        	TsneResult r = (TsneResult) worker.get();
//        	System.out.println(MatrixOps.doubleArrayToPrintString(r.tSneOutput, ", ", 5,5));
        	// make a basic plot and display
        	try {
				JFreeChart chart = ScatterChartFactory.createTsneChart(r.getFirstDataset());
				ChartPanel panel = new ChartPanel(chart);
				panel.addChartMouseListener(new ImageThumbnailGenerator(panel));
				
				JDialog d = new JDialog();
				d.setLayout(new BorderLayout());
				d.add(panel, BorderLayout.CENTER);
				d.setTitle("tSNE for "+r.getFirstDataset().getName());
				d.pack();
				d.setVisible(true);				
				
			} catch (ChartDatasetCreationException e) {
				error("Unable to make tSNE chart", e);
			}

            cleanup(); // do not cancel, we need the MainWindow listener to
                       // remain attached

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void eventReceived(DatasetEvent event) {
        finest("RunTsneAction heard dataset event");
//        if (event.method().equals(DatasetEvent.COPY_PROFILE_SEGMENTATION)) {
//            getDatasetEventHandler().fireDatasetEvent(DatasetEvent.COPY_PROFILE_SEGMENTATION, event.getDatasets(), event.secondaryDataset());
//        }

    }

    @Override
    public void eventReceived(InterfaceEvent event) {
        getInterfaceEventHandler().fireInterfaceEvent(event.method());

    }

	@Override
	public void eventReceived(DatasetUpdateEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void eventReceived(SignalChangeEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void eventReceived(ChartOptionsRenderedEvent event) {
		// TODO Auto-generated method stub
	}
}


