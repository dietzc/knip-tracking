package org.knime.knip.trackingrevised.nodes.trackletcombiner.solver;

import com.winvector.linalg.colt.NativeMatrix;
import com.winvector.lp.LPEQProb;
import com.winvector.lp.LPSoln;
import com.winvector.lp.LPSolver;
import com.winvector.lp.impl.RevisedSimplexSolver;
import com.winvector.sparse.ColumnMatrix;

public class WVLPSolver implements Solver {

	@Override
	public double[] solve(double[][] transposedMatrix, double[] propabilities) throws Throwable {
		
		// invert because solver minimizes instead of maximizes
		for (int i = 0; i < propabilities.length; i++) {
			propabilities[i] = -propabilities[i];
		}

		double[] b = new double[transposedMatrix.length];
		for (int i = 0; i < b.length; i++) {
			b[i] = 1;
		}

		NativeMatrix A = NativeMatrix.factory.newMatrix(
				transposedMatrix.length, transposedMatrix[0].length, true);
		for (int row = 0; row < A.rows(); row++) {
			A.setRow(row, transposedMatrix[row]);
		}
		ColumnMatrix Ac = new ColumnMatrix(A);

		LPEQProb prob = new LPEQProb(Ac, b, propabilities);
		LPSolver solver = new RevisedSimplexSolver();
		LPSoln sol = solver.solve(prob, null, 1e-6, 1000,
				NativeMatrix.factory);
				
		return sol.primalSolution;
	}

}
