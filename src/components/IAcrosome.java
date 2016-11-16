package components;

public interface IAcrosome extends CellularComponent, Comparable<IAcrosome> {

	void alignVertically();

	IAcrosome duplicate();

}