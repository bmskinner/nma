package components.nuclear;

import components.generic.MeasurementScale;

/**
   * These are the values that we can make boxplots from
   *
   */
  public enum NucleusStatistic {
	  AREA ("Area", StatisticDimension.AREA),
	  PERIMETER("Perimeter", StatisticDimension.LENGTH),
	  MAX_FERET("Max feret", StatisticDimension.LENGTH),
	  MIN_DIAMETER("Min diameter", StatisticDimension.LENGTH),
	  ASPECT("Aspect", StatisticDimension.DIMENSIONLESS),
	  CIRCULARITY("Circularity", StatisticDimension.DIMENSIONLESS),
	  VARIABILITY("Variability", StatisticDimension.DIMENSIONLESS), 
	  BOUNDING_HEIGHT ("Bounding height", StatisticDimension.LENGTH), 
	  BOUNDING_WIDTH ("Bounding width", StatisticDimension.LENGTH);

	  private String name;
	  private StatisticDimension dimension;

	  NucleusStatistic(String name, StatisticDimension dimension){
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
			  default:
				  break;

		  }
		  return result;
	  }
	  
	  public enum StatisticDimension {
		  
		  AREA, LENGTH, DIMENSIONLESS
	  }
  }