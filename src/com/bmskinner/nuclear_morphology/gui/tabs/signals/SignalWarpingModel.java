package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.IWarpedSignal;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalWarpingModel.ImageCache.WarpedImageKey;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * The model for signal warping views
 * @author bms41
 *
 */
public class SignalWarpingModel implements Loggable {
	
	public static final int THRESHOLD_ALL_VISIBLE = 255;
	
	/** images currently displayed */
	final private List<WarpedImageKey> displayImages = new ArrayList<>(); 
	
	private final ImageCache cache = new ImageCache();
	
	public void addSelection(@NonNull WarpedImageKey k) {
        displayImages.add(k);
    }

    public void removeSelection(@NonNull WarpedImageKey k) {
        displayImages.remove(k);
    }

    public boolean isSelected(@NonNull WarpedImageKey k) {
        return displayImages.contains(k);
    }

    public int selectedImageCount() {
        return displayImages.size();
    }

    public void clearSelection() {
        displayImages.clear();
    }
					
	public synchronized void toggleSelection(@NonNull WarpedImageKey k) {
		if(isSelected(k))
			removeSelection(k);
		else
			addSelection(k);
	}

	
	public void addSavedImages(@NonNull List<IAnalysisDataset> list) {
		for(IAnalysisDataset d : list) {
			addSavedImages(d);
		}
	}

	public void addSavedImages(@NonNull IAnalysisDataset d) {
		for(UUID signalGroupId : d.getCollection().getSignalGroupIDs()) {
			ISignalGroup sg  = d.getCollection().getSignalGroup(signalGroupId).get();
			
            Optional<IWarpedSignal> ws = sg.getWarpedSignals();
            if(ws.isPresent()) {
            	IWarpedSignal warpedSignal = ws.get();
            	for(CellularComponent c : warpedSignal.getTemplates()) {
            		
            		Optional<ImageProcessor> im = warpedSignal.getWarpedImage(c);
            		if(im.isPresent()) {
            			WarpedImageKey k = cache.new WarpedImageKey(c, warpedSignal.getTargetName(c), d, signalGroupId,warpedSignal.isCellsWithSignals());
            			cache.add(k, im.get());
                        Color col = sg.getGroupColour().orElse(Color.WHITE);
                        cache.setColour(k, col);
                        
                        cache.setThreshold(k, THRESHOLD_ALL_VISIBLE);
            		}
            		
            		
            	}                	
            }
		}
	}
	
	public int getThreshold(@NonNull WarpedImageKey k) {
		return cache.getThreshold(k);
	}
	
	public void setThresholdOfSelected(int threshold) {
		for(WarpedImageKey k : displayImages)
			cache.setThreshold(k, threshold);;
	}
	
	public void setThreshold(@NonNull WarpedImageKey k, int threshold) {
		cache.setThreshold(k, threshold);;
	}
	
	public void addImage(@NonNull CellularComponent consensusTemplate, @NonNull String targetName, @NonNull IAnalysisDataset signalSource, @NonNull UUID signalGroupId, boolean isCellsWithSignals, @NonNull ImageProcessor image) {
		WarpedImageKey k = cache.new WarpedImageKey(consensusTemplate, targetName, signalSource, signalGroupId,isCellsWithSignals);

        cache.add(k, image);
        Color c = signalSource.getCollection().getSignalGroup(signalGroupId).get().getGroupColour().orElse(Color.WHITE);
        cache.setColour(k, c);
        cache.setThreshold(k, THRESHOLD_ALL_VISIBLE);
	}
	
	/**
	 * Get the chart matching the current display criteria
	 * @return
	 */
	public JFreeChart getChart() {
		if(!isCommonTargetSelected())
			return OutlineChartFactory.makeEmptyChart();
		
		ImageProcessor image = createDisplayImage();

        ChartOptions options = new ChartOptionsBuilder()
        		.setCellularComponent(getCommonSelectedTarget())
        		.setShowXAxis(false).setShowYAxis(false)
                .setShowBounds(false).build();

        return new OutlineChartFactory(options).makeSignalWarpChart(image);
	}
	
	public void showImage(WarpedImageKey k) {
		new ImagePlus(k.toString(), cache.get(k)).show();
	}
	
	/**
	 * Get all the consensus targets in the model
	 * @return
	 */
	public List<CellularComponent> getTargets() {
        return cache.getTargets();
    }

    /**
     * Get all the keys in the model for the given target
     * @param n the target consenus shape
     * @return
     */
    public List<WarpedImageKey> getKeys(@NonNull CellularComponent n) {
        return cache.getKeys(n);
    }
    
    private CellularComponent getCommonSelectedTarget() {
    	if(!isCommonTargetSelected())
    		return null;
    	return displayImages.get(0).target;
    }
    
    /**
     * Test if all the selected visible keys have the same target
     * @return
     */
    private boolean isCommonTargetSelected() {
    	
    	WarpedImageKey k = displayImages.get(0);
    	for (WarpedImageKey j : displayImages) {
    		if(!k.target.equals(j.target))
    			return false;
    	}
    	return true;
    }
	
