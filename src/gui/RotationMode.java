package gui;

public enum RotationMode {
	
	ACTUAL ("Actual"), VERTICAL("Vertical");
	
	private String name;
	
	private RotationMode(String name){
		this.name = name;
	}
	
	public String toString(){
		return this.name;
	}

}
