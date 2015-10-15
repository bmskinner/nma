package components.generic;

import components.nuclear.NucleusStatistic;

public enum MeasurementScale {
	
	PIXELS ("Pixels"),
	MICRONS ("Microns");
	
	private String name;
	
	MeasurementScale(String name){
		this.name = name;
	}
	
	public String toString(){
		return this.name;
	}

	/**
	 * Get the appropriate chart y-label for the
	 * given statistic 
	 * @param stat
	 * @return
	 */
	public String yLabel(NucleusStatistic stat){
		String result = null;

		switch(stat){

		case AREA: 
			result = "Square "+name;
			break;
		case PERIMETER: 
			result = name;
			break;
		case MAX_FERET: 
			result = name;
			break;
		case MIN_DIAMETER: 
			result = name;
			break;
		case ASPECT: 
			result = "Aspect ratio (feret / min diameter)";
			break;
		case CIRCULARITY: 
			result = "Circularity";
			break;
		case VARIABILITY: 
			result = "Degrees per perimeter unit";
			break;
		}

		return result;
	}

}