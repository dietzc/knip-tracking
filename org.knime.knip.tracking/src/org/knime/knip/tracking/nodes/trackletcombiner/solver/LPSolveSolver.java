package org.knime.knip.tracking.nodes.trackletcombiner.solver;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import lpsolve.AbortListener;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;

public class LPSolveSolver implements Solver {

	@Override
	public double[] solve(List<Integer>[] transposedIndices, int hypoCount,
			double[] propabilities, long timeout, ExecutionContext exec)
			throws Throwable {
		// fugly fix
		System.loadLibrary("lpsolve55");

		int n = propabilities.length;
		LpSolve solver = LpSolve.makeLp(0, n);

		solver.setVerbose(LpSolve.IMPORTANT);

		// index 0 is not used -> copy
		double[] obj = new double[propabilities.length + 1];
		System.arraycopy(propabilities, 0, obj, 1, propabilities.length);
		solver.setObjFn(obj);
		obj = null;

		for (List<Integer> list : transposedIndices) {
			double[] values = new double[list.size()];
			Arrays.fill(values, 1);
			int[] cols = new int[values.length];
			Iterator<Integer> it = list.iterator();
			// +1 because lpsolve indices start from 1
			for (int i = 0; i < cols.length; i++) {
				cols[i] = it.next() + 1;
			}
			solver.addConstraintex(values.length, values, cols, LpSolve.EQ, 1.0);
		}

		// for(double[] row : transposedMatrix) {
		// double[] temp = new double[row.length+1];
		// System.arraycopy(row, 0, temp, 1, row.length);
		// solver.addConstraint(temp, LpSolve.EQ, 1.0);
		// }
		solver.setMaxim();
		for (int i = 1; i <= n; i++)
			solver.setBinary(i, true);
		// solver.writeLp("../lastone.lp");
		// solver.writeMps("../lastone.mps");

		solver.putAbortfunc(new AbortListener() {

			@Override
			public boolean abortfunc(LpSolve problem, Object userhandle)
					throws LpSolveException {
				try {
					((ExecutionMonitor) userhandle).checkCanceled();
					return false;
				} catch (Exception e) {
					return true;
				}
			}
		}, exec);
		//
		// System.out.println("#presolveloops: " + solver.getPresolveloops());

		// timeout in seconds
		solver.setTimeout(timeout);

		int retval = solver.solve();

		if (retval == LpSolve.OPTIMAL) {
			System.out.println("optimal solution");
		} else if (retval == LpSolve.SUBOPTIMAL) {
			System.out.println("suboptimal solution");
		} else if (retval == LpSolve.TIMEOUT) {
			System.out.println("TIMEOUT");
		} else {
			// Field[] fields = LpSolve.class.getDeclaredFields();
			// for(Field field : fields) {
			// try{
			// System.out.println(field + " " + field.getInt(solver));
			// } catch(Exception e) {}
			// }
			System.out.println("solver return value: " + retval);
		}

		// System.out.println(solver.getNrows() + "x" + solver.getNcolumns() +
		// " vs " + solver.getNorigRows() + "x" + solver.getNorigColumns());

		double[] result = solver.getPtrVariables();
		System.out.println("Lpsolve result: " + solver.getObjective());

		// solver.printLp();

		solver.deleteLp();
		return result;
	}
}
