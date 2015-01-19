package siebog.dnars.speedtest;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import siebog.dnars.base.Statement;
import siebog.dnars.base.StatementParser;
import siebog.dnars.graph.DNarsGraph;
import siebog.dnars.graph.DNarsGraphFactory;
import siebog.dnars.inference.ResolutionEngine;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;

public class DNarsDB extends DB {
	private DNarsGraph graph;

	@Override
	public void init() throws DBException {
		Properties props = getProperties();
		Map<String, Object> config = new HashMap<>();
		String hostname = props.getProperty("hostname");
		if (hostname == null)
			throw new DBException("Please specify the 'hostname' paramater (Cassanda hostname).");
		String domain = props.getProperty("domain");
		if (domain == null)
			throw new DBException("Please specify the 'domain' parameter.");
		config.put("storage.hostname", hostname);
		graph = DNarsGraphFactory.create(domain, config);
	}

	@Override
	public void cleanup() throws DBException {
		graph.shutdown();
	}

	@Override
	public int read(String domain, String questionStr, Set<String> fields,
			HashMap<String, ByteIterator> result) {
		Statement question = StatementParser.apply(questionStr);
		Statement[] answers = ResolutionEngine.answer(graph, question, 1);
		int err = answers.length > 0 ? 0 : 1;
		return err;
	}

	@Override
	public int delete(String table, String key) {
		return 0;
	}

	@Override
	public int insert(String table, String key, HashMap<String, ByteIterator> values) {
		return 0;
	}

	@Override
	public int scan(String table, String startkey, int recordcount, Set<String> fields,
			Vector<HashMap<String, ByteIterator>> result) {
		return 0;
	}

	@Override
	public int update(String table, String key, HashMap<String, ByteIterator> values) {
		return 0;
	}

	public static void main(String[] args) {
	}
}
