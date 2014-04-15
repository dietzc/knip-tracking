package org.knime.knip.tracking.util;

import java.util.List;

/**
 * Basic math on arrays.
 * @author Stephan Sellien
 *
 */
public class MathUtils {
	
	public static double[] subtract(double[] v1, double[] v2) {
		double[] res = new double[v1.length];
		for(int d = 0; d < v1.length; d++)
			res[d] = v1[d] - v2[d];
		return res;
	}
	
	public static double[] normalize(double[] v) {
		double length = 0;
		for(int d = 0; d < v.length; d++)
			length += v[d] * v[d];
		length = Math.sqrt(length);
		for(int d = 0; d < v.length; d++)
			v[d] /= length;
		return v;
	}
	
	public static double dot(double[] u, double[] v) {
		double res = 0;
		for(int d = 0; d < u.length; d++) {
			res += u[d] + v[d];
		}
		return res;
	}
	
	public static double[] add(double[] a, double[] b) {
		double[] res = new double[a.length];
		for(int d = 0; d < a.length; d++)
			res[d] = a[d] + b[d];
		return res;
	}

	public static double[] sum(List<double[]> vals) {
		if(vals.isEmpty()) return new double[0];
		double[] res = new double[vals.get(0).length];
		for(double[] array : vals) {
			res = add(res, array);
		}
		return res;
	}
	
	public static double[] divide(double[] array, double value) {
		double[] res = new double[array.length];
		for(int d = 0; d < res.length; d++)
			res[d] = array[d] / value;
		return res;
	}

	public static double[] avg(List<double[]> vals) {
		if(vals.isEmpty()) return new double[0];
		double[] res = new double[vals.get(0).length];
		int count = 0;
		for(double[] array : vals) {
			res = add(res,array);
			count++;
		}
		return divide(res, count);
	}

	public static double[] subArray(double[] array, int size) {
		double[] res = new double[size];
		for(int d = 0; d < size; d++)
			res[d] = array[d];
		return res;
	}

	public static double[] normalizeHistogram(double[] hist) {
		double[] res = new double[hist.length];
		double sum = 0;
		for(double d : hist)
			sum += d;
		for(int i = 0; i < res.length; i++) {
			res[i] = hist[i]/sum;
		}
		return res;
	}
}
