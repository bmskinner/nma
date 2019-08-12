package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;

import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex.MSSIMScore;
import com.bmskinner.nuclear_morphology.charting.charts.ViolinChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel.ImageCache.WarpedImageKey;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * Calculate and display structural similarities between images
 * @author ben
 * @since 1.15.0
 *
 */
public class StructuralSimilarityComparisonDialog extends LoadingIconDialog {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private static final String DIALOG_TITLE = "MS-SSIM* scores";
	
	private final SignalWarpingModel model;
	private final ExportableTable comparisonTable;
	
	private final ChartPanel chartPanel;
	
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	
	/** The MS-SSIM calculator */
	private MultiScaleStructuralSimilarityIndex msi = new MultiScaleStructuralSimilarityIndex();
	
	public StructuralSimilarityComparisonDialog(@NonNull final SignalWarpingModel model) {
		super();
		LOGGER.finer("Creating MS-SSIM dialog");
		this.model = model;
		chartPanel = new ExportableChartPanel(ViolinChartFactory.createLoadingChart());
		comparisonTable = new ExportableTable(AbstractTableCreator.createLoadingTable());
		
		JPanel headerPanel = createHeaderPanel();
		JPanel centrePanel = createCentrePanel();
		
		setLayout(new BorderLayout());
//		add(headerPanel, BorderLayout.NORTH);
		add(centrePanel, BorderLayout.CENTER);
		
		setLocationRelativeTo(null);
		centerOnScreen();
		setTitle(DIALOG_TITLE);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setModal(false);
		
		try {
			ThreadManager.getInstance().execute( () ->{
				TableModel compModel = createTableModel();
				comparisonTable.setModel(compModel);
				comparisonTable.setRowSorter(new TableRowSorter(compModel));
			});
			
//			makePerCellCharts();
			
		} catch(Exception e) {
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
			comparisonTable.setModel(AbstractTableCreator.createBlankTable());
		}
		validate();
		pack();
		LOGGER.finer("Showing MS-SSIM dialog");
		setVisible(true);
	}
	
	private JPanel createHeaderPanel() {
		JPanel headerPanel = new JPanel(new FlowLayout());
		headerPanel.add(getLoadingLabel());
		headerPanel.add(progressBar);
		return headerPanel;
	}
	
	private JPanel createCentrePanel() {
		
		JPanel centrePanel = new JPanel(new BorderLayout());
        
		JScrollPane scrollPane = new JScrollPane(comparisonTable);
		scrollPane.setColumnHeaderView(comparisonTable.getTableHeader());
		centrePanel.add(scrollPane, BorderLayout.CENTER);
        return centrePanel;
	}

	/**
	 * Create the MS-SSIM* table. Generate all pairwise
	 * combinations of MS-SSIM* scores excluding self-self
	 * comparisons and reciprocal comparisons.
	 * @return the MS-SSIM* table model
	 */
	private TableModel createTableModel() {
		DefaultTableModel compModel = new DefaultTableModel();
		Object[] columns = { "Source 1", "Signal 1", 
				"Source 2", "Signal 2", 
				"Target", "Luminance", "Contrast", "Structure", "MS-SSIM*"};
		compModel.setColumnIdentifiers(columns);
		
		for(CellularComponent c : model.getTargets())
			createTargetData(compModel, c);

		return compModel;
	}
	
	/**
	 * Create MS-SSIM* comparisons for all images warped onto the
	 * given target shape
	 * @param compModel the table model to add results to 
	 * @param c the target shape
	 */
	private void createTargetData(DefaultTableModel compModel, CellularComponent c) {
		LOGGER.finer("Creating comparisons for target shape "+c.getID());
		for(WarpedImageKey k1 : model.getKeys(c)) {
			for(WarpedImageKey k2 : model.getKeys(c)) {
				if(k1==k2)
					continue;
				LOGGER.finer("Comparing "+k1+" and "+k2);
				try {
					Object[] rowData = createRowData(k1, k2);
					if(!containsRow(rowData, compModel))
						compModel.addRow(rowData);
				} catch(Exception e) {
					LOGGER.log(Loggable.STACK, String.format("Error calculating MS-SSIM* for pair %s and %s: %s", k1, k2, e.getMessage()), e);
				}
			}
		}
	}
		
