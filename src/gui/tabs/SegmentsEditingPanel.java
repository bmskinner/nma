package gui.tabs;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class SegmentsEditingPanel extends DetailPanel {
	
	public SegmentsEditingPanel(Logger programLogger) {
		
		super(programLogger);
		
		//TODO: Contains the median profile with selectable segments only
		
	}
	
	@Override
	public void updateDetail(){

		programLogger.log(Level.FINE, "Updating segments editing panel");
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if(getDatasets()!=null && !getDatasets().isEmpty()){

					
				}
				setUpdating(false);
			}
		});
	}

}
