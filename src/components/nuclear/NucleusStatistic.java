package components.nuclear;

/**
   * These are the values that we can make boxplots from
   *
   */
  public enum NucleusStatistic {
	  AREA ("Area"),
	  PERIMETER("Perimeter"),
	  MAX_FERET("Max feret"),
	  MIN_DIAMETER("Min diameter"),
	  ASPECT("Aspect"),
	  CIRCULARITY("Circularity"),
	  VARIABILITY("Variability");

	  private String name;

	  NucleusStatistic(String name){
		  this.name = name;
	  }

	  public String toString(){
		  return this.name;
	  }
  }