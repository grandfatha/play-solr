/**
 * 
 */
package play.modules.solr;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import play.jobs.Job;

class SaveJob extends Job{
	
	private Collection<SolrInputDocument> docs;
	private SolrServer server;
	
	public SaveJob(SolrServer server, SolrInputDocument doc) {
		this.server = server;
		docs = new ArrayList<SolrInputDocument>();
		docs.add(doc);
	}
	
	public SaveJob(SolrServer server, Collection<SolrInputDocument> docs){
		this.server = server;
		this.docs = docs;
	}
	
	public void doJob() throws Exception {
		server.add(docs);
		server.commit();
	}
}