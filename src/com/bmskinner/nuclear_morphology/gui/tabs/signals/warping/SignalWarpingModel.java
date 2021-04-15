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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.table.DefaultTableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
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
 * @since 1.19.4
 *
 */
public class SignalWarpingModel extends DefaultTableModel {
	
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(SignalWarpingModel.class.getName());

	public static final int THRESHOLD_ALL_VISIBLE = 255;
	
	
	/** Datasets currently accessible */
	private List<IAnalysisDataset> datasets;
	
	/** images currently displayed */
	final private Set<WarpedImageKey> displayImages = new LinkedHashSet<>(); 
	
	private volatile ImageCache cache = new ImageCache();
	
	public SignalWarpingModel(@NonNull List<IAnalysisDataset> datasets) {
		super();
		this.datasets = datasets;

        addColumn(Labels.Signals.Warper.TABLE_HEADER_SOURCE_DATASET, new Vector<IAnalysisDataset>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_SOURCE_SIGNALS, new Vector<ISignalGroup>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_SIGNALS_ONLY, new Vector<Boolean>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_BINARISED, new Vector<Boolean>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_TARGET_SHAPE, new Vector<String>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_THRESHOLD, new Vector<String>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_COLOUR_COLUMN, new Vector<Color>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_DELETE_COLUMN, new Vector<JButton>());
        addColumn(Labels.Signals.Warper.TABLE_HEADER_KEY_COLUMN, new Vector<WarpedImageKey>());
		addSavedImages(datasets);
	}
	
	/**
	 * Get the datasets in this model
	 * @return
	 */
	public List<IAnalysisDataset> getDatasets(){
		return datasets;
	}
	
	/**
	 * Get the index of the column with the given name
	 * @param columnName
	 * @return
	 */
	public int getColumnIndex(String columnName) {
		for(int i=0; i<this.getColumnCount(); i++) {
			if(getColumnName(i).equals(columnName))
				return i;
		}
		return -1;
	}
		
	/**
	 * Get the image key for the given row
	 * @param row
	 * @return
	 */
	public WarpedImageKey getKey(int row) {
		int keyColumn = this.getColumnIndex(Labels.Signals.Warper.TABLE_HEADER_KEY_COLUMN);
		WarpedImageKey key = (WarpedImageKey) getValueAt(row, keyColumn);
		LOGGER.fine("Selecting key "+key.toString());
		return key;
	}
	
	/**
	 * Get the row for the given image key
	 * @param key
	 * @return
	 */
	public synchronized int getRow(WarpedImageKey key) {
		int keyColumn = this.getColumnIndex(Labels.Signals.Warper.TABLE_HEADER_KEY_COLUMN);
		for(int r=0; r<getRowCount(); r++) {
			if( getValueAt(r, keyColumn).equals(key))
				return r;
		}
		return -1;
	}
	
	/**
	 * Add the given row to the selected rows
	 * @param row
	 */
	public synchronized void addSelection(int row) {
		addSelection(getKey(row));
    }
	
	/**
	 * Add the given key to the selected keys
	 * @param k
	 */
	private synchronized void addSelection(@NonNull final WarpedImageKey k) {
        displayImages.add(k);
    }

	/**
	 * Remove then given key from the selected keys
	 * @param k
	 */
	private synchronized void removeSelection(@NonNull final WarpedImageKey k) {
        displayImages.remove(k);
    }

    /**
     * Test if the given key is currently selected
     * @param k
     * @return
     */
    public synchronized boolean isSelected(@NonNull final WarpedImageKey k) {
        return displayImages.contains(k);
    }

    /**
     * Get the number of selected images
     * @return
     */
    public synchronized int selectedImageCount() {
        return displayImages.size();
    }

    /**
     * Clear the image selection 
     */
    public synchronized void clearSelection() {
        displayImages.clear();
    }
					
	/**
	 * If the given key is selected, deselect it. Otherwise select
	 * it.
	 * @param k
	 */
	public synchronized void toggleSelection(@NonNull final WarpedImageKey k) {
		if(isSelected(k))
			removeSelection(k);
		else
			addSelection(k);
	}

	
	/**
	 * Add warped images saved in datasets to this model
	 * @param list
	 */
	private void addSavedImages(@NonNull List<IAnalysisDataset> list) {
		for(IAnalysisDataset d : list) {
			addSavedImages(d);
		}
	}

