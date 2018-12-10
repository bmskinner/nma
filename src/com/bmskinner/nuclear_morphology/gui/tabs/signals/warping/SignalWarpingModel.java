/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.swing.table.DefaultTableModel;

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
import com.bmskinner.nuclear_morphology.components.nuclear.WarpedSignalKey;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel.ImageCache.WarpedImageKey;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * The model for signal warping views
 * @author bms41
 * @since 1.14.0
 *
 */
public class SignalWarpingModel extends DefaultTableModel implements Loggable {

	public static final int THRESHOLD_ALL_VISIBLE = 255;
	private static final int KEY_COLUMN_INDEX = 5;
	
	/** images currently displayed */
	final private List<WarpedImageKey> displayImages = new ArrayList<>(); 
	
	private volatile ImageCache cache = new ImageCache();
	
	public SignalWarpingModel() {
		super();
        addColumn(Labels.Signals.Warper.TABLE_HEADER_SOURCE_DATASET, new Vector<IAnalysisDataset>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_SOURCE_SIGNALS, new Vector<ISignalGroup>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_SIGNALS_ONLY, new Vector<Boolean>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_TARGET_SHAPE, new Vector<String>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_THRESHOLD, new Vector<String>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_KEY_COLUMN, new Vector<WarpedImageKey>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_COLOUR_COLUMN, new Vector<Color>());
	}
	
	public SignalWarpingModel(@NonNull List<IAnalysisDataset> datasets) {
		this();
		addSavedImages(datasets);
	}
	
	public WarpedImageKey getKey(int row) {
		
		WarpedImageKey key = (WarpedImageKey) getValueAt(row, KEY_COLUMN_INDEX);
		fine("Selecting key "+key.toString());
		return key;
	}
	
	public synchronized void addSelection(int row) {
		addSelection(getKey(row));
    }
	
	private synchronized void addSelection(@NonNull final WarpedImageKey k) {
        displayImages.add(k);
    }

	private synchronized void removeSelection(@NonNull final WarpedImageKey k) {
        displayImages.remove(k);
    }

    public synchronized boolean isSelected(@NonNull final WarpedImageKey k) {
        return displayImages.contains(k);
    }

    public synchronized int selectedImageCount() {
        return displayImages.size();
    }

    public synchronized void clearSelection() {
        displayImages.clear();
    }
					
	public synchronized void toggleSelection(@NonNull final WarpedImageKey k) {
		if(isSelected(k))
			removeSelection(k);
		else
			addSelection(k);
	}

	
	private void addSavedImages(@NonNull List<IAnalysisDataset> list) {
		for(IAnalysisDataset d : list) {
			addSavedImages(d);
		}
	}

	private void addSavedImages(@NonNull IAnalysisDataset d) {
		for(UUID signalGroupId : d.getCollection().getSignalGroupIDs()) {
			ISignalGroup sg  = d.getCollection().getSignalGroup(signalGroupId).get();

			Optional<IWarpedSignal> ws = sg.getWarpedSignals();
			if(ws.isPresent()) {
				IWarpedSignal warpedSignal = ws.get();
				for(WarpedSignalKey c : warpedSignal.getWarpedSignalKeys()) {

					Optional<ImageProcessor> im = warpedSignal.getWarpedImage(c);
					if(im.isPresent()) {
						WarpedImageKey k = cache.new WarpedImageKey(c.getTargetShape(), warpedSignal.getTargetName(c), d, 
								signalGroupId,c.isCellWithSignalsOnly(), false, 0); // TODO default binarise and threshold for now 
						cache.add(k, im.get());
						Color col = sg.getGroupColour().orElse(Color.WHITE);
						cache.setColour(k, col);
						cache.setThreshold(k, THRESHOLD_ALL_VISIBLE);
						addTableRow(k);
					}
				}                	
			}
		}
	}
	
	public synchronized int getThreshold(int row) {
		return cache.getThreshold(getKey(row));
	}
		
	public synchronized void setThresholdOfSelected(int threshold) {
		for(WarpedImageKey k : displayImages)
			cache.setThreshold(k, threshold);;
	}
	
	public synchronized void setThreshold(@NonNull WarpedImageKey k, int threshold) {
		cache.setThreshold(k, threshold);;
	}
	
	@Override
	public void removeRow(int row) {
		WarpedImageKey k = (WarpedImageKey) this.getValueAt(row, 5);
		removeSelection(k);
		cache.remove(k);
		super.removeRow(row);

	}
	
