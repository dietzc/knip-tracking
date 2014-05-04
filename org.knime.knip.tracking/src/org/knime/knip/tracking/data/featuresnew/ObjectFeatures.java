package org.knime.knip.tracking.data.featuresnew;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.region.BresenhamLine;
import net.imglib2.img.Img;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.ConvexHull2D;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.knip.tracking.util.MathUtils;
import org.knime.knip.tracking.util.OffsetHandling;
import org.knime.knip.tracking.util.TrackingConstants;
import org.knime.knip.tracking.util.TransitionGraphUtil;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.exception.PersistenceException;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import com.telmomenezes.jfastemd.Feature1D;
import com.telmomenezes.jfastemd.JFastEMD;
import com.telmomenezes.jfastemd.Signature;

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
	 * Object volume evenness feature relates the volume of child cells.
	 * 
	 * @param tg
	 *            the {@link TransitionGraph}
	 * @return volume evenness
	 */
	public static TrackedNodeFeature volumeEvenness() {
		return new TrackedNodeFeature("ObjectVolumeEvenness") {

			@Override
			public double[] calcInt(TrackedNode node) {
				int count = 0;
				for (BitType pixel : node.getBitmask().getImgPlus()) {
					if (pixel.get())
						count++;
				}
				return new double[] { count };
			}
			
			@Override
			public double diff(List<double[]> fpVals, List<double[]> lpVals, TransitionGraph tg) {
				if(lpVals.size() != 2) return Double.NaN;
				double child1Volume = lpVals.get(0)[0];
				double child2Volume = lpVals.get(1)[0];
				double c1 = Math.min(child1Volume, child2Volume);
				double c2 = Math.max(child1Volume, child2Volume);
				return c1 / c2;
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
	 * only mean is used.
	 * 
	 * @param tg
	 *            the {@link TransitionGraph}
	 * @return position
	 */
	public static TrackedNodeFeature position() {
		return new TrackedNodeFeature("ObjectPosition") {
			
			public double[] calcInt(TrackedNode node) {
				int noDims = (int) node.getBitmask().getDimensions().length;
				if (noDims == 3)
					noDims = 2; // ignore 3th dimension
				// offset
				double[] upperLeftCorner = new double[noDims];
				upperLeftCorner[0] = node.getImageRectangle().getX();
				upperLeftCorner[1] = node.getImageRectangle().getY();
				
				double[] coords = new double[noDims];
				double count = 0;
				
				Cursor<BitType> cursor = node.getBitmask().getImgPlus().localizingCursor();
				
				while(cursor.hasNext()) {
					if(cursor.next().get()) {
						for(int d = 0; d < noDims; d++) {
							coords[d] += cursor.getDoublePosition(d);
						}
						count++;
					}
				}
				
				for(int d = 0; d < noDims; d++) {
					coords[d] /= count;
					
					coords[d] += upperLeftCorner[d];
				}
				
				return coords;
			}
			
			@Override
			public double diff(List<double[]> fpVals, List<double[]> lpVals, TransitionGraph tg) {
				double[] fp = MathUtils.sum(fpVals);
				double[] lp = MathUtils.sum(lpVals);
				//squared as in BOT
				return euclideanDistanceSqr(fp, lp);
			}

//			@Override
//			public double[] calcInt(TrackedNode node) {
//				int noDims = (int) node.getBitmask().getDimensions().length;
//				if (noDims == 3)
//					noDims = 2; // ignore 3th dimension
//				double[] res = new double[4 * noDims];
//
//				double[] coords = new double[noDims];
//				double[] variance = new double[noDims];
//				double[] skew = new double[noDims];
//				double[] kurtosis = new double[noDims];
//				int count = 0;
//
//				// offset
//				double[] upperLeftCorner = new double[noDims];
//				upperLeftCorner[0] = node.getImageRectangle().getX();
//				upperLeftCorner[1] = node.getImageRectangle().getY();
//
//				Cursor<BitType> cursor = node.getBitmask().getImgPlus()
//						.localizingCursor();
//				while (cursor.hasNext()) {
//					if (cursor.next().get()) {
//						for (int d = 0; d < noDims; d++) {
//							coords[d] += cursor.getDoublePosition(d)
//									+ upperLeftCorner[d];
//						}
//						count++;
//					}
//				}
//				// avg
//				for (int d = 0; d < noDims; d++)
//					res[d] = coords[d] / count;
//
//				cursor.reset();
//				while (cursor.hasNext()) {
//					if (cursor.next().get()) {
//						for (int d = 0; d < noDims; d++) {
//							double delta = cursor.getDoublePosition(d)
//									+ upperLeftCorner[d] - res[d];
//							variance[d] += delta * delta;
//							skew[d] += delta * delta * delta;
//							kurtosis[d] += delta * delta * delta * delta;
//						}
//					}
//				}
//				// avg
//				for (int d = 0; d < noDims; d++) {
//					res[noDims + d] = variance[d] / count;
//					if (variance[d] != 0) {
//						res[2 * noDims + d] = skew[d] / count
//								/ Math.pow(res[noDims + d], 3.0 / 2.0);
//
//						res[3 * noDims + d] = kurtosis[d] / count
//								/ (res[noDims + d] * res[noDims + d]);
//					}
//				}
//
//				return res;
//			}
		};
	}

	/**
	 * Weighted Mean position feature Calculate the mean position and its higher
	 * central moments. Each position is weighted with it's intensity value.
	 * Output structure: size = 4*N [0 .. N-1] mean coordinates [N .. 2*N-1]
	 * variance [2*N .. 3*N-1] skew [3*N .. 4*N-1] kurtosis
	 * 
	 * only mean is used.
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
				if (noDims == 3)
					noDims = 2; // ignore 3th dimension
				// offset
				double[] upperLeftCorner = new double[noDims];
				upperLeftCorner[0] = node.getImageRectangle().getX();
				upperLeftCorner[1] = node.getImageRectangle().getY();
				
				double[] coords = new double[noDims];
				double sum = 0;
				
				Cursor<BitType> cursor = node.getBitmask().getImgPlus().localizingCursor();
				RandomAccess<? extends RealType<?>> ra = node.getSegmentImage().getImgPlus().randomAccess();
				
				while(cursor.hasNext()) {
					if(cursor.next().get()) {
						ra.setPosition(cursor);
						double value = ra.get().getRealDouble();
						for(int d = 0; d < noDims; d++) {
							coords[d] += value * ra.getDoublePosition(d);
						}
						sum += value;
					}
				}
				
				for(int d = 0; d < noDims; d++) {
					coords[d] /= sum;
					
					coords[d] += upperLeftCorner[d];
				}
				
				return coords;
			}
			
			@Override
			public double diff(List<double[]> fpVals, List<double[]> lpVals, TransitionGraph tg) {
				double[] fp = MathUtils.sum(fpVals);
				double[] lp = MathUtils.sum(lpVals);
				//squared as in BOT
				return euclideanDistanceSqr(fp, lp);
			}

//			@Override
//			public double[] calcInt(TrackedNode node) {
//				int noDims = (int) node.getBitmask().getDimensions().length;
//				if (noDims == 3)
//					noDims = 2; // ignore 3th dimension
//				double[] res = new double[4 * noDims];
//
//				double[] coords = new double[noDims];
//				double[] variance = new double[noDims];
//				double[] skew = new double[noDims];
//				double[] kurtosis = new double[noDims];
//				double sum = 0.0;
//
//				// offset
//				double[] upperLeftCorner = new double[noDims];
//				upperLeftCorner[0] = node.getImageRectangle().getX();
//				upperLeftCorner[1] = node.getImageRectangle().getY();
//
//				Cursor<BitType> cursor = node.getBitmask().getImgPlus()
//						.localizingCursor();
//				RandomAccess<? extends RealType<?>> ra = node.getSegmentImage()
//						.getImgPlus().randomAccess();
//				while (cursor.hasNext()) {
//					if (cursor.next().get()) {
//						ra.setPosition(cursor);
//						double value = ra.get().getRealDouble();
//						for (int d = 0; d < noDims; d++) {
//							coords[d] += value
//									* (cursor.getDoublePosition(d) + upperLeftCorner[d]);
//						}
//						sum += value;
//					}
//				}
//				// avg
//				for (int d = 0; d < noDims; d++)
//					res[d] = coords[d] / sum;
//
//				cursor.reset();
//				while (cursor.hasNext()) {
//					if (cursor.next().get()) {
//						ra.setPosition(cursor);
//						double value = ra.get().getRealDouble();
//						for (int d = 0; d < noDims; d++) {
//							double delta = (cursor.getDoublePosition(d) + upperLeftCorner[d])
//									- res[d];
//							variance[d] += value * delta * delta;
//							skew[d] += value * delta * delta * delta;
//							kurtosis[d] += value * delta * delta * delta
//									* delta;
//						}
//					}
//				}
//				// avg
//				for (int d = 0; d < noDims; d++) {
//					res[noDims + d] = variance[d] / sum;
//					if (variance[d] != 0) {
//						res[2 * noDims + d] = skew[d] / sum
//								/ Math.pow(res[noDims + d], 3.0 / 2.0);
//						res[3 * noDims + d] = kurtosis[d] / sum
//								/ (res[noDims + d] * res[noDims + d]);
//					}
//				}
//
//				return res;
//			}
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
		
			@Override
			public double diff(List<double[]> fpVals, List<double[]> lpVals, TransitionGraph tg) {
				double[] fp = MathUtils.avg(fpVals);
				double[] lp = MathUtils.avg(lpVals);
				//only eigenvalues are used in bot
				if(fp.length > 2) {
					fp = MathUtils.subArray(fp, 2);
				} else
					System.err.println("fp.length <= 2 ?!");
				if(lp.length > 2) {
					lp = MathUtils.subArray(lp, 2);
				} else
					System.err.println("lp.length <= 2 ?!");
				return euclideanDistance(fp, lp);
			}
		};
	}

	/**
	 * Bounding Box feature Find the smallest possible box that contains the
	 * whole object Output stucture: size = 2*N + 1 [0 .. N-1] minimum
	 * coordinates (included by the object) [N .. 2*N-1] maximum coordinates
	 * (excluded by the object) [2*N] Fill factor: <object volume> / <bounding
	 * box volume>
	 * 
	 * Deprecated because not really useful ?
	 */
	@Deprecated
	public static TrackedNodeFeature boundingBox() {
		return new TrackedNodeFeature("ObjectBoundingBox") {

			@Override
			public double[] calcInt(TrackedNode node) {
				int noDims = node.getBitmask().getDimensions().length;
				if(noDims == 3) noDims = 2; //ignore 3rd dimension
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
				// offset
				double[] upperLeftCorner = new double[noDims];
				upperLeftCorner[0] = node.getImageRectangle().getX();
				upperLeftCorner[1] = node.getImageRectangle().getY();
				double fillFactor = volume;
				for (int d = 0; d < noDims; d++) {
					double length = max[d] - min[d] + 1;
					fillFactor /= length;
					res[d] += upperLeftCorner[d];
					res[noDims + d] += upperLeftCorner[d];
				}
				res[2 * noDims] = fillFactor;
				
				System.out.println(node.getImageRectangle() + " " + Arrays.toString(res));
				
				return res;
			}
			
			@Override
			public double diff(List<double[]> fpVals, List<double[]> lpVals, TransitionGraph tg) {
				//TODO: MERGE THEM
				return super.diff(fpVals, lpVals, tg);
			}
		};
	}

	/**
	 * Intensity feature Calculate the mean intensity. 
	 */
	public static TrackedNodeFeature intensityMean() {
		return new TrackedNodeFeature("ObjectIntensityMean") {

			@Override
			public double[] calcInt(TrackedNode node) {

				double mean = 0;
				int count = 0;
				Cursor<BitType> cursor = node.getBitmask().getImgPlus().localizingCursor();
				RandomAccess<? extends RealType<?>> ra = node.getSegmentImage().getImgPlus().randomAccess();
				while(cursor.hasNext()) {
					if(cursor.next().get()) {
						ra.setPosition(cursor);
						RealType<?> pixel = ra.get();
						mean += pixel.getRealDouble();
						count++;
					}
				}
				mean /= count;

				return new double[] {mean};
			}
		};
	}
	
	/**
	 * Intensity feature Calculate the intensity sum. 
	 */
	public static TrackedNodeFeature intensitySum() {
		return new TrackedNodeFeature("ObjectIntensitySum") {

			@Override
			public double[] calcInt(TrackedNode node) {

				double sum = 0;
				
				Cursor<BitType> cursor = node.getBitmask().getImgPlus().localizingCursor();
				RandomAccess<? extends RealType<?>> ra = node.getSegmentImage().getImgPlus().randomAccess();
				while(cursor.hasNext()) {
					if(cursor.next().get()) {
						ra.setPosition(cursor);
						RealType<?> pixel = ra.get();
						sum += pixel.getRealDouble();
					}
				}

				return new double[] {sum};
			}
		};
	}
	
	/**
	 * Intensity feature Calculate the intensity deviation. 
	 */
	public static TrackedNodeFeature intensityDeviation() {
		return new TrackedNodeFeature("ObjectIntensityDeviation") {

			@Override
			public double[] calcInt(TrackedNode node) {

				double mean = 0;
				int count = 0;
				
				Cursor<BitType> cursor = node.getBitmask().getImgPlus().localizingCursor();
				RandomAccess<? extends RealType<?>> ra = node.getSegmentImage().getImgPlus().randomAccess();
				while(cursor.hasNext()) {
					if(cursor.next().get()) {
						ra.setPosition(cursor);
						RealType<?> pixel = ra.get();
						mean += pixel.getRealDouble();
						count++;
					}
				}
				mean /= count;
				
				double variance = 0;
				
				cursor = node.getBitmask().getImgPlus().localizingCursor();
				ra = node.getSegmentImage().getImgPlus().randomAccess();
				while(cursor.hasNext()) {
					if(cursor.next().get()) {
						ra.setPosition(cursor);
						RealType<?> pixel = ra.get();
						double delta = (pixel.getRealDouble() - mean);
						variance += delta*delta;
					}
				}
				
				variance = Math.sqrt(variance);

				return new double[] {variance};
			}
		};
	}
	

	/**
	 * A histogram of the intensity.
	 */
	public static TrackedNodeFeature intensityHistogram() {
		return new TrackedNodeFeature("ObjectIntensityHistogram") {
			
			public final static int BIN_WIDTH = 5;
			public final static int BIN_MAX_VALUE = 255;
			public final static int BIN_MIN_VALUE = 0;
			
			public final static int NUMBER_BINS = (BIN_MAX_VALUE - BIN_MIN_VALUE) / BIN_WIDTH + 1; 

			@Override
			public double[] calcInt(TrackedNode node) {
				double[] hist = new double[NUMBER_BINS];
				double min = Double.MAX_VALUE;
				double max = 0;
								
				Cursor<BitType> cursor = node.getBitmask().getImgPlus().localizingCursor();
				RandomAccess<? extends RealType<?>> ra = node.getSegmentImage().getImgPlus().randomAccess();
				while(cursor.hasNext()) {
					if(cursor.next().get()) {
						ra.setPosition(cursor);
						RealType<?> pixel = ra.get();
						min = Math.min(min, pixel.getRealDouble());
						max = Math.max(max, pixel.getRealDouble());
					}
				}
				
				double interval = Math.ceil((max - min) / NUMBER_BINS);
				
				cursor = node.getBitmask().getImgPlus().localizingCursor();
				ra = node.getSegmentImage().getImgPlus().randomAccess();
				while(cursor.hasNext()) {
					if(cursor.next().get()) {
						ra.setPosition(cursor);
						RealType<?> pixel = ra.get();
						int bin = (int)((pixel.getRealDouble() - min) / interval);
						if(bin >= hist.length) {
							bin = hist.length - 1;
						}
						hist[bin]++;
					}
				}
				
				hist = MathUtils.normalizeHistogram(hist);
				
				return hist;
			}
			
			@Override
			public double diff(List<double[]> fpVals, List<double[]> lpVals, TransitionGraph tg) {
				double[] fp = MathUtils.avg(fpVals);
				double[] lp = MathUtils.avg(lpVals);
				
				Signature sigfp = Feature1D.createSignature(fp);
				Signature siglp = Feature1D.createSignature(lp);
				return JFastEMD.distance(sigfp, siglp, 0);
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
							.decode(node
									.getNetwork()
									.getFeatureString(
											node.getNetwork(),
											TrackingConstants.NETWORK_FEATURE_DIMENSION));
					result = Math.min(
							rect.getMinY(),
							Math.min(rect.getMinX(), Math.min(
									imgDims[0] - rect.getMaxX(), imgDims[1]
											- rect.getMaxY())));
				} catch (Exception e) {
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
							.decode(node
									.getNetwork()
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
					if (imgDims[1] - rect.getMaxY() == 0) {
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
					KPartiteGraph<PersistentObject, Partition> net = node
							.getNetwork();
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
				return new double[] { result };
			}
		};
	}
	
	public static TrackedNodeFeature shapeCompactness() {
		return new TrackedNodeFeature("ObjectShapeCompactness") {
			
			@Override
			public double[] calcInt(TrackedNode node) {
				return new double[0];
			}
			
			@Override
			protected int getFeatureDimension() {
				return 0;
			}
			
			@Override
			public double diff(List<double[]> fpVals, List<double[]> lpVals,
					TransitionGraph tg) {
				if (tg.getNodes(tg.getFirstPartition()).size() == 0
						|| tg.getNodes(tg.getLastPartition()).size() == 0)
					return Double.NaN;

				// remember.. there are only 1:n and m:1 transitions generated!

				Img<BitType> fpImg = tg.getNodes(tg.getFirstPartition()).iterator()
						.next().getBitmask().getImgPlus().getImg();
				Img<BitType> lpImg = tg.getNodes(tg.getLastPartition()).iterator()
						.next().getBitmask().getImgPlus().getImg();
				if (tg.getNodes(tg.getFirstPartition()).size() > 1) {
					fpImg = TransitionGraphUtil.createPartitionImg(tg, tg.getFirstPartition());
					ConvexHull2D convexHull = new ConvexHull2D(
							0, 1, true);
					convexHull.compute(fpImg, fpImg);
				}
				if (tg.getNodes(tg.getLastPartition()).size() > 1) {
					lpImg = TransitionGraphUtil.createPartitionImg(tg, tg.getLastPartition());
					ConvexHull2D convexHull = new ConvexHull2D(
							0, 1, true);
					convexHull.compute(lpImg, lpImg);
				}

				double fpNumPix = 0;
				for (BitType b : fpImg)
					if (b.get())
						fpNumPix++;

				double lpNumPix = 0;
				for (BitType b : lpImg)
					if (b.get())
						lpNumPix++;

				return fpNumPix / lpNumPix;
			}
		};
	}
	
	public static TrackedNodeFeature anglePattern() {
		return new TrackedNodeFeature("ObjectAnglePattern") {
			
			@Override
			public double[] calcInt(TrackedNode node) {
				return new double[]{node.getDoublePosition(0), node.getDoublePosition(1)};
			}
			
			@Override
			public double diff(List<double[]> fpVals, List<double[]> lpVals,
					TransitionGraph tg) {
				if(fpVals.size() == 1 && lpVals.size() == 2) {
					return anglePatternDistance(fpVals.get(0), lpVals);	
				} else if (lpVals.size() == 1 && fpVals.size() == 2) {
					return anglePatternDistance(lpVals.get(0), fpVals);
				}
				return Double.NaN;
			}
		};
	}
}
