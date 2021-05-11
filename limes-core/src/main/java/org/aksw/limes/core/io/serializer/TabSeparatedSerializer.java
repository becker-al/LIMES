/*
 * LIMES Core Library - LIMES – Link Discovery Framework for Metric Spaces.
 * Copyright © 2011 Data Science Group (DICE) (ngonga@uni-paderborn.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aksw.limes.core.io.serializer;

import org.aksw.limes.core.io.mapping.AMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Axel-C. Ngonga Ngomo (ngonga@informatik.uni-leipzig.de)
 * @author Mohamed Sherif (sherif@informatik.uni-leipzig.de)
 * @version Jul 12, 2016
 */
public class TabSeparatedSerializer extends NtSerializer {

    private static Logger logger = LoggerFactory.getLogger(TabSeparatedSerializer.class.getName());
    protected String seperator = "\t";

    /* (non-Javadoc)
     * @see org.aksw.limes.core.io.serializer.NtSerializer#addStatement(java.lang.String, java.lang.String, java.lang.String, double)
     */
    @Override
    public void addStatement(String subject, String predicate, String object, double similarity) {
        statements.add(subject + seperator + object + seperator + similarity);
    }

    /* (non-Javadoc)
     * @see org.aksw.limes.core.io.serializer.NtSerializer#printStatement(java.lang.String, java.lang.String, java.lang.String, double)
     */
    @Override
    public void printStatement(String subject, String predicate, String object, double similarity) {
        try {
            writer.println(subject + seperator + object + seperator + similarity);
        } catch (Exception e) {
            logger.warn("Error writing");
        }
    }

    /* (non-Javadoc)
     * @see org.aksw.limes.core.io.serializer.NtSerializer#getName()
     */
    public String getName() {
        return "TabSeparatedSerializer";
    }


    /**
     * Gets a mapping and serializes it to a file in the N3 format. The method
     * assume that the class already knows all the prefixes used in the uris and
     * expands those.
     *
     * @param mapping Mapping to serialize
     * @param predicate Predicate to use while serializing
     * @param file File in which the mapping is to be serialized
     */
    public void writeToFile(AMapping mapping, String predicate, String file) {
        open(file);

        if (mapping.size() > 0) {
            for (String s : mapping.getMap().keySet()) {
                for (String t : mapping.getMap().get(s).keySet()) {
                    writer.println("<" + s + ">\t<" + t + ">\t" + mapping.getConfidence(s, t));
                }
            }
        }
        close();
    }

    /* (non-Javadoc)
     * @see org.aksw.limes.core.io.serializer.NtSerializer#getFileExtension()
     */
    public String getFileExtension() {
        return "tsv";
    }

    /**
     * @return the CSV file separator
     */
    public String getSeperator() {
        return seperator;
    }

    /**
     * @param seperator to be set
     */
    public void setSeperator(String seperator) {
        this.seperator = seperator;
    }
}