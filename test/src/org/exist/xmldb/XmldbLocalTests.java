/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-2011 The eXist-db Project
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
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software Foundation
 *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *  $Id$
 */
package org.exist.xmldb;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    CreateCollectionsTest.class,
    ResourceTest.class,
    BinaryResourceUpdateTest.class,
    ResourceSetTest.class,
    TestEXistXMLSerialize.class,
    CopyMoveTest.class,
    ContentAsDOMTest.class,
    XmldbURITest.class,
    CollectionConfigurationTest.class,
    CollectionTest.class
    /* MultiDBTest.class */
})
public class XmldbLocalTests {

    public final static String ROOT_URI = XmldbURI.LOCAL_DB;

    public final static String ADMIN_UID = "admin";
    public final static String ADMIN_PWD = "";

    public final static String GUEST_UID = "guest";

    public static Path getExistDir() {
        final String existHome = System.getProperty("exist.home");
        return existHome == null ? Paths.get(".").normalize() : Paths.get(existHome).normalize();
    }

    public static Path getShakespeareSamplesDirectory() {
        final String directory = "samples/shakespeare";
        return getExistDir().resolve(directory).normalize();
    }
}
