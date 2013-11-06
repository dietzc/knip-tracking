//package org.knime.knip.trackingrevised.trackmate;
//
//
//import org.knime.core.node.NodeDialogPane;
//import org.knime.core.node.NodeFactory;
//import org.knime.core.node.NodeView;
//
///**
// * <code>NodeFactory</code> for the "TrackletCreator" Node.
// * Creates Tracklets (parts of a trajectory) out of a given segmentation as graphs.
// *
// * @author Stephan Sellien
// */
//public class LAPMergeSplitterFactory 
//        extends NodeFactory<LAPMergeSplitterModel> {
//
//	@Override
//	public LAPMergeSplitterModel createNodeModel() {
//		return new LAPMergeSplitterModel();
//	}
//
//	@Override
//	protected int getNrNodeViews() {
//		return 0;
//	}
//
//	@Override
//	public NodeView<LAPMergeSplitterModel> createNodeView(int viewIndex,
//			LAPMergeSplitterModel nodeModel) {
//		return null;
//	}
//
//	@Override
//	protected boolean hasDialog() {
//		return true;
//	}
//
//	@Override
//	protected NodeDialogPane createNodeDialogPane() {
//		return new LAPMergeSplitterDialog();
//	}
//
//	
// 
//}
//
