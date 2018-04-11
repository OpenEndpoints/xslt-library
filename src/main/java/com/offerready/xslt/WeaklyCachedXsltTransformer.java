package com.offerready.xslt;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import com.databasesandlife.util.gwtsafe.ConfigurationException;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.databasesandlife.util.ThreadPool;
import com.databasesandlife.util.Timer;

import net.sf.saxon.TransformerFactoryImpl;
import org.xml.sax.SAXException;

/**
 * Wraps an XSLT {@link Templates} capable of performing an XSLT transformation.
 *    <p>
 * Get an object by using the static method {@link #getTransformerOrScheduleCompilation(XsltCompilationThreads, String, Xslt)}
 * This method maintains a cache by the MD5 of the XSLT file, so that every time an object is requested
 * for the same XSLT file (even if this XSLT file exists in different places in the filesystem), the same object will be returned.
 *    <p>
 * Clients should keep a persistent reference to the {@link WeaklyCachedXsltTransformer}
 * as long as transformations might need to be applied using it;
 * the cache caches only weak references.
 *    <p>
 * Compilation of an XSLT file can fail (e.g. if the XSLT file is invalid).
 * In this case, the desired behaviour is that all other valid XSLTs can be applied, so no exception is thrown upon compilation.
 * Application of an XSLT which failed to compile will throw a {@link DocumentTemplateInvalidException}.
 * The method {@link #assertValid()} returns void if the template is OK and throws the DocumentTemplateInvalidException otherwise.
 */
@SuppressWarnings("serial")
public class WeaklyCachedXsltTransformer {
    
    private static final Map<String, WeakReference<WeaklyCachedXsltTransformer>> cache = new HashMap<>();
    
    public static class DocumentTemplateInvalidException extends Exception {
        public DocumentTemplateInvalidException(String msg) { super(msg); }
    }
    
    public interface Xslt {
        /**
         * @return For example md5 of xslt; globally unique across the world's XSLTs
         */
        String calculateCacheKey();

        /**
         * @return The XSLT file
         * @throws ConfigurationException if the XSLT file cannot be parsed
         */
        Document parseDocument() throws ConfigurationException;
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
  
    protected @CheckForNull String error = null;

    /** After object is initializd, this is never null */
    protected XsltTransformerFactory xsltTransformerFactory;
    
    protected class CompileJob implements Runnable {
        protected @Nonnull String md5, nameForLogging;
        protected @Nonnull Document xslt;
        
        protected CompileJob(@Nonnull String m, @Nonnull String n, @Nonnull Document x) { md5 = m; nameForLogging = n; xslt = x; }
        
        public void run() {
            val errorString = new StringBuilder();
            ErrorListener errorListener = new ErrorListener() {
                public void warning(TransformerException e) { errorString.append("\nERROR: ").append(e.getMessage()); }
                public void error(TransformerException e) { errorString.append("\nWARN: ").append(e.getMessage()); }
                public void fatalError(TransformerException e) { errorString.append("\nFATAL: ").append(e.getMessage()); }
            };

            try (val t = new Timer("Compiling XSLT '" + nameForLogging + "'")) {
                val transformerFactory = (TransformerFactoryImpl) TransformerFactory.newInstance(
                    TransformerFactoryImpl.class.getName(), DocumentGenerator.class.getClassLoader());
                transformerFactory.setErrorListener(errorListener);
                val templates = transformerFactory.newTemplates(new DOMSource(xslt));
                xsltTransformerFactory = new XsltTransformerFactory() {
                    @SneakyThrows(TransformerConfigurationException.class)
                    @Override public Transformer newTransformer() { return templates.newTransformer(); }
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
        final Map<String, WeaklyCachedXsltTransformer> toCompileForXsltMd5 = new HashMap<>();
        @Override public void execute() {
            try (val t = new Timer(threadNamePrefix)) {
                super.execute(); 
            }
        }
    }
    
    public synchronized static @Nonnull WeaklyCachedXsltTransformer getTransformerOrScheduleCompilation(
        @Nonnull XsltCompilationThreads threads, @Nonnull String nameForLogging, @Nonnull Xslt xslt
    ) throws ConfigurationException {
        val cacheKey = xslt.calculateCacheKey();

        val ref = cache.get(cacheKey);
        WeaklyCachedXsltTransformer result = (ref == null) ? null : ref.get();
        if (result != null) return result;
        
        result = threads.toCompileForXsltMd5.get(cacheKey);
        if (result != null) return result;
        
        result = new WeaklyCachedXsltTransformer();
        threads.toCompileForXsltMd5.put(cacheKey, result);
        threads.addTask(result.new CompileJob(cacheKey, nameForLogging, xslt.parseDocument()));
        return result;
    }

    public static @Nonnull WeaklyCachedXsltTransformer getIdentityTransformer() {
        val transformerFactory = (TransformerFactoryImpl) TransformerFactory.newInstance(
            TransformerFactoryImpl.class.getName(), DocumentGenerator.class.getClassLoader());

        val result = new WeaklyCachedXsltTransformer();
        result.xsltTransformerFactory = new XsltTransformerFactory() {
            @SneakyThrows(TransformerConfigurationException.class)
            @Override public Transformer newTransformer() { return transformerFactory.newTransformer(); }
        };
        return result;
    }
    
    public void assertValid() throws DocumentTemplateInvalidException {
        if (error != null) throw new DocumentTemplateInvalidException(error);
    }

    public static @Nonnull WeaklyCachedXsltTransformer newInvalidTransformer(@Nonnull String error) {
        val result = new WeaklyCachedXsltTransformer();
        result.error = error;
        return result;
    }
    
    public Transformer newTransformer() throws DocumentTemplateInvalidException {
        assertValid();
        return xsltTransformerFactory.newTransformer();
    }
}
