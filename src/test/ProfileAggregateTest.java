package test;

import static org.junit.Assert.*;
import no.components.Profile;
import no.components.ProfileAggregate;

import org.junit.Test;

public class ProfileAggregateTest {

	@Test
	public void profileShouldNotBeCreatedWithNullData(){

		Integer length = null;
		try {
			ProfileAggregate tester = new ProfileAggregate(length);
			fail("ProfileAggregate should not be created with null input");
		} catch (Exception e) {
			// expected
			// could also check for message of exception, etc.
		} 
	}
	
	@Test
	public void profileXPositionsAreCalculated(){
		
		double[] xArray   = { 5, 15, 25, 35,  45,  55, 65, 75, 85, 95 };

		Integer length = 10;

		ProfileAggregate tester = new ProfileAggregate(length);
		
		Profile xPositions = tester.getXPositions();
		
		for( int i =0;i<length; i++){
			assertEquals("Values should be identical", xArray[i], xPositions.asArray()[i],0);
		}
	}
	
	@Test
	public void addAProfileToTheAggregate(){
		
		double[] array   = { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 };
		Profile values = new Profile(array);

		ProfileAggregate tester = new ProfileAggregate(10);
		
		for( int i=0;i<50; i++){
			tester.addValues(values);
		}
		
		
		Profile median = tester.getMedian();
		
		for( int i =0;i<10; i++){
			assertEquals("Values should be identical", array[i], median.asArray()[i],0);
		}
	}

}
