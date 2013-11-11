package org.knime.knip.tracking.nodes.trackletcombiner.solver;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.linear.LinearConstraint;
import org.apache.commons.math3.optimization.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optimization.linear.Relationship;
import org.apache.commons.math3.optimization.linear.SimplexSolver;
import org.knime.core.node.ExecutionContext;

public class ApacheCommonsMathSolver implements Solver {

	@Override
	public double[] solve(List<Integer>[] transposedIndices, int hypoCount,
			double[] propabilities, long timeout, ExecutionContext exec)
			throws Throwable {
		LinearObjectiveFunction f = new LinearObjectiveFunction(propabilities,
				0);
		Collection<LinearConstraint> constraints = new LinkedList<LinearConstraint>();
		for (List<Integer> list : transposedIndices) {
			double row[] = new double[hypoCount];
			for (int i : list) {
				row[i] = 1;
			}
			constraints.add(new LinearConstraint(row, Relationship.EQ, 1));
		}
		SimplexSolver solver = new SimplexSolver();
		solver.setMaxIterations(100000);
		PointValuePair pvp = solver.optimize(f, constraints, GoalType.MAXIMIZE,
				true);
		double[] point = pvp.getPointRef();
		System.out.println("ACM result: " + pvp.getValue());
		return point;
	}

}
