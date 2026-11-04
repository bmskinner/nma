package com.bmskinner.nma.core;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bmskinner.nma.io.Io;

import net.sourceforge.argparse4j.annotation.Arg;

/**
 * Store the options provided to the program via the command line
 * 
 * @author Ben Skinner
 * @since 2.1.0
 *
 */
public class CommandOptions {

	private static final Logger LOGGER = Logger.getLogger(CommandOptions.class.getName());

	// Stores the subcommand (detect, export etc.)
	@Arg(dest = "runMode")
	public String runMode;

	// Detect arguments

	@Arg(dest = "directory")
	public File directory;

	@Arg(dest = "outline-file")
	public File outlineFile;

	// Used in Detect and Modify
	@Arg(dest = "options")
	public File options;

	// Export arguments

	// Export and modify: path to input nmd file
	@Arg(dest = "file")
	public File file;

	@Arg(dest = "measurements")
	public boolean isMeasurements = false;

	@Arg(dest = "profiles")
	public boolean isProfiles = false;

	@Arg(dest = "outlines")
	public boolean isOutlines = false;

	@Arg(dest = "signals")
	public boolean isSignals = false;

	@Arg(dest = "shells")
	public boolean isShells = false;

	@Arg(dest = "single-cell-images")
	public boolean isSingleCellImages = false;

	@Arg(dest = "analysis-options")
	public boolean isAnalysisOptions = false;

	@Arg(dest = "rulesets")
	public boolean isRulesets = false;

	@Arg(dest = "all")
	public boolean isAll = false;

	@Arg(dest = "consensus")
	public boolean isConsensus = false;

	@Arg(dest = "cell-locations")
	public boolean isCellLocations = false;

	// Modify arguments

	@Arg(dest = "cluster-file")
	public File clusterFile;

	// Merge

	// Corresponds to --input on cli
	@Arg(dest = "merge-sources")
	public List<File> mergeSources;

	@Arg(dest = "output")
	public File output;

	public CommandOptions() {
		// no data needed
	}

	/**
	 * Test if any of the options have been specified
	 * 
	 * @return
	 */
	public boolean hasOptions() {
		for (final Field f : this.getClass().getFields()) {
			try {
				if (f.get(this) != null)
					return true;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.log(Level.SEVERE,
						"Error accessing command options: %s".formatted(e.getMessage()),
						e);
				return false;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		for (final Field f : this.getClass().getFields()) {
			try {

				if (f.get(this) == null) {
					continue;
				}

				sb.append(f.getName() + ": " + f.get(this) + Io.NEWLINE);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.log(Level.SEVERE,
						"Error accessing command options: %s".formatted(e.getMessage()),
						e);
				return sb.toString();
			}
		}
		return sb.toString();
	}
}