	private void addTableRow(WarpedImageKey k) {
        Vector v = new Vector();
		v.add(k.getTemplate().getName());
        v.add(k.getTemplate().getCollection().getSignalGroup(k.getSignalGroupId()).get());
        v.add(k.isOnlyCellsWithSignals());
        v.add(k.getTargetName());
        v.add(k.minThreshold);
        v.add(k);
        v.add(cache.getColour(k));
        this.addRow(v);
	}
	
	public void addImage(@NonNull CellularComponent consensusTemplate, @NonNull String targetName, 
			@NonNull IAnalysisDataset signalSource, @NonNull UUID signalGroupId, boolean isCellsWithSignals,
			final boolean binarise, final int minThreshold, @NonNull ImageProcessor image) {
		WarpedImageKey k = cache.new WarpedImageKey(consensusTemplate, targetName, signalSource, signalGroupId,isCellsWithSignals, binarise, minThreshold);

        cache.add(k, image);
        Color c = signalSource.getCollection().getSignalGroup(signalGroupId).get().getGroupColour().orElse(Color.WHITE);
        cache.setColour(k, c);
        cache.setThreshold(k, THRESHOLD_ALL_VISIBLE);
        addTableRow(k);
	}
	
	/**
	 * Get the chart matching the current display criteria
	 * @return
	 */
	public synchronized JFreeChart getChart(boolean isPseudocolour, boolean isEnhance) {
		if(!isCommonTargetSelected())
			return OutlineChartFactory.createEmptyChart();
			
		fine("Creating display image from "+displayImages.size()+" selected keys");
		ImageProcessor image = createDisplayImage(isPseudocolour, isEnhance);

        ChartOptions options = new ChartOptionsBuilder()
        		.setCellularComponent(getCommonSelectedTarget())
        		.setShowXAxis(false).setShowYAxis(false)
                .setShowBounds(false).build();

        return new OutlineChartFactory(options).makeSignalWarpChart(image);
	}
		
	/**
	 * Get the image for display based on the current selection.
	 * @param isPseudocolour
	 * @param isEnhance
	 * @return
	 */
	public ImageProcessor getDisplayImage(boolean isPseudocolour, boolean isEnhance) {
		return createDisplayImage(isPseudocolour, isEnhance);
	}
	
	/**
	 * Get the image for a given key.
	 * @param k
	 * @return
	 */
	public ImageProcessor getImage(WarpedImageKey k) {
		return cache.get(k);
	}
	
	public void showImage(WarpedImageKey k) {
		new ImagePlus(k.toString(), cache.get(k)).show();
	}
	
	/**
	 * Get all the consensus targets in the model
	 * @return
	 */
	public synchronized List<CellularComponent> getTargets() {
        return cache.getTargets();
    }
	
    /**
     * Get all the keys in the model for the given target
     * @param n the target consenus shape
     * @return
     */
    public synchronized List<WarpedImageKey> getKeys(@NonNull CellularComponent n) {
        return cache.getKeys(n);
    }
    
    private synchronized CellularComponent getCommonSelectedTarget() {
    	if(!isCommonTargetSelected())
    		return null;
    	return displayImages.get(0).target;
    }
    
