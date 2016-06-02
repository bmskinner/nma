package analysis.profiles;


/**
 * An instruction for finding an index in a profile
 * @author bms41
 *
 */
public class Rule {

	private RuleType type;
	private double   value;

	public Rule(RuleType type, double value){

		this.type = type;
		this.value = value;
	}

	public Rule(RuleType type, boolean value){

		this.type = type;
		this.value = value ? 1d : 0d;
	} 

	public double getValue(){
		return value;
	}

	public boolean getBooleanValue(){
		if(value==1d){
			return true;
		} else {
			return false;
		}
	}

	public int getIntValue(){
		return (int) value;
	}
	
	public RuleType getType(){
		return type;
	}
	
	public String toString(){
		return type+" : "+value;
	}
	
	/**
	 * A type of instruction to follow
	 * @author bms41
	 *
	 */
	public enum RuleType{

		IS_MINIMUM,
		IS_MAXIMUM,
		
		IS_LOCAL_MINIMUM,
		IS_LOCAL_MAXIMUM,

		VALUE_IS_LESS_THAN,
		VALUE_IS_MORE_THAN,
		
		INDEX_IS_LESS_THAN,
		INDEX_IS_MORE_THAN;

	}
}