package org.exist.xquery.functions.util;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.*;
import org.exist.xquery.ErrorCodes;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import static org.junit.Assert.*;

import org.exist.xquery.XPathException;
import org.w3c.dom.Node;

import org.xmldb.api.base.Collection;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;

/**
 *
 * @author jim.fuller@webcomposite.com
 */
public class EvalTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true);

    private Resource invokableQuery;

    private final static String INVOKABLE_QUERY_FILENAME = "invokable.xql";
    private final static String INVOKABLE_QUERY_EXTERNAL_VAR_NAME = "some-value";

    public EvalTest() {
    }

    @Before
    public void setUp() throws Exception {
        invokableQuery = existEmbeddedServer.getRoot().createResource(INVOKABLE_QUERY_FILENAME, "BinaryResource");
        invokableQuery.setContent(
            "declare variable $" + INVOKABLE_QUERY_EXTERNAL_VAR_NAME + " external;\n" + "<hello>{$" + INVOKABLE_QUERY_EXTERNAL_VAR_NAME + "}</hello>"
        );
        ((EXistResource) invokableQuery).setMimeType("application/xquery");
        existEmbeddedServer.getRoot().storeResource(invokableQuery);
    }

    @After
    public void tearDown() throws Exception {
        existEmbeddedServer.getRoot().removeResource(invokableQuery);
    }

    @Test
    public void eval() throws XPathException, XMLDBException {
        final String query = "let $query := 'let $a := 1 return $a'\n" +
                "return\n" +
                "util:eval($query)";
        final ResourceSet result = existEmbeddedServer.executeQuery(query);
        final String r = (String) result.getResource(0).getContent();
        assertEquals("1", r);
    }

    @Test
    public void evalWithExternalVars() throws XPathException, XMLDBException {
        final String query = "let $value := 'world' return\n" +
                "\tutil:eval(xs:anyURI('/db/" + INVOKABLE_QUERY_FILENAME + "'), false(), (xs:QName('" + INVOKABLE_QUERY_EXTERNAL_VAR_NAME + "'), $value))";
        final ResourceSet result = existEmbeddedServer.executeQuery(query);

        final LocalXMLResource res = (LocalXMLResource)result.getResource(0);
        final Node n = res.getContentAsDOM();
        assertEquals(n.getLocalName(), "hello");
        assertEquals("world", n.getFirstChild().getNodeValue());
    }

    @Test
    public void evalwithPI() throws XPathException, XMLDBException {
        final String query = "let $query := 'let $a := <test><?pi test?></test> return count($a//processing-instruction())'\n" +
                "return\n" +
                "util:eval($query)";
        final ResourceSet result = existEmbeddedServer.executeQuery(query);
        final String r = (String) result.getResource(0).getContent();
        assertEquals("1", r);
    }

    @Test
    public void evalInline() throws XPathException, XMLDBException {
        final String query = "let $xml := document{<test><a><b/></a></test>}\n" +
                "let $query := 'count(.//*)'\n" +
                "return\n" +
                "util:eval-inline($xml,$query)";
        final ResourceSet result = existEmbeddedServer.executeQuery(query);
        final String r = (String) result.getResource(0).getContent();
        assertEquals("3", r);
    }

    @Test
    public void testEvalWithContextVariable() throws XPathException, XMLDBException {
        final String query = "let $xml := <test><a/><b/></test>\n" +
                "let $context := <static-context>\n" +
                "<variable name='xml'>{$xml}</variable>\n" +
                "</static-context>\n" +
                "let $query := 'count($xml//*) mod 2 = 0'\n" +
                "return\n" +
                "util:eval-with-context($query, $context, false())";
        final ResourceSet result = existEmbeddedServer.executeQuery(query);
        final String r = (String) result.getResource(0).getContent();
        assertEquals("true", r);
    }

    @Test
    public void testEvalSupplyingContext() throws XPathException, XMLDBException {
        final String query = "let $xml := <test><a/></test>\n" +
                "let $context := <static-context>\n" +
                "<default-context>{$xml}</default-context>\n" +
                "</static-context>\n" +
                "let $query := 'count(.//*) mod 2 = 0'\n" +
                "return\n" +
                "util:eval-with-context($query, $context, false())";
        final ResourceSet result = existEmbeddedServer.executeQuery(query);
        final String r = (String) result.getResource(0).getContent();
        assertEquals("true", r);
    }

    @Test
    public void testEvalSupplyingContextAndVariable() throws XPathException, XMLDBException {
        final String query = "let $xml := <test><a/></test>\n" +
                "let $context := <static-context>\n" +
                "<variable name='xml'>{$xml}</variable>\n" +
                "<default-context>{$xml}</default-context>\n" +
                "</static-context>\n" +
                "let $query := 'count($xml//*) + count(.//*)'\n" +
                "return\n" +
                "util:eval-with-context($query, $context, false())";
        final ResourceSet result = existEmbeddedServer.executeQuery(query);
        final String r = (String) result.getResource(0).getContent();
        assertEquals("3", r);
    }
    
    @Test
    public void testEvalSupplyingContextItem() throws XPathException, XMLDBException {
        final String query = "let $context := 'London'\n" +
                "let $query := '.'\n" +
                "return\n" +
                "util:eval-with-context($query, (), false(), $context)";
        final ResourceSet result = existEmbeddedServer.executeQuery(query);
        final String r = (String) result.getResource(0).getContent();
        assertEquals("London", r);
    }
    
    @Test
    public void evalInContextWithPreDeclaredNamespace() throws XMLDBException {
        createCollection("testEvalInContextWithPreDeclaredNamespace");
        final String query =
            "xquery version \"1.0\";\r\n" +
            "declare namespace db = \"http://docbook.org/ns/docbook\";\r\n" +
            "import module namespace util = \"http://exist-db.org/xquery/util\";\r\n" +
            "let $q := \"/db:article\" return\r\n" +
            "util:eval($q)";
        existEmbeddedServer.executeQuery(query);
    }
    
    @Test
    public void evalInContextWithPreDeclaredNamespaceAcrossLocalFunctionBoundary() throws XMLDBException {
        createCollection("testEvalInContextWithPreDeclaredNamespace");
        final String query =
            "xquery version \"1.0\";\r\n" +
            "import module namespace util = \"http://exist-db.org/xquery/util\";\r\n" +
            "declare namespace db = \"http://docbook.org/ns/docbook\";\r\n" +
            "declare function local:process($q as xs:string) {\r\n" +
            "\tutil:eval($q)\r\n" +
            "};\r\n" +
            "let $q := \"/db:article\" return\r\n" +
            "local:process($q)";
        existEmbeddedServer.executeQuery(query);
    }
    
    //should fail with - Error while evaluating expression: /db:article. XPST0081: No namespace defined for prefix db [at line 5, column 9]
    @Test(expected=XMLDBException.class)
    public void evalInContextWithPreDeclaredNamespaceAcrossModuleBoundary() throws XMLDBException {
        Collection testHome = createCollection("testEvalInContextWithPreDeclaredNamespace");
        final String processorModule =
                "xquery version \"1.0\";\r\n" +
                "module namespace processor = \"http://processor\";\r\n" +
                "import module namespace util = \"http://exist-db.org/xquery/util\";\r\n" +
                "declare function processor:process($q as xs:string) {\r\n" +
                "\tutil:eval($q)\r\n" +
                "};";
        
        writeModule(testHome, "processor.xqm", processorModule);
        
        final String query =
            "xquery version \"1.0\";\r\n" +
            "import module namespace processor = \"http://processor\" at \"xmldb:exist://" + testHome.getName() + "/processor.xqm\";\r\n" +
            "declare namespace db = \"http://docbook.org/ns/docbook\";\r\n" +
            "let $q := \"/db:article\" return\r\n" +
            "processor:process($q)";

        existEmbeddedServer.executeQuery(query);
    }

    /**
     * The original issue was caused by VariableReference inside util:eval
     * not calling XQueryContext#popNamespaceContext when a variable
     * reference could not be resolved, which led to the wrong
     * namespaces being present in the XQueryContext the next time
     * the same query was executed
     */
    @Test
    public void evalWithMissingVariableReferenceShouldReportTheSameErrorEachTime() throws XMLDBException {
        final String testHomeName = "testEvalWithMissingVariableReferenceShouldReportTheSameErrorEachTime";
        final Collection testHome = createCollection(testHomeName);

        final String configModuleName = "config-test.xqm";
        final String configModule = "xquery version \"1.0\";\r\n" +
            "module namespace ct = \"http://config/test\";\r\n" +
            "declare variable $ct:var1 { request:get-parameter(\"var1\", ()) };";

        writeModule(testHome, configModuleName, configModule);

        final String testModuleName = "test.xqy";
        final String testModule = "import module namespace ct = \"http://config/test\" at \"xmldb:exist:///db/" + testHomeName + "/" + configModuleName + "\";\r\n" +
            "declare namespace x = \"http://x\";\r\n" +
            "declare function local:hello() {\r\n" +
            " (\r\n" +
            "<x:hello>hello</x:hello>,\r\n" +
            "util:eval(\"$ct:var1\")\r\n" +
            ")\r\n" +
            "};\r\n" +
            "local:hello()";

        writeModule(testHome, testModuleName, testModule);

        //run the 1st time
        try {
            executeModule(testHome, testModuleName);
        } catch(final XMLDBException e) {
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof XPathException);
            assertEquals(ErrorCodes.XPDY0002, ((XPathException) cause).getErrorCode());
        }

        //run a 2nd time, error code should be the same!
        try {
            executeModule(testHome, testModuleName);
        } catch(final XMLDBException e) {
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof XPathException);
            assertEquals(ErrorCodes.XPDY0002, ((XPathException)cause).getErrorCode());
        }
    }
    
    private Collection createCollection(String collectionName) throws XMLDBException {
        Collection collection = existEmbeddedServer.getRoot().getChildCollection(collectionName);
        if (collection == null) {
            CollectionManagementService cmService = (CollectionManagementService) existEmbeddedServer.getRoot().getService("CollectionManagementService", "1.0");
            cmService.createCollection(collectionName);
        }

        collection = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/" + collectionName, "admin", "");
        assertNotNull(collection);
        return collection;
    }
    
    private void writeModule(Collection collection, String modulename, String module) throws XMLDBException {
        BinaryResource res = (BinaryResource) collection.createResource(modulename, "BinaryResource");
        ((EXistResource) res).setMimeType("application/xquery");
        res.setContent(module.getBytes());
        collection.storeResource(res);
        collection.close();
    }

    private ResourceSet executeModule(final Collection collection, final String moduleName) throws XMLDBException {
        final XPathQueryServiceImpl service = (XPathQueryServiceImpl) collection.getService("XQueryService", "1.0");
        final XmldbURI moduleUri = ((CollectionImpl)collection).getPathURI().append(moduleName);
        return service.executeStoredQuery(moduleUri.toString());
    }
}