package nl.tudelft.serg.evosql.evaluation.tools;

import java.util.ArrayList;
import java.util.List;

import in2test.application.services.SqlFpcServiceFacade;
import nl.tudelft.serg.evosql.EvoSQLException;
import nl.tudelft.serg.evosql.db.SchemaExtractor;
import nl.tudelft.serg.evosql.path.PathExtractor;
import nl.tudelft.serg.evosql.sql.ColumnSchema;
import nl.tudelft.serg.evosql.sql.TableSchema;

public class AluraPathExtractor extends PathExtractor {
	
	private Crypto crypto;

	public AluraPathExtractor(SchemaExtractor schemaExtractor, Crypto crypto) {
		super(schemaExtractor);
		this.crypto = crypto;
	}

	@Override
	public List<String> getPaths(String query) throws Exception {
		if (!configured) throw new EvoSQLException("Path extractor has not been initialized");
		
		List<String> encryptedPaths = new ArrayList<String>();
		
		String encryptedSchemaXml;
		AluraTableXMLFormatter aluraTableXMLFormatter = new AluraTableXMLFormatter(schemaExtractor, query, crypto);
		try {
			encryptedSchemaXml = aluraTableXMLFormatter.getSchemaXml();
		} catch (Exception e) {
			throw new Exception("Failed to extract the schema from the running database.", e);
		}
		String sqlfpcXml ="";
		SqlFpcServiceFacade wsFpc=new SqlFpcServiceFacade();
		
		String encryptedQuery = crypto.encryptSQLSelect(query);
		
		sqlfpcXml=wsFpc.getRulesXml(encryptedQuery, encryptedSchemaXml, "");
		
		extractPaths(sqlfpcXml, encryptedPaths);
		
		List<String> paths = new ArrayList<String>();
		// Decrypt the paths
		for (String path : encryptedPaths) {
			paths.add(crypto.decryptSQLSelect(path));
		}
		
		return paths;
	}


}
