package org.knime.knip.tracking.data.graph.renderer;

import org.knime.knip.tracking.data.TransitionGraphDataObject;

public interface TransitionGraphComponentListener {
	public void graphEdited(TransitionGraphDataObject tgdo);
}
