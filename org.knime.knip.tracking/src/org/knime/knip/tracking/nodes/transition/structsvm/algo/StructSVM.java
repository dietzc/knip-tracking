package org.knime.knip.tracking.nodes.transition.structsvm.algo;

import java.util.LinkedList;
import java.util.List;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.knip.base.node.NodeUtils;

public class StructSVM {

	private final static int MAX_ITERATIONS = 100;

	private double[] weights;

	private List<String> classes = new LinkedList<String>();

	private List<String> featureNames = null;

	public void train(BufferedDataTable labeledData) {
		for (int noIteration = 0; noIteration < MAX_ITERATIONS; noIteration++) {
			// determine most violated constraints

		}
	}

	public void predictLabeled(BufferedDataTable labeledData) {
		int classIdx = NodeUtils.firstCompatibleColumn(
				labeledData.getDataTableSpec(), StringValue.class,
				new Integer[0]);
		if(featureNames == null)
			extractFeatureNames(labeledData);
		double[] groundTruth = new double[labeledData.getRowCount()];
		int index = 0;
		// extract ground truth
		for (DataRow row : labeledData) {
			String clazz = ((StringValue) row.getCell(classIdx))
					.getStringValue();
			if (!classes.contains(clazz))
				classes.add(clazz);
			int clazzAsNumber = classes.indexOf(clazz);
			groundTruth[index] = clazzAsNumber;
			index++;
		}

		// get loss coefficients
		double[] losses = HammingLossFunction.coefficients(groundTruth);
		
		//Create Solver Problem
		try {
			LpSolve solver = LpSolve.makeLp(0, labeledData.getRowCount());

			//formulate objective function
			index = 0;
			for(DataRow row : labeledData) {
				double[] features = extractFeatureVector(row, labeledData);
				double tmp = 0.0;
				//<f, w>
				for(int i = 0; i < features.length; i++) {
					tmp += features[i] * weights[i];
				}
				tmp += losses[index];
				//+1 because lpsolve starts with index 1
				solver.setObj((index++)+1, tmp);
			}
			
			//extract constraint
			
		} catch (LpSolveException e) {
			e.printStackTrace();
		}
	}

	private double[] extractFeatureVector(DataRow row, DataTable table) {
		List<Double> features = new LinkedList<Double>();
		for (int col = 0; col < table.getDataTableSpec().getNumColumns(); col++) {
			if (table.getDataTableSpec().getColumnSpec(col).getType()
					.isCompatible(DoubleValue.class)) {
				features.add(((DoubleValue) row.getCell(col)).getDoubleValue());
			}
		}
		double[] res = new double[features.size()];
		int index = 0;
		for(double d : features)
			res[index++] = d;
		return res;
	}

	private void extractFeatureNames(DataTable table) {
		featureNames = new LinkedList<String>();
		for (int col = 0; col < table.getDataTableSpec().getNumColumns(); col++) {
			if (table.getDataTableSpec().getColumnSpec(col).getType()
					.isCompatible(DoubleValue.class)) {
				featureNames
						.add(table.getDataTableSpec().getColumnNames()[col]);
			}
		}
	}

}

class HammingLossFunction {
	public static double[] coefficients(double[] groundTruth) {
		double sum = 0;
		double[] res = new double[groundTruth.length];
		for (double d : groundTruth)
			sum += d;
		for (int i = 0; i < groundTruth.length; i++)
			res[i] = groundTruth[i] / sum;
		return res;
	}

	public static double loss(double[] groundTruth, double[] prediction) {
		double loss = 0;
		double sum = 0;
		for (int i = 0; i < prediction.length; i++) {
			loss += Math.abs(prediction[i] - groundTruth[i]);
			sum += groundTruth[i];
		}

		return loss / sum;
	}
}
