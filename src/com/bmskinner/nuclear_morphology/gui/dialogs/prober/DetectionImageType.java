package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

/**
 * Hold stages of the detection pipeline to display 
 * @since 1.13.4
 */
public enum DetectionImageType implements ImageType {
	KUWAHARA 			("Kuwahara filtering"),
	FLATTENED 			("Chromocentre flattening"),
	EDGE_DETECTION 		("Edge detection"),
	MORPHOLOGY_CLOSED 	("Gap closing"),
	DETECTED_OBJECTS 	("Detected objects");

	private String name;

	DetectionImageType(String name){
		this.name = name;
	}
	public String toString(){
		return this.name;
	}

	public ImageType[] getValues(){
		return DetectionImageType.values();
	}
	
	@Override
	public int getPosition() {
		// TODO Auto-generated method stub
		return 0;
	}
}

