package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.MemoryCache;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import org.locationtech.jts.io.ParseException;

//This code is taken form Abdullah Ahmed
public class PolygonReader {

    public static ACache cachePolygons(String fileName) throws ParseException {
        final String ngeo = "http://www.opengis.net/ont/geosparql#";
        Property p = ResourceFactory.createProperty(ngeo, "asWKT");
        Model model = ModelFactory.createDefaultModel();
        File file = new File(fileName);
        InputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (in == null) {
            throw new IllegalArgumentException(fileName + " not found");
        } else {
            model.read(in, null, "TTL");
        }
        StmtIterator iter = model.listStatements(null, p, (RDFNode) null);
        ACache cache = new MemoryCache();

        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();
            Resource sub = stmt.getSubject();
            Property pro = stmt.getPredicate();
            RDFNode o = stmt.getObject();
            String strO = o.toString();
            if (!strO.contains("MULTIPOLYGON")) {
                strO = strO.substring(strO.indexOf("("), strO.indexOf(")"));
                strO = "POLYGON " + strO + "))";
            }
            String strP = pro.toString();
            cache.addTriple(sub.toString(), strP.replaceAll(strP, "asWKT"), strO);
        }
        return cache;
    }
}