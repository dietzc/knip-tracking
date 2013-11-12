package org.knime.knip.tracking.nodes.transition.transitiongraphbuilder;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TransitionGraphBuilder" Node.
 * 
 * 
 * @author Stephan Sellien
 * @param <T>
 */
public class TransitionGraphBuilderNodeFactory<T extends NativeType<T> & IntegerType<T>>
		extends NodeFactory<TransitionGraphBuilderNodeModel<T>> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TransitionGraphBuilderNodeModel<T> createNodeModel() {
		return new TransitionGraphBuilderNodeModel<T>();
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
	public NodeView<TransitionGraphBuilderNodeModel<T>> createNodeView(
			final int viewIndex,
			final TransitionGraphBuilderNodeModel<T> nodeModel) {
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
		return new TransitionGraphBuilderNodeDialog();
	}

}
