import com.bmskinner.nuclear_morphology.main.Nuclear_Morphology_Analysis;

import ij.IJ;
import ij.plugin.PlugIn;

public class ImageJLoader implements PlugIn {
	
	/* 
     * The first method run when the plugin starts within ImageJ.
	 */
	public void run(String paramString){
				
		try {
			new Nuclear_Morphology_Analysis().run(paramString);

		} catch(Exception e){
			IJ.log(e.toString());
		} 
	}

}
