package com.bmskinner.nuclear_morphology.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Check for updates to the program on the Bitbucket repository
 * @author bms41
 * @since 1.15.0
 */
public class UpdateChecker {
	
	private static final String DOWNLOAD_URL = "https://api.bitbucket.org/2.0/repositories/bmskinner/nuclear_morphology/downloads/";
	private static final String NAME_PATTERN = "Nuclear_Morphology_Analysis_(\\d+\\.\\d+\\.\\d+)";
	
	/**
	 * Get the latest version of the software found in the repository
	 * @return the latest version found. Will be the current version on error.
	 */
	public static Version fetchLatestVersion() {
		Version latestVersion = Version.currentVersion();
		try {

			Pattern p = Pattern.compile(NAME_PATTERN);

			String jsonString = downloadJson();

			JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();

			JsonElement values = jsonObject.get("values");

			JsonArray array = values.getAsJsonArray();

			for(JsonElement e : array) {
				String name = e.getAsJsonObject().get("name").toString();
				Matcher m =p.matcher(name);
				if(m.find()) {
					String vString = m.group(1);
					Version v = Version.fromString(vString);
					if(v.isNewerThan(latestVersion)) {
						latestVersion = v;
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return latestVersion;
	}
	
	/**
	 * Check if an updated version of the software is available in the default
	 * repository. 
	 * @return true if an update is found, false on error, timeout or if this is the latest version
	 */
	public static boolean isUpdateAvailable() {		

		boolean isLaterVersion = false;
		
		Version latestVersion = fetchLatestVersion();
		isLaterVersion = latestVersion.isNewerThan(Version.currentVersion());

		if(isLaterVersion)
			System.out.println("Found later version: " +latestVersion );
		else
			System.out.println("Up to date: " +latestVersion );

		return isLaterVersion;
	}
	
	private static String downloadJson() throws IOException {

		String result = "";
		
		URL url = new URL(DOWNLOAD_URL);
				
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));) {

	        StringBuffer buffer = new StringBuffer();
	        int read;
	        char[] chars = new char[1024];
	        while ((read = reader.read(chars)) != -1)
	            buffer.append(chars, 0, read); 

	        result = buffer.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

}
