import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import models.Forum;
import models.Topic;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;

import play.modules.solr.SolrMapper;
import play.modules.solr.Solr;
import play.test.UnitTest;


public class SolrMapperTests extends UnitTest {

	@Test
	public void isSolrSearchable(){
		Topic t = new Topic();
		Forum f = new Forum();
		assertTrue(SolrMapper.isSolrSearchable(t));
		assertFalse(SolrMapper.isSolrSearchable(new String("")));
		assertFalse(SolrMapper.isSolrSearchable(f));
	}
	
	@Test
	public void buildDynamicFieldType(){
		
		assertEquals("_i", SolrMapper.buildDynamicFieldType(int.class));
		assertEquals("_i", SolrMapper.buildDynamicFieldType(Integer.class));

		assertEquals("_b", SolrMapper.buildDynamicFieldType(boolean.class));
		assertEquals("_b", SolrMapper.buildDynamicFieldType(Boolean.class));
		
		assertEquals("_l", SolrMapper.buildDynamicFieldType(long.class));
		assertEquals("_l", SolrMapper.buildDynamicFieldType(Long.class));

		assertEquals("_d", SolrMapper.buildDynamicFieldType(double.class));
		assertEquals("_d", SolrMapper.buildDynamicFieldType(Double.class));

		assertEquals("_f", SolrMapper.buildDynamicFieldType(float.class));
		assertEquals("_f", SolrMapper.buildDynamicFieldType(Float.class));

		assertEquals("_s", SolrMapper.buildDynamicFieldType(String.class));
		
	}

	@Test
	public void getClassPrefix(){
		assertEquals("topic.", SolrMapper.getClassPrefix(new Topic()));
	}
	
	@Test
	public void buildFieldName() throws Exception{
		
		Topic t = new Topic();
		Field field = t.getClass().getField("subject");
		
		assertEquals("topic.subject_s", SolrMapper.buildFieldName(field, t));
		assertEquals("topic.subject_s", SolrMapper.buildFieldName("subject", "_s", t));
	}
	
	
	@Test
	public void buildIdField(){
		
		Topic t = new Topic();
		t.id = new Long(1);
		assertEquals("topic.1", SolrMapper.buildIdField(t, t.id.toString()));
	}
	
	@Test
	public void mapSolrIdField() throws Exception{
		
		SolrInputDocument doc = new SolrInputDocument();
		Topic t = new Topic();
		t.id = new Long(1);
		Field field = t.getClass().getField("id");
		
		SolrMapper.mapSolrIdField(doc, field, t);
		
		assertEquals("topic.1", doc.getFieldValue("id"));
		assertEquals("1", doc.getFieldValue("model.id_l"));
		
	}
	
	@Test
	public void mapSummaryField(){
		
		SolrInputDocument doc = new SolrInputDocument();
		Topic t = new Topic();
		List<Object> summaryValues = Arrays.asList((Object)"a-sample-subject", (Object)"test2");
		
		SolrMapper.mapSummaryField(t, doc, summaryValues);
		assertNotNull(doc.getFieldValue("topic.summary_t"));
		assertEquals("a-sample-subject test2", doc.getFieldValue("topic.summary_t"));
	}
	
	@Test
	public void getAnnotatedMethods(){
		
		List<Method> m = SolrMapper.getAnnotatedMethods(new Topic());
		assertNotNull(m);
		assertFalse(m.isEmpty());
		assertEquals(1, m.size());
		assertEquals("computeSomethingHere", m.get(0).getName());
	}
	
	@Test
	public void getAllFieldsRecursive(){
		
		List<Field> f = SolrMapper.getFields(new Topic());
		assertNotNull(f);
		assertFalse(f.isEmpty());
		
		List<String> names = new ArrayList<String>();
		for (Field field : f) {
			names.add(field.getName());
		}
		
		// we need all fields here, since we need to handle @Id annotated
		// field and not only ones with @SolrField annotation
		// assure direct fields
		assertTrue(names.contains("subject"));
		assertTrue(names.contains("forum"));
		// assure recursion, fields from Model, JPASupport
		assertTrue(names.contains("id"));
		assertTrue(names.contains("willBeSaved"));
		
	}
	
	@Test
	public void convertToSolrDoc(){
		
		Topic t = new Topic();
		t.id = 1L;
		t.subject = "sample-subject";
		SolrInputDocument doc = SolrMapper.convertToSolrDoc(t);
		
		assertNotNull(doc);
		assertEquals(5, doc.size());
		
		assertNotNull(doc.getFieldValue("id"));
		assertEquals("topic.1", doc.getFieldValue("id"));
		
		assertNotNull(doc.getFieldValue("model.id_l"));
		assertEquals("1", doc.getFieldValue("model.id_l"));
		
		assertNotNull(doc.getFieldValue("topic.subject_s"));
		assertEquals("sample-subject", doc.getFieldValue("topic.subject_s"));

		assertNotNull(doc.getFieldValue("topic.computeSomethingHere_s"));
		assertEquals("this-is-just-for-the-testcase", doc.getFieldValue("topic.computeSomethingHere_s"));
	
		assertNotNull(doc.getFieldValue("topic.summary_t"));
		assertEquals("sample-subject this-is-just-for-the-testcase", doc.getFieldValue("topic.summary_t"));

	}

}
