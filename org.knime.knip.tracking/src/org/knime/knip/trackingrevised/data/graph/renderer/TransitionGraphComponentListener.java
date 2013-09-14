package org.knime.knip.trackingrevised.data.graph.renderer;

import org.knime.knip.trackingrevised.data.TransitionGraphDataObject;

public interface TransitionGraphComponentListener {
	public void graphEdited(TransitionGraphDataObject tgdo);
}
