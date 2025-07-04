/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.components.options;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.io.Io;

/**
 * The default implementation of the IAnalysisOptions interface
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultAnalysisOptions implements IAnalysisOptions {

	/** Store the options used to detect objects */
	private Map<String, HashOptions> detectionOptions = new HashMap<>();

	/** Store the folders used to detect objects - keys mirror detectionOptions */
	private Map<String, File> detectionFolders = new HashMap<>();

	private RuleSetCollection rulesets;

	private long analysisTime;

	/*
	 * Store options that are not detection options. For example, clustering or tSNE
	 * options
	 */
	private Map<String, HashOptions> secondaryOptions = new HashMap<>();

	/**
	 * The default constructor, which sets default options specified in
	 * IAnalysisOptions
	 */
	public DefaultAnalysisOptions() {
		analysisTime = System.currentTimeMillis();

		HashOptions profilingOptions = new DefaultOptions();
		profilingOptions.setBoolean(HashOptions.IS_SEGMENT_PROFILES,
				HashOptions.DEFAULT_IS_SEGMENT_PROFILES);

		profilingOptions.setDouble(HashOptions.PROFILE_WINDOW_SIZE,
				HashOptions.DEFAULT_PROFILE_WINDOW);

		secondaryOptions.put(IAnalysisOptions.PROFILING_OPTIONS, profilingOptions);
	}

	/**
	 * Construct from a template options
	 * 
	 * @param template the options to use as a template
	 */
	public DefaultAnalysisOptions(@NonNull IAnalysisOptions template) {
		// Set all options except for detection folders
		set(template);

		// Copy the detection folders
		for (String key : template.getDetectionOptionTypes()) {
			Optional<File> file = template.getDetectionFolder(key);
			if (file.isPresent()) {
				setDetectionFolder(key, file.get());
			}
		}

		analysisTime = template.getAnalysisTime();
	}

	public DefaultAnalysisOptions(@NonNull Element e) throws ComponentCreationException {

		// Add the detection folders
		for (Element i : e.getChildren(XMLNames.XML_FOLDER))
			detectionFolders.put(i.getAttributeValue(XMLNames.XML_NAME),
					new File(i.getText()));

		// Add the detection options
		for (Element i : e.getChildren(XMLNames.XML_DETECTION)) {
			String name = i.getAttributeValue(XMLNames.XML_NAME);
			detectionOptions.put(name,
					new DefaultOptions(i.getChild(XMLNames.XML_OPTIONS)));

			if (i.getAttribute(XMLNames.XML_FOLDER) != null)
				detectionFolders.put(name,
						new File(i.getAttributeValue(XMLNames.XML_FOLDER)));
		}

		if (e.getAttribute(XMLNames.XML_ANALYSIS_TIME) != null)
			analysisTime = Long.valueOf(e.getAttributeValue(XMLNames.XML_ANALYSIS_TIME));
		else
			analysisTime = System.currentTimeMillis(); // if no time set, assume it's new

		rulesets = new RuleSetCollection(e.getChild(XMLNames.XML_RULE_SET_COLLECTION));

		// Add the secondary options
		for (Element i : e.getChildren(XMLNames.XML_SECONDARY))
			secondaryOptions.put(i.getAttributeValue(XMLNames.XML_NAME),
					new DefaultOptions(i.getChild(XMLNames.XML_OPTIONS)));

		// Legacy: if the profile window is in the top level, move to secondary options
		if (e.getAttribute(XMLNames.XML_PROFILE_WINDOW) != null) {
			HashOptions profilingOptions = secondaryOptions.computeIfAbsent(PROFILING_OPTIONS,
					k -> new DefaultOptions());
			profilingOptions.setDouble(HashOptions.PROFILE_WINDOW_SIZE,
					Double.valueOf(e.getAttributeValue(XMLNames.XML_PROFILE_WINDOW)));
		}
	}

	@Override
	public IAnalysisOptions duplicate() {
		return new DefaultAnalysisOptions(this);
	}

	@Override
	public Optional<HashOptions> getDetectionOptions(String key) {
		if (detectionOptions.containsKey(key)) {
			return Optional.of(detectionOptions.get(key));
		}
		return Optional.empty();
	}

	@Override
	public HashOptions getProfilingOptions() {
		return secondaryOptions.get(PROFILING_OPTIONS);
	}

	@Override
	public Optional<File> getDetectionFolder(@NonNull String key) {
		if (detectionFolders.containsKey(key)) {
			return Optional.of(detectionFolders.get(key));
		}
		return Optional.empty();
	}

	@Override
	public Optional<File> getNucleusDetectionFolder() {
		return getDetectionFolder(CellularComponent.NUCLEUS);
	}

	@Override
	public void removeDetectionFolder(@NonNull String key) {
		detectionFolders.remove(key);
	}

	@Override
	public void setNucleusDetectionFolder(@NonNull File folder) {
		setDetectionFolder(CellularComponent.NUCLEUS, folder);
	}

	@Override
	public void setDetectionFolder(@NonNull String key, @NonNull File folder) {
		detectionFolders.put(key, folder);
	}

	@Override
	public Set<String> getDetectionOptionTypes() {
		return detectionOptions.keySet();
	}

	@Override
	public RuleSetCollection getRuleSetCollection() {
		return rulesets;
	}

	@Override
	public void setRuleSetCollection(@NonNull RuleSetCollection rsc) {
		rulesets = rsc;
	}

	@Override
	public Optional<HashOptions> getNucleusDetectionOptions() {
		return getDetectionOptions(CellularComponent.NUCLEUS);
	}

	@Override
	public boolean hasDetectionOptions(@NonNull String type) {
		return detectionOptions.containsKey(type);
	}

	@Override
	public Optional<HashOptions> getSecondaryOptions(@NonNull String key) {
		if (secondaryOptions.containsKey(key)) {
			return Optional.of(secondaryOptions.get(key));
		}
		return Optional.empty();
	}

	@Override
	public Set<String> getSecondaryOptionKeys() {
		return secondaryOptions.keySet();
	}

	@Override
	public boolean hasSecondaryOptions(@NonNull String key) {
		return secondaryOptions.containsKey(key);
	}

	@Override
	public double getProfileWindowProportion() {
		return secondaryOptions.get(PROFILING_OPTIONS).getDouble(HashOptions.PROFILE_WINDOW_SIZE);
	}

	@Override
	public Set<UUID> getNuclearSignalGroups() {
		Set<UUID> result = new HashSet<>();
		for (String s : detectionOptions.keySet()) {
			try {
				if (s.startsWith(SIGNAL_GROUP)) {
					UUID id = UUID.fromString(s.replace(SIGNAL_GROUP, ""));
					result.add(id);
				}
			} catch (IllegalArgumentException e) {
				// not a UUID
			}
		}
		return result;
	}

	@Override
	public Optional<HashOptions> getNuclearSignalOptions(@NonNull UUID signalGroup) {
		return getDetectionOptions(SIGNAL_GROUP + signalGroup.toString());
	}

	@Override
	public boolean hasNuclearSignalDetectionOptions(@NonNull UUID signalGroup) {
		return hasDetectionOptions(SIGNAL_GROUP + signalGroup.toString());
	}

	@Override
	public void setNuclearSignalDetectionOptions(@NonNull HashOptions options) {
		setDetectionOptions(SIGNAL_GROUP + options.getString(HashOptions.SIGNAL_GROUP_ID), options);
	}

	@Override
	public void setNuclearSignalDetectionFolder(@NonNull UUID id, @NonNull File folder) {
		setDetectionFolder(SIGNAL_GROUP + id.toString(), folder);
	}

	@Override
	public Optional<File> getNuclearSignalDetectionFolder(@NonNull UUID id) {
		return getDetectionFolder(SIGNAL_GROUP + id.toString());
	}

	@Override
	public long getAnalysisTime() {
		return analysisTime;
	}

	@Override
	public void clearAnalysisTime() {
		analysisTime = -1;
	}

	@Override
	public void setDetectionOptions(@NonNull String key, @NonNull HashOptions options) {
		detectionOptions.put(key, options);
	}

	@Override
	public void setSecondaryOptions(@NonNull String key, @NonNull HashOptions options) {
		secondaryOptions.put(key, options);
	}

	@Override
	public void setAngleWindowProportion(double proportion) {
		secondaryOptions.get(PROFILING_OPTIONS).setDouble(HashOptions.PROFILE_WINDOW_SIZE,
				proportion);

	}

	@Override
	public void set(@NonNull IAnalysisOptions template) {
		for (String key : template.getDetectionOptionTypes()) {
			Optional<HashOptions> op = template.getDetectionOptions(key);
			if (op.isPresent()) {
				setDetectionOptions(key, op.get().duplicate());
			}
		}

		for (String key : template.getSecondaryOptionKeys()) {
			Optional<HashOptions> op = template.getSecondaryOptions(key);
			if (op.isPresent()) {
				setSecondaryOptions(key, op.get().duplicate());
			}
		}

		rulesets = template.getRuleSetCollection().duplicate();
	}

	@Override
	public int hashCode() {
		return Objects.hash(detectionOptions, detectionFolders, rulesets,
				secondaryOptions);
	}

	/**
	 * Note that we don't include analysis time in equality comparison since it is
	 * necessarily different and not informative
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultAnalysisOptions other = (DefaultAnalysisOptions) obj;
		return Objects.equals(detectionOptions, other.detectionOptions)
				&& Objects.equals(rulesets, other.rulesets)
				&& Objects.equals(detectionFolders, other.detectionFolders)
				&& Objects.equals(secondaryOptions, other.secondaryOptions);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("Analysis options" + Io.NEWLINE);
		b.append("Run at: " + analysisTime + Io.NEWLINE);
		for (Entry<String, HashOptions> e : detectionOptions.entrySet()) {
			b.append(e.getKey() + Io.NEWLINE);
			b.append(detectionFolders.get(e.getKey()) + Io.NEWLINE);
			b.append(e.getValue().toString());
		}
		b.append(Io.NEWLINE + rulesets.getName());
		return b.toString();
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element(XMLNames.XML_ANALYSIS_OPTIONS);

		if (analysisTime > -1)
			e.setAttribute(XMLNames.XML_ANALYSIS_TIME, String.valueOf(analysisTime));

		e.addContent(rulesets.toXmlElement());

		// Add the detection options, including folder if set
		for (Entry<String, HashOptions> entry : detectionOptions.entrySet()) {
			Element el = new Element(XMLNames.XML_DETECTION)
					.setAttribute(XMLNames.XML_NAME, entry.getKey());
			if (detectionFolders.get(entry.getKey()) != null) {
				el.setAttribute(XMLNames.XML_FOLDER,
						detectionFolders.get(entry.getKey()).getAbsolutePath());
			}
			el.addContent(entry.getValue().toXmlElement());
			e.addContent(el);
		}

		// Add the secondary options
		for (Entry<String, HashOptions> entry : secondaryOptions.entrySet()) {
			e.addContent(new Element(XMLNames.XML_SECONDARY)
					.setAttribute(XMLNames.XML_NAME, entry.getKey())
					.addContent(entry.getValue().toXmlElement()));
		}

		return e;
	}

}
