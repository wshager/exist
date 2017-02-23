/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-2015 The eXist Project
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.indexing.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.exist.TestUtils;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.util.FileUtils;
import org.exist.util.MimeTable;
import org.exist.util.MimeType;
import org.exist.xmldb.IndexQueryService;
import org.exist.xmldb.XQueryService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XUpdateQueryService;

public class ConcurrencyTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer();

    private static Collection test;

    private static final String COLLECTION_CONFIG1 =
        "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">" +
    	"	<index>" +
        "       <lucene>" +
        "           <text qname=\"LINE\"/>" +
        "           <text qname=\"SPEAKER\"/>" +
        "       </lucene>" +
        "	</index>" +
    	"</collection>";

    @Test
	public void store() {
		final ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 10; i++) {
            final String name = "thread" + i;
            final Runnable run = () -> {
                try {
                    storeRemoveDocs(name);
                } catch(final XMLDBException | IOException e) {
                    e.printStackTrace();;
                    fail(e.getMessage());
                }
            };
            executor.submit(run);
        }

        executor.shutdown();
		boolean terminated = false;
		try {
			terminated = executor.awaitTermination(60 * 60, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
		    //Nothing to do
		}
		assertTrue(terminated);
    }

    @Test
	public void update() {
		final ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 5; i++) {
            final String name = "thread" + i;
            Runnable run = () -> {
                try {
                    xupdateDocs(name);
                } catch (final XMLDBException | IOException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            };
            executor.submit(run);
        }

        executor.shutdown();
		boolean terminated = false;
		try {
			terminated = executor.awaitTermination(60 * 60, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
		    //Nothing to do
		}
		assertTrue(terminated);
    }

    private void storeRemoveDocs(final String collectionName) throws XMLDBException, IOException {
        storeDocs(collectionName);

        final XQueryService xqs = (XQueryService) test.getService("XQueryService", "1.0");
        ResourceSet result = xqs.query("//SPEECH[ft:query(LINE, 'king')]");
        assertEquals(98, result.getSize());

        result = xqs.query("//SPEECH[ft:query(SPEAKER, 'juliet')]");
        assertEquals(118, result.getSize());

        final String[] resources = test.listResources();
        for (int i = 0; i < resources.length; i++) {
            final Resource resource = test.getResource(resources[i]);
            test.removeResource(resource);
        }
        result = xqs.query("//SPEECH[ft:query(LINE, 'king')]");
        assertEquals(0, result.getSize());

        result = xqs.query("//SPEECH[ft:query(SPEAKER, 'juliet')]");
        assertEquals(0, result.getSize());
    }

    private void xupdateDocs(final String collectionName) throws XMLDBException, IOException {
        storeDocs(collectionName);

        final XQueryService xqs = (XQueryService) test.getService("XQueryService", "1.0");
        ResourceSet result = xqs.query("//SPEECH[ft:query(SPEAKER, 'juliet')]");
        assertEquals(118, result.getSize());

        final String xupdate =
            LuceneIndexTest.XUPDATE_START +
            "   <xu:remove select=\"//SPEECH[ft:query(SPEAKER, 'juliet')]\"/>" +
            LuceneIndexTest.XUPDATE_END;
        final XUpdateQueryService xuqs = (XUpdateQueryService) test.getService("XUpdateQueryService", "1.0");
        xuqs.update(xupdate);

        result = xqs.query("//SPEECH[ft:query(SPEAKER, 'juliet')]");
        assertEquals(0, result.getSize());

        result = xqs.query("//SPEECH[ft:query(LINE, 'king')]");
        assertEquals(98, result.getSize());
    }

    private void storeDocs(final String collectionName) throws XMLDBException, IOException {
        Collection collection = null;
        try {
            collection = existEmbeddedServer.createCollection(test, collectionName);

            final IndexQueryService iqs = (IndexQueryService) collection.getService("IndexQueryService", "1.0");
            iqs.configureCollection(COLLECTION_CONFIG1);

            final String existHome = System.getProperty("exist.home");
            Path existDir = existHome == null ? Paths.get(".") : Paths.get(existHome);
            existDir = existDir.normalize();

            final Path samples = existDir.resolve("samples/shakespeare");
            final List<Path> files = FileUtils.list(samples);
            final MimeTable mimeTab = MimeTable.getInstance();
            for (final Path file : files) {
                final MimeType mime = mimeTab.getContentTypeFor(FileUtils.fileName(file));
                if (mime != null && mime.isXMLType()) {
                    final Resource resource = collection.createResource(FileUtils.fileName(file), XMLResource.RESOURCE_TYPE);
                    resource.setContent(file);
                    collection.storeResource(resource);
                }
            }
        } finally {
            if(collection != null) {
                collection.close();
            }
        }
    }

    @BeforeClass
    public static void initDB() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XMLDBException {
        test = existEmbeddedServer.createCollection(existEmbeddedServer.getRoot(), "test");
    }

    @AfterClass
    public static void closeDB() throws XMLDBException {
        test.close();
        TestUtils.cleanupDB();
    }
}
