package eu.nimble.service.bp.cache;

import eu.nimble.service.model.ubl.document.IDocument;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.URL;

@Component
public class CacheHelper {
    private CacheManager cacheManager;
    private Cache documentCache;

    private final String xmlConfigurationFile = "/ehcache.xml";

    @PostConstruct
    private void initCacheManager(){
        URL url = getClass().getResource(xmlConfigurationFile);
        XmlConfiguration xmlConfiguration = new XmlConfiguration(url);
        cacheManager = CacheManagerBuilder.newCacheManager(xmlConfiguration);
        // initialize cache manager
        cacheManager.init();
        // set document cache
        documentCache = cacheManager.getCache("document",Object.class, Object.class);
    }

    @PreDestroy
    private void closeCacheManager(){
        cacheManager.close();
    }

    public Object getDocument(String documentId) {
        if(documentCache.containsKey(documentId)){
            return documentCache.get(documentId);
        }
        return null;
    }

    public void putDocument(Object document) {
        IDocument iDocument = (IDocument) document;
        documentCache.put(iDocument.getDocumentId(),document);
    }

    public void removeDocument(Object document) {
        IDocument iDocument = (IDocument) document;
        if(documentCache.containsKey(iDocument.getDocumentId())){
            documentCache.remove(iDocument.getDocumentId(),document);
        }
    }
}