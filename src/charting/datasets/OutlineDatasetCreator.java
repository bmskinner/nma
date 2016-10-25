/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package charting.datasets;

import java.util.List;

import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import analysis.profiles.Taggable;
import components.CellularComponent;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import components.nuclei.sperm.RodentSpermNucleus;

public class OutlineDatasetCreator extends AbstractDatasetCreator {
	
	private final CellularComponent component;
	
	public OutlineDatasetCreator(final CellularComponent c){
		
		if(c==null){
			throw new IllegalArgumentException("Component cannot be null");
		}
		
		component = c;
	}
	
	/**
	 * Create a dataset with the outline of the current object. 
	 * @param segmented should the outline be segmented (where segments are available)
	 * @return an XYDataset with the outline. Segments will be as separate series if segmented is true.
	 * @throws ChartDatasetCreationException
	 */
	public XYDataset createOutline(boolean segmented) throws ChartDatasetCreationException {
		
		if(segmented){
			return createSegmentedOutline();
		} else {
			return createNonSegmentedOutline();
		}
		
//		if(component instanceof Nucleus ){
//			return createNucleusOutline((Nucleus) component, segmented);
//		}
//		
//		throw new ChartDatasetCreationException("Cannot make outline for this component type");
		
	}
	
	/**
	 * Create a dataset with the outline of the current object. 
	 * @param ds the dataset the data should be added to
	 * @param segmented should the outline be segmented (where segments are available)
	 * @return an XYDataset with the outline. Segments will be as separate series if segmented is true.
	 * @throws ChartDatasetCreationException
	 */
	public XYDataset createOutline(ComponentOutlineDataset ds, Comparable seriesKey, boolean segmented) throws ChartDatasetCreationException {

		if(segmented){
			return createSegmentedOutline(ds);
		} else {
			ds = (ComponentOutlineDataset) createNonSegmentedOutline(ds, seriesKey);
			return ds;
		}
//		
//		
//		
//		throw new ChartDatasetCreationException("Cannot make outline for this component type");
		
	}
			
	/**
	 * Get the outline for a specific nucleus in a dataset. Sets the position
	 * to the original coordinates in the image 
	 * @param cell
	 * @param segmented true to include each segment separately, false for an unsegmented outline
	 * @return
	 * @throws ChartDatasetCreationException 
	 */
	private XYDataset createNucleusOutline(Nucleus nucleus, boolean segmented) throws ChartDatasetCreationException {
		ComponentOutlineDataset ds = new ComponentOutlineDataset();
		finest("Creating nucleus outline");
		if(segmented){
			createSegmentedOutline();

		} else {
			return createNonSegmentedOutline();

		}		
		return ds;
	}
	
	/**
	 * Get the outline for a specific nucleus in a dataset. Sets the position
	 * to the original coordinates in the image 
	 * @param cell
	 * @param segmented true to include each segment separately, false for an unsegmented outline
	 * @return
	 * @throws ChartDatasetCreationException 
	 */
	private XYDataset createSegmentedOutline() throws ChartDatasetCreationException {
		ComponentOutlineDataset ds = new ComponentOutlineDataset();
		return createSegmentedOutline(ds);
	}
	
	/**
	 * Get the outline for the loaded component and add it to the given dataset. Sets the position
	 * to the original coordinates in the image. 
     * @param ds the chart dataset to add the data to
	 * @return
	 * @throws ChartDatasetCreationException 
	 */
	private XYDataset createSegmentedOutline(ComponentOutlineDataset ds) throws ChartDatasetCreationException {
				
		if( ! (component instanceof Taggable)){
			throw new ChartDatasetCreationException("Component is not segmentable");
		}
		
		finest("Creating segmented outline");
		
		Taggable t = (Taggable) component;

		List<NucleusBorderSegment> segmentList = t.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT).getSegments();


