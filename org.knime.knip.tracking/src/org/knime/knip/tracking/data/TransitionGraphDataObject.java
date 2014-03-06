package org.knime.knip.tracking.data;

import java.util.LinkedList;
import java.util.List;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.core.data.RowKey;
import org.knime.core.util.Pair;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;

public class TransitionGraphDataObject<T extends NativeType<T> & IntegerType<T>> {
	private RowKey rowKey;
	private TransitionGraph tg;
	private String label;
	// edges added in gui in current iterations. Needed to make reset possible in network.
	private List<Pair<TrackedNode, TrackedNode>> possibleEdges = new LinkedList<Pair<TrackedNode, TrackedNode>>();

	private ImgPlus<T> img;

	public TransitionGraphDataObject(final RowKey key, TransitionGraph tg,
			String label, ImgPlus<T> img) {
		this.rowKey = key;
		this.tg = tg;
		this.label = label;
		this.img = img;
	}

	public RowKey getRowKey() {
		return rowKey;
	}

	public TransitionGraph getTransitionGraph() {
		return tg;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public List<Pair<TrackedNode, TrackedNode>> getPossibleEdges() {
		return possibleEdges;
	}
	
	public ImgPlus<T> getImgPlus() {
		return img;
	}
}
