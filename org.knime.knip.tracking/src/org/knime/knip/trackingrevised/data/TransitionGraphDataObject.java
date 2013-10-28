package org.knime.knip.trackingrevised.data;

import net.imglib2.type.numeric.RealType;

import org.knime.core.data.RowKey;
import org.knime.knip.trackingrevised.data.graph.TransitionGraph;

public class TransitionGraphDataObject<T extends RealType<T>> {
	private RowKey rowKey;
	private TransitionGraph<T> tg;
	private String label;
	//TODO: ImgPlus ?
	
	public TransitionGraphDataObject(final RowKey key, TransitionGraph<T> tg, String label) {
		this.rowKey = key;
		this.tg = tg;
		this.label = label;
	}
	
	public RowKey getRowKey() {
		return rowKey;
	}
	
	public TransitionGraph<T> getTransitionGraph() {
		return tg;
	}
	
	public String getLabel() {
		return label;
	}
}
