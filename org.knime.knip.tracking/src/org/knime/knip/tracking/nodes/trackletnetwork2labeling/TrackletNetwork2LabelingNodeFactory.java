package org.knime.knip.tracking.nodes.trackletnetwork2labeling;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;

/**
 * <code>NodeFactory</code> for the "TrackletNetwork2Labeling" Node. Extracts
 * the tracklets (parts of trajectories) of a tracking network and creates a
 * labeling image.
 * 
 * @author Stephan Sellien
 */
public class TrackletNetwork2LabelingNodeFactory extends
		NodeFactory<TrackletNetwork2LabelingNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TrackletNetwork2LabelingNodeModel createNodeModel() {
		return new TrackletNetwork2LabelingNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<TrackletNetwork2LabelingNodeModel> createNodeView(
			final int viewIndex,
			final TrackletNetwork2LabelingNodeModel nodeModel) {
		return new TableCellViewNodeView<TrackletNetwork2LabelingNodeModel>(
				nodeModel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasDialog() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeDialogPane createNodeDialogPane() {
		return null;
	}

}
