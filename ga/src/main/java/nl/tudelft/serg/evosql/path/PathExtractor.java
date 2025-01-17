package nl.tudelft.serg.evosql.path;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import in2test.application.services.SqlMutationServiceFacade;
import nl.tudelft.serg.evosql.db.ISchemaExtractor;
import nl.tudelft.serg.evosql.db.TableXMLFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import in2test.application.common.SQLToolsConfig;
import in2test.application.services.SqlFpcServiceFacade;
import nl.tudelft.serg.evosql.EvoSQLException;
import nl.tudelft.serg.evosql.sql.parser.SqlSecurer;

public class PathExtractor {
	protected boolean configured = false;
	
	protected static Logger log = LogManager.getLogger(PathExtractor.class);
	
	protected ISchemaExtractor schemaExtractor;
	
	public PathExtractor(ISchemaExtractor schemaExtractor) {
		this.schemaExtractor = schemaExtractor;
	}
	
	public void initialize() {
		SQLToolsConfig.configure(); //this is required to use the SQLFpc web service Facade
		configured = true;
	}
	
	public List<String> getPaths(String query) throws Exception {
		if (!configured) throw new EvoSQLException("Path extractor has not been initialized");
		
		List<String> paths = new ArrayList<String>();
		
		String schemaXml;
		TableXMLFormatter tableXMLFormatter = new TableXMLFormatter(schemaExtractor, query);
		try {
			schemaXml = tableXMLFormatter.getSchemaXml();
		} catch (Exception e) {
			throw new Exception("Failed to extract the schema from the running database.", e);
		}
		String sqlfpcXml ="";
		SqlFpcServiceFacade wsFpc=new SqlFpcServiceFacade();
		sqlfpcXml=wsFpc.getRulesXml(query, schemaXml, "");

//		SQLMutationWSFacade wsMut = new SQLMutationWSFacade();
//		sqlfpcXml = wsMut.getMutants(query, schemaXml, "");
		System.out.println("00");
		extractPaths(sqlfpcXml, paths);

		return paths;
	}


	/**
	 * Extracts all SQL paths into list
	 * @param sqlfpcXml XML from the web service
	 * @param intoList list to put the SQL paths in
	 * @return true if successful, false if any error
	 */
	protected boolean extractPaths(String sqlfpcXml, List<String> intoList) throws Exception {
		InputStream stream = new ByteArrayInputStream(sqlfpcXml.getBytes("UTF-8"));
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbFactory.newDocumentBuilder();
		Document doc = builder.parse(stream);
		doc.getDocumentElement().normalize();
		
		if (doc.getElementsByTagName("error").getLength() > 0) {
			System.err.println(doc.getElementsByTagName("error").item(0).getTextContent());
			return false;
		}
		
		NodeList rules = doc.getElementsByTagName("fpcrule");
		for (int i = 0; i < rules.getLength(); i++) {
			Node n = rules.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element eNode = (Element) n;
				String sql = eNode.getElementsByTagName("sql").item(0).getTextContent();
				// Secure the sql in case SQLFpc messed it up
				sql = new SqlSecurer(sql).getSecureSql();
				intoList.add(sql);
			}
		}
		
		return true;
	}
	
	
}