	/**
	 * Create an MS-SSIM* row based on the given warped image keys
	 * @param k1 the first key
	 * @param k2 the second key
	 * @return 
	 */
	private Object[] createRowData(WarpedImageKey k1, WarpedImageKey k2) {
		DecimalFormat df = new DecimalFormat("0.000");
		// choose the order of keys 
		List<WarpedImageKey> keys = new ArrayList<>();
		keys.add(k1);
		keys.add(k2);
		keys.sort((c1, c2)-> (c1.getSignalGroupName().compareTo(c2.getSignalGroupName())*10+c1.getTemplate().getName().compareTo(c2.getTemplate().getName())));


		ImageProcessor ip1 = model.getImage(keys.get(0));
		ImageProcessor ip2 = model.getImage(keys.get(1));
		LOGGER.finer( keys.get(0)+" vs "+keys.get(1));
		MSSIMScore score = msi.calculateMSSIM(ip1, ip2);

		return new Object[]  { keys.get(0).getTemplate().getName(), 
				keys.get(0).getSignalGroupName(), 
				keys.get(1).getTemplate().getName(), 
				keys.get(1).getSignalGroupName(), 
				keys.get(1).getTargetName(), 
				df.format(score.luminance), df.format(score.contrast),  df.format(score.structure),  df.format(score.msSsimIndex) };
	}
	
