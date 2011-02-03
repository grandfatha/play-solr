package play.modules.solr;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.Id;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;

import play.templates.JavaExtensions;

public class SolrMapper {
	
	private static Map<Class<?>, String> types = new HashMap<Class<?>, String>();
	
	private static final Logger log = Logger.getLogger(SolrMapper.class);
	
	/*
	   <dynamicField name="*_i"  type="int"  />
	   <dynamicField name="*_s"  type="string" />
	   <dynamicField name="*_l"  type="long"  />
	   <dynamicField name="*_t"  type="text" />
	   <dynamicField name="*_b"  type="boolean"/>
	   <dynamicField name="*_f"  type="float" />
	   <dynamicField name="*_d"  type="double"/>
	   <dynamicField name="*_dt" type="date"   />
	*/
	static {
		types.put(Integer.class, "_i");
		types.put(Float.class, "_f");
		types.put(Double.class, "_d");
		types.put(Long.class, "_l");
		
		types.put(Boolean.class, "_b");
		types.put(Date.class, "_dt");
		types.put(String.class, "_s");
	}

	public static SolrInputDocument convertToSolrDoc(Object o){
		
		SolrInputDocument doc = new SolrInputDocument();
		
		List<Field> fields = getFields(o);
		List<Method> methods = getAnnotatedMethods(o);
		
		try {
			
			for (Field field : fields) {
	
				if (field.isAnnotationPresent(Id.class)) {
					mapSolrIdField(doc, field, o);
				}
			}
			
			List<Object> fieldValues = mapAnnotatedFields(doc,fields, o);
			List<Object> methodValues = mapAnnotatedMethods(doc, methods, o);
			
			fieldValues.addAll(methodValues);
			
			mapSummaryField(o, doc, fieldValues);

		} catch (Exception e) {
			log.error(String.format("Failure during conversion of [%s] to SolrInputDocument [%s]", o, doc), e);
		}
		
		return doc;
	}



	public static List<Method> getAnnotatedMethods(Object o) {
		
		List<Method> matchingMethods = new ArrayList<Method>();
		
		if(o != null){
			Class c = o.getClass();
			while(!c.equals(Object.class)){
				
				Method[] methods = c.getMethods();
				
				if(methods != null && methods.length > 0){
					
					for (Method method : methods) {
						
						if(method.isAnnotationPresent(SolrField.class)){
							matchingMethods.add(method);
						}
					}
				}
				c = c.getSuperclass();
			}
		}
		
		return matchingMethods;
	}

	public static List<Field> getFields(Object o) {
		
		List<Field> fields = new ArrayList<Field>();
		
		Class c = o.getClass();
		while (!c.equals(Object.class)) {
			fields.addAll(Arrays.asList(c.getDeclaredFields()));
			c = c.getSuperclass();
		}
		return fields;
	}

	public static List<Object> mapAnnotatedMethods(SolrInputDocument doc, List<Method> methods, Object o) throws Exception{
		
		List<Object> values = new ArrayList<Object>();
		
		for (Method method : methods) {
			
			Object fieldValue = method.invoke(o);
			if(fieldValue != null){
				
				String name = method.getName();
				float boost = 1.0f;
				
				SolrField anno = method.getAnnotation(SolrField.class);
				
				if(anno != null){
					if(anno.name() != null && anno.name().length() > 0){
						name = anno.name();
					}
					if(anno.boost() != 1.0f){
						boost = anno.boost();
					}
				}
				
				String fieldType = buildDynamicFieldType(fieldValue.getClass());
				String fieldName = buildFieldName(name, fieldType, o);
				
				doc.addField(fieldName, fieldValue, boost);
				values.add(fieldValue);
			}
		}
		
		return values;
		
	}
	
