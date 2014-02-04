package org.knime.knip.tracking.nodes.labmerger;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;

/**
 * <code>NodeFactory</code> for the "LabelingMerger" Node.
 * Merges a table with labelings. * nSame labels in different labelings are seperated!
 *
 * @author Stephan Sellien
 */
public class LabelingMergerNodeFactory 
        extends NodeFactory<LabelingMergerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public LabelingMergerNodeModel createNodeModel() {
        return new LabelingMergerNodeModel();
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
    public NodeView<LabelingMergerNodeModel> createNodeView(final int viewIndex,
            final LabelingMergerNodeModel nodeModel) {
        return new TableCellViewNodeView<LabelingMergerNodeModel>(nodeModel,0);
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
        return new LabelingMergerNodeDialog();
    }

}

