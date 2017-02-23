package org.exist.collections.triggers;

import org.exist.EXistException;
import org.exist.TestUtils;
import org.exist.security.PermissionDeniedException;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.xmldb.api.base.Collection;
import org.exist.xmldb.CollectionManagementServiceImpl;
import org.exist.xmldb.IndexQueryService;
import org.exist.xmldb.XmldbURI;
import org.junit.*;
import org.xmldb.api.base.XMLDBException;

import static org.junit.Assert.assertEquals;


public class CollectionTriggerTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer();

    private final static String TEST_COLLECTION = "testCollectionTrigger";
    private static Collection testCollection;
    private static CollectionManagementServiceImpl rootSrv;


    @Test
    public void move() throws XMLDBException, EXistException, PermissionDeniedException {

        //create /db/testCollectionTrigger/srcCollection
        final CollectionManagementServiceImpl colMgmtSrv = (CollectionManagementServiceImpl)testCollection.getService("CollectionManagementService", "1.0");
        final Collection srcCollection = colMgmtSrv.createCollection("col1");

        final XmldbURI baseUri = XmldbURI.create(testCollection.getName());
        final XmldbURI srcUri = XmldbURI.create(srcCollection.getName());
        final XmldbURI newDest = XmldbURI.create("moved");

        //perform the move
        colMgmtSrv.move(srcUri, baseUri, newDest);


        //get the trigger and check its count
        CountingCollectionTrigger.CountingCollectionTriggerState triggerState = CountingCollectionTrigger.CountingCollectionTriggerState.getInstance();

        //trigger move methods should have only been
        //invoked once as we only moved one resource
        assertEquals(1, triggerState.getBeforeMove());
        assertEquals(1, triggerState.getAfterMove());
    }

    @Before
    public void createTestCollection() throws XMLDBException {
        //create a test collection
        testCollection = rootSrv.createCollection(TEST_COLLECTION);

        // configure the test collection with the trigger
        IndexQueryService idxConf = (IndexQueryService)testCollection.getService("IndexQueryService", "1.0");
        idxConf.configureCollection(COLLECTION_CONFIG);
    }

    @After
    public void removeTestCollection() throws XMLDBException {
        rootSrv.removeCollection(XmldbURI.create(testCollection.getName()));
    }

    /** just start the DB and create the test collection */
    @BeforeClass
    public static void startDB() throws XMLDBException {
        rootSrv = (CollectionManagementServiceImpl)existEmbeddedServer.getRoot().getService("CollectionManagementService", "1.0");
    }

    @AfterClass
    public static void shutdownDB() {
        TestUtils.cleanupDB();
        testCollection = null;
        rootSrv = null;
    }

    private final static String COLLECTION_CONFIG =
        "<exist:collection xmlns:exist='http://exist-db.org/collection-config/1.0'>" +
        "  <exist:triggers>" +
        "     <exist:trigger class='org.exist.collections.triggers.CountingCollectionTrigger'/>" +
        "  </exist:triggers>" +
        "</exist:collection>";
}
