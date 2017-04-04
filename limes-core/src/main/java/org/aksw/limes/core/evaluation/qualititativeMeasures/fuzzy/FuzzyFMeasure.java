package org.aksw.limes.core.evaluation.qualititativeMeasures.fuzzy;

import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.evaluation.qualititativeMeasures.APRF;
import org.aksw.limes.core.evaluation.qualititativeMeasures.IQualitativeMeasure;
import org.aksw.limes.core.evaluation.qualititativeMeasures.Precision;
import org.aksw.limes.core.evaluation.qualititativeMeasures.Recall;
import org.aksw.limes.core.io.mapping.AMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * F-Measure is the weighted average of the precision and recall
 *
 * @author Mohamed Sherif(sherif@informatik.uni-leipzig.de)
 * @version 1.1.2
 * @since 1.1.2
 */
public class FuzzyFMeasure extends AFuzzeyMeasures implements IQualitativeMeasure {
    static Logger logger = LoggerFactory.getLogger(FuzzyFMeasure.class);

    /** 
     * The method calculates the F-Measure of the machine learning predictions 
     * compared to a gold standard.
     * 
     * @param predictions The predictions provided by a machine learning algorithm
     * @param goldStandard It contains the gold standard (reference mapping) 
     *          combined with the source and target URIs
     * @return double - This returns the calculated F-Measure
     */
    @Override
    public double calculate(AMapping predictions, GoldStandard goldStandard) {

        double p = precision(predictions, goldStandard);
        double r = recall(predictions, goldStandard);

        if (p + r > 0d)
            return 2 * p * r / (p + r);
        else
            return 0d;

    }

    /** 
     * The method calculates the fuzzy recall of the machine learning 
     * predictions compared to a gold standard.
     * 
     * @param predictions The predictions provided by a machine learning algorithm
     * @param goldStandard It contains the gold standard (reference mapping) 
     *          combined with the source and target URIs
     * @return double - This returns the calculated recall
     */
    public double recall(AMapping predictions, GoldStandard goldStandard) {
        return new FuzzyRecall().calculate(predictions, goldStandard);
    }

    /** 
     * The method calculates the fuzzy precision of the machine learning
     * predictions compared to a gold standard.
     * 
     * @param predictions The predictions provided by a machine learning algorithm
     * @param goldStandard It contains the gold standard (reference mapping) 
     *             combined with the source and target URIs
     * @return double - This returns the calculated precision
     */
    public double precision(AMapping predictions, GoldStandard goldStandard) {
        return new FuzzyPrecision().calculate(predictions, goldStandard);
    }

}