	/**
	 * Add warped images saved in a dataset to the model
	 * @param d
	 */
	private void addSavedImages(@NonNull IAnalysisDataset d) {
		for(UUID signalGroupId : d.getCollection().getSignalGroupIDs()) {
			ISignalGroup sg  = d.getCollection().getSignalGroup(signalGroupId).get();

			// Get any warped signals saved in the signal group
			Optional<IWarpedSignal> ws = sg.getWarpedSignals();
			if(ws.isPresent()) {
				LOGGER.finer("Found saved warped image(s) for "+d.getName());
				IWarpedSignal warpedSignal = ws.get();
				
				// Convert the warped signals to warped image keys used internally
				// This allows decoupling of the serialised data from the display data
				for(WarpedSignalKey key : warpedSignal.getWarpedSignalKeys()) {
					LOGGER.finer("Trying to add warped image "+key);
					Optional<ImageProcessor> im = warpedSignal.getWarpedImage(key);
					
					// Check if the warped image is available 
					if(!im.isPresent()) {
						LOGGER.fine("Cannot add warped image; image not present");
						LOGGER.fine(ws.toString());
						continue;
					}
					
//					if(d.getId().equals(key.getTemplateId())) { // skip child dataset signals
						WarpedImageKey k = cache.new WarpedImageKey(key.getTargetShape(), 
								warpedSignal.getTargetName(key), d, 
								signalGroupId, 
								key.isCellWithSignalsOnly(), 
								key.isBinarised(), 
								key.getThreshold());
						cache.add(k, im.get());
						Color col = sg.getGroupColour().orElse(Color.WHITE);
						cache.setColour(k, col);
						cache.setThreshold(k, THRESHOLD_ALL_VISIBLE); // display threshold, not detection threshold
						addTableRow(k);
//					} else {
//						LOGGER.fine("Cannot add warped image; dataset ID does not match template");
//					}
				}                	
			}
		}
	}
	
	/**
	 * Get the threshold for the given row
	 * @param row
	 * @return
	 */
	public synchronized int getThreshold(int row) {
		return cache.getThreshold(getKey(row));
	}
		
	/**
	 * Set the threshold of selected rows
	 * @param threshold
	 */
	public synchronized void setThresholdOfSelected(int threshold) {
		for(WarpedImageKey k : displayImages)
			cache.setThreshold(k, threshold);;
	}
	
	/**
	 * Set the threshold for the given key
	 * @param k
	 * @param threshold
	 */
	public synchronized void setThreshold(@NonNull WarpedImageKey k, int threshold) {
		cache.setThreshold(k, threshold);;
	}
	
	
	/**
	 * Remove the row with the given key
	 * @param k
	 */
	public void removeRow(WarpedImageKey k) {
		removeSelection(k);
		cache.remove(k);
		super.removeRow(getRow(k));

	}
	
	@Override
	public void removeRow(int row) {
		WarpedImageKey k = (WarpedImageKey) this.getValueAt(row, 5);
		removeRow(k);
	}
	
	/**
	 * Add a row to the table displaying the given key
	 * @param k
	 */
	private void addTableRow(WarpedImageKey k) {
        Vector<Object> v = new Vector<>();
		v.add(k.getTemplate().getName());
        v.add(k.getSignalGroupName());
        v.add(k.isOnlyCellsWithSignals());
        v.add(k.isBinarise);
        v.add(k.getTargetName());
        v.add(k.minThreshold);
        v.add(cache.getColour(k));       
        JButton jb = new JButton("-");
        jb.addActionListener(e->removeRow(k));
        v.add(jb);
        v.add(k);
        this.addRow(v);
	}
	
	/**
	 * Update pseudocolours for all rows with the same template
	 * as the given key
	 * @param key
	 */
	public void recachePseudoColour(WarpedImageKey key) {
		
		List<WarpedImageKey> keysToUpdate = cache.getKeysForTemplate(key.template);
		
		for(WarpedImageKey k : keysToUpdate) {
			Color c = k.getTemplate().getCollection()
					.getSignalGroup(k.getSignalGroupId()).get()
					.getGroupColour().orElse(Color.WHITE);
			cache.setColour(k, c);

			int row = getRow(k);
			int col = getColumnIndex(Labels.Signals.Warper.TABLE_HEADER_COLOUR_COLUMN);
			this.setValueAt(c, row, col); 
		}
	}
	
