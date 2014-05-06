/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 */

package org.knime.knip.tracking.layout;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.network.core.api.GraphObjectIterator;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.PartitionType;
import org.knime.network.node.viz.viewer.VisualizationSettings;
import org.knime.network.node.viz.viewer.layout.AbstractCachedLayout;
import org.knime.network.util.adapter.jung.JungKNIMEAdapter;
import org.knime.network.util.adapter.jung.JungReadOnlyAdapter;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class MyTrackingLayout extends AbstractCachedLayout {

	KPartiteGraphView<PersistentObject, Partition> m_view;

	/**
	 * Based on grid layout for testing by Tobias Koetter, University of
	 * Konstanz.
	 * 
	 * @author tcriess, University of Konstanz
	 */
	public class PartitionLayout extends
			AbstractLayout<PersistentObject, PersistentObject> {

		/**
		 * Constructor for class GridLayout.
		 * 
		 * @param graph
		 *            the graph to layout
		 */
		protected PartitionLayout(
				final Graph<PersistentObject, PersistentObject> graph) {
			super(graph);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void reset() {
			initialize();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void initialize() {
			final Dimension d = getSize();
			if (d != null) {

				try {
					final Collection<PersistentObject> vertices = getGraph()
							.getVertices();
					if (vertices == null || vertices.isEmpty()) {
						return;
					}
					//Collection<Feature> features = m_view.getFeatures();

					Collection<Partition> partitions = m_view.getPartitions();

					ArrayList<String> nodepartitionnames = new ArrayList<String>();
					for (Partition p : partitions) {
						if (p.getType() == PartitionType.NODE) {
							nodepartitionnames.add(p.getId());
						}
					}
					Collections.sort(nodepartitionnames,
							new Comparator<String>() {
								@Override
								public int compare(String o1, String o2) {
									Integer i1 = Integer.parseInt(o1
											.substring(1));
									Integer i2 = Integer.parseInt(o2
											.substring(1));
									return i1.compareTo(i2);
								}
							});

					final int offset = 50;
//					final int width = (int) d.getWidth() - 2 * offset;
//					final int height = (int) d.getHeight() - 2 * offset;
					// have at least 10 pixel per object
					final int distance = 50; /*int temp = Math.max(
							(int) (Math.sqrt(width * height) / Math
									.sqrt(vertices.size())), 100);*/

					int i, j = 0;
					for (String pn : nodepartitionnames) {
						Partition p = m_view.getPartition(pn);
						GraphObjectIterator<PersistentObject> nodeiterator = m_view
								.getObjects(p);
						if (nodeiterator != GraphObjectIterator.EMPTY_ITERATOR
								&& nodeiterator != null) {
							i = 0;
							while (nodeiterator.hasNext()) {
								PersistentObject node = nodeiterator.next();
								final Point2D coord = transform(node);
								final int x = offset + j * distance;
								final int y = i * distance + offset;
								coord.setLocation(x, y);
								i++;
							}
							j++;
						}
					}

					/*
					 * 
					 * Partition sinks = null;
					 * 
					 * try { sinks = m_view.getPartition("sinks"); } catch
					 * (Exception e1) { // TODO Auto-generated catch block
					 * e1.printStackTrace(); } //sinks =
					 * (Partition)partitions.toArray()[0];
					 */

					/*
					 * //m_view.getFeatureString(object, feature); final int
					 * offset = 50; final int width = (int)d.getWidth() - 2 *
					 * offset; final int height = (int)d.getHeight() - 2 *
					 * offset; //have at least 10 pixel per object final int
					 * distance = Math.max( (int)(Math.sqrt(width * height) /
					 * Math.sqrt(vertices.size())), 100);
					 * List<Collection<PersistentObject>> layers = new
					 * LinkedList<Collection<PersistentObject >>();
					 * Collection<PersistentObject> sources = new
					 * LinkedHashSet<PersistentObject>(); int i = 0; int j = 0;
					 * 
					 * boolean dir = false; try { dir = m_view.isDirected(); }
					 * catch (Exception e) { // TODO Auto-generated catch block
					 * e.printStackTrace(); }
					 * 
					 * 
					 * 
					 * for (final PersistentObject v : vertices) { try {
					 * //if(m_view.getNodePartitions (v).contains(sinks)) { if
					 * (getGraph().inDegree(v)==0) { sources.add(v); final
					 * Point2D coord = transform(v); final int x = offset; final
					 * int y = i * distance + offset; coord.setLocation(x, y);
					 * i++; } else { final Point2D coord = transform(v); final
					 * int x = 3*offset; final int y = j * distance + offset;
					 * coord.setLocation(x, y); j++; } } catch (Exception e) {
					 * // TODO Auto-generated catch block e.printStackTrace(); }
					 * } layers.add(sources); Collection<PersistentObject>
					 * newlayer = new LinkedHashSet<PersistentObject>(); for
					 * (final PersistentObject v : sources) {
					 * Collection<PersistentObject> partialnewlayer =
					 * getGraph().getSuccessors(v);
					 * newlayer.addAll(partialnewlayer); }
					 * 
					 * i = 0; for (final PersistentObject v : newlayer) { final
					 * Point2D coord = transform(v); final int x = 2*offset;
					 * final int y = i * distance + offset; coord.setLocation(x,
					 * y); i++; }
					 */

					/*
					 * //have at least 10 pixel per object final int distance =
					 * Math.max( (int)(Math.sqrt(width * height) /
					 * Math.sqrt(vertices.size())), 100); final int
					 * noOfNodesPerRow = width / distance; if (noOfNodesPerRow
					 * == 0) { return; } int i = 0; for (final PersistentObject
					 * v : vertices) { final Point2D coord = transform(v); final
					 * int x = i % noOfNodesPerRow * distance + offset; final
					 * int y = i / noOfNodesPerRow * distance + offset;
					 * coord.setLocation(x, y); i++; }
					 */
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Constructor for class TestLayout.
	 * 
	 */
	public MyTrackingLayout() {
		super("MyTracking");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AbstractCachedLayout createInstance() {
		return new MyTrackingLayout();
	}

	@Override
	protected Layout<PersistentObject, PersistentObject> createLayout(
			ExecutionMonitor exec, JungReadOnlyAdapter graphAdapter) {
		m_view = graphAdapter.getView();
		return new PartitionLayout(graphAdapter);
	}

	@Override
	protected Layout<PersistentObject, PersistentObject> createLayout(
			ExecutionMonitor exec, JungKNIMEAdapter graphAdapter,
			VisualizationSettings vizSettings)
			throws CanceledExecutionException {
		m_view = graphAdapter.getView();
		return new PartitionLayout(graphAdapter);
	}
}
