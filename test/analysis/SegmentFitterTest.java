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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

import components.NucleusTest;
import components.SegmentedProfileTest;
import components.active.generic.SegmentedFloatProfile;
import components.generic.ISegmentedProfile;
import components.generic.Profile;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

public class SegmentFitterTest {
	
	public static ISegmentedProfile createRodentSpermMedianProfile(){
		
		// This is based on the median profile from /Testing
		float[] data = {107.4740444f, 109.8145479f, 112.1186561f, 115.5660842f, 120.5245383f, 125.8270102f, 
				131.8406741f, 136.7094901f, 141.786581f, 146.9091081f, 151.6641077f, 154.9818155f, 157.8454842f, 
				160.8319482f, 161.2958521f, 161.1592669f, 161.6305998f, 160.5378846f, 158.4281351f, 157.3296499f, 
				155.391726f, 153.1605239f, 152.1336184f, 147.9938718f, 146.0472084f, 143.9371892f, 141.565737f, 
				140.2832963f, 139.4804612f, 139.1066434f, 139.2171187f, 139.6143622f, 140.5694089f, 142.2163288f, 
				143.9061116f, 145.1650204f, 146.7199926f, 149.6106553f, 151.912988f, 154.3649661f, 155.8876104f, 
				158.4263097f, 160.6925368f, 162.5483954f, 163.7618201f, 165.0832408f, 166.075218f, 166.5656121f, 
				167.138599f, 167.5054969f, 167.6553307f, 167.8037395f, 168.0700599f, 168.1581666f, 167.7659965f, 
				167.5056897f, 167.6891683f, 167.8367536f, 168.0306533f, 168.0577956f, 167.8057135f, 168.2105555f, 
				168.1547843f, 168.0816109f, 167.8330844f, 167.8704818f, 167.9733535f, 168.2855528f, 168.722851f, 
				168.6312522f, 168.5960052f, 168.5750949f, 168.4899894f, 168.5682676f, 168.584926f, 168.5863019f, 
				168.7825292f, 169.2627271f, 169.4873638f, 169.3446441f, 169.5438167f, 169.9740198f, 169.4633729f, 
				169.3065235f, 169.28033f, 168.3685037f, 168.2512688f, 168.4212496f, 168.8013984f, 168.9702623f, 
				168.8479896f, 168.7656442f, 168.8680482f, 168.9650772f, 169.0643149f, 169.1917946f, 169.1571722f, 
				169.4861549f, 169.7729988f, 169.349675f, 169.5322296f, 169.7040292f, 170.287874f, 170.3496372f, 
				170.5520525f, 170.9377365f, 171.0973023f, 171.3356608f, 171.5493443f, 171.2822612f, 170.8114724f, 
				170.7112539f, 170.8665206f, 170.8807533f, 170.7809076f, 170.7765092f, 170.7782609f, 170.4652602f, 
				170.0349984f, 170.2572959f, 170.3086171f, 170.5096766f, 170.5107339f, 170.3199631f, 170.3230544f, 
				170.177547f, 169.9873693f, 170.1052332f, 170.3607929f, 170.2530346f, 170.5773503f, 170.8741011f, 
				170.7319807f, 170.4561327f, 170.1957261f, 170.0964576f, 170.2625528f, 170.4401576f, 170.4919084f, 
				170.5279275f, 170.3149593f, 170.9360466f, 170.9019247f, 171.065066f, 170.9613967f, 170.4067069f, 
				170.579176f, 170.7597785f, 170.9063064f, 171.1798733f, 171.4299657f, 171.6545237f, 171.3451584f, 
				171.0805824f, 171.2409207f, 171.1424132f, 170.9314859f, 170.7399355f, 170.5202081f, 170.7427033f, 
				170.8122696f, 171.1845864f, 171.0783312f, 170.8648432f, 170.5238182f, 169.8403837f, 169.1895084f, 
				168.7244993f, 168.2019442f, 168.0761284f, 167.7078615f, 167.9206699f, 167.6180489f, 166.8615422f, 
				166.5174006f, 166.266705f, 166.2887578f, 166.2826724f, 165.2586868f, 163.611485f, 161.5005334f, 
				158.282925f, 154.5459988f, 149.8425406f, 144.5008583f, 138.9019517f, 130.4657277f, 120.7605056f, 
				109.7213921f, 97.41949765f, 84.81248013f, 73.12614183f, 62.99662541f, 54.97996427f, 49.02377736f, 
				44.68215569f, 42.08193275f, 41.44396541f, 42.58396973f, 45.28452914f, 50.14119664f, 56.32942696f, 
				65.45045114f, 76.10171457f, 89.90990558f, 103.8006317f, 118.8258127f, 133.5179083f, 146.2533798f, 
				157.8972616f, 166.9157781f, 176.2194721f, 182.8536241f, 187.8689847f, 192.8664483f, 197.1768684f, 
				200.8786438f, 202.710345f, 203.561619f, 204.1168536f, 203.6031127f, 203.1780729f, 202.8112027f, 
				201.9334829f, 201.2203867f, 200.4444816f, 199.5697417f, 199.1998835f, 198.7361558f, 198.6803863f, 
				199.0592207f, 199.224618f, 199.6258148f, 201.1351532f, 202.579301f, 203.3212869f, 204.1509588f, 
				205.103319f, 206.3986952f, 207.9411827f, 209.2626599f, 209.6011969f, 209.7839489f, 209.6108514f, 
				209.0507155f, 208.0706948f, 207.8266969f, 206.1350272f, 204.3474666f, 202.4240751f, 200.5114296f, 
				198.6433001f, 196.2869158f, 193.5059843f, 191.2904668f, 189.0706379f, 186.7971334f, 185.3519193f, 
				183.9851455f, 183.385253f, 182.1299813f, 181.0756622f, 180.4744827f, 179.8667012f, 179.7937724f, 
				179.6293289f, 179.4756566f, 179.6597471f, 179.9757862f, 180.0420315f, 180.0643134f, 180.1890414f, 
				180.6746452f, 180.9979049f, 180.9249613f, 180.7753929f, 180.7208434f, 180.7062927f, 180.7057273f, 
				180.8957279f, 181.1744446f, 180.6876498f, 180.0217363f, 179.7854491f, 179.4365873f, 178.8728773f, 
				178.2982808f, 177.651942f, 177.041158f, 176.3957419f, 175.6556781f, 174.8114319f, 173.6391864f, 
				172.3419718f, 170.9228971f, 169.47057f, 168.1331674f, 166.4859145f, 164.2161962f, 161.6615876f, 
				158.7743405f, 155.7107948f, 152.8961417f, 149.8717203f, 146.1356516f, 142.6986797f, 140.0762977f, 
				138.5513093f, 137.6333857f, 137.3922704f, 137.7590539f, 138.8876138f, 141.8731826f, 145.3047292f, 
				148.6758587f, 153.3348851f, 160.2862663f, 168.7319573f, 175.5558935f, 180.1639748f, 187.0446394f, 
				193.3616628f, 199.1537607f, 202.5051176f, 205.7281863f, 207.1058256f, 208.0386157f, 206.3581701f, 
				201.7312541f, 197.8288302f, 193.0671323f, 187.0819763f, 179.6046409f, 170.9054828f, 161.7403322f, 
				152.5409506f, 143.8659958f, 135.5769287f, 127.6579614f, 121.0820828f, 115.4253148f, 111.3921922f, 
				108.4905961f, 107.0757422f, 105.2479992f};
		try {
			return new SegmentedFloatProfile(data);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static File makeLogFile(){
		File log = new File("Z:\\log.txt");
		if(!log.getParentFile().exists()){
			log = new File("E:\\log.txt");
		}
		return log;
	}
	
	/**
	 * Get a list of segments within the median profile
	 * @return
	 */
//	public static List<NucleusBorderSegment> getMedianRodentSpermSegments(){
//		Profile median = createRodentSpermMedianProfile();
//		File log = makeLogFile();
//		ProfileSegmenter segmenter = new ProfileSegmenter(median, log);
//		List<NucleusBorderSegment> segments = segmenter.segment(); 
//		return segments;
//	}

//	@Test
//	public void assignMedianToNucleus() {
//		
//		try {
//			Nucleus n = NucleusTest.createTestRodentSpermNucleus();
//
//			SegmentedProfile median = SegmentedProfileTest.createMedianProfile();
//
//			File log = makeLogFile();
//			if(log.exists()){
//				log.delete();
//			}
//
//
//			System.out.println("Beginning test");
//			System.out.println("Median profile length: "+median.size());
//
//			// get the method and make it accessible
//			Class<?>[] partypes = new Class[2];
//			partypes[0] = Nucleus.class;
//			partypes[1] = SegmentedProfile.class;
//			
//			
//			Class<?> cls = Class.forName("no.analysis.MorphologyAnalysis");
//			Method method = cls.getDeclaredMethod("assignSegmentsToNucleus", partypes);
//			method.setAccessible(true);
//
//			// Try assigning segments to the nucleus
//			method.invoke(null, n, median);
//			
//			System.out.println("Assigned segments:");
//			int length = 0;
//			for(NucleusBorderSegment seg : n.getAngleProfile().getSegments()){
//				seg.print();
//				assertEquals("Endpoints should be linked", seg.getEndIndex(), seg.nextSegment().getStartIndex());
//				assertTrue(seg.hasNextSegment());
//				assertTrue(seg.hasPrevSegment());
//				
//				length += seg.length();
//				
//			}
//			assertEquals("Lengths should match", n.getLength(), length);
//			
//			System.out.println("Running fitter");
//			long startTime = System.currentTimeMillis();
////			SegmentFitter fitter = new SegmentFitter(median, log);
//			
////			fitter.fit(n, null);
//			
//			long endTime = System.currentTimeMillis();
//			long time = endTime - startTime;
//			System.out.println("Fitting took "+time+" milliseconds");
//			length = 0;
//			for(NucleusBorderSegment seg : n.getAngleProfile().getSegments()){
//				seg.print();
//				assertEquals("Endpoints should be linked", seg.getEndIndex(), seg.nextSegment().getStartIndex());
//				assertTrue(seg.hasNextSegment());
//				assertTrue(seg.hasPrevSegment());
//				
//				length += seg.length();
//			}
//			assertEquals("Lengths should match", n.getLength(), length);
//
//		} catch (ClassNotFoundException e) {
//			fail("Class error");
//			e.printStackTrace();
//		} catch (SecurityException e) {
//			fail("Security error");
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			fail("Access error");
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//			fail("Argument error");
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//			fail("Invocation error");
//		} catch (NoSuchMethodException e) {
//			e.printStackTrace();
//			fail("Method error");
//		} catch (Exception e){
//			e.printStackTrace();
//			fail("Other error");
//		}
//
//	}

}
