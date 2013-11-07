package org.knime.knip.tracking.nodes.trackletcreator;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TrackletCreator" Node. Creates Tracklets
 * (parts of a trajectory) out of a given segmentation as graphs.
 * 
 * @author Stephan Sellien
 */
public class TrackletCreatorNodeFactory extends
		NodeFactory<TrackletCreatorNodeModel> {

	@Override
	public TrackletCreatorNodeModel createNodeModel() {
		return new TrackletCreatorNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<TrackletCreatorNodeModel> createNodeView(int viewIndex,
			TrackletCreatorNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new TrackletCreatorNodeDialog();
	}

}
