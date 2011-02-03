import models.Topic;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.modules.solr.Solr;
import play.test.Fixtures;
import play.test.UnitTest;


public class SolrTests extends UnitTest {

	@Before
	public void setup(){
		// save an entity
		Topic t = new Topic();
		t.subject = "Solr rocks!";
		t.save();
	}
	
	@After
	public void tearDown(){
		Fixtures.deleteAll();
		Solr.deleteAll();
	}
	
	
	@Test
	public void query() throws Exception{
		
		// waiting for http-communication of Solr to complete 
		// otherwise too short of a time period between 
		// saving/searching/deleting in a unit-test
		Thread.sleep(50);
		
		// search for a solr field
		QueryResponse resp = Solr.query("Solr rocks");
		
		assertNotNull(resp);
		
		SolrDocumentList results = resp.getResults();
		assertNotNull(results);
		assertFalse(results.isEmpty());
		
		assertEquals(1, results.size());
		
		String fieldValue = results.get(0).getFieldValue("topic.subject_s").toString();
		
		assertNotNull(fieldValue);
		assertEquals("Solr rocks!", fieldValue);
		
	}
	
}
