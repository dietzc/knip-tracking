package org.knime.knip.tracking.data.features;

import net.imglib2.Cursor;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;

public class IntensityFeatures extends FeatureClass {
	@Override
	public String getName() {
		return "Intensity features";
	}

	@Feature(name = "Intensity difference histogram")
	public static <T extends RealType<T>> double diffHisto(
			final TransitionGraph tg) {
		// diff inten hist
		return traverseConnectedDiffMV(tg, new CalculationMV() {

			@Override
			public double[] calculate(TrackedNode node) {
				ImgPlusValue<? extends RealType<?>> img = node
						.getSegmentImage();
				return createHistogram(img.getImgPlus(), 64);
			}
		});
	}

	@Feature(name = "Intensity difference sum")
	public static <T extends RealType<T>> double diffSum(
			final TransitionGraph tg) {
		// diff inten sum
		return traverseConnectedDiffSV(tg, new CalculationSV() {

			@Override
			public double calculate(TrackedNode node) {
				ImgPlusValue<? extends RealType<?>> img = node
						.getSegmentImage();
				double sum = 0.0;
				Cursor<? extends RealType<?>> cursor = img.getImgPlus()
						.cursor();
				while (cursor.hasNext()) {
					sum += cursor.next().getRealDouble();
				}
				return sum;
			}
		});
	}

	@Feature(name = "Intensity difference mean")
	public static <T extends RealType<T>> double diffMean(
			final TransitionGraph tg) {
		// diff inten mean
		return traverseConnectedDiffSV(tg, new CalculationSV() {

			@Override
			public double calculate(TrackedNode node) {
				ImgPlusValue<? extends RealType<?>> img = node
						.getSegmentImage();
				double sum = 0.0;
				long count = 0;
				Cursor<? extends RealType<?>> cursor = img.getImgPlus()
						.cursor();
				while (cursor.hasNext()) {
					sum += cursor.next().getRealDouble();
					count++;
				}
				return sum / count;
			}
		});
	}

	@Feature(name = "Intensity difference deviation")
	public static <T extends RealType<T>> double diffDeviation(
			final TransitionGraph tg) {
		// diff inten devia
		return traverseConnectedDiffSV(tg, new CalculationSV() {

			@Override
			public double calculate(TrackedNode node) {
				ImgPlusValue<? extends RealType<?>> img = node
						.getSegmentImage();
				double sum = 0.0;
				long count = 0;
				Cursor<? extends RealType<?>> cursor = img.getImgPlus()
						.cursor();
				while (cursor.hasNext()) {
					sum += cursor.next().getRealDouble();
					count++;
				}
				double mean = sum / count;
				double deviation = 0.0;
				cursor = img.getImgPlus().cursor();
				while (cursor.hasNext()) {
					deviation += Math.abs(cursor.next().getRealDouble() - mean);
				}
				return deviation;
			}
		});
	}

	private static <T extends RealType<T>> double[] createHistogram(
			ImgPlus<T> img, int noBins) {
		double[] hist = new double[noBins];
		double min = img.firstElement().createVariable().getMinValue();
		double max = img.firstElement().createVariable().getMinValue();
		double scale = (hist.length - 1) * (max - min);
		for (T val : img) {
			hist[(int) ((val.getRealDouble() - min) * scale)]++;
		}
		return hist;
	}
}
