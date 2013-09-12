package org.knime.knip.trackingrevised.nodes.trackingimagegenerator.generator;

import java.util.List;

public interface TraceCodeParseRule {
	public List<Segment> parse(String code, Setting setting);
}