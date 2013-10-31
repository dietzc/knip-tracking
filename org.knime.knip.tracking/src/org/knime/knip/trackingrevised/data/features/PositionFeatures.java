package org.knime.knip.trackingrevised.data.features;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.trackingrevised.data.graph.Node;
import org.knime.knip.trackingrevised.data.graph.TransitionGraph;
import org.knime.network.core.api.PersistentObject;

public class PositionFeatures extends FeatureClass {

	@Override
	public String getName() {
		return "Position Features";
	}
	
	@Feature(name = "diff. position")
	public static <T extends RealType<T>> double diffPos(final TransitionGraph<T> tg) {
		return traverseConnectedDiffMV(tg, new CalculationMV() {
			
			@Override
			public double[] calculate(PersistentObject po) {
				Node<T> node = new Node<T>(tg.getNet(), po);
				double[] pos = new double[node.getPosition().numDimensions()];
				node.getPosition().setPosition(pos);
				return pos;
			}
		});
	}
	
	@Feature(name = "diff. size")
	public static <T extends RealType<T>> double diffSize(final TransitionGraph<T> tg) {
		return traverseConnectedDiffSV(tg, new CalculationSV() {
			
			@Override
			public double calculate(PersistentObject po) {
				Node<T> node = new Node<T>(tg.getNet(), po);
				ImgPlus<BitType> img = node.getBitmask().getImgPlus();
				int count = 0;
				for(BitType pixel : img)
					if(pixel.get())
						count++;
				
				return count;
			}
		});
	}
	
	//TODO: overlap with border ?
	//TODO: distance to border ?

}
