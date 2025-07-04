package com.bmskinner.nma.components.signals;

import java.util.Objects;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Element;

import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * A key that allows distinction of cellular components
 * 
 * @author bms41
 *
 */
public class ShellKey implements XmlSerializable {
	private final UUID cellId;
	private final UUID componentId;
	private final UUID signalId;

	public ShellKey(@NonNull UUID cellId, @NonNull UUID componentId) {
		this(cellId, componentId, null);
	}

	public ShellKey(@NonNull UUID cellId, @NonNull UUID componentId, @Nullable UUID signalId) {

		this.cellId = cellId;
		this.componentId = componentId;
		this.signalId = signalId;
	}

	public ShellKey(Element e) throws ComponentCreationException {
		cellId = UUID.fromString(e.getAttributeValue(XMLNames.XML_SIGNAL_SHELL_CELLID));
		componentId = UUID.fromString(e.getAttributeValue(XMLNames.XML_SIGNAL_SHELL_COMPONENTID));

		if (cellId == null)
			throw new ComponentCreationException(
					"Unable to find cell id for shell key: " + e.toString());

		if (componentId == null)
			throw new ComponentCreationException(
					"Unable to find component id for shell key: " + e.toString());

		if (e.getAttributeValue(XMLNames.XML_SIGNAL_SHELL_SIGNALID) != null)
			signalId = UUID.fromString(e.getAttributeValue("SignalId"));
		else
			signalId = null;
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element(XMLNames.XML_SIGNAL_SHELL_VALUES);

		e.setAttribute(XMLNames.XML_SIGNAL_SHELL_CELLID, cellId.toString());
		e.setAttribute(XMLNames.XML_SIGNAL_SHELL_COMPONENTID, componentId.toString());

		if (signalId != null)
			e.setAttribute(XMLNames.XML_SIGNAL_SHELL_SIGNALID, signalId.toString());
		return e;
	}

	public ShellKey duplicate() {
		if (signalId == null)
			return new ShellKey(UUID.fromString(cellId.toString()),
					UUID.fromString(componentId.toString()), null);
		return new ShellKey(UUID.fromString(cellId.toString()),
				UUID.fromString(componentId.toString()), UUID.fromString(signalId.toString()));
	}

	/**
	 * Fetch the key covering the cell and component only
	 * 
	 * @return
	 */
	public ShellKey componentKey() {
		return new ShellKey(cellId, componentId);
	}

	public boolean hasCell(@NonNull UUID cellId) {
		return this.cellId.equals(cellId);
	}

	public boolean hasComponent(@NonNull UUID id) {
		return this.componentId.equals(id);
	}

	public boolean hasSignal(@NonNull UUID id) {
		return signalId != null && signalId.equals(id);
	}

	public boolean hasSignal() {
		return signalId != null;
	}

	@Override
	public String toString() {
		return signalId == null
				? cellId.toString() + "_" + componentId.toString() + "Hash: " + hashCode()
				: cellId.toString() + "_" + componentId.toString() + "_" + signalId.toString()
						+ "Hash: " + hashCode();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cellId == null) ? 0 : cellId.hashCode());
		result = prime * result + ((componentId == null) ? 0 : componentId.hashCode());
		result = prime * result + ((signalId == null) ? 0 : signalId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ShellKey other = (ShellKey) obj;
		return Objects.equals(cellId, other.cellId)
				&& Objects.equals(componentId, other.componentId)
				&& Objects.equals(signalId, other.signalId);
	}

}
