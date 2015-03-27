package no.components;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.measure.Calibration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// holds e.g. the normalised and raw plots
public class ProfilePlot {
	private Map<String, Plot> collection = new HashMap<String, Plot>();
	
	public ProfilePlot(){
		
	}
	
	public void add(String s, Plot p){
		collection.put(s, p);
	}
	
	public Plot get(String s){
		return this.collection.get(s);
	}
	
	public Set<String> getKeys(){
		return this.collection.keySet();
	}
	
	public void export(String plotName, String exportName){
	    ImagePlus image = this.get(plotName).getImagePlus();
	    Calibration cal = image.getCalibration();
	    cal.setUnit("pixels");
	    cal.pixelWidth = 1;
	    cal.pixelHeight = 1;
	    IJ.saveAsTiff(image, exportName+"."+plotName+".tiff");
	  }

}
