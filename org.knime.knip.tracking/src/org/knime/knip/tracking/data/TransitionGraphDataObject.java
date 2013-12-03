package org.knime.knip.tracking.data;

import org.knime.core.data.RowKey;
import org.knime.knip.tracking.data.graph.TransitionGraph;

public class TransitionGraphDataObject {
	private RowKey rowKey;
	private TransitionGraph tg;
	private String label;

	// TODO: ImgPlus ?

	public TransitionGraphDataObject(final RowKey key, TransitionGraph tg,
			String label) {
		this.rowKey = key;
		this.tg = tg;
		this.label = label;
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
}
