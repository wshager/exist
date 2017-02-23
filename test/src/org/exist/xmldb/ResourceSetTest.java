package org.exist.xmldb;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.junit.*;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;

import java.nio.file.Path;

import static org.exist.xmldb.XmldbLocalTests.getShakespeareSamplesDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ResourceSetTest {

	@ClassRule
	public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer();

	private final static String TEST_COLLECTION = "testResourceSet";

	private Collection testCollection;

	@Before
	public void setUp() throws Exception {
		final CollectionManagementService service = (CollectionManagementService) existEmbeddedServer.getRoot().getService("CollectionManagementService", "1.0");
		testCollection = service.createCollection(TEST_COLLECTION);
		assertNotNull(testCollection);

		final Path shakes = getShakespeareSamplesDirectory().resolve("shakes.xsl");
		final Resource shakesRes = testCollection.createResource("shakes.xsl", XMLResource.RESOURCE_TYPE);
		shakesRes.setContent(shakes.toFile());
		testCollection.storeResource(shakesRes);

		final Path hamlet = getShakespeareSamplesDirectory().resolve("hamlet.xml");
		final Resource hamletRes = testCollection.createResource("hamlet.xml", XMLResource.RESOURCE_TYPE);
		hamletRes.setContent(hamlet.toFile());
		testCollection.storeResource(hamletRes);
	}

	@After
	public void tearDown() throws XMLDBException {
		//delete the test collection
		final CollectionManagementService service = (CollectionManagementService)testCollection.getParentCollection().getService("CollectionManagementService", "1.0");
		service.removeCollection(TEST_COLLECTION);
	}

	@Ignore
    @Test
	public void intersection1() throws XMLDBException {
		final String xpathPrefix = "doc('/db/" + TEST_COLLECTION + "/shakes.xsl')/*/*"; // "xmldb:document('" + DBBroker.ROOT_COLLECTION + "/test/macbeth.xml')/*/*";
		final String query1 = xpathPrefix + "[position() >= 5 ]";
		final String query2 = xpathPrefix + "[position() <= 10]";
		final int expected = 87;

        final XPathQueryService service = (XPathQueryService)
            testCollection.getService("XPathQueryService", "1.0");

        final ResourceSet result1 = service.query(query1);
        final ResourceSet result2 = service.query(query2);

        assertEquals("size of intersection of " + query1 + " and " + query2 + " yields ", expected, ResourceSetHelper.intersection(result1, result2).getSize());
	}

	@Ignore
	@Test
	public void intersection2() throws XMLDBException {
	   	final String xpathPrefix = "doc('/db/" + TEST_COLLECTION + "/hamlet.xml')//LINE";
		final String query1 = xpathPrefix + "[. = 'funeral' ]";		// count=4
		final String query2 = xpathPrefix + "[. = 'dirge']";		// count=1, intersection=1
		final int expected = 1;

		final XPathQueryService service = (XPathQueryService)
				testCollection.getService("XPathQueryService", "1.0");

		final ResourceSet result1 = service.query(query1);
		final ResourceSet result2 = service.query(query2);

		assertEquals("size of intersection of " + query1 + " and " + query2 + " yields ", expected, ResourceSetHelper.intersection(result1, result2).getSize());
	}
}
