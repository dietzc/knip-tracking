package org.knime.knip.tracking.nodes.trackingimagegenerator.generator.rules;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.knip.tracking.nodes.trackingimagegenerator.generator.Segment;
import org.knime.knip.tracking.nodes.trackingimagegenerator.generator.Setting;
import org.knime.knip.tracking.nodes.trackingimagegenerator.generator.TraceCodeParseRule;

public class FromToRule implements TraceCodeParseRule {

	private static final String PATTERN = "^(\\(.+\\))-(\\(.*\\))$";

	@Override
	public List<Segment> parse(String code, Setting setting) {
		Pattern pattern = Pattern.compile(PATTERN);
		Matcher matcher = pattern.matcher(code);

		List<Segment> result = new LinkedList<Segment>();

		if (matcher.matches()) {
			Segment start = new SegmentRule().parse(matcher.group(1), setting)
					.get(0);
			Segment end = new SegmentRule().parse(matcher.group(2), setting)
					.get(0);

			assert start.time < end.time;

			double[] diff = new double[start.position.length];

			Random rnd = new Random();
			byte color = (byte) Math.max(50, (byte) rnd.nextInt(256));

			for (int d = 0; d < diff.length; d++) {
				diff[d] = (end.position[d] - start.position[d])
						/ (end.time - start.time);
			}

			double[] position = start.position.clone();
			for (int time = start.time; time <= end.time; time++) {
				result.add(new Segment(position, color));
				for (int d = 0; d < diff.length; d++) {
					position[d] += diff[d];
				}
			}
		}
		return result;
	}

}
