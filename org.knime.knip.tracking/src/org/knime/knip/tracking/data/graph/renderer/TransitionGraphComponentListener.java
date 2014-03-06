package org.knime.knip.tracking.data.graph.renderer;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.knip.tracking.data.TransitionGraphDataObject;

public interface TransitionGraphComponentListener<T extends NativeType<T> & IntegerType<T>> {
	public void graphEdited(TransitionGraphDataObject<T> tgdo);
}