	/**
	 * Add a warped image to the model
	 * @param targetShape the target shape to warp onto
	 * @param targetName the name of the target
	 * @param signalSource the dataset the signals came from
	 * @param signalGroupId the signal group id
	 * @param isCellsWithSignals should cells with signals only be included
	 * @param isBinarise should images be binarised before warped
	 * @param minThreshold the threshold to set before binarisation
	 * @param image the warped image
	 */
	public void addImage(@NonNull CellularComponent targetShape, 
			@NonNull String targetName, 
			@NonNull IAnalysisDataset signalSource, 
			@NonNull UUID signalGroupId,
			boolean isCellsWithSignals,
			final boolean isBinarise, 
			final int minThreshold, 
			@NonNull ImageProcessor image) {
		WarpedImageKey k = cache.new WarpedImageKey(targetShape, 
				targetName, 
				signalSource, 
				signalGroupId, 
				isCellsWithSignals, 
				isBinarise, 
				minThreshold);

        cache.add(k, image);
        Color c = signalSource.getCollection()
        		.getSignalGroup(signalGroupId).get()
        		.getGroupColour().orElse(Color.WHITE);
        cache.setColour(k, c);
        cache.setThreshold(k, THRESHOLD_ALL_VISIBLE);
        addTableRow(k);
	}
	
	/**
	 * Get the chart matching the current display criteria
	 * @return
	 */
	public synchronized JFreeChart getChart(SignalWarpingDisplaySettings displayOptions) {			

		ImageProcessor image = createDisplayImage(displayOptions);

		ChartOptionsBuilder optionsBuilder = new ChartOptionsBuilder();
		
		if(isCommonTargetSelected()) {
			LOGGER.fine("Creating image from "+displayImages.size()+" keys with common target");
			optionsBuilder.addCellularComponent(getCommonSelectedTarget());
		} else {
			LOGGER.fine("Creating image from "+displayImages.size()+" keys with different targets");
			for(WarpedImageKey k : this.getSelectedKeys()) {
				optionsBuilder.addCellularComponent(k.getTarget());
			}
		}
	
		optionsBuilder.setShowXAxis(false)
		.setShowYAxis(false)
		.setShowBounds(false);
		
        return new OutlineChartFactory(optionsBuilder.build())
        		.makeSignalWarpChart(image);
	}
		
	/**
	 * Get the image for display based on the current selection.
	 * @param isPseudocolour
	 * @param isEnhance
	 * @return
	 */
	public ImageProcessor getDisplayImage(SignalWarpingDisplaySettings options) {
		return createDisplayImage(options);
	}
	
	/**
	 * Get the image for a given key. This does not have thresholding
	 * or pseudocolouring applied.
	 * @param k the key of the image to fetch
	 * @return the raw image for the key 
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
	
	public synchronized List<IAnalysisDataset> getTemplates() {
        return cache.getTemplates();
    }
	
    /**
     * Get all the keys in the model for the given target
     * @param n the target consenus shape
     * @return
     */
    public synchronized List<WarpedImageKey> getKeys(@NonNull CellularComponent n) {
        return cache.getKeys(n);
    }
    
    /**
     * Get the keys for images currently displayed
     * @return
     */
    public synchronized List<WarpedImageKey> getSelectedKeys(){
    	return new ArrayList<>(displayImages);
    }
    
    
    /**
     * Get the target shape in common to all selected keys
     * @return
     */
    private synchronized CellularComponent getCommonSelectedTarget() {
    	if(!isCommonTargetSelected())
    		return null;
    	return displayImages.stream().findFirst().get().target;
    }
    
    /**
     * Test if all the selected visible keys have the same target
     * @return
     */
    private synchronized boolean isCommonTargetSelected() {
    	WarpedImageKey k = displayImages.stream().findFirst().get();
    	for (WarpedImageKey j : displayImages) {
    		if(!k.target.getID().equals(j.target.getID()))
    			return false;
    	}
    	return true;
    }
	