    /**
     * Test if all the selected visible keys have the same target
     * @return
     */
    private synchronized boolean isCommonTargetSelected() {
    	
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
    private synchronized ImageProcessor createDisplayImage(boolean isPseudoColour, boolean isEnhance) {
    	if (selectedImageCount() == 0 || !isCommonTargetSelected()) 
            return ImageFilterer.createWhiteByteProcessor(100, 100);

    	// Recolour each of the grey images according to the stored colours
        List<ImageProcessor> recoloured = new ArrayList<>();

        for (WarpedImageKey k : displayImages) {        	
        	// The image from the warper is greyscale. Change to use the signal colour
        	ImageProcessor raw = cache.get(k); // a short processor
        	ImageProcessor bp = raw.convertToByteProcessor();
        	bp.invert();

        	ImageProcessor recol = bp;
        	if(isPseudoColour)
        		recol = ImageFilterer.recolorImage(bp, cache.getColour(k));
        	else 
        		recol = bp.convertToColorProcessor();
        	
        	recol.setMinAndMax(0, cache.getThreshold(k));
    		recoloured.add(recol);
        }

        if (selectedImageCount() == 1)
            return recoloured.get(0);

        // If multiple images are in the list, make an average of their RGB
        // values so territories can be compared
        try {
            ImageProcessor averaged = ImageFilterer.averageRGBImages(recoloured);
            if(isEnhance)
              return ImageFilterer.rescaleRGBImageIntensity(averaged, 128 ,255);
            else
            	return averaged;

        } catch (Exception e) {
            warn("Error averaging images");
            stack(e);
            return ImageFilterer.createWhiteByteProcessor(100, 100);
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
        final private Map<WarpedImageKey, ImageProcessor> map  = new ConcurrentHashMap<>(); 
        
        /** pseudo-colours for warped images */
        final private Map<WarpedImageKey, Color> imageColours  = new ConcurrentHashMap<>();
        
        /** thresholds for warped images */
        final private Map<WarpedImageKey, Integer> thresholds  = new ConcurrentHashMap<>();


        public int getThreshold(@NonNull final WarpedImageKey k){
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
        public synchronized void setThreshold(@NonNull final WarpedImageKey k, int i) {
        	thresholds.put(k, i);
        }

        public synchronized Color getColour(@NonNull final WarpedImageKey k) {
            return imageColours.get(k);
        }

        public synchronized void setColour(@NonNull final WarpedImageKey k, final Color c) {
            imageColours.put(k, c);
        }

        public synchronized void add(@NonNull final WarpedImageKey k, @NonNull final ImageProcessor ip) {
            map.put(k, ip);
        }

        public synchronized void add(@NonNull final CellularComponent target, @NonNull final String targetName, 
        		@NonNull final IAnalysisDataset template, @NonNull final UUID signalGroupId, 
        		final boolean isCellsWithSignals, final boolean binarise, final int minThreshold,
        		@NonNull final ImageProcessor ip) {
        	add(new WarpedImageKey(target, targetName, template, signalGroupId, isCellsWithSignals, binarise, minThreshold), ip);
        }

        public synchronized ImageProcessor get(@NonNull final WarpedImageKey k) {
            return map.get(k);
        }
        
        public void remove(@NonNull final WarpedImageKey k) {
        	map.remove(k);
        	imageColours.remove(k);
        	thresholds.remove(k);
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
         * @since 1.14.0
         *
         */
        public class WarpedImageKey {

        	// These will never change hashcode during normal activity
        	private final @NonNull UUID targetId;
        	private final @NonNull UUID templateId;
            private final @NonNull String targetName;
            private final @NonNull UUID  signalGroupId;
            private final boolean isOnlyCellsWithSignals;
            private final boolean isBinarise;
            private final int minThreshold;
            
        	
         // These may change hashcode due to normal activity, so should not be part of the hashed key
            private final @NonNull CellularComponent target;
            private final @NonNull IAnalysisDataset  template;


            public WarpedImageKey(@NonNull final CellularComponent target, @NonNull final String targetName, 
            		@NonNull final IAnalysisDataset template, @NonNull final UUID signalGroupId, 
            		final boolean isCellsWithSignals, final boolean binarise, final int minThreshold) {
            	targetId = target.getID();
            	templateId = template.getId();
            	
            	this.target   = target;
                this.targetName = targetName;
                this.template = template;
                this.signalGroupId = signalGroupId;
                this.isOnlyCellsWithSignals = isCellsWithSignals;
                this.isBinarise = binarise;
                this.minThreshold = minThreshold;
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
				result = prime * result + (isOnlyCellsWithSignals ? 1231 : 1237);
				result = prime * result + ((signalGroupId == null) ? 0 : signalGroupId.hashCode());
				result = prime * result + ((targetId == null) ? 0 : targetId.hashCode());
				result = prime * result + ((targetName == null) ? 0 : targetName.hashCode());
				result = prime * result + ((templateId == null) ? 0 : templateId.hashCode());
				result = prime * result + (isBinarise ? 1231 : 1237);
				result = prime * result + minThreshold;
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
				if (targetId == null) {
					if (other.targetId != null)
						return false;
				} else if (!targetId.equals(other.targetId))
					return false;
				if (targetName == null) {
					if (other.targetName != null)
						return false;
				} else if (!targetName.equals(other.targetName))
					return false;
				if (templateId == null) {
					if (other.templateId != null)
						return false;
				} else if (!templateId.equals(other.templateId))
					return false;
				if (isBinarise != other.isBinarise)
					return false;
				if (minThreshold != other.minThreshold)
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
