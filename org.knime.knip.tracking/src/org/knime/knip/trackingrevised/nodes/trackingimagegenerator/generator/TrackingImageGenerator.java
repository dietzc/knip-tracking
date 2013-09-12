package org.knime.knip.trackingrevised.nodes.trackingimagegenerator.generator;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.knime.knip.trackingrevised.nodes.trackingimagegenerator.generator.rules.FromByRule;
import org.knime.knip.trackingrevised.nodes.trackingimagegenerator.generator.rules.FromToRule;
import org.knime.knip.trackingrevised.nodes.trackingimagegenerator.generator.rules.SegmentRule;

public class TrackingImageGenerator {

	public final static String TESTCODE = "500x500x25\n"
			+ "(0,0,0)-(250,250,15)\n" + "(400,100,10)\n"
			+ "(0,500,0)+(1,-1,1)\n";

	private final static Set<TraceCodeParseRule> RULES = new HashSet<TraceCodeParseRule>();

	public TrackingImageGenerator() {
		RULES.add(new FromToRule());
		RULES.add(new FromByRule());
		RULES.add(new SegmentRule());
	}

	public Img<UnsignedByteType> parse(String code) {
		String[] parts = code.split("\\n");
		// get setting
		Setting setting = new Setting(parts[0]);
		List<Segment> pqueue = new LinkedList<Segment>();
		boolean isMultilineComment = false;
		for (int l = 1; l < parts.length; l++) {
			String line = parts[l];
			// ignore comments
			if (line.startsWith("//") || line.startsWith("#"))
				continue;
			// multiline comments
			if (line.startsWith("/*")) {
				isMultilineComment = true;
				continue;
			}
			if (line.startsWith("*/")) {
				isMultilineComment = false;
				continue;
			}
			if (isMultilineComment)
				continue;

			for (TraceCodeParseRule rule : RULES) {
				List<Segment> result = rule.parse(line, setting);
				pqueue.addAll(result);
			}
		}

		return drawSegments(pqueue, setting);
	}

	private Img<UnsignedByteType> drawSegments(List<Segment> segments,
			Setting setting) {
		// add channel dimension
		long[] dims = setting.dimension;
		Img<UnsignedByteType> img = new ArrayImgFactory<UnsignedByteType>()
				.create(dims, new UnsignedByteType());
		RandomAccess<UnsignedByteType> ra = img.randomAccess();
		for (Segment segment : segments) {
			long[] position = segment.longPosition();
			int radius = segment.radius;
			for (int d = 2; d < position.length; d++)
				ra.setPosition(position[d], d);
			// draw a rectangle
			for (int y = -radius; y < radius; y++) {
				if (position[1] + y < 0 || position[1] + y >= dims[1])
					continue;
				for (int x = -radius; x < radius; x++) {
					if (position[0] + x < 0 || position[0] + x >= dims[0])
						continue;
					ra.setPosition(position[0] + x, 0);
					ra.setPosition(position[1] + y, 1);
					// set color
					ra.get().set(segment.color);
				}
			}
			// single pixel variant
			// ra.setPosition(position[0], 0);
			// ra.setPosition(position[1], 1);
			// ra.get().set(255);
		}
		return img;
	}

	// public static void main(String[] args) {
	// TrackingImageGenerator tng = new TrackingImageGenerator();
	//
	// Img<UnsignedByteType> img = tng.parse(TESTCODE);
	// }

}
