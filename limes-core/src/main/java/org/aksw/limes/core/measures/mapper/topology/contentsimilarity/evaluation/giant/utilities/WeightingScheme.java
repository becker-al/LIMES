package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation.giant.utilities;

public enum WeightingScheme {
    CF,         // Co-occurrence Frequency
    JS,         // Jaccard Similarity
    X2,         // Pearson x^2
    MBR,        // MBR Overlap
    POINTS      // Inverse Sum of Points
}
