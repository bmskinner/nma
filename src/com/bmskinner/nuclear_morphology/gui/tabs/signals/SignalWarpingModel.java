package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.JSlider;

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
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalWarpingModel.ImageCache.Key;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * The model for signal warping views
 * @author bms41
 *
 */
public class SignalWarpingModel implements Loggable {
	
	public static final int ALL_VISIBLE = 255;
	
	private final ImageCache cache = new ImageCache();
	private int threshold = ALL_VISIBLE;
	
	public SignalWarpingModel(Object parent) {
		
	}
	
	public void addSelection(Key k) {
		cache.addDisplayImage(k);
	}
	
	public void removeSelection(Key k) {
		cache.removeDisplayImage(k);
	}
	
	public void clearSelection() {
		cache.clearDisplayImages();
	}
	
	public boolean isSelected(Key k) {
		return cache.hasDisplayImage(k);
	}
	
	public synchronized void toggleSelection(Key k) {
		if( cache.hasDisplayImage(k)){
			cache.removeDisplayImage(k);
		} else {
			cache.addDisplayImage(k);
		}
	}

	public int selectedSize() {
		return cache.displayCount();
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
            	for(CellularComponent c : ws.get().getTemplates()) {
            		
            		Optional<ImageProcessor> im = ws.get().getWarpedImage(c);
            		if(im.isPresent()) {
            			Key k = cache.new Key(c, d, signalGroupId,ws.get().isCellsWithSignals());
            			cache.add(k, im.get());


                        cache.addDisplayImage(k);

                        Color col = sg.getGroupColour().orElse(Color.WHITE);
                        cache.setColour(k, col);
                        
                        cache.setThreshold(k, 255);
            		}
            		
            		
            	}                	
            }
		}
	}
	
	public int getThreshold() {
		return threshold;
	}
	
	public void setThreshold(int threshold) {
//		cache.setThreshold(k, threshold);
		this.threshold = threshold;
	}
	
	public void addImage(CellularComponent consensusTemplate, IAnalysisDataset signalSource, UUID signalGroupId, boolean isCellsWithSignals, ImageProcessor image) {
		Key k = cache.new Key(consensusTemplate, signalSource, signalGroupId,isCellsWithSignals);

        cache.add(k, image);

        cache.addDisplayImage(k);

        Color c = signalSource.getCollection().getSignalGroup(signalGroupId).get().getGroupColour().orElse(Color.WHITE);
        cache.setColour(k, c);
        
        cache.setThreshold(k, ALL_VISIBLE);
        
        ImageProcessor display = createDisplayImage();
	}
	
	/**
	 * Get the chart matching the current display criteria
	 * @return
	 */
	public JFreeChart getChart() {
		if(!isCommonTargetSelected()) {
			return OutlineChartFactory.makeEmptyChart();
		}
		ImageProcessor image = createDisplayImage();

        ChartOptions options = new ChartOptionsBuilder()
        		.setCellularComponent(getCommonSelectedTarget())
        		.setShowXAxis(false).setShowYAxis(false)
                .setShowBounds(false).build();

        return new OutlineChartFactory(options).makeSignalWarpChart(image);
	}
	
	public void showImage(Key k) {
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
    public List<Key> getKeys(@NonNull CellularComponent n) {
        return cache.getKeys(n);
    }
    
    private CellularComponent getCommonSelectedTarget() {
    	if(!isCommonTargetSelected())
    		return null;
    	return cache.getDisplayKeys().get(0).target;
    }
    
    /**
     * Test if all the selected visible keys have the same target
     * @return
     */
    private boolean isCommonTargetSelected() {
    	
    	Key k = cache.getDisplayKeys().get(0);
    	for (Key j : cache.getDisplayKeys()) {
    		if(!k.target.equals(j.target))
    			return false;
    	}
    	return true;
    }
	
	/**
     * Create an image for display based on the given greyscale image
     * 
     * @param image
     * @return
     */
    private ImageProcessor createDisplayImage() {
    	if (cache.displayCount() == 0 || !isCommonTargetSelected()) 
            return ImageFilterer.createBlankByteProcessor(100, 100);

//        if(cache.displayCount() == 1)
//        	return cache.get(cache.getDisplayKeys().get(0));
//        
//        fine("More than 1 image selected");
        
     // Recolour each of the grey images according to the stored colours
        List<ImageProcessor> recoloured = new ArrayList<>();
        
        for (Key k : cache.getDisplayKeys()) {
            // The image from the warper is greyscale. Change to use the signal
            // colour
        	fine(k.toString());
        	ImageProcessor raw = cache.get(k);
        	ImageProcessor thresh = raw.duplicate();
        	ImageProcessor recol = ImageFilterer.recolorImage(thresh, cache.getColour(k));
        	recol.setMinAndMax(0, threshold);
//        	recol.setMinAndMax(0, cache.getThreshold(k));
            recoloured.add(recol);
        }

        if (cache.displayCount() == 1) {
            return recoloured.get(0);
        }

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

        final private Map<Key, ImageProcessor> map  = new HashMap<>();   // all generated images
        final private List<Key> displayImages = new ArrayList<>(); // images currently displayed
        final private Map<Key, Color> imageColours  = new HashMap<>();   // colours for warped images
        final private Map<Key, Integer> thresholds  = new HashMap<>();   // thresholds for warped images

        public void addDisplayImage(@NonNull Key k) {
            displayImages.add(k);
        }

        public void addDisplayImage(@NonNull CellularComponent target, @NonNull IAnalysisDataset template, boolean isCellsWithSignals, @NonNull UUID signalGroupId) {
            displayImages.add(new Key(target, template, signalGroupId, isCellsWithSignals));
        }

        public void removeDisplayImage(@NonNull Key k) {
            displayImages.remove(k);
        }

        public boolean hasDisplayImage(@NonNull Key k) {
            return displayImages.contains(k);
        }

        public int displayCount() {
            return displayImages.size();
        }

        public void clearDisplayImages() {
            displayImages.clear();
        }

        public List<ImageProcessor> getDisplayImages() {
            return map.entrySet().stream().filter(e -> displayImages.contains(e.getKey())).map(e -> e.getValue())
                    .collect(Collectors.toList());
        }

        public List<Key> getDisplayKeys() {
            return displayImages;
        }
        
        public int getThreshold(@NonNull Key k){
        	if(!thresholds.containsKey(k))
        		return ALL_VISIBLE;
        	return thresholds.get(k);
        }
        
        /**
         * Set the thresholding value for the image. This is the minimum intensity
         * to display. 
         * @param k
         * @param i
         */
        public void setThreshold(@NonNull Key k, int i) {
        	thresholds.put(k, i);
        }

        public Color getColour(@NonNull Key k) {
            return imageColours.get(k);
        }

        public void setColour(@NonNull Key k, Color c) {
            imageColours.put(k, c);
        }

        public void add(@NonNull Key k, @NonNull ImageProcessor ip) {
            map.put(k, ip);
        }

        public void add(@NonNull CellularComponent target, @NonNull IAnalysisDataset template, @NonNull UUID signalGroupId, boolean isCellsWithSignals, @NonNull ImageProcessor ip) {
            map.put(new Key(target, template, signalGroupId, isCellsWithSignals), ip);
        }

        public ImageProcessor get(@NonNull Key k) {
            return map.get(k);
        }
        
        public List<CellularComponent> getTargets() {
            return map.keySet().stream().map(k ->  k.target)
            		.distinct()
            		.collect(Collectors.toList());
        }

        public List<Key> getKeys(@NonNull CellularComponent n) {
            return map.keySet().stream().filter(k -> k.target.getID().equals(n.getID()))
                    .collect(Collectors.toList());
        }

        /**
         * Key to store warped images
         * 
         * @author ben
         *
         */
        public class Key {

            private final @NonNull CellularComponent target;
            private final @NonNull IAnalysisDataset  template;
            private final @NonNull UUID              signalGroupId;
            private final boolean isOnlyCellsWithSignals;

            public Key(@NonNull CellularComponent target, @NonNull IAnalysisDataset template, @NonNull UUID signalGroupId, boolean isCellsWithSignals) {
                this.target   = target;
                this.template = template;
                this.signalGroupId = signalGroupId;
                isOnlyCellsWithSignals = isCellsWithSignals;
            }

            public CellularComponent getTarget() {
				return target;
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
                result = prime * result + ((signalGroupId == null) ? 0 : signalGroupId.hashCode());
                result = prime * result + ((target == null) ? 0 : target.hashCode());
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
                Key other = (Key) obj;
                if (!getOuterType().equals(other.getOuterType()))
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
				        + template.getCollection().getSignalManager().getSignalGroupName(signalGroupId);
            }

        }

    }

}
