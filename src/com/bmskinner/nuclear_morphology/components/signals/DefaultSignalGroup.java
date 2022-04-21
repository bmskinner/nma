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
package com.bmskinner.nuclear_morphology.components.signals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

/**
 * This contains information about nuclear signals within an
 * {@link ICellCollection},
 * 
 * @author bms41
 *
 */
public class DefaultSignalGroup implements ISignalGroup {

	private static final Logger LOGGER = Logger.getLogger(DefaultSignalGroup.class.getName());

	private UUID id;
	private String groupName = "";
	private boolean isVisible = true;
	private Color groupColour = null;

	private IShellResult shellResult = null;

	// Space to store warped signals from this signal group against a template
	// consensus
	private List<IWarpedSignal> warpedSignals = new ArrayList<>();

	/**
	 * Default constructor
	 */
	public DefaultSignalGroup(@NonNull String name, @NonNull UUID id) {
		this.id = id;
		groupName = name;
	}

	/**
	 * Construct from an XML element. Use for unmarshalling. The element should
	 * conform to the specification in {@link XmlSerializable}.
	 * 
	 * @param e the XML element containing the data.
	 * @throws ComponentCreationException
	 */
	public DefaultSignalGroup(@NonNull Element e) throws ComponentCreationException {
		id = UUID.fromString(e.getAttributeValue("id"));
		groupName = e.getAttributeValue("name");
		isVisible = Boolean.parseBoolean(e.getAttributeValue("isVisible"));
		groupColour = Color.decode(e.getAttributeValue("colour"));

		if (e.getChild("ShellResult") != null)
			shellResult = new DefaultShellResult(e.getChild("ShellResult"));

		for (Element e1 : e.getChildren("WarpedSignal")) {
			warpedSignals.add(new DefaultWarpedSignal(e1));
		}
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element("SignalGroup").setAttribute("id", id.toString()).setAttribute("name", groupName)
				.setAttribute("isVisible", String.valueOf(isVisible));

		if (groupColour != null)
			e = e.setAttribute("colour", String.valueOf(groupColour.getRGB()));

		if (shellResult != null)
			e = e.addContent(shellResult.toXmlElement().setAttribute("id", id.toString()));

		for (IWarpedSignal s : warpedSignals) {
			e.addContent(s.toXmlElement());
		}

		return e;
	}

	/**
	 * Construct from an existing group, duplicating the values in the template
	 * group.
	 * 
	 * @param s
	 */
	public DefaultSignalGroup(@NonNull ISignalGroup s) {

		shellResult = null;
		groupName = s.getGroupName();
		isVisible = s.isVisible();
		groupColour = s.getGroupColour().isPresent() ? s.getGroupColour().get() : null;

		for (IWarpedSignal w : s.getWarpedSignals()) {
			warpedSignals.add(w.duplicate());
		}
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public ISignalGroup duplicate() {
		return new DefaultSignalGroup(this);
	}

	@Override
	public boolean hasWarpedSignals() {
		return !warpedSignals.isEmpty();
	}

	@Override
	public List<IWarpedSignal> getWarpedSignals() {
		return warpedSignals;
	}

	@Override
	public void addWarpedSignal(@NonNull IWarpedSignal result) {
		warpedSignals.add(result);
	}

	@Override
	public Optional<IShellResult> getShellResult() {
		return Optional.ofNullable(shellResult);
	}

	@Override
	public void setShellResult(@NonNull IShellResult shellResult) {
		this.shellResult = shellResult;
	}

	@Override
	public void clearShellResult() {
		shellResult = null;
	}

	@Override
	public boolean hasShellResult() {
		return (shellResult != null);
	}

	@Override
	public String getGroupName() {
		return groupName;
	}

	@Override
	public void setGroupName(@NonNull String groupName) {
		this.groupName = groupName;
	}

	@Override
	public boolean isVisible() {
		return isVisible;
	}

	@Override
	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

	@Override
	public boolean hasColour() {
		return groupColour != null;
	}

	@Override
	public Optional<Color> getGroupColour() {
		return Optional.ofNullable(groupColour);
	}

	@Override
	public void setGroupColour(@NonNull Color groupColour) {
		this.groupColour = groupColour;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(groupName + "\n").append(groupColour + "\n").append(id.toString() + "\n").append(isVisible + "\n")
				.append(shellResult + "\n").append(warpedSignals);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = Objects.hash(groupName, id, isVisible);
		result = prime * result + ((groupColour == null) ? 0 : groupColour.hashCode());
		result = prime * result + ((shellResult == null) ? 0 : shellResult.hashCode());
		result = prime * result + ((warpedSignals == null) ? 0 : warpedSignals.hashCode());

		return result;
//		return Objects.hash(groupColour, groupName, id, isVisible, shellResult, warpedSignals);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultSignalGroup other = (DefaultSignalGroup) obj;
		return Objects.equals(groupColour, other.groupColour) && Objects.equals(groupName, other.groupName)
				&& Objects.equals(id, other.id) && isVisible == other.isVisible
				&& Objects.equals(shellResult, other.shellResult) && Objects.equals(warpedSignals, other.warpedSignals);
	}

}
