package no.analysis;

import weka.clusterers.EM;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;


public class NucleusClusterer {


	public void cluster(){
		
		// instance holds data
		// Create empty instance with three attribute values
		Instance inst = new SparseInstance(3);

		// Set instance's values for the attributes "length", "weight", and "position"
		inst.setValue(0, 5.3);
		inst.setValue(1, 300);
		inst.setValue(2, "first");

		// Set instance's dataset to be the dataset "race"
//		inst.setDataset(race);

		// create Instances to hold Instance
		Instances instances = new Instances( (Instances)null);
		instances.add(inst);
				
		
		// create the clusterer to run on the Instances
		String[] options = new String[2];
		options[0] = "-I";                 // max. iterations
		options[1] = "100";
		try {
			EM clusterer = new EM();   // new instance of clusterer
			clusterer.setOptions(options);     // set the options
			clusterer.buildClusterer(instances);    // build the clusterer
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
