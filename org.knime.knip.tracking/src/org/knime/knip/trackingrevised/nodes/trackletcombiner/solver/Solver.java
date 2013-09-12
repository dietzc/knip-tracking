package org.knime.knip.trackingrevised.nodes.trackletcombiner.solver;

import java.util.List;

import org.knime.core.node.ExecutionContext;

public interface Solver {
	public double[] solve(List<Integer>[] transposedIndices, int hypoCount,
			double[] propabilities, long timeout, ExecutionContext exec)
			throws Throwable;
}
