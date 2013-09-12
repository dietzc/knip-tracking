package org.knime.knip.trackingrevised.nodes.trackletcombiner.solver;

import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.SolverFactoryLpSolve;

public class JavaILPSolver implements Solver {

	@Override
	public double[] solve(double[][] transposedMatrix, double[] propabilities)
			throws Throwable {
		Problem problem = new Problem();
		String[] ilpVars = new String[propabilities.length];
		for(int i = 0; i < ilpVars.length; i++) {
			ilpVars[i] = "x" + i;
			problem.setVarType(ilpVars[i], Integer.class);
		}	
		
		Linear linear = new Linear();
		for(int i = 0; i < ilpVars.length; i++) {
			linear.add(propabilities[i], ilpVars[i]);
		}
		problem.setObjective(linear, OptType.MAX);
		
		for(int row = 0; row < transposedMatrix.length; row++) {
			linear = new Linear();
			for(int col = 0; col < transposedMatrix[row].length; col++) {
				linear.add(transposedMatrix[row][col], ilpVars[col]);
			}
			problem.add(linear, "=", 1);
		}
		
		System.loadLibrary("lpsolve55");
		
		net.sf.javailp.Solver ilpsolver = new SolverFactoryLpSolve().get();
		Result res = ilpsolver.solve(problem);
		System.out.println(res);
		return new double[0];
	}

}
