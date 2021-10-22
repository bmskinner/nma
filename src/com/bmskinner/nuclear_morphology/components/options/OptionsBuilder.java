package com.bmskinner.nuclear_morphology.components.options;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Builder pattern for creating options.
 * @author ben
 * @since 2.0.0
 *
 */
public class OptionsBuilder {
	
	private @NonNull HashOptions options = new DefaultOptions();
	
	/**
	 * Constructor with an internal options object
	 */
	public OptionsBuilder() { 
		// no inputs needed 
	}
	
	/**
	 * Get the options
	 * @return
	 */
	public @NonNull HashOptions build() {
		return options;
	}
	
	public OptionsBuilder setAll(HashOptions o) {
		options.set(o);
		return this;
	}
	
	public OptionsBuilder withValue(String key, int i) {
		options.setInt(key, i);
		return this;
	}
	
	public OptionsBuilder withValue(String key, double i) {
		options.setDouble(key, i);
		return this;
	}
	
	public OptionsBuilder withValue(String key, float i) {
		options.setFloat(key, i);
		return this;
	}
	
	public OptionsBuilder withValue(String key, boolean i) {
		options.setBoolean(key, i);
		return this;
	}
	
	public OptionsBuilder withValue(String key, String i) {
		options.setString(key, i);
		return this;
	}
	
	public OptionsBuilder withSubOptions(String key, HashOptions o) {
		options.setSubOptions(key, o);
		return this;
	}
	
	

}
