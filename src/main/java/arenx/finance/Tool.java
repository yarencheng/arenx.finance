package arenx.finance;

import java.util.stream.Collectors;

import javax.jdo.JDOHelper;
import javax.jdo.annotations.PersistenceCapable;

import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.schema.SchemaAwareStoreManager;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

public class Tool {

	private final static Logger logger = LoggerFactory.getLogger(Tool.class);

	private final static RestTemplate restTemplate;

	static {
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		clientHttpRequestFactory.setConnectionRequestTimeout(30000);
		clientHttpRequestFactory.setConnectTimeout(30000);
		clientHttpRequestFactory.setReadTimeout(30000);
		restTemplate = new RestTemplate(clientHttpRequestFactory);

	}

	public static RestOperations getRestOperations() {
		return restTemplate;
	}

	public static void createAllSchema(){
		JDOPersistenceManagerFactory d=(JDOPersistenceManagerFactory) JDOHelper.getPersistenceManagerFactory("arenx.finance");
		PersistenceNucleusContext p =d.getNucleusContext();
		StoreManager s=p.getStoreManager();
		SchemaAwareStoreManager sa=(SchemaAwareStoreManager) s;
		Reflections reflections = new Reflections("arenx.finance");
		sa.createSchemaForClasses(reflections.getTypesAnnotatedWith(PersistenceCapable.class).stream().map(m->m.getName()).collect(Collectors.toSet()), null);
	}


}