		if(!segmentList.isEmpty()){ // only draw if there are segments
			finest("Nucleus has "+segmentList.size()+" segments");

			for(NucleusBorderSegment seg  : segmentList){

				finest("Drawing segment "+seg.getID());


				// If we make the array the length of the segment, 
				// there will be a gap between the segment end and the
				// next segment start. Include a position for the next
				// segment start as well
				double[] xpoints = new double[seg.length()+1];
				double[] ypoints = new double[seg.length()+1];


				int segmentPosition = seg.getPosition();

				for(int j=0; j<=seg.length();j++){

					int index = seg.getStartIndex()+j;
					int offsetIndex = t.getOffsetBorderIndex(BorderTagObject.REFERENCE_POINT, index);

					BorderPoint p = t.getBorderPoint(offsetIndex); // get the border points in the segment

					xpoints[j] = p.getX()-0.5;
					ypoints[j] = p.getY()-0.5;
				}

				double[][] data = { xpoints, ypoints };

				String seriesKey = "Seg_"+segmentPosition;
				ds.addSeries(seriesKey, data);
				ds.setComponent(seriesKey, component);
				finest("Added segment data to chart dataset");
			}
		} else {
			finest("Component does not have segments; falling back to bare outline");
			return createNonSegmentedOutline();
		}


		return ds;
	}
	
	
	/**
	 * Create a non-segmented outline and add it to a new dataset
	 * @param ds
	 * @return
	 */
	private XYDataset createNonSegmentedOutline(){
		ComponentOutlineDataset ds = new ComponentOutlineDataset();
		
		String seriesKey = chooseSeriesKey();
		
		return createNonSegmentedOutline(ds, seriesKey);
	}
	
	
	/**
	 * Create a non-segmented outline and add it to the given dataset
	 * @param ds
	 * @return
	 */
	private XYDataset createNonSegmentedOutline(ComponentOutlineDataset ds, Comparable seriesKey){
		finest("Creating non-segmented outline from component");
		
		double[] xpoints = new double[component.getOriginalBorderList().size()];
		double[] ypoints = new double[component.getOriginalBorderList().size()];

		int i =0;
		for(XYPoint p : component.getBorderList()){
						
			xpoints[i] = p.getX()-0.5;
			ypoints[i] = p.getY()-0.5;
			i++;
		}

		double[][] data = { xpoints, ypoints };

		finest("Adding series for component border centred on "+component.getCentreOfMass().toString());
		ds.addSeries(seriesKey, data);
		ds.setComponent(seriesKey, component);
		return ds;
	}
	
	private String chooseSeriesKey(){
		String seriesKey = component.getID().toString();
		
		if (component instanceof Nucleus){
			seriesKey = ((Nucleus) component).getNameAndNumber();
		}
		return seriesKey;
	}
	
	
	/**
	 * Create a dataset with the hook and hump rois for a rodent sperm nucleus. If the
	 * given cell does not contain a rodent sperm nucleus, the returned dataset is empty
	 * @param cell
	 * @return
	 * @throws ChartDatasetCreationException 
	 */
	public XYDataset createNucleusHookHumpOutline() throws ChartDatasetCreationException {
		
		DefaultXYDataset ds = new DefaultXYDataset();
		
		if(  ! (component instanceof Nucleus) ){
			throw new ChartDatasetCreationException("Component is not a Nucleus");
		}
		
		if(  ! (component instanceof RodentSpermNucleus) ){
			throw new ChartDatasetCreationException("Component is not a rodent sperm nucleus");
		}
					
		RodentSpermNucleus nucleus = (RodentSpermNucleus) component;

		double[] xpoints = new double[nucleus.getHookRoi().size()];
		double[] ypoints = new double[nucleus.getHookRoi().size()];

		int i =0;
		for(XYPoint p : nucleus.getHookRoi()){
			xpoints[i] = p.getX()-0.5;
			ypoints[i] = p.getY()-0.5;
			i++;
		}

		double[][] data = { xpoints, ypoints };
		ds.addSeries("Hook", data);


		return ds;
	}

}
