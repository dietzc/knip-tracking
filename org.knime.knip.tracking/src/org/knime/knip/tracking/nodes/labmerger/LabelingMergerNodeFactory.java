package org.knime.knip.tracking.nodes.labmerger;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;

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
public class LabelingMergerNodeFactory<L extends Comparable<L>, T extends IntegerType<T> &NativeType<T>> 
        extends NodeFactory<LabelingMergerNodeModel<L,T>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public LabelingMergerNodeModel<L,T> createNodeModel() {
        return new LabelingMergerNodeModel<L,T>();
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
    public NodeView<LabelingMergerNodeModel<L,T>> createNodeView(final int viewIndex,
            final LabelingMergerNodeModel<L,T> nodeModel) {
        return new TableCellViewNodeView<LabelingMergerNodeModel<L,T>>(nodeModel,0);
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