	public static List<Object> mapAnnotatedFields(SolrInputDocument doc, List<Field> fields, Object o) throws Exception{

		 List<Object> summaryFields = new ArrayList<Object>();
		
		for (Field field : fields) {

			if (field.isAnnotationPresent(SolrField.class)) {
				
				Object fieldValue = field.get(o);
				
				if (fieldValue != null) {
				
					SolrField fieldAnno = field.getAnnotation(SolrField.class);
					
					String fieldName = buildFieldName(field, o);
					float fieldBoost = 1.0f;	

					if (fieldAnno != null) {
						if (fieldAnno.name() != null
								&& fieldAnno.name().length() > 0) {
							fieldName = fieldAnno.name();
						}
						if (fieldAnno.boost() != 1.0f) {
							fieldBoost = fieldAnno.boost();
						}
					}

					doc.addField(fieldName, fieldValue, fieldBoost);
					summaryFields.add(fieldValue);
				}
			}

			if (field.isAnnotationPresent(SolrEmbedded.class)) {
				
				SolrEmbedded fieldAnno = field.getAnnotation(SolrEmbedded.class);
				
				String fieldName = buildFieldName(field, o);
				Object embedded = field.get(o);
				
				if(fieldAnno != null && fieldAnno.name() != null && fieldAnno.name().length() > 0){
					fieldName = fieldAnno.name();
				}
				
				if(embedded != null){
				
					List<Object> subFieldValues = new ArrayList<Object>();
					
					// OneToMany Case 
					if(Collection.class.isAssignableFrom(embedded.getClass())){
						
						Iterator iter = ((Collection)embedded).iterator();
						
						while(iter.hasNext()){
							Object current = iter.next();
							subFieldValues.addAll(extractEmbeddedFields(doc, current));
						}
					}
					// ManyToOne Case
					else{
						subFieldValues.addAll(extractEmbeddedFields(doc, embedded));
					}
					
					String joined = JavaExtensions.join(subFieldValues, " ");
					
					doc.addField(fieldName, joined);
					summaryFields.add(joined);
					
				}
			}

		}
		
		return summaryFields;
	}

	
	public static List<Object> extractEmbeddedFields(SolrInputDocument doc, Object current) throws Exception{
		
		List<Object> subFieldValues = new ArrayList<Object>();
		
		if(current.getClass().isPrimitive() | String.class.isAssignableFrom(current.getClass())){
			subFieldValues.add(current);
		}
		else{
			
			List<Object> vals = mapAnnotatedFields(doc, getFields(current), current);
			if(vals != null && !vals.isEmpty()){
					subFieldValues.addAll(vals);
			}
		}
			
		return subFieldValues;
	}
	
	/**
	 * join the fieldvalues into a summary-field that will be searchable 
	 * without specifying a specific field, e.g., a simple user entered string
	 */
	public static void mapSummaryField(Object o, SolrInputDocument doc, List<Object> summaryFields) {

		String fieldValue = JavaExtensions.join(summaryFields, " ");
		
		if(fieldValue != null && fieldValue.length() > 0){
			doc.addField(buildFieldName("summary", "_t", o), fieldValue);
		}
	}

	public static void mapSolrIdField(SolrInputDocument doc, Field field, Object o) throws Exception {
		
		// Since Ids are not always unique across entities, we need to prefix the Id
		// with the simple class name, otherwise e.g. a Blog post with id=1 would be
		// removed from Solr by saving a Post Comment with id=1 to Solr

		String id = getJPAKey( o);
		doc.addField("id", buildIdField(o, id));
		doc.addField("model.id_s", id);
	}



	public static String getJPAKey(Object o) {
		if (o != null && o instanceof play.db.Model) {
			play.db.Model m = (play.db.Model) o;
			return play.db.Model.Manager.factoryFor(m.getClass()).keyValue(m).toString();
		}
		else{
			return null;
		}
	}
	
	public static String buildIdField(Object o, String id){
		if(o != null && id != null && id.length() > 0){
			return new StringBuilder().append(getClassPrefix(o)).append(id).toString();
		}
		else{
			return "";
		}
	}

	public static String buildFieldName(Field field, Object o) {
		return buildFieldName(field.getName(), buildDynamicFieldType(field.getType()), o);
	}
	
	public static String buildFieldName(String fieldName, String fieldType, Object o){
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(getClassPrefix(o));
		sb.append(fieldName);
		sb.append(fieldType);
		
		return sb.toString();
	}

	public static String getClassPrefix(Object o) {
		if(o != null){
			return new StringBuilder().append(o.getClass().getSimpleName().toLowerCase()).append(".").toString();
		}
		else{
			return "";
		}
	}


	public static String buildDynamicFieldType(Class clazz) {
		
		String val;
		
		if( (val = types.get(clazz)) != null){
			return val;
		}
		else if (clazz.equals(boolean.class)) {
            return types.get(Boolean.class);
        }
		else if (clazz.equals(int.class)) {
        	return types.get(Integer.class);
        }
		else if (clazz.equals(long.class)) {
        	return types.get(Long.class);
        }
		else if (clazz.equals(double.class)) {
        	return types.get(Double.class);
        }
		else if (clazz.equals(float.class)) {
        	return types.get(Float.class);
        }
		else{
			return types.get(String.class);
		}
	}

	public static boolean isSolrSearchable(Object o) {
		return o != null ? o.getClass().isAnnotationPresent(SolrSearchable.class) : false;
	}
}
