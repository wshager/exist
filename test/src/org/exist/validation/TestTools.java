/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-07 The eXist Project
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
 *
 * $Id$
 */

package org.exist.validation;

import java.io.ByteArrayInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.exist.util.ConfigurationHelper;
import org.exist.util.FileUtils;

/**
 *  A set of helper methods for the validation tests.
 *
 * @author Dannes Wessels (dizzzz@exist-db.org)
 */
public class TestTools {

    public final static String VALIDATION_HOME_COLLECTION = "validation";
    public final static String VALIDATION_DTD_COLLECTION = "dtd";
    public final static String VALIDATION_XSD_COLLECTION = "xsd";
    public final static String VALIDATION_TMP_COLLECTION = "tmp";
    
    // Transfer bytes from in to out
    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
    }
    
    /**
     *
     * @param file     File to be uploaded
     * @param target  Target URL (e.g. xmldb:exist:///db/collection/document.xml)
     * @throws java.lang.Exception  Oops.....
     */
    public static void insertDocumentToURL(final Path file, final String target) throws IOException {
        final URL url = new URL(target);
        final URLConnection connection = url.openConnection();
        try(final OutputStream os = connection.getOutputStream()) {
            Files.copy(file, os);
        }
    }

    public static Optional<Path> getEXistHome() {
        return ConfigurationHelper.getExistHome().map(Path::toAbsolutePath);
    }

    public static byte[] getHamlet() throws IOException {
        return loadSample("shakespeare/hamlet.xml");
    }

    public static byte[] loadSample(String sampleRelativePath) throws IOException {
        final Path file = FileUtils.resolve(getEXistHome(), "samples/" + sampleRelativePath);
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Files.copy(file, baos);
            return baos.toByteArray();
        }
    }

    public static void insertDocumentToURL(byte[] data, String target) throws IOException {
        final URL url = new URL(target);
        final URLConnection connection = url.openConnection();
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new ByteArrayInputStream(data);
            os = connection.getOutputStream();
            TestTools.copyStream(is, os);
            os.flush();
         } finally {
            if(is != null){
                is.close();
            }
            if(os != null) {
                os.close();
            }
        }
    }
}