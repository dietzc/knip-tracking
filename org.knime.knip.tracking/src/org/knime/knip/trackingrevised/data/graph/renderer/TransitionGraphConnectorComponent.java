package org.knime.knip.trackingrevised.data.graph.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JPanel;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.Type;

import org.knime.base.util.kdtree.KDTree;
import org.knime.base.util.kdtree.KDTreeBuilder;
import org.knime.base.util.kdtree.NearestNeighbour;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.ImageRenderer;
import org.knime.knip.core.awt.RendererFactory;
import org.knime.knip.trackingrevised.data.graph.Node;
import org.knime.knip.trackingrevised.data.graph.TransitionGraph;

import cern.colt.Arrays;

public class TransitionGraphConnectorComponent<T extends Type<T>> extends
		JPanel implements MouseListener, MouseMotionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6266875939313108708L;

	private TransitionGraph tg;
	private ImgPlus<T> img;
	private BufferedImage bimg;

	private KDTree<Node> kdTree;

	private Node selectedNode = null;

	private long[] imgOffsets;

	private int partWidth;

	public TransitionGraphConnectorComponent(TransitionGraph tg, ImgPlus<T> img) {
		this.tg = tg;
		this.img = img;

		Dimension dim = new Dimension((int) img.max(0), (int) img.max(1));
		setSize(dim);
		setMinimumSize(dim);
		setPreferredSize(dim);
		setMaximumSize(dim);

		bimg = prepareBufferedImage();

		this.addMouseListener(this);
		this.addMouseMotionListener(this);

		KDTreeBuilder<Node> treeBuilder = new KDTreeBuilder<Node>(2);
		imgOffsets = tg.getImageOffsets();
		partWidth = ((int) img.max(0) - (TransitionGraphRenderer.BORDER * (tg
				.getPartitions().size() - 1))) / (tg.getPartitions().size());
		System.out.println(partWidth);
		for (String partition : tg.getPartitions()) {
			for (Node node : tg.getNodes(partition)) {
				double[] coords = new double[2];
				coords[0] = getX(node);
				coords[1] = getY(node);
				treeBuilder.addPattern(coords, node);
				System.out.println(Arrays.toString(coords) + " -> " + node);
			}
		}
		kdTree = treeBuilder.buildTree();
	}

	private double getX(Node n) {
		return n.getPosition().getDoublePosition(0)
				- imgOffsets[0]
				+ (((n.getTime() - tg.getStartTime()) * (TransitionGraphRenderer.BORDER + partWidth)));
	}

	private double getY(Node n) {
		return n.getPosition().getDoublePosition(1) - imgOffsets[1];
	}

	public BufferedImage prepareBufferedImage() {
		ImageRenderer<T> renderer = RendererFactory.createSuitableRenderer(img)[0];
		BufferedImage bimg = AWTImageTools.renderScaledStandardColorImg(img,
				renderer, 1.0, new long[img.numDimensions()]);
		return bimg;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.drawImage(bimg, 0, 0, null);

		if (selectedNode != null) {
			g.setColor(Color.RED);
			g.drawRect((int) getX(selectedNode), (int) getY(selectedNode), 5, 5);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		Point point = e.getPoint();
		List<NearestNeighbour<Node>> list = kdTree.getMaxDistanceNeighbours(
				new double[] { point.x, point.y }, 20);
		if (!list.isEmpty()) {
			selectedNode = list.get(0).getData();
			repaint();
		}
	}

}
