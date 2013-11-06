package org.knime.knip.trackingrevised.data.features;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.trackingrevised.data.graph.TrackedNode;
import org.knime.knip.trackingrevised.data.graph.TransitionGraph;
import org.knime.network.core.api.PersistentObject;

public class PositionFeatures extends FeatureClass {

	@Override
	public String getName() {
		return "Position Features";
	}

	@Feature(name = "diff. position")
	public static <T extends RealType<T>> double diffPos(
			final TransitionGraph tg) {
		return traverseConnectedDiffMV(tg, new CalculationMV() {

			@Override
			public double[] calculate(PersistentObject po) {
				TrackedNode node = new TrackedNode(tg.getNet(), po);
				double[] pos = new double[node.numDimensions()];
				// TODO: Sorry willst du hier echt von dem Node die Position
				// SETZEN? :-) Habs mal auskommentiert
				// node.getPosition().setPosition(pos);
				return pos;
			}
		});
	}

	@Feature(name = "diff. size")
	public static <T extends RealType<T>> double diffSize(
			final TransitionGraph tg) {
		return traverseConnectedDiffSV(tg, new CalculationSV() {

			@Override
			public double calculate(PersistentObject po) {
				TrackedNode node = new TrackedNode(tg.getNet(), po);
				ImgPlus<BitType> img = node.getBitmask().getImgPlus();
				int count = 0;
				for (BitType pixel : img)
					if (pixel.get())
						count++;

				return count;
			}
		});
	}

	// TODO: overlap with border ?
	// TODO: distance to border ?

}
