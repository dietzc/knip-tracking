package org.knime.knip.tracking.util;

public class DoubleHandler {
	public static double[] decode(String s) {
		String[] parts = s.split("\\s+");
		double[] vals = new double[parts.length];
		for(int p = 0; p < parts.length; p++) {
			vals[p] = Double.parseDouble(parts[p]);
		}
		return vals;
	}
	
	public static String encode(double[] vals) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < vals.length;i++){
			sb.append(vals);
			if(i < vals.length - 1)
				sb.append(' ');
		}
		return sb.toString();
	}
	
	public static void increaseBy(double[] a, double[] b) {
		for(int d = 0; d < a.length; d++) {
			a[d] += b[d];
		}
	}
	
	public static double[] average(double[]... vals) {
		double[] result = new double[vals[0].length];
		int counter = 0;
		for(double[] array : vals) {
			for(int i = 0; i < array.length; i++) {
				result[i] += array[i];
			}
			counter++;
		}
		for(int i = 0; i < result.length; i++) {
			result[i] /= counter;
		}
		return result;
	}
}
