package gui.components;

import java.awt.FlowLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import components.CellCollection.NucleusStatistic;

@SuppressWarnings("serial")
public class MeasurementUnitSettingsPanel extends JPanel {
	
	public JRadioButton pixelsButton  = new JRadioButton("Pixels"); 
	public JRadioButton micronsButton = new JRadioButton("Microns");
	
	public MeasurementUnitSettingsPanel(){
		this.setLayout(new FlowLayout());
		
		final ButtonGroup alignGroup = new ButtonGroup();
		alignGroup.add(pixelsButton);
		alignGroup.add(micronsButton);
		
		pixelsButton.setSelected(true);
		pixelsButton.setActionCommand("DisplayMeasurementsPixel");
		micronsButton.setActionCommand("DisplayMeasurementsMicron");

		this.add(pixelsButton);
		this.add(micronsButton);

		
	}
	
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
}
