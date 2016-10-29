package components.nuclear;

import components.CellularComponent;

public interface INuclearSignal 
	extends CellularComponent {

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