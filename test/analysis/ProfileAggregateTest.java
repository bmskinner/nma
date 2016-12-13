/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileAggregate;
import com.bmskinner.nuclear_morphology.components.generic.Profile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileAggregate;

public class ProfileAggregateTest {

	@Test
	public void profileShouldNotBeCreatedWithNullData(){

		Integer length = null;
		try {
			IProfileAggregate tester = new ProfileAggregate(length);
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

		IProfileAggregate tester = new ProfileAggregate(length);
		
		IProfile xPositions = tester.getXPositions();
		
		for( int i =0;i<length; i++){
			assertEquals("Values should be identical", xArray[i], xPositions.asArray()[i],0);
		}
	}
	
	@Test
	public void addAProfileToTheAggregate(){
		
		double[] array   = { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 };
		IProfile values = new Profile(array);

		IProfileAggregate tester = new ProfileAggregate(10);
		
		for( int i=0;i<50; i++){
			try {
				tester.addValues(values);
			} catch (ProfileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		IProfile median;
		try {
			median = tester.getMedian();
			
			for( int i =0;i<10; i++){
				assertEquals("Values should be identical", array[i], median.asArray()[i],0);
			}
			
		} catch (ProfileException e) {
			e.printStackTrace();
		}
		
		
	}

}
