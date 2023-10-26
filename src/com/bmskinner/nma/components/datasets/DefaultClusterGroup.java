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
package com.bmskinner.nma.components.datasets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;

/**
 * This holds the ids of datasets created by clustering, plus the clustering
 * options that were used to generate the clusters
 *
 */
public class DefaultClusterGroup implements IClusterGroup {

	private static final long serialVersionUID = 1L;

	/** Dataset IDs in the cluster */
	private List<UUID> ids = new ArrayList<>();

	/** Options used to generate the cluster */
	private HashOptions options = null;
	private String name;
	private String newickTree = null;
	private UUID id;

	/**
	 * Create a new cluster group
	 * 
	 * @param name    the group name (informal)
	 * @param options the options used to create the cluster
	 */
	public DefaultClusterGroup(@NonNull String name, @NonNull HashOptions options,
			@NonNull UUID id) {
		this.name = name;
		this.options = options;
		this.id = id;
	}

	public DefaultClusterGroup(@NonNull Element e) {
		id = UUID.fromString(e.getAttributeValue(XMLNames.XML_ID));
		name = e.getAttributeValue(XMLNames.XML_NAME);

		if (e.getChild(XMLNames.XML_NEWICK) != null)
			newickTree = e.getChildText(XMLNames.XML_NEWICK);

		options = new DefaultOptions(e.getChild((XMLNames.XML_OPTIONS)));

		for (Element el : e.getChildren(XMLNames.XML_DATASET_ID))
			ids.add(UUID.fromString(el.getText()));

	}

	private DefaultClusterGroup(DefaultClusterGroup g) {
		ids.addAll(g.ids);
		options = g.options.duplicate();
		name = new String(g.name);
		newickTree = new String(g.newickTree);
		id = g.id;
	}

	/**
	 * Create a new cluster group with a tree
	 * 
	 * @param name    the group name (informal)
	 * @param options the options used to create the cluster
	 * @param tree    the Newick tree for the cluster as a String
	 */
	public DefaultClusterGroup(@NonNull String name, @NonNull HashOptions options,
			@NonNull String tree, @NonNull UUID id) {
		this(name, options, id);
		this.newickTree = tree;
		this.id = id;
	}

	/**
	 * Create a cluster group from a template
	 * 
	 * @param template
	 */
	public DefaultClusterGroup(@NonNull IClusterGroup template) {
		if (template.getOptions().isPresent()) {
			options = template.getOptions().get().duplicate();
		} else {
			options = null;
		}

		this.name = template.getName();
		this.newickTree = template.getTree();
		this.ids = template.getUUIDs();
		this.id = template.getId();
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element(XMLNames.XML_CLUSTER_GROUP)
				.setAttribute(XMLNames.XML_ID, id.toString())
				.setAttribute(XMLNames.XML_NAME, name);

		e.addContent(options.toXmlElement());

		if (newickTree != null)
			e.addContent(new Element(XMLNames.XML_NEWICK).setText(newickTree));

		for (UUID i : ids)
			e.addContent(new Element(XMLNames.XML_DATASET_ID).setText(i.toString()));

		return e;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String s) {
		name = s;
	}

	@Override
	public int size() {
		return this.ids.size();
	}

	@Override
	public String getTree() {
		return this.newickTree;
	}

	@Override
	public List<UUID> getUUIDs() {
		return this.ids;
	}

	@Override
	public void addDataset(final IAnalysisDataset dataset) {
		this.ids.add(dataset.getId());
	}

	@Override
	public void addDataset(final ICellCollection collection) {
		this.ids.add(collection.getId());
	}

	@Override
	public void removeDataset(final IAnalysisDataset dataset) {
		removeDataset(dataset.getId());
	}

	@Override
	public void removeDataset(final UUID id) {
		this.ids.remove(id);
	}

	@Override
	public Optional<HashOptions> getOptions() {
		return Optional.ofNullable(options);
	}

	@Override
	public boolean hasDataset(final UUID id) {
		return ids.contains(id);
	}

	@Override
	public boolean hasTree() {
		return newickTree != null;
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, ids, name, newickTree, options);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultClusterGroup other = (DefaultClusterGroup) obj;
		return Objects.equals(id, other.id) && Objects.equals(ids, other.ids)
				&& Objects.equals(name, other.name)
				&& Objects.equals(newickTree, other.newickTree)
				&& Objects.equals(options, other.options);
	}

	private void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if (id == null)
			id = UUID.randomUUID();
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	@Override
	public IClusterGroup duplicate() {
		return new DefaultClusterGroup(this);
	}

}
