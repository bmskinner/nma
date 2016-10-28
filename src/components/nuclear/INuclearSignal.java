package components.nuclear;

import components.CellularComponent;
import components.Rotatable;

public interface INuclearSignal 
	extends CellularComponent,
			Rotatable {

	/**
	 * Get the index of the closest point in the nuclear 
	 * periphery to this signal
	 * @return
	 */
	int getClosestBorderPoint();

	/**
	 * Set the index of the closest point in the nuclear 
	 * periphery to this signal
	 * @return
	 */
	void setClosestBorderPoint(int p);

	INuclearSignal duplicate();

}