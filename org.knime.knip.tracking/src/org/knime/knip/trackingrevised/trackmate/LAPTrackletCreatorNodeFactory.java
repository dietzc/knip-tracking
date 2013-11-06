package org.knime.knip.trackingrevised.trackmate;


import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TrackletCreator" Node.
 * Creates Tracklets (parts of a trajectory) out of a given segmentation as graphs.
 *
 * @author Stephan Sellien
 */
public class LAPTrackletCreatorNodeFactory 
        extends NodeFactory<LAPTrackletCreatorNodeModel> {

	@Override
	public LAPTrackletCreatorNodeModel createNodeModel() {
		return new LAPTrackletCreatorNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<LAPTrackletCreatorNodeModel> createNodeView(int viewIndex,
			LAPTrackletCreatorNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new LAPTrackletCreatorNodeDialog();
	}

	
 
}

