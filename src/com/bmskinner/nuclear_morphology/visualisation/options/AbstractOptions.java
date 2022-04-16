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
package com.bmskinner.nuclear_morphology.visualisation.options;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;

/**
 * This implements the requirements of the DisplayOptions interface, providing
 * access to options common to charts and tables.
 * 
 * @author bms41
 * @since 1.12.0
 *
 */
public abstract class AbstractOptions extends com.bmskinner.nuclear_morphology.components.options.DefaultOptions implements DisplayOptions, HashOptions {

    private final List<IAnalysisDataset>   list        = new ArrayList<>();
    private final List<Measurement> stats       = new ArrayList<>();
    
    /** A segment id */
    private UUID segID = null;
   
    /** A segment position */
    private int segPosition = 0;
    
    private MeasurementScale         scale       = MeasurementScale.PIXELS;
    private ColourSwatch             swatch      = ColourSwatch.REGULAR_SWATCH;
    private ICell                    cell        = null;

    private Aggregation agg = Aggregation.BY_NUCLEUS;
    private Normalisation norm = Normalisation.NONE;
    private ShrinkType shrinkType = ShrinkType.AREA;
    
    public static final String IS_NORMALISED_KEY = "Is normalised";
    public static final String SHOW_RECOVER_MERGE_SOURCE_KEY = "Enable recover merge sources";


    /**
     * Create with a list of datasets.
     * 
     * @param list the datasets to display
     */
    public AbstractOptions(List<IAnalysisDataset> list) {
        if (list == null) {
            return;
        }
        this.list.addAll(list);
    }

    /**
     * Set the list of datasets.
     * 
     * @param list
     *            the datasets to display
     */
    protected void setDatasets(List<IAnalysisDataset> list) {
        if (list == null)
            return;
        this.list.clear();
        this.list.addAll(list);
    }

    /**
     * Get the list of datasets.
     * 
     * @return the stored datasets
     */
    @Override
	public List<IAnalysisDataset> getDatasets() {
    	List<IAnalysisDataset> result = new ArrayList<>();
    	result.addAll(list);
        return result;
    }

    /**
     * Set the colour swatch to use in the chart or table
     * 
     * @param swatch the colour swatch
     */
    public void setSwatch(ColourSwatch swatch) {
        this.swatch = swatch;
    }

    /**
     * Get the stored colour swatch
     * 
     * @return a swatch
     */
    @Override
	public ColourSwatch getSwatch() {
        return this.swatch;
    }

    @Override
	public boolean hasDatasets() {
        return (list != null && !list.isEmpty());
    }

    @Override
    public int datasetCount() {
        return list.size();
    }

    @Override
    public boolean isSingleDataset() {
        return (list.size() == 1);
    }

    @Override
    public boolean isMultipleDatasets() {
        return (list.size() > 1);
    }

    @Override
    public IAnalysisDataset firstDataset() {
    	if(list.isEmpty())
    		return null;
        return this.list.get(0);
    }

    @Override
    public Measurement getMeasurement() {
        return stats.get(0);
    }

    /**
     * Set the first statistic in the list (for backwards compatibility)
     * 
     * @param stat
     */
    public void setStat(Measurement stat) {
        this.stats.set(0, stat);
    }

    /**
     * Replace all internal stats with the given list
     * 
     * @param stats
     */
    public void setStats(List<Measurement> stats) {
        this.stats.addAll(stats);
    }

    /**
     * Append the given stat to the end of the internal list
     * 
     * @param stat
     */
    public void addStat(Measurement stat) {
        stats.add(stat);
    }

    /**
     * Get the saved stats
     * 
     * @return
     */
    @Override
    public List<Measurement> getStats() {
        return stats;
    }

    /*
     * (non-Javadoc)
     * 
     * @see charting.options.DisplayOptions#getStat(int)
     */
    @Override
    public Measurement getStat(int index) {
        return stats.get(index);
    }

    @Override
    public UUID getSegID() {
        return segID;
    }

    /**
     * Set the segment ID
     * 
     * @param segID
     *            the ID
     */
    public void setSegID(UUID segID) {
        this.segID = segID;
    }

    @Override
    public int getSegPosition() {
        return segPosition;
    }

    /**
     * Set the segment position to display
     * 
     * @param segPosition
     *            the position, starting from 0
     */
    public void setSegPosition(int segPosition) {
        this.segPosition = segPosition;
    }

    @Override
    public MeasurementScale getScale() {
        return scale;
    }

    /**
     * Set the scale to display at. Default is pixels.
     * 
     * @param scale
     *            the measurement scale
     */
    public void setScale(MeasurementScale scale) {
        this.scale = scale;
    }

    /*
     * (non-Javadoc)
     * 
     * @see charting.options.ChartOptions#getCell()
     */
    @Override
    public ICell getCell() {
        return cell;
    }

    /**
     * Set the cell to diaplay
     * 
     * @param cell
     *            the cell
     */
    public void setCell(ICell cell) {
        this.cell = cell;
    }

    @Override
    public boolean hasCell() {
        return this.cell != null;
    }

    @Override
	public Aggregation getAggregation() {
        return agg;
    }

    public void setAggregation(Aggregation t) {
    	agg = t;
    }
    
    @Override
	public Normalisation getNormalisation() {
        return norm;
    }

    public void setNormalisation(Normalisation t) {
    	norm = t;
    }
    
    @Override
    public ShrinkType getShrinkType() {
        return shrinkType;
    }

    public void setShrinkType(ShrinkType t) {
    	shrinkType = t;
    }
    
    public boolean isNormalised(){
    	return getBoolean(IS_NORMALISED_KEY);
    }
    
    public void setNormalised(boolean b){
    	setBoolean(IS_NORMALISED_KEY, b);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        
        if(list!=null) // calculating the hashcode for the entire dataset is pointless and unnecessary
        	for(IAnalysisDataset d : list)
        		result = prime * result + ((d == null) ? 0 : d.getId().hashCode());

        result = prime * result + ((scale == null) ? 0 : scale.hashCode());
        result = prime * result + ((segID == null) ? 0 : segID.hashCode());
        result = prime * result + segPosition;
        result = prime * result + ((stats == null) ? 0 : stats.hashCode());
        result = prime * result + ((swatch == null) ? 0 : swatch.hashCode());
        result = prime * result + ((cell == null) ? 0 : cell.hashCode());
        result = prime * result + ((agg == null) ? 0 : agg.hashCode());
        result = prime * result + ((norm == null) ? 0 : norm.hashCode());
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

        AbstractOptions other = (AbstractOptions) obj;
        
        if(!super.equals(other))
        	return false;

        if (list == null) {
            if (other.list != null)
                return false;
        } else if (!list.equals(other.list))
            return false;
        if (scale != other.scale)
            return false;
        if (segID == null) {
            if (other.segID != null)
                return false;
        } else if (!segID.equals(other.segID))
            return false;
        if (segPosition != other.segPosition)
            return false;
        if (stats == null) {
            if (other.stats != null)
                return false;
        } else if (!stats.equals(other.stats))
            return false;
        if (swatch != other.swatch)
            return false;
        if (agg == null) {
            if (other.agg != null)
                return false;
        } else if (!agg.equals(other.agg))
            return false;
        if (norm == null) {
            if (other.norm != null)
                return false;
        } else if (!norm.equals(other.norm))
            return false;
        if (cell == null) {
            if (other.cell != null)
                return false;
        } else if (!cell.equals(other.cell))
            return false;
        return true;
    }
}
