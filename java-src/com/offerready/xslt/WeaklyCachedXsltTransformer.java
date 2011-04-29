package com.offerready.xslt;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.databasesandlife.util.ThreadPool;
import com.databasesandlife.util.Timer;

import net.sf.saxon.TransformerFactoryImpl;

/**
 * Encapsulates an "XSLT Templates" capable of performing an XSLT transformation.
 *    <p>
 * <strong>There is a cache of these objects</strong>, by the MD5 of the XSLT file, so that every time an object is requested
 * for the same XSLT file (even if this XSLT file exists in different places in the filesystem), the same object will be returned.
 * However, clients should keep a persistent reference to this object as long as transformations might need to be applied using it;
 * the cache caches only weak references.
 *    <p>
 * <strong>Compilation of an XSLT file can fail</strong> (e.g. if the XSLT file is invalid).
 * In this case, the desired behaviour is that all other valid XSLTs can be applied, so no exception is thrown upon compilation.
 * Application of an XSLT which failed to compile will throw a {@link #DocumentTemplateInvalidException}.
 * The method {@link #assertValid()} returns void if the template is OK and throws the DocumentTemplateInvalidException otherwise.
 */
@SuppressWarnings("serial")
public class WeaklyCachedXsltTransformer {
    
    protected static Map<String, WeakReference<WeaklyCachedXsltTransformer>> cache 
        = new HashMap<String, WeakReference<WeaklyCachedXsltTransformer>>();
    
    public static class DocumentTemplateInvalidException extends Exception {
        public DocumentTemplateInvalidException(String msg) { super(msg); }
    }
    
    public interface Xslt {
        /** For example md5 of xslt; globally unique across the world's XSLTs */ public String calculateCacheKey();
        /** xslt xml */ public Document parseDocument();
    }
    
    /**
     * Can produce a {@link Transformer}.
     *   <p>
     * In principle a {@link Templates} can do this,
     * however for the identity transformation there is a method from SAXON to produce an identity {@link Transformer}
     * but not identity {@link Templates}.
     */
    protected interface XsltTransformerFactory {
        Transformer newTransformer();
    }
  
    protected String error = null;
    
    /** never null */ 
    protected XsltTransformerFactory xsltTransformerFactory;
    
    protected class CompileJob implements Runnable {
        protected String md5, nameForLogging;
        protected Document xslt;
        
        protected CompileJob(String m, String n, Document x) { md5 = m; nameForLogging = n; xslt = x; }
        
        public void run() {
            final StringBuilder errorString = new StringBuilder();
            ErrorListener errorListener = new ErrorListener() {
                public void warning(TransformerException e) { errorString.append("\nERROR: " + e.getMessage()); }
                public void error(TransformerException e) { errorString.append("\nWARN: " + e.getMessage()); }
                public void fatalError(TransformerException e) { errorString.append("\nFATAL: " + e.getMessage()); }
            };

            try (Timer t = new Timer("Compiling XSLT '" + nameForLogging + "'")) {
                TransformerFactoryImpl transformerFactory = (TransformerFactoryImpl) TransformerFactory.newInstance(
                    TransformerFactoryImpl.class.getName(), DocumentGenerator.class.getClassLoader());
                transformerFactory.setErrorListener(errorListener);
                Templates templates = transformerFactory.newTemplates(new DOMSource(xslt));
                xsltTransformerFactory = new XsltTransformerFactory() {
                    @Override public Transformer newTransformer() {
                        try { return templates.newTransformer(); }
                        catch (TransformerConfigurationException e) { throw new RuntimeException(e); }
                    }
                };
            }
            catch (Exception exception) {
                if (errorString.length() > 0) error = nameForLogging + ": " + errorString.toString();
                else error = nameForLogging + ": " + exception.getMessage();
                
                Logger.getLogger(getClass()).error(error, exception);
            }
            
            cache.put(md5, new WeakReference<WeaklyCachedXsltTransformer>(WeaklyCachedXsltTransformer.this));
        }
    }
    
    public static class XsltCompilationThreads extends ThreadPool {
        Map<String, WeaklyCachedXsltTransformer> toCompileForXsltMd5 = new HashMap<String, WeaklyCachedXsltTransformer>();
        @Override public void execute() {
            try (Timer t = new Timer(threadNamePrefix)) { 
                super.execute(); 
            }
        }
    }
    
    public synchronized static WeaklyCachedXsltTransformer getTransformerOrScheduleCompilation(
        XsltCompilationThreads threads, String nameForLogging, Xslt xslt
    ) {
        String cacheKey = xslt.calculateCacheKey();
        
        WeakReference<WeaklyCachedXsltTransformer> ref = cache.get(cacheKey);
        WeaklyCachedXsltTransformer result = (ref == null) ? null : ref.get();
        if (result != null) return result;
        
        result = threads.toCompileForXsltMd5.get(cacheKey);
        if (result != null) return result;
        
        result = new WeaklyCachedXsltTransformer();
        threads.toCompileForXsltMd5.put(cacheKey, result);
        threads.addTask(result.new CompileJob(cacheKey, nameForLogging, xslt.parseDocument()));
        return result;
    }
    
    public static WeaklyCachedXsltTransformer getIdentityTransformer() {
        TransformerFactoryImpl transformerFactory = (TransformerFactoryImpl) TransformerFactory.newInstance(
            TransformerFactoryImpl.class.getName(), DocumentGenerator.class.getClassLoader());
        
        WeaklyCachedXsltTransformer result = new WeaklyCachedXsltTransformer();
        result.xsltTransformerFactory = new XsltTransformerFactory() {
            @Override public Transformer newTransformer() {
                try { return transformerFactory.newTransformer(); }
                catch (TransformerConfigurationException e) { throw new RuntimeException(e); }
            }
        };
        return result;
    }
    
    public void assertValid() throws DocumentTemplateInvalidException {
        if (error != null) throw new DocumentTemplateInvalidException(error);
    }

    public static WeaklyCachedXsltTransformer newInvalidTransformer(String error) {
        WeaklyCachedXsltTransformer result = new WeaklyCachedXsltTransformer();
        result.error = error;
        return result;
    }
    
    public Transformer newTransformer() throws DocumentTemplateInvalidException {
        assertValid();
        return xsltTransformerFactory.newTransformer();
    }
}
