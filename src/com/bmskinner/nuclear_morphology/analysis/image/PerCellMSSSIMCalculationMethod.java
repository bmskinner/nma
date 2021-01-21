package com.bmskinner.nuclear_morphology.analysis.image;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex.MSSIMScore;
import com.bmskinner.nuclear_morphology.analysis.image.PerCellMSSSIMCalculationMethod.ViolinKey;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * Calculate the similarities between warped images for signal pairs in the same nuclei.
 * For example, if a nucleus has signals A and B, this will calculate the MS-SSIM* between 
 * A and B warped onto a common template. If a nucleus has only one of these signals, no 
 * score is generated, and an empty chart will result
 * @author bms41
 * @since 1.15.0
 *
 */
public class PerCellMSSSIMCalculationMethod extends SwingWorker<Map<ViolinKey, List<MSSIMScore>>, Integer> {
	
	private static final Logger LOGGER = Logger.getLogger(PerCellMSSSIMCalculationMethod.class.getName());
	
	private final SignalWarpingModel model;
	
	int totalCells =  0;
	
	public PerCellMSSSIMCalculationMethod(final SignalWarpingModel model) {
		this.model = model;
		for(IAnalysisDataset d : model.getTemplates())
			totalCells += d.getCollection().getNucleusCount();
	}
	
	@Override
    protected Map<ViolinKey, List<MSSIMScore>> doInBackground() throws Exception {
		Map<ViolinKey, List<MSSIMScore>> result = new HashMap<>();
        try {
            LOGGER.fine( "Running warping");
            result = calculatePerCellMSSSIMs();

        } catch (Exception e) {
        	LOGGER.warning("Error in warper");
            LOGGER.log(Loggable.STACK, "Error in signal warper", e);
        }
        
        return result;
    }
	
	private Map<ViolinKey, List<MSSIMScore>> calculatePerCellMSSSIMs() {
		LOGGER.fine( "Calculating per cell MS-SSIM*s");
		int progress = 0;
		MultiScaleStructuralSimilarityIndex msi = new MultiScaleStructuralSimilarityIndex();
		
		Map<ViolinKey, List<MSSIMScore>> scores = new HashMap<>();
		for(IAnalysisDataset d : model.getTemplates()) {
			LOGGER.fine("Calculating MS-SSIM* scores for "+d.getName());
			
			// Get the consensus mesh for the current target shape
			Mesh<Nucleus> meshConsensus;
			try {
				meshConsensus = new DefaultMesh<>(d.getCollection().getConsensus());
			} catch (MeshCreationException e2) {
				LOGGER.log(Level.FINE, "Error making mesh of consensus", e2);
				progress+=d.getCollection().getNucleusCount();
				publish(progress);
				continue;
			}
			
			LOGGER.fine("Consensus mesh built for "+d.getName());
			
			// Choose the signals and threshold to set for warping
			Map<UUID, String>  signalNames = new HashMap<>();
			Map<UUID, Integer> signalThresholds = new HashMap<>();
			for(UUID id : d.getCollection().getSignalGroupIDs()) {
				signalNames.put(id, d.getCollection().getSignalGroup(id).get().getGroupName());
				signalThresholds.put(id, d.getAnalysisOptions().get().getNuclearSignalOptions(id).getThreshold());
			}
			
			
			Rectangle r = meshConsensus.toPath().getBounds();

			for(Nucleus n : d.getCollection().getNuclei()) {
								
				if(n.getSignalCollection().getSignalGroupIds().size()==2) {
					List<UUID> signalIds = new ArrayList<>(n.getSignalCollection().getSignalGroupIds());
					UUID sig0 = signalIds.get(0);
					UUID sig1 = signalIds.get(1);
					if(n.getSignalCollection().hasSignal(sig0)&&n.getSignalCollection().hasSignal(sig1)) {
						List<MSSIMScore> scoreList;
						ViolinKey vkRev = new ViolinKey(d.getName(), signalNames.get(sig1)+"_"+signalNames.get(sig0));
						if(scores.containsKey(vkRev))
							scoreList = scores.get(vkRev);
						else {
							ViolinKey vk = new ViolinKey(d.getName(), signalNames.get(sig0)+"_"+signalNames.get(sig1));
							if(!scores.containsKey(vk))
								scores.put(vk, new ArrayList<>());
							scoreList = scores.get(vk);
						}
							
						ImageProcessor ip1 = generateNucleusImage(meshConsensus, r.width, r.height, n, sig0, signalThresholds.get(sig0));
						ImageProcessor ip2 = generateNucleusImage(meshConsensus, r.width, r.height, n, sig1, signalThresholds.get(sig1));
						MSSIMScore score =  msi.calculateMSSIM(ip1, ip2);
						scoreList.add(score);
					}
				}
				publish(++progress);
			}
		}
		return scores;
	}

   
	private ImageProcessor generateNucleusImage(@NonNull Mesh<Nucleus> meshConsensus, int w, int h, @NonNull Nucleus n, UUID signalGroup, int threshold) {

		try {
			Mesh<Nucleus> cellMesh = new DefaultMesh<>(n, meshConsensus);

		    // Get the image with the signal
		    ImageProcessor ip;
		    if(n.getSignalCollection().hasSignal(signalGroup)){ // if there is no signal, getImage will throw exception
		    	ip = n.getSignalCollection().getImage(signalGroup);
		    	ip.invert();
		    	ip = new ImageFilterer(ip).setBlackLevel(150).toProcessor();
		    } else {
		    	return ImageFilterer.createBlackByteProcessor(w, h);
		    }

		    MeshImage<Nucleus> meshImage = new DefaultMeshImage<>(cellMesh, ip);

		    // Draw NucleusMeshImage onto consensus mesh.
		    return meshImage.drawImage(meshConsensus);

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error making nucleus mesh image",  e);
			return ImageFilterer.createBlackByteProcessor(w, h);
		} 
	}
	
	@Override
    protected void process(List<Integer> chunks) {
        for (Integer i : chunks) {
            int percent = (int) ((double) i / (double) totalCells * 100);
            if (percent >= 0 && percent <= 100)
                setProgress(percent);
        }
    }

    @Override
    public void done() {
        try {
            if (this.get() != null) {
            	LOGGER.finest("Firing trigger for sucessful task");
                firePropertyChange("Finished", getProgress(), IAnalysisWorker.FINISHED);
            } else {
            	LOGGER.finest("Firing trigger for failed task");
                firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
            }
        } catch (InterruptedException e) {
        	LOGGER.log(Loggable.STACK, "Interruption error in worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        } catch (ExecutionException e) {
        	LOGGER.log(Loggable.STACK, "Execution error in worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        }
    }
    
	/**
	 * A key for values entered into the violin datasets
	 * @author bms41
	 * @since 1.15.0
	 *
	 */
	public class ViolinKey {
		public final String colKey;
		public final String rowKey;
		
		public ViolinKey(String col, String row) {
			colKey = col;
			rowKey = row;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((colKey == null) ? 0 : colKey.hashCode());
			result = prime * result + ((rowKey == null) ? 0 : rowKey.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ViolinKey other = (ViolinKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (colKey == null) {
				if (other.colKey != null)
					return false;
			} else if (!colKey.equals(other.colKey))
				return false;
			if (rowKey == null) {
				if (other.rowKey != null)
					return false;
			} else if (!rowKey.equals(other.rowKey))
				return false;
			return true;
		}

		private PerCellMSSSIMCalculationMethod getOuterType() {
			return PerCellMSSSIMCalculationMethod.this;
		}
		
		
	}
}
