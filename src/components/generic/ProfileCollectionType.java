package components.generic;

public enum ProfileCollectionType { 
	  REGULAR ("Regular profile"), 
	  FRANKEN ("Franken profile");
	  
	  private String name;
	  	  
	  ProfileCollectionType(String name){
		  this.name = name;
	  }
	  
	  public String toString(){
		  return this.name;
	  }
  }