	/**
     * Create an image for display
     * 
     * @param image
     * @return
     */
    private ImageProcessor createDisplayImage() {
    	if (selectedImageCount() == 0 || !isCommonTargetSelected()) 
            return ImageFilterer.createBlankByteProcessor(100, 100);

    	// Recolour each of the grey images according to the stored colours
        List<ImageProcessor> recoloured = new ArrayList<>();
        
        for (WarpedImageKey k : displayImages) {
        	fine("Adding to chart: "+k);
            // The image from the warper is greyscale. Change to use the signal colour

        	ImageProcessor raw = cache.get(k);
        	ImageProcessor recol = ImageFilterer.recolorImage(raw, cache.getColour(k));
        	recol.setMinAndMax(0, cache.getThreshold(k));
            recoloured.add(recol);
        }

        if (selectedImageCount() == 1)
            return recoloured.get(0);

        // If multiple images are in the list, make an average of their RGB
        // values so territories can be compared
        try {
            ImageProcessor averaged = ImageFilterer.averageRGBImages(recoloured);
            return averaged;

        } catch (Exception e) {
            warn("Error averaging images");
            stack(e);
            return ImageFilterer.createBlankByteProcessor(100, 100);
        }

    }
	
	
    /**
     * Store the warped images
     * 
     * @author ben
     * @since 1.13.7
     *
     */
    public class ImageCache {

    	/** all generated images */
        final private Map<WarpedImageKey, ImageProcessor> map  = new HashMap<>(); 
        
        /** pseudo-colours for warped images */
        final private Map<WarpedImageKey, Color> imageColours  = new HashMap<>();
        
        /** thresholds for warped images */
        final private Map<WarpedImageKey, Integer> thresholds  = new HashMap<>();


        public int getThreshold(@NonNull WarpedImageKey k){
        	if(!thresholds.containsKey(k))
        		return THRESHOLD_ALL_VISIBLE;
        	return thresholds.get(k);
        }
        
        /**
         * Set the thresholding value for the image. This is the minimum intensity
         * to display. 
         * @param k
         * @param i
         */
        public void setThreshold(@NonNull WarpedImageKey k, int i) {
        	thresholds.put(k, i);
        }

        public Color getColour(@NonNull WarpedImageKey k) {
            return imageColours.get(k);
        }

        public void setColour(@NonNull WarpedImageKey k, Color c) {
            imageColours.put(k, c);
        }

        public void add(@NonNull WarpedImageKey k, @NonNull ImageProcessor ip) {
            map.put(k, ip);
        }

        public void add(@NonNull CellularComponent target, @NonNull String targetName, @NonNull IAnalysisDataset template, @NonNull UUID signalGroupId, boolean isCellsWithSignals, @NonNull ImageProcessor ip) {
            map.put(new WarpedImageKey(target, targetName, template, signalGroupId, isCellsWithSignals), ip);
        }

        public ImageProcessor get(@NonNull WarpedImageKey k) {
            return map.get(k);
        }
        
        public List<CellularComponent> getTargets() {
            return map.keySet().stream().map(k ->  k.target)
            		.distinct()
            		.collect(Collectors.toList());
        }

        public List<WarpedImageKey> getKeys(@NonNull CellularComponent n) {
            return map.keySet().stream().filter(k -> k.target.getID().equals(n.getID()))
                    .collect(Collectors.toList());
        }

        /**
         * Key to store warped images
         * 
         * @author ben
         *
         */
        public class WarpedImageKey {

            private final @NonNull CellularComponent target;
            private final @NonNull String targetName;
            private final @NonNull IAnalysisDataset  template;
            private final @NonNull UUID              signalGroupId;
            private final boolean isOnlyCellsWithSignals;

            public WarpedImageKey(@NonNull CellularComponent target, @NonNull String targetName, @NonNull IAnalysisDataset template, @NonNull UUID signalGroupId, boolean isCellsWithSignals) {
                this.target   = target;
                this.targetName = targetName;
                this.template = template;
                this.signalGroupId = signalGroupId;
                isOnlyCellsWithSignals = isCellsWithSignals;
            }

            public CellularComponent getTarget() {
				return target;
			}

            public String getTargetName() {
				return targetName;
			}

			public IAnalysisDataset getTemplate() {
				return template;
			}

			public UUID getSignalGroupId() {
				return signalGroupId;
			}

			public boolean isOnlyCellsWithSignals() {
				return isOnlyCellsWithSignals;
			}

			
            

            @Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + getOuterType().hashCode();
				result = prime * result + (isOnlyCellsWithSignals ? 1231 : 1237);
				result = prime * result + ((signalGroupId == null) ? 0 : signalGroupId.hashCode());
				result = prime * result + ((target == null) ? 0 : target.hashCode());
				result = prime * result + ((targetName == null) ? 0 : targetName.hashCode());
				result = prime * result + ((template == null) ? 0 : template.hashCode());
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
				WarpedImageKey other = (WarpedImageKey) obj;
				if (!getOuterType().equals(other.getOuterType()))
					return false;
				if (isOnlyCellsWithSignals != other.isOnlyCellsWithSignals)
					return false;
				if (signalGroupId == null) {
					if (other.signalGroupId != null)
						return false;
				} else if (!signalGroupId.equals(other.signalGroupId))
					return false;
				if (target == null) {
					if (other.target != null)
						return false;
				} else if (!target.equals(other.target))
					return false;
				if (targetName == null) {
					if (other.targetName != null)
						return false;
				} else if (!targetName.equals(other.targetName))
					return false;
				if (template == null) {
					if (other.template != null)
						return false;
				} else if (!template.equals(other.template))
					return false;
				return true;
			}

			private SignalWarpingModel getOuterType() {
                return SignalWarpingModel.this;
            }

            @Override
            public String toString() {
                return template.getName() 
                		+ " - "
				        + template.getCollection().getSignalManager().getSignalGroupName(signalGroupId)
				        +" - "
				        + targetName
				        +" - "
				        + isOnlyCellsWithSignals;
            }

        }

    }

}
