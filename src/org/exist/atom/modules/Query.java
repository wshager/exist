/*
 * AtomProtocol.java
 *
 * Created on June 16, 2006, 11:39 AM
 *
 * (C) R. Alexander Milowski alex@milowski.com
 */

package org.exist.atom.modules;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.exist.EXistException;
import org.exist.atom.Atom;
import org.exist.atom.IncomingMessage;
import org.exist.atom.OutgoingMessage;
import org.exist.collections.Collection;
import org.exist.http.BadRequestException;
import org.exist.http.NotFoundException;
import org.exist.security.PermissionDeniedException;
import org.exist.security.xacml.AccessContext;
import org.exist.source.StringSource;
import org.exist.storage.DBBroker;
import org.exist.storage.serializers.Serializer;
import org.exist.util.MimeTable;
import org.exist.util.MimeType;
import org.exist.util.serializer.SAXSerializer;
import org.exist.util.serializer.SerializerPool;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.CompiledXQuery;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Sequence;
import org.xml.sax.SAXException;

/**
 *
 * @author R. Alexander Milowski
 */
public class Query extends AtomModuleBase implements Atom {
   
   protected final static Logger LOG = Logger.getLogger(Topics.class);
   MimeType xqueryMimeType;
   
   /** Creates a new instance of AtomProtocol */
   public Query() {
      xqueryMimeType = MimeTable.getInstance().getContentType("application/xquery");
   }
   
   public void doPost(DBBroker broker,IncomingMessage request,OutgoingMessage response)
      throws BadRequestException,PermissionDeniedException,NotFoundException,EXistException
   {
      
      Collection collection = broker.getCollection(XmldbURI.create(request.getPath()));
      if (collection == null) {
         throw new BadRequestException("Collection "+request.getPath()+" does not exist.");
      }
      
      XQuery xquery = broker.getXQueryService();

      XQueryContext context = xquery.newContext(AccessContext.REST);
      
      String contentType = request.getHeader("Content-Type");
      String charset = getContext().getDefaultCharset();
      
      MimeType mime = MimeType.XML_TYPE;
      if (contentType != null) {
         int semicolon = contentType.indexOf(';');
         if (semicolon>0) {
            contentType = contentType.substring(0,semicolon).trim();
         }
         mime = MimeTable.getInstance().getContentType(contentType);
         int equals = contentType.indexOf('=',semicolon);
         if (equals>0) {
            String param = contentType.substring(semicolon+1,equals).trim();
            if (param.compareToIgnoreCase("charset=")==0) {
               charset = param.substring(equals+1).trim();
            }
         }
      }
      
      if (!mime.isXMLType() && !mime.equals(xqueryMimeType)) {
         throw new BadRequestException("The xquery mime type is not an XML mime type nor application/xquery");
      }
      
      CompiledXQuery compiledQuery = null;
      try {
         StringBuilder builder = new StringBuilder();
         Reader r = new InputStreamReader(request.getInputStream(),charset);
         char [] buffer = new char[4096];
         int len;
         int count = 0;
         int contentLength = request.getContentLength();
         while ((len=r.read(buffer))>=0 && count<contentLength) {
            count += len;
            builder.append(buffer,0,len);
         }
         compiledQuery = xquery.compile(context, new StringSource(builder.toString()));
      } catch (XPathException ex) {
         throw new EXistException("Cannot compile xquery.",ex);
      } catch (IOException ex) {
         throw new EXistException("I/O exception while compiling xquery.",ex);
      }
      
      context.setStaticallyKnownDocuments(new XmldbURI[] { XmldbURI.create(request.getPath()).append(AtomProtocol.FEED_DOCUMENT_NAME) });

      try {
         Sequence resultSequence = xquery.execute(compiledQuery, null);
         if (resultSequence.isEmpty()) {
            throw new BadRequestException("No topic was found.");
         }
         response.setStatusCode(200);
         response.setContentType(Atom.MIME_TYPE+"; charset="+charset);
         Serializer serializer = broker.getSerializer();
         serializer.reset();
         try {
            Writer w = new OutputStreamWriter(response.getOutputStream(),charset);
            SAXSerializer sax = (SAXSerializer) SerializerPool.getInstance().borrowObject(SAXSerializer.class);
            Properties outputProperties = new Properties();
            sax.setOutput(w, outputProperties);
            serializer.setProperties(outputProperties);
            serializer.setSAXHandlers(sax, sax);

            serializer.toSAX(resultSequence, 1, 1, false);

            SerializerPool.getInstance().returnObject(sax);
            w.flush();
            w.close();
         } catch (IOException ex) {
            LOG.fatal("Cannot read resource "+request.getPath(),ex);
            throw new EXistException("I/O error on read of resource "+request.getPath(),ex);
         } catch (SAXException saxe) {
            LOG.warn(saxe);
            throw new BadRequestException("Error while serializing XML: " + saxe.getMessage());
         }
         resultSequence.itemAt(0);
      } catch (XPathException ex) {
         throw new EXistException("Cannot execute xquery.",ex);
      }
      
   }
   
}
