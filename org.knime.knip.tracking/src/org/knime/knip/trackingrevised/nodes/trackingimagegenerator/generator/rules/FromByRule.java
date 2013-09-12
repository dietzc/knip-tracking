package org.knime.knip.trackingrevised.nodes.trackingimagegenerator.generator.rules;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.knip.trackingrevised.nodes.trackingimagegenerator.generator.Segment;
import org.knime.knip.trackingrevised.nodes.trackingimagegenerator.generator.Setting;
import org.knime.knip.trackingrevised.nodes.trackingimagegenerator.generator.TraceCodeParseRule;

public class FromByRule implements TraceCodeParseRule {

	private static final String PATTERN = "^(\\(.+\\))\\+(\\(.*\\))$";

	@Override
	public List<Segment> parse(String code, Setting setting) {
		Pattern pattern = Pattern.compile(PATTERN);
		Matcher matcher = pattern.matcher(code);

		List<Segment> result = new LinkedList<Segment>();

		if (matcher.matches()) {
			Segment start = new SegmentRule().parse(matcher.group(1), setting)
					.get(0);
			Segment moveBy = new SegmentRule().parse(matcher.group(2), setting)
					.get(0);

			double[] diff = moveBy.position;

			Random rnd = new Random();
			byte color = (byte) Math.max(50, (byte) rnd.nextInt(256));

			double[] position = start.position.clone();
			for (int time = start.time; time < setting.time; time++) {
				result.add(new Segment(position.clone(), color));
				for (int d = 0; d < diff.length; d++) {
					position[d] += diff[d];
				}
			}
		}
		return result;
	}

}
