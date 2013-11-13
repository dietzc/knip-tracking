package org.knime.knip.tracking.data.features;

import java.awt.geom.Rectangle2D;
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
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.exception.PersistenceException;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class ObjectFeatures extends FeatureClass {

	@Override
	public String getName() {
		return "Object Features";
	}

	/**
	 * Object volume feature Count the number of voxels the object consists of.
	 * 
	 * @param tg
	 *            the {@link TransitionGraph}
	 * @return volume
	 */
	@Feature(name = "ObjectVolume")
	public static double volume(final TransitionGraph tg) {
		return traverseConnectedDiffSV(tg, new CalculationSV() {

			@Override
			public double calculate(TrackedNode node) {
				int count = 0;
				for (BitType pixel : node.getBitmask().getImgPlus()) {
					if (pixel.get())
						count++;
				}
				return count;
			}
		});
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
	@Feature(name = "ObjectPosition")
	public static double position(final TransitionGraph tg) {
		return traverseConnectedDiffMV(tg, new CalculationMV() {

			@Override
			public double[] calculate(TrackedNode node) {
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

			@Override
			public double[] sum(double[] sum, double[] newValue) {
				// TODO merge!!!
				return super.sum(sum, newValue);
			}
		});
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
	@Feature(name = "ObjectWeightedPosition")
	public static double weightedPosition(final TransitionGraph tg) {
		return traverseConnectedDiffMV(tg, new CalculationMV() {

			@Override
			public double[] calculate(TrackedNode node) {
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

			@Override
			public double[] sum(double[] sum, double[] newValue) {
				// TODO merge?!?
				return super.sum(sum, newValue);
			}
		});
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
	@Feature(name = "ObjectPrincipalComponents")
	public static double principalComponents(final TransitionGraph tg) {
		return traverseConnectedDiffMV(tg, new CalculationMV() {

			@Override
			public double[] calculate(TrackedNode node) {
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

				EigenvalueDecomposition ev = coordsMat.eig();

				for (int d = 0; d < noDims; d++) {
					res[d] = ev.getRealEigenvalues()[d];
					for (int j = 0; j < noDims; j++) {
						res[(d + 1) * noDims + j] = ev.getV().get(j, d);
					}
				}

				return res;
			}
		});
	}

	/**
	 * Bounding Box feature Find the smallest possible box that contains the
	 * whole object Output stucture: size = 2*N + 1 [0 .. N-1] minimum
	 * coordinates (included by the object) [N .. 2*N-1] maximum coordinates
	 * (excluded by the object) [2*N] Fill factor: <object volume> / <bounding
	 * box volume>
	 */
	@Feature(name = "ObjectBoundingBox")
	public static double boundingBox(TransitionGraph tg) {
		return traverseConnectedDiffMV(tg, new CalculationMV() {

			@Override
			public double[] calculate(TrackedNode node) {
				int noDims = node.getBitmask().getDimensions().length;
				Cursor<BitType> cursor = node.getBitmask().getImgPlus()
						.localizingCursor();
				int[] min = new int[noDims];
				int[] max = new int[noDims];
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
		});
	}
	
	/** Intensity feature
     * Calculate the mean intensity and its central moments of all object
     * Output stucture:
     * size = 5
     * [0] Mean of intensity distribution
     * [1] Variance of intensity distribution
     * [2] Skew of Intensity distribution
     * [3] Kurtosis of Intensity distribution
     * [4] Kurtosis of Intensity distribution
     */
	@Feature(name ="ObjectIntensity")
	public static double intensity(TransitionGraph tg) {
		return traverseConnectedDiffMV(tg, new CalculationMV() {
			
			@Override
			public double[] calculate(TrackedNode node) {
				double[] res = new double[5];
				
				return res;
			}
		});
	}

	@Feature(name = "ObjectFeatureBorderDistance")
	public static double distanceToBorder(final TransitionGraph tg) {
		return traverseConnectedDiffSV(tg, new CalculationSV() {

			@Override
			public double calculate(TrackedNode node) {
				Rectangle2D rect = node.getImageRectangle();
				try {
					long[] imgDims = OffsetHandling.decode(tg
							.getNet()
							.getStringFeature(tg.getNet(),
									TrackingConstants.NETWORK_FEATURE_DIMENSION));
					return Math.min(
							rect.getMinY(),
							Math.min(rect.getMinX(), Math.min(
									imgDims[0] - rect.getMaxX(), imgDims[1]
											- rect.getMaxY())));
				} catch (InvalidFeatureException e) {
					e.printStackTrace();
				} catch (PersistenceException e) {
					e.printStackTrace();
				}
				return Double.MAX_VALUE;
			}
		});
	}

	@Feature(name = "ObjectFeatureBorderOverlap")
	public static double overlapBorder(final TransitionGraph tg) {
		return traverseConnectedDiffSV(tg, new CalculationSV() {

			@Override
			public double calculate(TrackedNode node) {
				Rectangle2D rect = node.getImageRectangle();
				try {
					long[] imgDims = OffsetHandling.decode(tg
							.getNet()
							.getStringFeature(tg.getNet(),
									TrackingConstants.NETWORK_FEATURE_DIMENSION));
					int count = 0;
					List<Cursor<BitType>> list = new LinkedList<Cursor<BitType>>();
					if (rect.getMinX() == 0) {
						// left border
						BresenhamLine<BitType> cursor = new BresenhamLine<BitType>(
								node.getBitmask().getImgPlus(), new Point(0L,
										0L),
								new Point(0, (long) rect.getMinY()));
						list.add(cursor);
					}
					if (rect.getMinY() == 0) {
						// upper border
						BresenhamLine<BitType> cursor = new BresenhamLine<BitType>(
								node.getBitmask().getImgPlus(), new Point(0L,
										0L), new Point((long) rect.getMaxX(),
										0L));
						list.add(cursor);
					}
					if (imgDims[0] - rect.getMaxX() == 0) {
						// right border
						BresenhamLine<BitType> cursor = new BresenhamLine<BitType>(
								node.getBitmask().getImgPlus(), new Point(
										(long) rect.getMaxX(), 0L), new Point(
										(long) rect.getMaxX(), (long) rect
												.getMaxY()));
						list.add(cursor);
					}
					if (imgDims[1] - rect.getMinY() == 0) {
						// lower border
						BresenhamLine<BitType> cursor = new BresenhamLine<BitType>(
								node.getBitmask().getImgPlus(), new Point(0L,
										(long) rect.getMaxY()), new Point(
										(long) rect.getMaxX(), (long) rect
												.getMaxY()));
						list.add(cursor);
					}
					for (Cursor<BitType> cursor : list) {
						while (cursor.hasNext()) {
							if (cursor.next().get())
								count++;
						}
					}
					return count;
				} catch (InvalidFeatureException e) {
					e.printStackTrace();
				} catch (PersistenceException e) {
					e.printStackTrace();
				}
				return Double.MAX_VALUE;
			}
		});
	}
}