	/**
     * Create an image for display. This applies thresholding and
     * pseudocolouring.
     * 
     * @param image
     * @return
     */
    private synchronized ImageProcessor createDisplayImage(SignalWarpingDisplaySettings options) {
    	if(selectedImageCount() == 0) 
            return ImageFilterer.createWhiteByteProcessor(100, 100);

    	// Recolour each of the grey images according to the stored colours
    	List<ImageProcessor> recoloured;
    	if(isCommonTargetSelected())
    		recoloured = recolourImagesWithSameTarget(options);
    	else
    		recoloured = recolourImagesWithDifferentTargets(options);
        
    	LOGGER.fine("Found "+recoloured.size()+" images");
        if (selectedImageCount() == 1)
            return recoloured.get(0);

        // If multiple images are in the list, make an blend of their RGB
        // values so territories can be compared
        try {
        	ImageProcessor ip1 = recoloured.get(0);
        	for(int i=1; i<recoloured.size(); i++) {
        		// Weighting by fractions reduces intensity across the image
        		// Weighting by integer multiples washes the image out.
        		// Since there is little benefit to 3 or more blended, 
        		// just keep equal weighting
//        		float weight1 = i/(i+1);
//        		float weight2 = 1-weight1; 

        		ip1 = ImageFilterer.blendImages(ip1, 1, recoloured.get(i),1);
        	}
        	return ip1;
        	
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error averaging images", e);
            return ImageFilterer.createWhiteByteProcessor(100, 100);
        }
    }
    
    private synchronized List<ImageProcessor> recolourImagesWithSameTarget(SignalWarpingDisplaySettings options){
    	List<ImageProcessor> recoloured = new ArrayList<>();
        for (WarpedImageKey k : displayImages) {        	
        	// The image from the warper is greyscale. Change to use the signal colour
        	ImageProcessor raw = cache.get(k); // a short processor
        	ImageProcessor bp = raw.convertToByteProcessor();
        	bp.invert();

        	ImageProcessor recol = bp;
        	if(options.getBoolean(SignalWarpingDisplaySettings.PSEUDOCOLOUR_KEY))
        		recol = ImageFilterer.recolorImage(bp, cache.getColour(k));
        	else 
        		recol = bp.convertToColorProcessor();
        	
        	recol.setMinAndMax(0, cache.getThreshold(k));
    		recoloured.add(recol);
        }
        return recoloured;
    }
    
    private synchronized List<ImageProcessor> recolourImagesWithDifferentTargets(SignalWarpingDisplaySettings options){

    	// Ensure we keep order of keys and images consistent
    	List<WarpedImageKey> keys = new ArrayList<>(displayImages);
    	
    	List<ImageProcessor> images = ImageFilterer.fitToCommonCanvas(keys.stream()
    			.map(k->cache.get(k))
    			.collect(Collectors.toList()));
    	
    	List<ImageProcessor> recoloured = new ArrayList<>();
    	
    	for(int i=0; i<keys.size(); i++) {
    		WarpedImageKey k = keys.get(i);
    		// The image from the warper is greyscale. Change to use the signal colour
    		ImageProcessor raw = images.get(i); // a short processor
    		
    		ImageProcessor bp = raw.convertToByteProcessor();
    		bp.invert();

    		ImageProcessor recol = bp;
    		if(options.getBoolean(SignalWarpingDisplaySettings.PSEUDOCOLOUR_KEY))
    			recol = ImageFilterer.recolorImage(bp, cache.getColour(k));
    		else 
    			recol = bp.convertToColorProcessor();

    		recol.setMinAndMax(0, cache.getThreshold(k));
    		recoloured.add(recol);
    		
    	}
    	return recoloured;
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
        
        public List<IAnalysisDataset> getTemplates() {
            return map.keySet().stream().map(k ->  k.template)
            		.distinct()
            		.collect(Collectors.toList());
        }

        public List<WarpedImageKey> getKeys(@NonNull CellularComponent n) {
            return map.keySet().stream().filter(k -> k.target.getID().equals(n.getID()))
                    .collect(Collectors.toList());
        }
        
        /**
         * Get all the keys for the given source dataset
         * @param template
         * @return
         */
        public List<WarpedImageKey> getKeysForTemplate(@NonNull IAnalysisDataset template){
        	return map.keySet().stream().filter(k -> k.template.getId().equals(template.getId()))
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
            	target.alignVertically();
            	
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
			
			public String getSignalGroupName() {
				return getTemplate().getCollection().getSignalGroup(signalGroupId).get().getGroupName();
			}

			public boolean isOnlyCellsWithSignals() {
				return isOnlyCellsWithSignals;
			}
			
			public int getThreshold() {
				return minThreshold;
			}
			
			public boolean isBinarised() {
				return isBinarise;
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
