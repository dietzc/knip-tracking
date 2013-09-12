package org.knime.knip.trackingrevised.nodes.adddistanceedges;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.port.PortObjectSpec;
import org.knime.network.core.api.Feature;
import org.knime.network.core.knime.port.GraphPortObjectSpec;
import org.nfunk.jep.JEP;

abstract class JEPHelper {
	public static List<String> encodeNumericColumns(PortObjectSpec spec) {
		List<String> list = new ArrayList<String>();
		Feature[] features = ((GraphPortObjectSpec) spec).getMetaData()
				.getFeatures();
		for (Feature feature : features) {
			if (feature.getType().isNumeric()) {
				String encoded = encode(feature);
				if (!list.contains(encoded))
					list.add(encoded);
				else
					throw new RuntimeException(
							"Feature name is ambigous. Please rename "
									+ feature.getName());
			}
		}
		return list;
	}

	public static String encode(Feature feature) {
		return "$" + feature.getName().replaceAll(" ", "") + "$";
	}

	public static JEP generateJEPwithVariables(PortObjectSpec spec) {
		JEP jep = new JEP();
		jep.addStandardConstants();
		jep.addStandardFunctions();
		for (String var : encodeNumericColumns(spec)) {
			jep.addVariable(var, 0);
		}
		return jep;
	}
}
