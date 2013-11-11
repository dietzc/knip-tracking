package org.knime.knip.tracking.util;

public interface TrackingConstants {
	public final static String DISTANCE_EDGE_PARTITION = "distanceEdges";
	public final static String TRACKLET_EDGE_PARTITION = "trackletEdges";
	public final static int MIN_TRACKLET_PROBABILITY = 80;

	public static final String FEATURE_BITMASK = "Bitmask";
	public final static String FEATURE_TRACKLET_NUMBER = "trackletNumber";
	public final static String FEATURE_ISTRACKLETEND = "isTrackletEnd";
	public final static String FEATURE_TRACKLETSTARTNODE = "trackletStartNodeID";
	public final static String FEATURE_TRACKLET_SIZE = "trackletSize";

	public final static String FEATURE_BITMASK_OFFSET = "BitmaskOffset";
	public final static String FEATURE_SEGMENT_IMG = "SegmentImg";

	// network features
	public final static String NETWORK_FEATURE_DIMENSION = "nfImageDimension";
	public final static String NETWORK_FEATURE_IMAGE_ROWKEY = "nfImageRowKey";
	public final static String NETWORK_FEATURE_MAX_EUCLIDEAN_DISTANCE = "nfMaxEuclideanDistance";
	public static final String NETWORK_FEATURE_TIME_AXIS = "nfImageTimeAxis";
	public static final String NETWORK_FEATURE_IMAGE_AXES = "nfImageAxes";
	public static final String NETWORK_FEATURE_STDEV = "nfStDev";

	// transition graph features
	public final static String TRANSITION_GRAPH_FEATURE_OFFSET = "tgfOffset";

	// Might become parameters in future
	// Adjust distribution
	public final static double LAMDA_1 = 5;
	public final static double LAMDA_2 = 30;
	public final static double LAMDA_3 = 25;// 25;
	// Maximum values
	public final static double DELTA_T = 0;// 15;
	public final static double DELTA_S = 2;// 40;
	// miss detection rate of the cell detector
	public final static double ALPHA = 0.0001;
	// minimal init propability
	public final static double ETA = 0.00000001;
	
	// name of the GAP partition
	public static final String GAP_PARTITION_NAME = "gapPartition";

}
