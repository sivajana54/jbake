package org.jbake.template;

import com.orientechnologies.orient.core.record.impl.ODocument;
import groovy.lang.GString;
import groovy.lang.Writable;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import groovy.text.XmlTemplateEngine;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.codehaus.groovy.runtime.MethodClosure;
import org.jbake.app.ConfigUtil.Keys;
import org.jbake.app.ContentStore;
import org.jbake.app.DBUtil;
import org.jbake.app.DocumentList;
import org.jbake.model.DocumentTypes;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

/**
 * Renders documents using the GroovyMarkupTemplateEngine.
 *
 * @see <a href="http://groovy-lang.org/templating.html#_the_markuptemplateengine">Groovy MarkupTemplateEngine Documentation</a>
 *
 * The file extension to activate this Engine is .tpl
 */
public class GroovyMarkupTemplateEngine extends AbstractTemplateEngine {

    private TemplateConfiguration templateConfiguration;
    private MarkupTemplateEngine templateEngine;

    public GroovyMarkupTemplateEngine(final CompositeConfiguration config, final ContentStore db, final File destination, final File templatesPath) {
        super(config, db, destination, templatesPath);
        setupTemplateConfiguration();
        initializeTemplateEngine();
    }

    private void setupTemplateConfiguration() {
        templateConfiguration = new TemplateConfiguration();
        templateConfiguration.setUseDoubleQuotes(true);
        templateConfiguration.setAutoIndent(true);
        templateConfiguration.setAutoNewLine(true);
        templateConfiguration.setAutoEscape(true);
    }

    private void initializeTemplateEngine() {
        templateEngine = new MarkupTemplateEngine(MarkupTemplateEngine.class.getClassLoader(),templatesPath,templateConfiguration);
    }

    @Override
    public void renderDocument(final Map<String, Object> model, final String templateName, final Writer writer) throws RenderingException {
        try {
            Template template = templateEngine.createTemplateByPath(templateName);
            Map<String, Object> wrappedModel = wrap(model);
            Writable writable = template.make(wrappedModel);
            writable.writeTo(writer);
        } catch (Exception e) {
            throw new RenderingException(e);
        }
    }

    private Map<String, Object> wrap(final Map<String, Object> model) {
        return new HashMap<String, Object>(model) {
            @Override
            public Object get(final Object property) {
                if (property instanceof String || property instanceof GString) {
                    String key = property.toString();
                    if ("published_posts".equals(key)) {
                        List<ODocument> query = db.getPublishedPosts();
                        return DocumentList.wrap(query.iterator());
                    }
                    if ("published_pages".equals(key)) {
                        List<ODocument> query = db.getPublishedPages();
                        return DocumentList.wrap(query.iterator());
                    }
                    if ("published_content".equals(key)) {
                    	List<ODocument> publishedContent = new ArrayList<ODocument>();
                    	String[] documentTypes = DocumentTypes.getDocumentTypes();
                    	for (String docType : documentTypes) {
                    		List<ODocument> query = db.getPublishedContent(docType);
                    		publishedContent.addAll(query);
                    	}
                    	return DocumentList.wrap(publishedContent.iterator());
                    }
                    if ("all_content".equals(key)) {
                    	List<ODocument> allContent = new ArrayList<ODocument>();
                    	String[] documentTypes = DocumentTypes.getDocumentTypes();
                    	for (String docType : documentTypes) {
                    		List<ODocument> query = db.getAllContent(docType);
                    		allContent.addAll(query);
                    	}
                    	return DocumentList.wrap(allContent.iterator());
                    }
                    if ("alltags".equals(key)) {
                        List<ODocument> query = db.getAllTagsFromPublishedPosts();
                        Set<String> result = new HashSet<String>();
                        for (ODocument document : query) {
                            String[] tags = DBUtil.toStringArray(document.field("tags"));
                            Collections.addAll(result, tags);
                        }
                        return result;
                    }
                    String[] documentTypes = DocumentTypes.getDocumentTypes();
                    for (String docType : documentTypes) {
                        if ((docType+"s").equals(key)) {
                            return DocumentList.wrap(db.getAllContent(docType).iterator());
                        }
                    }
                    if ("tag_posts".equals(key)) {
                        String tag = model.get("tag").toString();
                        // fetch the tag posts from db
                        List<ODocument> query = db.getPublishedPostsByTag(tag);
                        return DocumentList.wrap(query.iterator());
                    }
                    if ("published_date".equals(key)) {
                        return new Date();
                    }
                }

                return super.get(property);
            }
        };
    }
}
