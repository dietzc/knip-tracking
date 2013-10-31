package org.knime.knip.trackingrevised.data.features;

import net.imglib2.type.numeric.RealType;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.knip.trackingrevised.data.graph.TransitionGraph;
import org.knime.network.core.algorithm.search.dfs.WeakConnectedComponent;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.exception.PersistenceException;

public abstract class FeatureClass {
	/**
	 * Get the display name for this feature class.
	 * 
	 * @return the display name
	 */
	public abstract String getName();

	protected static <T extends RealType<T>> double traverseConnectedDiffSV(TransitionGraph<T> tg,
			CalculationSV calc) {
		try {
			Partition firstPartition = tg.getNet().getPartition(
					tg.getFirstPartition());
			Partition lastPartition = tg.getNet().getPartition(
					tg.getLastPartition());
			WeakConnectedComponent wcc = new WeakConnectedComponent(
					new ExecutionMonitor(), tg.getNet());
			wcc.start(new ExecutionMonitor());
			for (KPartiteGraph<PersistentObject, Partition> cc : wcc
					.createComponentViews(new ExecutionMonitor(), tg.getNet())) {
				double fpsum = 0.0;
				for (PersistentObject po : cc.getNodes(firstPartition)) {
					double newNumber = calc.calculate(po);
					fpsum = calc.sum(fpsum, newNumber);
				}

				double lpsum = 0.0;
				for (PersistentObject po : cc.getNodes(lastPartition)) {
					double newNumber = calc.calculate(po);
					lpsum = calc.sum(lpsum, newNumber);
				}

				return calc.diff(fpsum, lpsum);
			}
		} catch (PersistenceException e) {
			e.printStackTrace();
		} catch (CanceledExecutionException e) {
			e.printStackTrace();
		}
		return 0.0;
	}

	protected static abstract class CalculationSV {

		public double sum(double sum, double newValue) {
			return sum + newValue;
		}

		public double diff(double fpsum, double lpsum) {
			return lpsum - fpsum;
		}

		public abstract double calculate(PersistentObject po);
	}

	protected static <T extends RealType<T>> double traverseConnectedDiffMV(TransitionGraph<T> tg,
			CalculationMV calc) {
		try {
			Partition firstPartition = tg.getNet().getPartition(
					tg.getFirstPartition());
			Partition lastPartition = tg.getNet().getPartition(
					tg.getLastPartition());
			WeakConnectedComponent wcc = new WeakConnectedComponent(
					new ExecutionMonitor(), tg.getNet());
			wcc.start(new ExecutionMonitor());
			for (KPartiteGraph<PersistentObject, Partition> cc : wcc
					.createComponentViews(new ExecutionMonitor(), tg.getNet())) {
				int count = 0;
				double[] fpsum = null;
				for (PersistentObject po : cc.getNodes(firstPartition)) {
					double[] newNumber = calc.calculate(po);
					fpsum = calc.sum(fpsum, newNumber);
					count++;
				}

				if (fpsum != null) {
					for (int i = 0; i < fpsum.length; i++) {
						fpsum[i] /= count;
					}
				}

				count = 0;
				double[] lpsum = null;
				for (PersistentObject po : cc.getNodes(lastPartition)) {
					double[] newNumber = calc.calculate(po);
					lpsum = calc.sum(lpsum, newNumber);
				}

				if (lpsum != null) {
					for (int i = 0; i < lpsum.length; i++) {
						lpsum[i] /= count;
					}
				}

				return calc.diff(fpsum, lpsum);
			}
		} catch (PersistenceException e) {
			e.printStackTrace();
		} catch (CanceledExecutionException e) {
			e.printStackTrace();
		}
		return 0.0;
	}

	protected static abstract class CalculationMV {

		public double[] sum(double[] sum, double newValue[]) {
			if (sum == null) {
				sum = new double[newValue.length];
			}
			for (int i = 0; i < sum.length; i++) {
				sum[i] += newValue[i];
			}
			return sum;
		}

		public double diff(double[] fpsum, double[] lpsum) {
			// euclidean
			double dist = 0.0;
			if(fpsum == null && lpsum == null)
				return 0.0;
			if(fpsum == null) 
				fpsum = new double[lpsum.length];
			if(lpsum == null)
				lpsum = new double[fpsum.length];
			for (int i = 0; i < fpsum.length; i++) {
				dist += (fpsum[i] * fpsum[i]) - (lpsum[i] * lpsum[i]);
			}
			dist = Math.sqrt(dist);
			return dist;
		}

		public abstract double[] calculate(PersistentObject po);
	}
}
