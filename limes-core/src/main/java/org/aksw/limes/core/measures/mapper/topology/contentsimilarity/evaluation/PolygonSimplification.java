package org.aksw.limes.core.measures.mapper.topology.contentsimilarity.evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.MemoryCache;
import org.aksw.limes.core.measures.mapper.pointsets.OrchidMapper;
import org.aksw.limes.core.measures.mapper.pointsets.Polygon;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;




public class PolygonSimplification {


	Set<Polygon> sourceWithSimpilification(String str1,String str2) throws ParseException {
		double time1;
		double time2 ;
		double time3=0;
		final String ngeo = "http://www.opengis.net/ont/geosparql#";
		Set<Polygon> polygons=new HashSet<Polygon>();
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		WKTReader reader = new WKTReader(geometryFactory  );
		Property p = ResourceFactory.createProperty(ngeo,"asWKT");
		Model model=ModelFactory.createDefaultModel();
		double value = Double.parseDouble(str1);
		
		File file = new File(str2);
		InputStream in = null;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (in == null) {
			throw new IllegalArgumentException(str2 + " not found");
		}
		else
			model.read(in, null, "TTL");

		StmtIterator iter = model.listStatements(null, p, (RDFNode) null);
		while (iter.hasNext()) {

			Statement stmt = iter.nextStatement();
			Resource sub=stmt.getSubject();
			Property pro=stmt.getPredicate();
			RDFNode o = stmt.getObject();

			String strO = o.toString();
			if(!strO.contains("MULTIPOLYGON")) {
				strO=	strO.substring(strO.indexOf("("),strO.indexOf(")"));
				strO="POLYGON "+strO+"))";}
			time1= System.currentTimeMillis();
			Geometry pLtemp=  reader.read(strO);
			Geometry geomTemp=	TopologyPreservingSimplifier.simplify(pLtemp,value);
			String str= geomTemp.toString();
			time2= System.currentTimeMillis();
			time3=time3+(time2-time1);

			String strP = pro.toString();
			strP.replaceAll(strP, "asWKT");
			String strS = sub.toString();
			Polygon sourceWithSimpilification=new Polygon(strS, OrchidMapper.getPoints(str));
			polygons.add(sourceWithSimpilification);
		}
		return polygons ;
	}

	public static Set<Polygon> sourceWithoutSimpilification(String str2) {

		final String ngeo = "http://www.opengis.net/ont/geosparql#";
		Set<Polygon> polygons1=new HashSet<Polygon>();
		Property p = ResourceFactory.createProperty(ngeo,"asWKT");
		Model model=ModelFactory.createDefaultModel();
		File file = new File(str2);
		InputStream in = null;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (in == null) {
			throw new IllegalArgumentException(str2 + " not found");
		}
		else
			model.read(in, null, "TTL");
		StmtIterator iter = model.listStatements(null, p, (RDFNode) null);
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			Resource sub=stmt.getSubject();
			Property pro=stmt.getPredicate();
			RDFNode o = stmt.getObject();
			String strO = o.toString();
			if(!strO.contains("MULTIPOLYGON")) {
				strO=	strO.substring(strO.indexOf("("),strO.indexOf(")"));
				strO="POLYGON "+strO+"))";}
			String strP = pro.toString();
			strP.replaceAll(strP, "asWKT");
			String strS = sub.toString();
			Polygon sourceWithoutSimpilification=new Polygon(strS, OrchidMapper.getPoints(strO));
			polygons1.add(sourceWithoutSimpilification);
		}
		return polygons1 ;
	}

	public static ACache cacheWithoutSimplification(String str2) throws ParseException {

		final String ngeo = "http://www.opengis.net/ont/geosparql#";
		Property p = ResourceFactory.createProperty(ngeo,"asWKT");
		Model model=ModelFactory.createDefaultModel();
		File file = new File(str2);
		InputStream in = null;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (in == null) {
			throw new IllegalArgumentException(str2 + " not found");
		}
		else
			model.read(in, null, "TTL");
		StmtIterator iter = model.listStatements(null, p, (RDFNode) null);
		ACache s1 = new MemoryCache();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			Resource sub=stmt.getSubject();
			Property pro=stmt.getPredicate();
			RDFNode o = stmt.getObject();
			String strO = o.toString();
			if(!strO.contains("MULTIPOLYGON")) {
				strO=	strO.substring(strO.indexOf("("),strO.indexOf(")"));
				strO="POLYGON "+strO+"))";}
			String strP = pro.toString();
			s1.addTriple(sub.toString(), strP.replaceAll(strP, "asWKT"), strO);
		}
		return s1 ;
	}

	static ACache cacheWithSimpilification(String str1,String str2) throws ParseException {
		double time1;
		double time2 ;
		double time3=0;
		final String ngeo = "http://www.opengis.net/ont/geosparql#";
		ACache s1 = new MemoryCache();
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
		WKTReader reader = new WKTReader(geometryFactory );
		
		Property p = ResourceFactory.createProperty(ngeo,"asWKT");
		Model model=ModelFactory.createDefaultModel();
		double value = Double.parseDouble(str1);
		File file = new File(str2);
		InputStream in = null;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (in == null) {
			throw new IllegalArgumentException(str2 + " not found");
		}
		else
			model.read(in, null, "TTL");
		StmtIterator iter = model.listStatements(null, p, (RDFNode) null);
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			Resource sub=stmt.getSubject();
			Property pro=stmt.getPredicate();
			RDFNode o = stmt.getObject();
			String strO = o.toString();
			if(!strO.contains("MULTIPOLYGON")) {
				strO=	strO.substring(strO.indexOf("("),strO.indexOf(")"));
				strO="POLYGON "+strO+"))";}
			time1= System.nanoTime()/1000000;
			Geometry pLtemp=  reader.read(strO);
			Geometry geomTemp=	TopologyPreservingSimplifier.simplify(pLtemp,value); //TopologyPreservingSimplifier VWSimplifier
			time2= System.nanoTime()/1000000;
			time3=time3+(time2-time1);
			String strP = pro.toString();
			s1.addTriple(sub.toString(), strP.replaceAll(strP, "asWKT"), geomTemp.toString());
		}
		System.out.println("the total time is "+time3);
		return s1 ;
	}
}