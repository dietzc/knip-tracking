package org.knime.knip.tracking.nodes.tableCopy;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TableCopy" Node.
 * Just creates a copy of the input.
 *
 * @author Stephan Sellien
 */
public class TableCopyNodeFactory 
        extends NodeFactory<TableCopyNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public TableCopyNodeModel createNodeModel() {
        return new TableCopyNodeModel();
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
    public NodeView<TableCopyNodeModel> createNodeView(final int viewIndex,
            final TableCopyNodeModel nodeModel) {
        return new TableCopyNodeView(nodeModel);
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
        return new TableCopyNodeDialog();
    }

}

