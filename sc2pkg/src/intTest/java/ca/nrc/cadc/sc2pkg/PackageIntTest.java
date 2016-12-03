/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2011.                            (c) 2011.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*  $Revision: 5 $
*
************************************************************************
*/

package ca.nrc.cadc.sc2pkg;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.SSLUtil;
import ca.nrc.cadc.caom2.PlaneURI;
import ca.nrc.cadc.caom2.pkg.PackageRunner;
import ca.nrc.cadc.net.HttpDownload;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.HexUtil;
import ca.nrc.cadc.util.Log4jInit;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class PackageIntTest 
{
    private static final Logger log = Logger.getLogger(PackageIntTest.class);

    static final String SINGLE_ARTIFACT = "caom:IRIS/f212h000/IRAS-12um";
    static final String SINGLE_ARTIFACT2 = "caom:IRIS/f212h000/IRAS-25um";
    static final String SINGLE_ARTIFACT3 = "caom:IRIS/f212h000/IRAS-60um";
    static final String SINGLE_ARTIFACT4 = "caom:IRIS/f212h000/IRAS-100um";
    
    static final String SINGLE_ARTIFACT_PUB = "ivo://cadc.nrc.ca/IRIS?f212h000/IRAS-12um";
    
    static final String SERVICE_ID = "ivo://cadc.nrc.ca/sc2pkg";
    
    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.caom2.pkg", Level.INFO);
        Log4jInit.setLevel("ca.nrc.cadc.sc2pkg", Level.INFO);
    }
    
    RegistryClient reg;
    
    public PackageIntTest() 
    { 
        this.reg = new RegistryClient();
        try
        {
            File crt = FileUtil.getFileFromResource("x509_CADCRegtest1.pem", PackageIntTest.class);
            SSLUtil.initSSL(crt);
            log.debug("initSSL: " + crt);
        }
        catch(Throwable t)
        {
            throw new RuntimeException("failed to init SSL", t);
        }

    }
    
    @Test
    public void testSingleArtifactRedirectCAOM()
    {
        try
        {
            URL serviceURL = reg.getServiceURL(URI.create(SERVICE_ID), Standards.PKG_10, AuthMethod.ANON);
            URL url = new URL(serviceURL.toExternalForm() + "?ID="+SINGLE_ARTIFACT);
            log.info("testSingleArtifactRedirect: " + url);
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(url,bos);
            get.setFollowRedirects(false);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(303, get.getResponseCode());
            URL rurl = get.getRedirectURL();
            log.info("testSingleArtifactRedirect location: " + rurl);
            Assert.assertNotNull(rurl);
            Assert.assertEquals("/data/pub/IRIS/I212B1H0", rurl.getPath());
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    @Test
    public void testSingleArtifactRedirectIVO()
    {
        try
        {
            URL serviceURL = reg.getServiceURL(URI.create(SERVICE_ID), Standards.PKG_10, AuthMethod.ANON);
            URL url = new URL(serviceURL.toExternalForm() + "?ID="+SINGLE_ARTIFACT_PUB);
            log.info("testSingleArtifactRedirect: " + url);
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(url,bos);
            get.setFollowRedirects(false);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(303, get.getResponseCode());
            URL rurl = get.getRedirectURL();
            log.info("testSingleArtifactRedirect location: " + rurl);
            Assert.assertNotNull(rurl);
            Assert.assertEquals("/data/pub/IRIS/I212B1H0", rurl.getPath());
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    //@Test
    public void testSingleArtifactRedirectAuthCAOM()
    {
        try
        {
            URL serviceURL = reg.getServiceURL(URI.create(SERVICE_ID), Standards.PKG_10, AuthMethod.PASSWORD);
            URL url = new URL(serviceURL.toExternalForm() + "?ID="+SINGLE_ARTIFACT);
            log.info("testSingleArtifactRedirectAuth: " + url);
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(url,bos);
            get.setRequestProperty("Authorization", "***REDACTED***");
            get.setFollowRedirects(false);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(303, get.getResponseCode());
            URL rurl = get.getRedirectURL();
            log.info("testSingleArtifactRedirectAuth location: " + rurl);
            Assert.assertNotNull(rurl);
            Assert.assertEquals("http", rurl.getProtocol());
            Assert.assertEquals("/data/auth/IRIS/I212B1H0", rurl.getPath());
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    //@Test
    public void testSingleArtifactRedirectAuthIVO()
    {
        try
        {
            URL serviceURL = reg.getServiceURL(URI.create(SERVICE_ID), Standards.PKG_10, AuthMethod.PASSWORD);
            URL url = new URL(serviceURL.toExternalForm() + "?ID="+SINGLE_ARTIFACT_PUB);
            log.info("testSingleArtifactRedirectAuth: " + url);
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(url,bos);
            get.setRequestProperty("Authorization", "***REDACTED***");
            get.setFollowRedirects(false);
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(303, get.getResponseCode());
            URL rurl = get.getRedirectURL();
            log.info("testSingleArtifactRedirectAuth location: " + rurl);
            Assert.assertNotNull(rurl);
            Assert.assertEquals("http", rurl.getProtocol());
            Assert.assertEquals("/data/auth/IRIS/I212B1H0", rurl.getPath());
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testMutliID()
    {
        File tar = null;
        try
        {
            StringBuilder sb = new StringBuilder();
            sb.append("?ID=").append(SINGLE_ARTIFACT);
            sb.append("&ID=").append(SINGLE_ARTIFACT2);
            sb.append("&ID=").append(SINGLE_ARTIFACT3);
            sb.append("&ID=").append(SINGLE_ARTIFACT4);
            URL serviceURL = reg.getServiceURL(URI.create(SERVICE_ID), Standards.PKG_10, AuthMethod.ANON);
            URL url = new URL(serviceURL.toExternalForm() + sb.toString());
            File tmp = new File(System.getProperty("java.io.tmpdir"));
            
            log.info("testMutliID: " + url + " -> " + tmp.getAbsolutePath());
            
            HttpDownload get = new HttpDownload(url, tmp);
            get.setOverwrite(true); // file exists from create above
            get.run();
            Assert.assertNull("throwable", get.getThrowable());
            Assert.assertEquals(200, get.getResponseCode());
            tar = get.getFile();
            Assert.assertTrue("tar file exists", tar.exists());
            
            // validate
            List<String> expectedFiles = new ArrayList<String>();
            expectedFiles.add("IRIS-f212h000-IRAS-12um/I212B1H0.fits");
            expectedFiles.add("IRIS-f212h000-IRAS-25um/I212B2H0.fits");
            expectedFiles.add("IRIS-f212h000-IRAS-60um/I212B3H0.fits");
            expectedFiles.add("IRIS-f212h000-IRAS-100um/I212B4H0.fits");
            expectedFiles.add("IRIS-f212h000-IRAS-12um/README");
            expectedFiles.add("IRIS-f212h000-IRAS-25um/README");
            expectedFiles.add("IRIS-f212h000-IRAS-60um/README");
            expectedFiles.add("IRIS-f212h000-IRAS-100um/README");
            
            // extract tar file and check all the files vs the md5sum in README?
            FileInputStream fis = new FileInputStream(tar);
            TarArchiveInputStream tis = new TarArchiveInputStream(fis);
            Content c1 = getEntry(tis);
            Content c2 = getEntry(tis);
            Content c3 = getEntry(tis);
            Content c4 = getEntry(tis);
            Content r1 = getEntry(tis);
            Content r2 = getEntry(tis);
            Content r3 = getEntry(tis);
            Content r4 = getEntry(tis);
            
            ArchiveEntry te = tis.getNextTarEntry();
            Assert.assertNull(te);

            Assert.assertTrue( expectedFiles.contains(c1.name) );
            Assert.assertTrue( expectedFiles.contains(c2.name) );
            Assert.assertTrue( expectedFiles.contains(c3.name) );
            Assert.assertTrue( expectedFiles.contains(c4.name) );
            Assert.assertTrue( expectedFiles.contains(r1.name) );
            Assert.assertTrue( expectedFiles.contains(r2.name) );
            Assert.assertTrue( expectedFiles.contains(r3.name) );
            Assert.assertTrue( expectedFiles.contains(r4.name) );
            
            log.debug("testMutliID: " + c1.name + " " + c1.contentMD5);
            log.debug("testMutliID: " + c2.name + " " + c2.contentMD5);
            log.debug("testMutliID: " + c3.name + " " + c3.contentMD5);
            log.debug("testMutliID: " + c4.name + " " + c4.contentMD5);

            // merge md5map(s)
            Map<String,String> md5map = new HashMap<String,String>();
            md5map.putAll(r1.md5map);
            md5map.putAll(r2.md5map);
            md5map.putAll(r3.md5map);
            md5map.putAll(r4.md5map);
            log.debug("testMutliID: " + md5map.size());
            
            String c1md5 = md5map.get(getFilename(c1.name));
            String c2md5 = md5map.get(getFilename(c2.name));
            String c3md5 = md5map.get(getFilename(c3.name));
            String c4md5 = md5map.get(getFilename(c4.name));
            
            Assert.assertNotNull(c1md5);
            Assert.assertEquals(c1md5, c1.contentMD5);
            
            Assert.assertNotNull(c2md5);
            Assert.assertEquals(c2md5, c2.contentMD5);
            
            Assert.assertNotNull(c3md5);
            Assert.assertEquals(c3md5, c3.contentMD5);
            
            // cleanup here so we leave file behind on fail
            if (tar.exists())
                tar.delete();
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    class Content
    {
        String name;
        String contentMD5;
        
        Map<String,String> md5map = new HashMap<String,String>();
    }
    
    private String getFilename(String entryName)
    {
        int i = entryName.lastIndexOf('/');
        return entryName.substring(i+1);
    }

    
    private Content getEntry(TarArchiveInputStream tar)
            throws IOException, NoSuchAlgorithmException
    {
        Content ret = new Content();
        
        TarArchiveEntry entry = tar.getNextTarEntry();
        ret.name = entry.getName();
        
        if (ret.name.endsWith("README"))
        {
            byte[] buf = new byte[(int) entry.getSize()];
            tar.read(buf);
            ByteArrayInputStream bis = new ByteArrayInputStream(buf);
            LineNumberReader r = new LineNumberReader(new InputStreamReader(bis));
            String line = r.readLine();
            while ( line != null)
            {
                String[] tokens = line.split(" ");
                // status [md5 filename url]
                String status = tokens[0];
                if ("OK".equals(status))
                {
                    String fname = tokens[1];
                    String md5 = tokens[2];
                    ret.md5map.put(fname, md5);
                }
                else
                {
                    throw new RuntimeException("tar content failure: " + line);
                }
                line = r.readLine();
            }
        }
        else
        {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int n = tar.read(buf);
            while (n > 0)
            {
                md5.update(buf, 0, n);
                n = tar.read(buf);
            }
            byte[] md5sum = md5.digest();
            ret.contentMD5 = HexUtil.toHex(md5sum);
        }
        
        return ret;
    }
}
