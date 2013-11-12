package org.knime.knip.tracking.data.graph.renderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;

import org.knime.base.util.kdtree.KDTree;
import org.knime.base.util.kdtree.KDTreeBuilder;
import org.knime.base.util.kdtree.NearestNeighbour;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.ImageRenderer;
import org.knime.knip.core.awt.RendererFactory;
import org.knime.knip.tracking.data.TransitionGraphDataObject;
import org.knime.knip.tracking.data.graph.Edge;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.network.core.core.exception.PersistenceException;

public class TransitionGraphConnectorComponent<T extends RealType<T>> extends
		JPanel implements MouseListener, MouseMotionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6266875939313108708L;

	private TransitionGraphDataObject tgdo;
	private TransitionGraph tg;
	private ImgPlus<T> img;
	private BufferedImage bimg;

	private KDTree<TrackedNode> kdTree;

	// would be set if mouse is near a node
	private TrackedNode nearestNode = null;
	// start point for dragging event
	private TrackedNode startNode = null;
	// last mouse position
	private Point lastPoint;

	private long[] imgOffsets;

	private int partitionWidth;

	public TransitionGraphConnectorComponent(TransitionGraphDataObject tgdo,
			ImgPlus<T> img) {
		this.tgdo = tgdo;
		this.tg = tgdo.getTransitionGraph();
		this.img = img;

		Dimension dim = new Dimension((int) img.max(0), (int) img.max(1));
		setSize(dim);
		setMinimumSize(dim);
		setPreferredSize(dim);
		setMaximumSize(dim);

		bimg = prepareBufferedImage();

		this.addMouseListener(this);
		this.addMouseMotionListener(this);

		KDTreeBuilder<TrackedNode> treeBuilder = new KDTreeBuilder<TrackedNode>(2);
		imgOffsets = tg.getImageOffsets();
		partitionWidth = ((int) img.max(0) - (TransitionGraphRenderer.BORDER * (tg
				.getPartitions().size() - 1))) / (tg.getPartitions().size());
		System.out.println(partitionWidth);
		for (String partition : tg.getPartitions()) {
			for (TrackedNode node : tg.getNodes(partition)) {
				double[] coords = new double[2];
				coords[0] = getX(node);
				coords[1] = getY(node);
				treeBuilder.addPattern(coords, node);
				System.out.println(Arrays.toString(coords) + " -> " + node);
			}
		}
		kdTree = treeBuilder.buildTree();
	}

	private int getX(TrackedNode n) {
		return (int) Math
				.round(n.getDoublePosition(0)
						- imgOffsets[0]
						+ (((n.frame() - tg.getStartTime()) * (TransitionGraphRenderer.BORDER + partitionWidth))));
	}

	private int getY(TrackedNode n) {
		return (int) Math.round(n.getDoublePosition(1) - imgOffsets[1]);
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
		Graphics2D g2d = (Graphics2D) g;
		g.drawImage(bimg, 0, 0, null);

		int radius = 10;

		g.setColor(Color.YELLOW);
		g.drawString("#edges: " + tg.getEdges().size(), 0, 10);

		for (Edge edge : tg.getEdges()) {
			g.drawLine(getX(edge.getStartNode()), getY(edge.getStartNode()),
					getX(edge.getEndNode()), getY(edge.getEndNode()));
		}

		if (startNode != null) {
			g2d.setColor(Color.RED);
			g2d.fillRect(getX(startNode) - radius / 2, getY(startNode) - radius
					/ 2, radius, radius);
			g2d.drawLine(getX(startNode), getY(startNode), lastPoint.x,
					lastPoint.y);
		}
		if (nearestNode != null) {
			g2d.setColor(Color.CYAN);
			Stroke stroke = g2d.getStroke();
			g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER, 10.0f, new float[] { 2 }, 0.0f));
			g2d.drawRect(getX(nearestNode) - radius / 2, getY(nearestNode)
					- radius / 2, radius, radius);
			g2d.setStroke(stroke);
		}

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// nothing
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// nothing
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// nothing
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// nothing
		if (e.getButton() == MouseEvent.BUTTON2)
			fireGraphEdited();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		System.out.println("to connect: " + startNode + " -> " + nearestNode);
		try {
			if (startNode.frame() >= nearestNode.frame())
				System.err
						.println(startNode + " must be before " + nearestNode);
			else {
				tg.createEdge(startNode, nearestNode);
			}
		} catch (PersistenceException e1) {
			e1.printStackTrace();
		}
		startNode = null;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		mouseMovement(e);
		if (startNode == null) {
			startNode = nearestNode;
		}
		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		mouseMovement(e);
	}

	private void mouseMovement(MouseEvent e) {
		lastPoint = e.getPoint();
		List<NearestNeighbour<TrackedNode>> list = kdTree.getMaxDistanceNeighbours(
				new double[] { lastPoint.x, lastPoint.y }, 20);
		if (!list.isEmpty()) {
			nearestNode = list.get(0).getData();
			repaint();
		}
	}

	private TransitionGraphComponentListener m_listener = null;

	public void setTransitionGraphComponentListener(
			TransitionGraphComponentListener listener) {
		m_listener = listener;
	}

	private void fireGraphEdited() {
		if (m_listener != null)
			m_listener.graphEdited(tgdo);
	}
}
