package org.knime.knip.tracking.nodes.trackingimagegenerator.generator.rules;

import java.util.LinkedList;
import java.util.List;

import org.knime.knip.tracking.nodes.trackingimagegenerator.generator.Segment;
import org.knime.knip.tracking.nodes.trackingimagegenerator.generator.Setting;
import org.knime.knip.tracking.nodes.trackingimagegenerator.generator.TraceCodeParseRule;

public class SegmentRule implements TraceCodeParseRule {

	@Override
	public List<Segment> parse(String code, Setting setting) {
		List<Segment> result = new LinkedList<Segment>();
		if (!code.matches("\\((-?\\d+,?)+\\)"))
			return result;
		String[] parts = code.replaceAll("\\(|\\)", "").split(",");
		double[] position = new double[parts.length];
		for (int d = 0; d < parts.length; d++)
			position[d] = Double.parseDouble(parts[d]);
		result.add(new Segment(position));
		return result;
	}

}
