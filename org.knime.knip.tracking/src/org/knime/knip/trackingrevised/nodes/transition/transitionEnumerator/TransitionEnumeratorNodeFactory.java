package org.knime.knip.trackingrevised.nodes.transition.transitionEnumerator;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TransitionEnumerator" Node. Enumerates all
 * possible transitions of a given transition graph.
 * 
 * @author Stephan Sellien
 */
public class TransitionEnumeratorNodeFactory<T extends IntegerType<T> & NativeType<T>>
		extends NodeFactory<TransitionEnumeratorNodeModel<T>> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TransitionEnumeratorNodeModel<T> createNodeModel() {
		return new TransitionEnumeratorNodeModel<T>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<TransitionEnumeratorNodeModel<T>> createNodeView(
			final int viewIndex, final TransitionEnumeratorNodeModel<T> nodeModel) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasDialog() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeDialogPane createNodeDialogPane() {
		return new TransitionEnumeratorNodeDialog();
	}

}
