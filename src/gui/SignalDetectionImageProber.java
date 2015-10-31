package gui;

import java.io.File;
import java.util.logging.Logger;

import analysis.AnalysisOptions;


@SuppressWarnings("serial")
public class SignalDetectionImageProber extends ImageProber {

	private enum SignalImageType implements ImageType {
		DETECTED_OBJECTS ("Detected objects");
		
		private String name;
		
		SignalImageType(String name){
			this.name = name;
		}
		public String toString(){
			return this.name;
		}
		
		public ImageType[] getValues(){
			SignalImageType[] a = SignalImageType.values();
			ImageType[] r = new ImageType[a.length];
			for(int i=0; i<a.length; i++){
				r[i] = a[i];
			}
			return r;
		}
	}
	
	public SignalDetectionImageProber(AnalysisOptions options, Logger logger, File folder) {
		super(options, logger, SignalImageType.DETECTED_OBJECTS, folder);

	}
	
	@Override
	protected void importAndDisplayImage(File imageFile){

	}

}
