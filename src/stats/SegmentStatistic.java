package stats;

import components.generic.MeasurementScale;
import utility.Utils;

public enum SegmentStatistic implements PlottableStatistic {
	
	LENGTH("Length", StatisticDimension.LENGTH),
	DISPLACEMENT("Displacement", StatisticDimension.ANGLE);
	
	private String name;
	  private StatisticDimension dimension;

	  SegmentStatistic(String name, StatisticDimension dimension){
		  this.name = name;
		  this.dimension = dimension;
	  }

	  public String toString(){
		  return this.name;
	  }
	  
	  public boolean isDimensionless(){
		  if(this.dimension.equals(StatisticDimension.DIMENSIONLESS)){
			  return true;
		  } else {
			  return false;
		  }
	  }
	  
	  /**
	   * Get the dimension of the statistic (area, length, none)
	   * @return
	   */
	  public StatisticDimension getDimension(){
		  return this.dimension;
	  }
	  
	  /**
	   * Get the label (name and units) for the stat
	   * @return
	   */
	  public String label(MeasurementScale scale){
		  String result = "";
		  if(this.isDimensionless()){
			  result = this.toString();
		  } else {
			  result = this.toString() +" ("+ this.units(scale) + ")";
		  }
		  return result;
	  }
	  
	  /**
	   * Convert the input value (assumed to be pixels) using the given
	   * factor ( Nucleus.getScale() ) into the appropriate scale
	   * @param value the pixel measure
	   * @param factor the conversion factor to microns
	   * @param scale the desired scale
	   * @return
	   */
	  public double convert(double value, double factor, MeasurementScale scale){
		  double result = value;

		  switch(scale){
		  case MICRONS:
		  {
			  switch(this.dimension){
			  case AREA:
				  result = Utils.micronArea(value, factor);
				  break;
			  case DIMENSIONLESS:
				  break;
			  case LENGTH:
				  result = Utils.micronLength(value, factor);
				  break;
			  case ANGLE:
				  break;
			  default:
				  break;

			  }
		  }
		  break;
		  case PIXELS:
			  break;
		  default:
			  break;
		  }
		  return result;
	  }
	  
	  public String units(MeasurementScale scale){
		  String result = "";
		  switch(dimension){
	
			  case AREA:
				  result = "square "+scale.toString().toLowerCase();
				  break;
			  case DIMENSIONLESS:
				  break;
			  case LENGTH:
				  result = scale.toString().toLowerCase();
				  break;
			  case ANGLE:
				  result = "degrees";
				  break;
			  default:
				  break;

		  }
		  return result;
	  }
	  
	  public PlottableStatistic[] getValues(){
		  return SegmentStatistic.values();
	  }

}
