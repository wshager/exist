package org.exist.xquery;

import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;

/**
 * @author Adam Retter <adam.retter@googlemail.com>
 */
public class MemtreeDescendantOrSelfNodeKindTest extends AbstractDescendantOrSelfNodeKindTest {

    private String getInMemoryQuery(final String queryPostfix) {
        return "declare boundary-space preserve;\n"
                + "let $doc := document {\n" +
            TEST_DOCUMENT +
            "\n}\n" +
            "return\n" +
            queryPostfix;
    }

    @Override
    protected ResourceSet executeQueryOnDoc(final String docQuery) throws XMLDBException {
        final String query = getInMemoryQuery(docQuery);
        return existEmbeddedServer.executeQuery(query);
    }
}
