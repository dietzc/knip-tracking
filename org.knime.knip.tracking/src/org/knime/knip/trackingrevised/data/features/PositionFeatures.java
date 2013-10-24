package org.knime.knip.trackingrevised.data.features;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.logic.BitType;

import org.knime.knip.trackingrevised.data.graph.Node;
import org.knime.knip.trackingrevised.data.graph.TransitionGraph;
import org.knime.network.core.api.PersistentObject;

public class PositionFeatures extends FeatureClass {

	@Override
	public String getName() {
		return "Position Features";
	}
	
	@Feature(name = "diff. position")
	public static double diffPos(final TransitionGraph tg) {
		return traverseConnectedDiffMV(tg, new CalculationMV() {
			
			@Override
			public double[] calculate(PersistentObject po) {
				Node node = new Node(tg.getNet(), po);
				double[] pos = new double[node.getPosition().numDimensions()];
				node.getPosition().setPosition(pos);
				return pos;
			}
		});
	}
	
	@Feature(name = "diff. size")
	public static double diffSize(final TransitionGraph tg) {
		return traverseConnectedDiffSV(tg, new CalculationSV() {
			
			@Override
			public double calculate(PersistentObject po) {
				Node node = new Node(tg.getNet(), po);
				ImgPlus<BitType> img = node.getBitmask().getImgPlus();
				int count = 0;
				for(BitType pixel : img)
					if(pixel.get())
						count++;
				
				return count;
			}
		});
	}

}
