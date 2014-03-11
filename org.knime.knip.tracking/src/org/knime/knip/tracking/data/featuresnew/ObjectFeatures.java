package org.knime.knip.tracking.data.featuresnew;

import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.region.BresenhamLine;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.knip.tracking.util.OffsetHandling;
import org.knime.knip.tracking.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.exception.PersistenceException;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class ObjectFeatures {

	public String getName() {
		return "Object Features";
	}

	/**
	 * Object volume feature Count the number of pixels the object consists of.
	 * 
	 * @param tg
	 *            the {@link TransitionGraph}
	 * @return volume
	 */
	public static TrackedNodeFeature volume() {
		return new TrackedNodeFeature("ObjectVolume") {

			@Override
			public double[] calcInt(TrackedNode node) {
				int count = 0;
				for (BitType pixel : node.getBitmask().getImgPlus()) {
					if (pixel.get())
						count++;
				}
				return new double[] { count };
			}
		};
	}

	/**
	 * (Unweighted) Mean position feature Calculate the mean position and its
	 * higher central moments. Only the shape of the object is used, not the
	 * intensities/values. Output structure: size = 4*N [0 .. N-1] mean
	 * coordinates [N .. 2*N-1] variance [2*N .. 3*N-1] skew [3*N .. 4*N-1]
	 * kurtosis
	 * 
	 * @param tg
	 *            the {@link TransitionGraph}
	 * @return position
	 */
	public static TrackedNodeFeature position() {
		return new TrackedNodeFeature("ObjectPosition") {

			@Override
			public double[] calcInt(TrackedNode node) {
				int noDims = (int) node.getBitmask().getDimensions().length;
				double[] res = new double[4 * noDims];

				double[] coords = new double[noDims];
				double[] variance = new double[noDims];
				double[] skew = new double[noDims];
				double[] kurtosis = new double[noDims];
				int count = 0;

				Cursor<BitType> cursor = node.getBitmask().getImgPlus()
						.localizingCursor();
				while (cursor.hasNext()) {
					if (cursor.next().get()) {
						for (int d = 0; d < noDims; d++) {
							coords[d] += cursor.getDoublePosition(d);
						}
						count++;
					}
				}
				// avg
				for (int d = 0; d < noDims; d++)
					res[d] = coords[d] / count;

				cursor.reset();
				while (cursor.hasNext()) {
					if (cursor.next().get()) {
						for (int d = 0; d < noDims; d++) {
							double delta = cursor.getDoublePosition(d) - res[d];
							variance[d] += delta * delta;
							skew[d] += delta * delta * delta;
							kurtosis[d] += delta * delta * delta * delta;
						}
					}
				}
				// avg
				for (int d = 0; d < noDims; d++) {
					if (variance[d] != 0)
						res[2 * noDims + d] = variance[d] / count;
					if (skew[d] != 0)
						res[3 * noDims + d] = skew[d] / count
								/ Math.pow(res[noDims + d], 3.0 / 2.0);
					if (kurtosis[d] != 0)
						res[3 * noDims + d] = kurtosis[d] / count
								/ (res[noDims + d] * res[noDims + d]);
				}

				return res;
			}
		};
	}

	/**
	 * Weighted Mean position feature Calculate the mean position and its higher
	 * central moments. Each position is weighted with it's intensity value.
	 * Output structure: size = 4*N [0 .. N-1] mean coordinates [N .. 2*N-1]
	 * variance [2*N .. 3*N-1] skew [3*N .. 4*N-1] kurtosis
	 * 
	 * @param tg
	 *            the {@link TransitionGraph}
	 * @return position
	 */
	public static TrackedNodeFeature weightedPosition() {
		return new TrackedNodeFeature("ObjectWeightedPosition") {

			@Override
			public double[] calcInt(TrackedNode node) {
				int noDims = (int) node.getBitmask().getDimensions().length;
				double[] res = new double[4 * noDims];

				double[] coords = new double[noDims];
				double[] variance = new double[noDims];
				double[] skew = new double[noDims];
				double[] kurtosis = new double[noDims];
				double sum = 0.0;

				Cursor<BitType> cursor = node.getBitmask().getImgPlus()
						.localizingCursor();
				RandomAccess<? extends RealType<?>> ra = node.getSegmentImage()
						.getImgPlus().randomAccess();
				while (cursor.hasNext()) {
					if (cursor.next().get()) {
						ra.setPosition(cursor);
						double value = ra.get().getRealDouble();
						for (int d = 0; d < noDims; d++) {
							coords[d] += value * cursor.getDoublePosition(d);
						}
						sum += value;
					}
				}
				// avg
				for (int d = 0; d < noDims; d++)
					res[d] = coords[d] / sum;

				cursor.reset();
				while (cursor.hasNext()) {
					if (cursor.next().get()) {
						ra.setPosition(cursor);
						double value = ra.get().getRealDouble();
						for (int d = 0; d < noDims; d++) {
							double delta = cursor.getDoublePosition(d) - res[d];
							variance[d] += value * delta * delta;
							skew[d] += value * delta * delta * delta;
							kurtosis[d] += value * delta * delta * delta
									* delta;
						}
					}
				}
				// avg
				for (int d = 0; d < noDims; d++) {
					if (variance[d] != 0)
						res[2 * noDims + d] = variance[d] / sum;
					if (skew[d] != 0)
						res[3 * noDims + d] = skew[d] / sum
								/ Math.pow(res[noDims + d], 3.0 / 2.0);
					if (kurtosis[d] != 0)
						res[3 * noDims + d] = kurtosis[d] / sum
								/ (res[noDims + d] * res[noDims + d]);
				}

				return res;
			}
		};
	}

	/**
	 * Principal components feature Calculate the major axes (principal
	 * components) of object shape Output stucture: size = N*(N+1) [0 .. N-1]
	 * Eigenvalues of covariance matrix [k*(N+1) .. k*(N+2)-1] Eigenvector for
	 * eigenvalue [k]
	 * 
	 * @param tg
	 *            {@link TransitionGraph}
	 * @return pca
	 */
	public static TrackedNodeFeature principalComponents() {
		return new TrackedNodeFeature("ObjectPrincipalComponents") {

			@Override
			public double[] calcInt(TrackedNode node) {
				int noDims = (int) node.getBitmask().getDimensions().length;
				double[] res = new double[4 * noDims];

				double[] coords = new double[noDims];
				int count = 0;

				long[] size = node.getBitmask().getDimensions();

				Cursor<BitType> cursor = node.getBitmask().getImgPlus()
						.localizingCursor();
				while (cursor.hasNext()) {
					if (cursor.next().get()) {
						for (int d = 0; d < noDims; d++) {
							coords[d] += cursor.getDoublePosition(d);
						}
						count++;
					}
				}
				// avg
				for (int d = 0; d < noDims; d++)
					coords[d] = coords[d] / count;

				// shift coordinates to zero mean & create matrix
				Matrix coordsMat = new Matrix((int) (size[0] * size[1]), noDims);
				count = 0;
				while (cursor.hasNext()) {
					if (cursor.next().get()) {
						for (int d = 0; d < noDims; d++) {
							coordsMat.set(count, d, cursor.getDoublePosition(d)
									- coords[d]);
						}
						count++;
					}
				}

				// ignore empty bitmasks
				if (count <= 0)
					return res;

				EigenvalueDecomposition ev = coordsMat.eig();
				for (int d = 0; d < noDims; d++) {
					res[d] = ev.getRealEigenvalues()[d];
					for (int j = 0; j < noDims; j++) {
						res[(d + 1) * noDims + j] = ev.getV().get(j, d);
					}
				}

				return res;
			}
		};
	}

	/**
	 * Bounding Box feature Find the smallest possible box that contains the
	 * whole object Output stucture: size = 2*N + 1 [0 .. N-1] minimum
	 * coordinates (included by the object) [N .. 2*N-1] maximum coordinates
	 * (excluded by the object) [2*N] Fill factor: <object volume> / <bounding
	 * box volume>
	 */
	public static TrackedNodeFeature boundingBox() {
		return new TrackedNodeFeature("ObjectBoundingBox") {

			@Override
			public double[] calcInt(TrackedNode node) {
				int noDims = node.getBitmask().getDimensions().length;
				Cursor<BitType> cursor = node.getBitmask().getImgPlus()
						.localizingCursor();
				double[] min = new double[noDims];
				double[] max = new double[noDims];
				for (int d = 0; d < min.length; d++)
					min[d] = Integer.MAX_VALUE;
				long volume = 0;
				while (cursor.hasNext()) {
					if (cursor.next().get()) {
						for (int d = 0; d < noDims; d++) {
							min[d] = Math.min(min[d], cursor.getIntPosition(d));
							// max is excluded => +1
							max[d] = Math.max(max[d],
									cursor.getIntPosition(d) + 1);
						}
						volume++;
					}
				}
				double[] res = new double[2 * noDims + 1];
				System.arraycopy(min, 0, res, 0, min.length);
				System.arraycopy(max, 0, res, noDims, max.length);
				double fillFactor = volume;
				for (int d = 0; d < noDims; d++) {
					double length = max[d] - min[d] + 1;
					fillFactor /= length;
				}
				res[2 * noDims] = fillFactor;
				return res;
			}
		};
	}

	/**
	 * Intensity feature Calculate the mean intensity and its central moments of
	 * all object Output stucture: size = 5 [0] Mean of intensity distribution
	 * [1] Variance of intensity distribution [2] Skew of Intensity distribution
	 * [3] Kurtosis of Intensity distribution [4] Sum of Intensity
	 */
	public static TrackedNodeFeature intensity() {
		return new TrackedNodeFeature("ObjectIntensity") {

			@Override
			public double[] calcInt(TrackedNode node) {
				double[] res = new double[5];

				double mean = 0;
				int count = 0;

				for (RealType<?> pixel : node.getSegmentImage().getImgPlus()) {
					mean += pixel.getRealDouble();
					count++;
				}
				mean /= count;

				double var = 0;
				double skew = 0;
				double kurt = 0;

				for (RealType<?> pixel : node.getSegmentImage().getImgPlus()) {
					double delta = pixel.getRealDouble() - mean;
					var += delta * delta;
					skew += delta * delta * delta;
					kurt += delta * delta * delta * delta;
				}
				res[0] = mean;
				res[1] = var / count;
				res[2] = skew / count / Math.pow(var, 3. / 2.);
				res[3] = kurt / count / (var * var);
				// mean * count = sum
				res[4] = mean * count;
				return res;
			}
		};
	}

	/**
	 * Minimum/Maximum Intensity feature Find the minimum and the maximum
	 * intensity of a object and find the quantiles of the intensity
	 * distribution. Output stucture: size = 9 [0] Minimum intensity [1] Maximum
	 * intensity [2] 5% quantile [3] 10% quantile [4] 25% quantile [5] 50%
	 * quantile [6] 75% quantile [7] 90% quantile [8] 95% quantile
	 */
	public static TrackedNodeFeature minMaxIntensity() {
		return new TrackedNodeFeature("ObjectMinMaxIntensity") {

			@Override
			public double[] calcInt(TrackedNode node) {
				List<Double> pixels = new LinkedList<Double>();
				double min = Double.MAX_VALUE;
				double max = 0;
				for (RealType<?> pixel : node.getSegmentImage().getImgPlus()) {
					pixels.add(pixel.getRealDouble());
					min = Math.min(min, pixel.getRealDouble());
					max = Math.max(max, pixel.getRealDouble());
				}
				int size = pixels.size();
				Collections.sort(pixels);
				double[] res = new double[9];
				res[0] = min;
				res[1] = max;
				res[2] = pixels.get((int) Math.floor(0.05 * size));
				res[3] = pixels.get((int) Math.floor(0.1 * size));
				res[4] = pixels.get((int) Math.floor(0.25 * size));
				res[5] = pixels.get((int) Math.floor(0.5 * size));
				res[6] = pixels.get((int) Math.floor(0.75 * size));
				res[7] = pixels.get((int) Math.floor(0.90 * size));
				res[8] = pixels.get((int) Math.floor(0.95 * size));
				return res;
			}
		};
	}

	/**
	 * Maximum Intensity feature Find the maximum intensity of a object. Output
	 * stucture: size = N+1 [0] Maximum intensity [1 .. N] coordinates of max.
	 * intensity
	 */
	public static TrackedNodeFeature maxIntensity() {
		return new TrackedNodeFeature("ObjectMaxIntensity") {

			@Override
			public double[] calcInt(TrackedNode node) {
				Cursor<? extends RealType<?>> cursor = node.getSegmentImage()
						.getImgPlus().localizingCursor();
				int noDims = cursor.numDimensions();
				int[] coords = new int[noDims];
				double max = 0;
				while (cursor.hasNext()) {
					double value = cursor.next().getRealDouble();
					if (value > max) {
						max = value;
						cursor.localize(coords);
					}
				}
				double[] res = new double[noDims + 1];
				res[0] = max;
				for (int d = 0; d < noDims; d++) {
					res[d] = coords[d];
				}
				return res;
			}
		};
	}

	// ObjectPairwise -> deprecated ? only in 3d
	// ObjectSGF -> only in 3d

	public static TrackedNodeFeature distanceToBorder() {
		return new TrackedNodeFeature("ObjectFeatureBorderDistance") {

			@Override
			public double[] calcInt(TrackedNode node) {
				double result = Double.MAX_VALUE;
				Rectangle2D rect = node.getImageRectangle();
				try {
					long[] imgDims = OffsetHandling
							.decode(node.getNetwork().getFeatureString(node.getNetwork(), TrackingConstants.NETWORK_FEATURE_DIMENSION));
					result = Math.min(
							rect.getMinY(),
							Math.min(rect.getMinX(), Math.min(
									imgDims[0] - rect.getMaxX(), imgDims[1]
											- rect.getMaxY())));
				} catch( Exception e) {
					e.printStackTrace();
				}
				return new double[] { result };
			}
		};
	}

	public static TrackedNodeFeature overlapBorder() {
		return new TrackedNodeFeature("ObjectFeatureBorderOverlap") {

			@Override
			public double[] calcInt(TrackedNode node) {
				Rectangle2D rect = node.getImageRectangle();
				double result = Double.MAX_VALUE;
				try {
					long[] imgDims = OffsetHandling
							.decode(node.getNetwork()
									.getStringFeature(
											node.getNetwork(),
											TrackingConstants.NETWORK_FEATURE_DIMENSION));
					int count = 0;
					List<Cursor<BitType>> list = new LinkedList<Cursor<BitType>>();

					long width = (long) rect.getWidth() - 1;
					long heigth = (long) rect.getHeight() - 1;

					if (rect.getMinX() == 0) {
						// left border
						BresenhamLine<BitType> cursor = new BresenhamLine<BitType>(
								node.getBitmask().getImgPlus(), new Point(0L,
										0L, 0L), new Point(0, heigth, 0L));
						list.add(cursor);
					}
					if (rect.getMinY() == 0) {
						// upper border
						BresenhamLine<BitType> cursor = new BresenhamLine<BitType>(
								node.getBitmask().getImgPlus(), new Point(0L,
										0L, 0L), new Point(width, 0L, 0L));
						list.add(cursor);
					}
					if (imgDims[0] - rect.getMaxX() == 0) {
						// right border
						BresenhamLine<BitType> cursor = new BresenhamLine<BitType>(
								node.getBitmask().getImgPlus(), new Point(
										width, 0L, 0L), new Point(width,
										heigth, 0L));
						list.add(cursor);
					}
					if (imgDims[1] - rect.getMinY() == 0) {
						// lower border
						BresenhamLine<BitType> cursor = new BresenhamLine<BitType>(
								node.getBitmask().getImgPlus(), new Point(0L,
										heigth, 0L), new Point(width, heigth,
										0L));
						list.add(cursor);
					}
					for (Cursor<BitType> cursor : list) {
						while (cursor.hasNext()) {
							if (cursor.next().get())
								count++;
						}
					}
					result = count;
				} catch (InvalidFeatureException e) {
					System.out.println("IFE: " + e.getMessage());
					KPartiteGraph<PersistentObject, Partition> net = node.getNetwork();
					System.out.println("Features:");
					try {
						for (org.knime.network.core.api.Feature feature : net
								.getFeatures()) {
							System.out.println("\t" + feature.getName() + " "
									+ feature.getType().getName());
						}
					} catch (PersistenceException e1) {
						e1.printStackTrace();
					}
				} catch (PersistenceException e) {
				}
				return new double[]{result};
			}
		};
	}
}
