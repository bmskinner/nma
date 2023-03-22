package com.bmskinner.nma.core;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.logging.Loggable;

import net.sourceforge.argparse4j.annotation.Arg;

/**
 * Store the options provided to the program via the command line
 * 
 * @author ben
 * @since 2.1.0
 *
 */
public class CommandOptions {

	private static final Logger LOGGER = Logger.getLogger(CommandOptions.class.getName());

	// Detect arguments

	@Arg(dest = "runMode")
	public String runMode;

	@Arg(dest = "directory")
	public File directory;

	@Arg(dest = "options")
	public File options;

	// Export arguments

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

	// Analyse arguments

	@Arg(dest = "cluster-file")
	public File clusterFile;

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
		for (Field f : this.getClass().getFields()) {
			try {
				if (f.get(this) != null)
					return true;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.log(Loggable.STACK,
						"Error accessing command options: %s".formatted(e.getMessage()),
						e);
				return false;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (Field f : this.getClass().getFields()) {
			try {
				sb.append(f.getName() + ": " + f.get(this) + Io.NEWLINE);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.log(Loggable.STACK,
						"Error accessing command options: %s".formatted(e.getMessage()),
						e);
				return sb.toString();
			}
		}
		return sb.toString();
	}
}
