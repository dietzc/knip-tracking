package org.knime.knip.tracking.util.adapter;

import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.util.adapter.jung.JungAdapter;

import edu.uci.ics.jung.graph.DirectedGraph;

public class JungDirectedGraphAdapter extends JungAdapter implements
		DirectedGraph<PersistentObject, PersistentObject> {

	public JungDirectedGraphAdapter(
			KPartiteGraph<PersistentObject, Partition> net) {
		super(net);
	}

	@Override
	public boolean isDirected() {
		return true;
	}

	@Override
	public boolean isNeighbor(PersistentObject v1, PersistentObject v2) {
		if (super.isNeighbor(v1, v2)) {
			return new TrackedNode(getNet(), v1).frame() < new TrackedNode(getNet(), v2)
					.frame();
		}
		return false;
	}

	@Override
	public boolean isPredecessor(PersistentObject vertex1,
			PersistentObject vertex2) {
		System.out.println("isP");
		return super.isPredecessor(vertex1, vertex2);
	}

	@Override
	public boolean isSuccessor(PersistentObject vertex1,
			PersistentObject vertex2) {
		System.out.println("isS");
		return super.isSuccessor(vertex1, vertex2);
	}

}