	/**
	 * Test if a table model already contains a row with the given data
	 * @param rowData the data to check
	 * @param tableModel the model to look within
	 * @return true if the row is present in the table model, false otherwise
	 */
	private boolean containsRow(Object[] rowData, TableModel tableModel) {
		for(int row=0; row<tableModel.getRowCount(); row++) {
			boolean isMatching = true;
			for(int col=0; col<tableModel.getColumnCount(); col++)
				isMatching &= tableModel.getValueAt(row, col).equals(rowData[col]);
			if(isMatching)
				return true;
		}
		return false;
	}
	
		
//	private void makePerCellCharts() {
//		LOGGER.fine("Creating per cell charts");
//		
//		
//		PerCellMSSSIDCalculator calc = new PerCellMSSSIDCalculator();
//		calc.addPropertyChangeListener( evt ->{
//			if (evt.getNewValue() instanceof Integer) {
//	            int percent = (Integer) evt.getNewValue(); // should be percent
//	            if (percent >= 0 && percent <= 100) {
//	                if (progressBar.isIndeterminate()) {
//	                    progressBar.setIndeterminate(false);
//	                }
//	                progressBar.setValue(percent);
//	            }
//	        }
//
//	        if (IAnalysisWorker.FINISHED_MSG.equals(evt.getPropertyName())) {
//	            progressBar.setValue(0);
//	            progressBar.setVisible(false);
//				setLoadingLabelText("");
//				
//				Map<ViolinKey, List<MSSIMScore>> scores;
//				try {
//					scores = calc.get();
//					chartPanel.setChart(makeCharts(scores));
//				} catch (InterruptedException | ExecutionException e) {
//					LOGGER.log(Loggable.STACK, e);
//					chartPanel.setChart(ViolinChartFactory.createErrorChart());
//				}
//				
//	        }
//		});
//		ThreadManager.getInstance().execute(calc);		
//
//	}
//	
//	private JFreeChart makeCharts(Map<ViolinKey, List<MSSIMScore>> scores) {
//		ViolinCategoryDataset ds = new ViolinCategoryDataset();
//		for(ViolinKey key : scores.keySet()) {
//			List<Double> msssims = scores.get(key).stream().map(s->s.msSsimIndex).collect(Collectors.toList());
//			ds.add(msssims, key.colKey, key.rowKey);
//		}
//
//		ViolinRenderer renderer = new ViolinRenderer();
//		CategoryAxis categoryAxis = new CategoryAxis("Sample");
//		NumberAxis valueAxis = new NumberAxis("Score");
//		valueAxis.setAutoRangeIncludesZero(true);
//		CategoryPlot plot = new CategoryPlot(ds, categoryAxis, valueAxis, renderer);
//		valueAxis.setRange(0, 1);
//		return new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
//	}
//	
//	/**
//	 * A key for values entered into the violin datasets
//	 * @author bms41
//	 * @since 1.15.0
//	 *
//	 */
//	private class ViolinKey {
//		public final String colKey;
//		public final String rowKey;
//		
//		public ViolinKey(String col, String row) {
//			colKey = col;
//			rowKey = row;
//		}
//
//		@Override
//		public int hashCode() {
//			final int prime = 31;
//			int result = 1;
//			result = prime * result + getOuterType().hashCode();
//			result = prime * result + ((colKey == null) ? 0 : colKey.hashCode());
//			result = prime * result + ((rowKey == null) ? 0 : rowKey.hashCode());
//			return result;
//		}
//
//		@Override
//		public boolean equals(Object obj) {
//			if (this == obj)
//				return true;
//			if (obj == null)
//				return false;
//			if (getClass() != obj.getClass())
//				return false;
//			ViolinKey other = (ViolinKey) obj;
//			if (!getOuterType().equals(other.getOuterType()))
//				return false;
//			if (colKey == null) {
//				if (other.colKey != null)
//					return false;
//			} else if (!colKey.equals(other.colKey))
//				return false;
//			if (rowKey == null) {
//				if (other.rowKey != null)
//					return false;
//			} else if (!rowKey.equals(other.rowKey))
//				return false;
//			return true;
//		}
//
//		private StructuralSimilarityComparisonDialog getOuterType() {
//			return StructuralSimilarityComparisonDialog.this;
//		}
//		
//		
//	}
//	
//	
//	/**
//	 * Calculate the similarities between warped images for signal pairs in nuclei.
//	 * @author bms41
//	 * @since 1.15.0
//	 *
//	 */
//	private class PerCellMSSSIDCalculator extends SwingWorker<Map<ViolinKey, List<MSSIMScore>>, Integer> {
//		
//		int totalCells =  0;
//		
//		public PerCellMSSSIDCalculator() {
//			for(IAnalysisDataset d : model.getTemplates())
//				totalCells += d.getCollection().getNucleusCount();
//		}
//		
//		@Override
//	    protected Map<ViolinKey, List<MSSIMScore>> doInBackground() throws Exception {
//			Map<ViolinKey, List<MSSIMScore>> result = new HashMap<>();
//	        try {
//	            LOGGER.finer( "Running warping");
//	            result = calculatePerCellMSSSIMs();
//
//	        } catch (Exception e) {
//	            warn("Error in warper");
//	            LOGGER.log(Loggable.STACK, "Error in signal warper", e);
//	        }
//	        
//	        return result;
//	    }
//		
//		private Map<ViolinKey, List<MSSIMScore>> calculatePerCellMSSSIMs() {
//			int progress = 0;
//			MultiScaleStructuralSimilarityIndex msi = new MultiScaleStructuralSimilarityIndex();
//			Map<ViolinKey, List<MSSIMScore>> scores = new HashMap();
//			for(IAnalysisDataset d : model.getTemplates()) {
//				setLoadingLabelText("Generating pairwise comparison plots for "+d.getName()+"...");
//				LOGGER.fine("Calculating score for "+d.getName());
//				Mesh<Nucleus> meshConsensus;
//				try {
//					meshConsensus = new DefaultMesh<Nucleus>(d.getCollection().getConsensus());
//				} catch (MeshCreationException e2) {
//					LOGGER.log(Loggable.STACK, e2);
//					progress+=d.getCollection().getNucleusCount();
//					publish(progress);
//					continue;
//				}
//				
//				Map<UUID, String>  signalNames = new HashMap<>();
//				Map<UUID, Integer> signalThresholds = new HashMap<>();
//				for(UUID id : d.getCollection().getSignalGroupIDs()) {
//					signalNames.put(id, d.getCollection().getSignalGroup(id).get().getGroupName());
//					signalThresholds.put(id, d.getAnalysisOptions().get().getNuclearSignalOptions(id).getThreshold());
//				}
//				
//				
//			
//
//				
//
//				Rectangle r = meshConsensus.toPath().getBounds();
//
//				for(Nucleus n : d.getCollection().getNuclei()) {
//					LOGGER.finer( "Calculating "+n.getNameAndNumber());
//					if(n.getSignalCollection().getSignalGroupIds().size()==2) {
//						List<UUID> signalIds = new ArrayList<>(n.getSignalCollection().getSignalGroupIds());
//						UUID sig0 = signalIds.get(0);
//						UUID sig1 = signalIds.get(1);
//						if(n.getSignalCollection().hasSignal(sig0)&&n.getSignalCollection().hasSignal(sig1)) {
//							List<MSSIMScore> scoreList;
//							ViolinKey vkRev = new ViolinKey(d.getName(), signalNames.get(sig1)+"_"+signalNames.get(sig0));
//							if(scores.containsKey(vkRev))
//								scoreList = scores.get(vkRev);
//							else {
//								ViolinKey vk = new ViolinKey(d.getName(), signalNames.get(sig0)+"_"+signalNames.get(sig1));
//								if(!scores.containsKey(vk))
//									scores.put(vk, new ArrayList<>());
//								scoreList = scores.get(vk);
//							}
//								
//							ImageProcessor ip1 = generateNucleusImage(meshConsensus, r.width, r.height, n, sig0, signalThresholds.get(sig0));
//							ImageProcessor ip2 = generateNucleusImage(meshConsensus, r.width, r.height, n, sig1, signalThresholds.get(sig1));
//							MSSIMScore score =  msi.calculateMSSIM(ip1, ip2);
//							scoreList.add(score);
//						}
//					}
//					publish(++progress);
//				}
//			}
//			return scores;
//		}
//
//	   
//		private ImageProcessor generateNucleusImage(@NonNull Mesh<Nucleus> meshConsensus, int w, int h, @NonNull Nucleus n, UUID signalGroup, int threshold) {
//
//			try {
//				Mesh<Nucleus> cellMesh = new DefaultMesh<>(n, meshConsensus);
//
//			    // Get the image with the signal
//			    ImageProcessor ip;
//			    if(n.getSignalCollection().hasSignal(signalGroup)){ // if there is no signal, getImage will throw exception
//			    	ip = n.getSignalCollection().getImage(signalGroup);
//			    	ip.invert();
//			    	ip = new ImageFilterer(ip).setBlackLevel(150).toProcessor();
//			    } else {
//			    	return ImageFilterer.createBlackByteProcessor(w, h);
//			    }
//
//			    MeshImage<Nucleus> meshImage = new DefaultMeshImage<>(cellMesh, ip);
//
//			    // Draw NucleusMeshImage onto consensus mesh.
//			    return meshImage.drawImage(meshConsensus);
//
//			} catch (Exception e) {
//				LOGGER.log(Loggable.STACK, e);
//				return ImageFilterer.createBlackByteProcessor(w, h);
//			} 
//		}
//		
//		@Override
//	    protected void process(List<Integer> chunks) {
//	        for (Integer i : chunks) {
//	            int percent = (int) ((double) i / (double) totalCells * 100);
//	            if (percent >= 0 && percent <= 100)
//	                setProgress(percent);
//	        }
//	    }
//
//	    @Override
//	    public void done() {
//	        try {
//	            if (this.get() != null) {
//	                finest("Firing trigger for sucessful task");
//	                firePropertyChange("Finished", getProgress(), IAnalysisWorker.FINISHED);
//	            } else {
//	                finest("Firing trigger for failed task");
//	                firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
//	            }
//	        } catch (InterruptedException e) {
//	            error("Interruption error in worker", e);
//	            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
//	        } catch (ExecutionException e) {
//	            error("Execution error in worker", e);
//	            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
//	        }
//	    }
//	}
//	

}
