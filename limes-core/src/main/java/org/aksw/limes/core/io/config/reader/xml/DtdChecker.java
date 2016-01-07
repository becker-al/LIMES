/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.limes.core.io.config.reader.xml;

import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.apache.log4j.*;

/**
 * Checks a link specification against the LIMES DTD.
 * @author ngonga
 * @author Mohamed Sherif <sherif@informatik.uni-leipzig.de>
 * @version Nov 12, 2015
 */
public class DtdChecker implements org.xml.sax.ErrorHandler {

    private Logger logger = Logger.getLogger(DtdChecker.class.getName());
    protected boolean valid = true;

    /** Carries out the validation in case a fatal parsing error occur.
     *
     * @param e Exception generated by the fatal parsing error
     * @throws SAXParseException
     */
    public void fatalError(SAXParseException e) throws SAXException {
        logger.fatal("Error at " + e.getLineNumber() + " line.");
        logger.fatal(e.getMessage());
        valid = false;
    }
    
    /** Carry out the validation in case a parsing error occurs.
     *
     * @param e Exception generated by the parsing error
     * @throws SAXParseException
     */
    public void error(SAXParseException e) throws SAXParseException {
        logger.warn("Error at " + e.getLineNumber() + " line.");
        logger.warn(e.getMessage());
        valid = false;
    }

    /** Carry out the validation in case a warning is issued during the parsing.
     *
     * @param err Exception generated by the warning
     * @throws SAXParseException
     */
    public void warning(SAXParseException err) throws SAXParseException {
        logger.warn(err.getMessage());
        valid = false;
    }
}

