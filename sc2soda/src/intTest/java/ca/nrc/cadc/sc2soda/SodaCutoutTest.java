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

package ca.nrc.cadc.sc2soda;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.SSLUtil;
import ca.nrc.cadc.net.HttpDownload;
import ca.nrc.cadc.net.HttpPost;
import ca.nrc.cadc.net.NetUtil;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;

/**
 *
 * @author pdowler
 */
public class SodaCutoutTest
{
    private static final Logger log = Logger.getLogger(SodaCutoutTest.class);

    protected static final String QUERY_URI = "ad:IRIS/I212B2H0";
    protected static final String QUERY_PATH = "/IRIS/I212B2H0";

    protected static final String VOS_QUERY_URI = "vos://cadc.nrc.ca~vospace/CAOMworkshop/CADC/I001B4H0";

    // this test is pretty extensive: 4-D cube, spatial is in GLON-CAR,GLAT-CAR, energy is VRAD, and
    // it has a 1-bin polarization axis for a total of 1024x1024x272x1
    //protected static final String CUBE_URI = "ad:CGPS/cgps_ma1_hi_line_image";

    protected static URL httpResourceURL;
    protected static URL httpsResourceURL;
    protected static URL httpDataURL;
    protected static URL httpsDataURL;

    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.sc2soda", Level.INFO);
    }

    public SodaCutoutTest() { }

    @BeforeClass
    public static void before() throws Exception
    {
        try
        {
            File crt = FileUtil.getFileFromResource("x509_CADCRegtest1.pem", SodaCutoutTest.class);
            SSLUtil.initSSL(crt);
            log.debug("initSSL: " + crt);
        }
        catch(Throwable t)
        {
            throw new RuntimeException("failed to init SSL", t);
        }

        RegistryClient rc = new RegistryClient();

        URI serviceURI = new URI("ivo://cadc.nrc.ca/sc2soda");
        httpResourceURL = rc.getServiceURL(serviceURI, Standards.SODA_SYNC_10, AuthMethod.ANON);
        httpsResourceURL = rc.getServiceURL(serviceURI, Standards.SODA_SYNC_10, AuthMethod.CERT);

        serviceURI = new URI("ivo://cadc.nrc.ca/data");
        httpDataURL = rc.getServiceURL(serviceURI, Standards.DATA_10, AuthMethod.ANON);
        httpsDataURL = rc.getServiceURL(serviceURI, Standards.DATA_10, AuthMethod.CERT);

        log.debug("httpResourceURL: " + httpResourceURL);
        log.debug("httpsResourceURL: " + httpsResourceURL);
    }

    @Test
    public void testPOS_Circle()
    {
        log.debug("testPOS_Circle");
        try
        {
            URI uri = new URI(QUERY_URI);

            Map<String,Object> params = new HashMap<String,Object>();
            params.put("ID", uri);
            params.put("POS", "circle 140 0 0.1");
            log.debug("POST: " + httpResourceURL + " params: " + params.size());
            HttpPost hp = new HttpPost(httpResourceURL, params, false);
            hp.run();
            if (hp.getThrowable() != null)
                throw (Exception) hp.getThrowable();

            Assert.assertEquals("redirect", 303, hp.getResponseCode());
            URL actual = hp.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testPOS_Circle job redirect: " + actual);
            HttpDownload get = new HttpDownload(actual, new ByteArrayOutputStream());
            get.setFollowRedirects(false);
            get.run();
            if (get.getThrowable() != null)
                throw (Exception) get.getThrowable();

            Assert.assertEquals("redirect", 303, get.getResponseCode());
            actual = get.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testPOS_Circle redirect: " + actual);

            String query = actual.getQuery();
            Assert.assertNotNull("query", query);

            String[] qps = query.split("&");
            boolean foundCutout = false;
            for (String p : qps)
            {
                String[] pv = p.split("=");
                Assert.assertEquals("param=value", 2, pv.length);
                if ("cutout".equals(pv[0]))
                {
                    foundCutout = true;
                    String val = NetUtil.decode(pv[1]);
                    Assert.assertEquals("cutout", "[0][271:279,254:262,*]", val);
                }
            }
            Assert.assertTrue("foundCutout", foundCutout);

        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testPOS_CircleNoOverlap()
    {
        log.debug("testPOS_CircleNoOverlap");
        try
        {
            URI uri = new URI(QUERY_URI);

            Map<String,Object> params = new HashMap<String,Object>();
            params.put("ID", uri);
            params.put("POS", "CIRCLE 20 20 0.1");
            log.debug("POST: " + httpResourceURL + " params: " + params.size());
            HttpPost hp = new HttpPost(httpResourceURL, params, false);
            hp.run();
            if (hp.getThrowable() != null)
                throw (Exception) hp.getThrowable();

            Assert.assertEquals("redirect", 303, hp.getResponseCode());
            URL actual = hp.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testPOS_CircleNoOverlap job redirect: " + actual);
            HttpDownload get = new HttpDownload(actual, new ByteArrayOutputStream());
            get.setFollowRedirects(true);
            get.run();

            log.info("testPOS_CircleNoOverlap: " + get.getResponseCode() + "," + get.getThrowable());
            Assert.assertNotNull("throwable", get.getThrowable());
            Assert.assertEquals("response code", 400, get.getResponseCode());

        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testVOSpaceCutout()
    {
        log.debug("testVOSpaceCutout");
        try
        {
            URI uri = new URI(VOS_QUERY_URI);

            Map<String,Object> params = new HashMap<String,Object>();
            params.put("ID", uri);
            params.put("POS", "circle 357 -89 0.1");
            log.debug("POST: " + httpResourceURL + " params: " + params.size());
            HttpPost hp = new HttpPost(httpResourceURL, params, false);
            hp.run();
            if (hp.getThrowable() != null)
                throw (Exception) hp.getThrowable();

            Assert.assertEquals("redirect", 303, hp.getResponseCode());
            URL actual = hp.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testVOSpaceCutout job redirect: " + actual);
            HttpDownload get = new HttpDownload(actual, new ByteArrayOutputStream());
            get.setFollowRedirects(false);
            get.run();
            if (get.getThrowable() != null)
                throw (Exception) get.getThrowable();

            Assert.assertEquals("redirect", 303, get.getResponseCode());
            actual = get.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testPOS_Circle redirect: " + actual);

            String query = actual.getQuery();
            Assert.assertNotNull("query", query);

            String[] qps = query.split("&");
            boolean foundCutout = false;
            for (String p : qps)
            {
                String[] pv = p.split("=");
                Assert.assertEquals("param=value", 2, pv.length);
                if ("view".equals(pv[0]))
                {
                    foundCutout = true;
                    String val = NetUtil.decode(pv[1]);
                    Assert.assertEquals("view", "cutout", val);
                }
            }
            Assert.assertTrue("foundCutout", foundCutout);

        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    /*
    //@Test
    public void testBAND()
    {
        log.debug("testBAND");
        try
        {
            URI uri = new URI(CUBE_URI);

            Map<String,Object> params = new HashMap<String,Object>();
            params.put("ID", uri);
            params.put("BAND", "211.0e-3 211.05e-3"); // cube is [0.2109,0.2111]
            log.debug("POST: " + httpResourceURL + " params: " + params.size());
            HttpPost hp = new HttpPost(httpResourceURL, params, false);
            hp.run();

            if (hp.getThrowable() != null)
                throw (Exception) hp.getThrowable();

            Assert.assertEquals("redirect", 303, hp.getResponseCode());
            URL actual = hp.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testBAND job redirect: " + actual);
            HttpDownload get = new HttpDownload(actual, new ByteArrayOutputStream());
            get.setFollowRedirects(false);
            get.run();
            if (get.getThrowable() != null)
                throw (Exception) get.getThrowable();

            Assert.assertEquals("redirect", 303, get.getResponseCode());
            actual = get.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testBAND redirect: " + actual);

            String query = actual.getQuery();
            Assert.assertNotNull("query", query);

            String[] qps = query.split("&");
            boolean foundCutout = false;
            for (String p : qps)
            {
                String[] pv = p.split("=");
                Assert.assertEquals("param=value", 2, pv.length);
                if ("cutout".equals(pv[0]))
                {
                    foundCutout = true;
                    String val = NetUtil.decode(pv[1]);
                    Assert.assertEquals("cutout", "[0][*,*,91:177,*]", val);
                }
            }
            Assert.assertTrue("foundCutout", foundCutout);

        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    */

    @Test
    public void testBAND_Overlap()
    {
        log.debug("testBAND_Overlap");
        try
        {
            URI uri = new URI(QUERY_URI);

            Map<String,Object> params = new HashMap<String,Object>();
            params.put("ID", uri);
            params.put("BAND", "20.0e-6 25.0e-6"); // IRIS is [0.000019425, 0.000030575] aka 19 to 30um
            log.debug("POST: " + httpResourceURL + " params: " + params.size());
            HttpPost hp = new HttpPost(httpResourceURL, params, false);
            hp.run();

            Assert.assertEquals("redirect", 303, hp.getResponseCode());
            URL actual = hp.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testBAND_Overlap job redirect: " + actual);
            HttpDownload get = new HttpDownload(actual, new ByteArrayOutputStream());
            get.setFollowRedirects(false);
            get.run();
            if (get.getThrowable() != null)
                throw (Exception) get.getThrowable();

            Assert.assertEquals("redirect", 303, get.getResponseCode());
            actual = get.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testBAND_Overlap redirect: " + actual);

            String query = actual.getQuery();
            Assert.assertNotNull("query", query);

            String[] qps = query.split("&");
            boolean foundCutout = false;
            for (String p : qps)
            {
                String[] pv = p.split("=");
                Assert.assertEquals("param=value", 2, pv.length);
                if ("cutout".equals(pv[0]))
                {
                    foundCutout = true;
                    String val = NetUtil.decode(pv[1]);
                    Assert.assertEquals("cutout", "[0][*,*,*]", val); // TODO: check why this actually generates a cutout
                }
            }
            Assert.assertTrue("foundCutout", foundCutout);
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testBAND_NoOverlap()
    {
        log.debug("testBAND_NoOverlap");
        try
        {
            URI uri = new URI(QUERY_URI);

            Map<String,Object> params = new HashMap<String,Object>();
            params.put("ID", uri);
            params.put("BAND", "212.0 213.0"); // cube is [0.2109,0.2111]
            log.debug("POST: " + httpResourceURL + " params: " + params.size());
            HttpPost hp = new HttpPost(httpResourceURL, params, false);
            hp.run();

            Assert.assertEquals("redirect", 303, hp.getResponseCode());
            URL actual = hp.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testBAND_NoOverlap job redirect: " + actual);
            HttpDownload get = new HttpDownload(actual, new ByteArrayOutputStream());
            get.setFollowRedirects(true);
            get.run();

            log.info("testBAND_NoOverlap: " + get.getResponseCode() + "," + get.getThrowable());
            Assert.assertNotNull("throwable", get.getThrowable());
            Assert.assertEquals("response code", 400, get.getResponseCode());
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    /*
    //@Test
    public void testPOS_BAND()
    {
        log.debug("testPOS_BAND");
        try
        {
            URI uri = new URI(CUBE_URI);

            Map<String,Object> params = new HashMap<String,Object>();
            params.put("ID", uri);
            params.put("POS", "circle 25.0 60.0 0.5");
            params.put("BAND", "211.0e-3 211.05e-3");
            log.debug("POST: " + httpResourceURL + " params: " + params.size());
            HttpPost hp = new HttpPost(httpResourceURL, params, false);
            hp.run();

            if (hp.getThrowable() != null)
                throw (Exception) hp.getThrowable();

            Assert.assertEquals("redirect", 303, hp.getResponseCode());
            URL actual = hp.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testPOS_BAND job redirect: " + actual);
            HttpDownload get = new HttpDownload(actual, new ByteArrayOutputStream());
            get.setFollowRedirects(false);
            get.run();
            if (get.getThrowable() != null)
                throw (Exception) get.getThrowable();

            Assert.assertEquals("redirect", 303, get.getResponseCode());
            actual = get.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testPOS_BAND redirect: " + actual);

            String query = actual.getQuery();
            Assert.assertNotNull("query", query);

            String[] qps = query.split("&");
            boolean foundCutout = false;
            for (String p : qps)
            {
                String[] pv = p.split("=");
                Assert.assertEquals("param=value", 2, pv.length);
                if ("cutout".equals(pv[0]))
                {
                    foundCutout = true;
                    String val = NetUtil.decode(pv[1]);
                    Assert.assertEquals("cutout", "[0][350:584,136:370,91:177,*]", val);
                }
            }
            Assert.assertTrue("foundCutout", foundCutout);

        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    */

    /*
    //@Test
    public void testPOS_NamedParts()
    {
        log.debug("testPOS_NamedParts");
        try
        {
            URI uri = new URI(NAMED_PART_URI);

            Map<String,Object> params = new HashMap<String,Object>();
            params.put("ID", uri);
            params.put("POS", "circle 76.44528 -69.62175 0.01");
            log.debug("POST: " + httpResourceURL + " params: " + params.size());
            HttpPost hp = new HttpPost(httpResourceURL, params, false);
            hp.run();

            if (hp.getThrowable() != null)
                throw (Exception) hp.getThrowable();

            Assert.assertEquals("redirect", 303, hp.getResponseCode());
            URL actual = hp.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testPOS_NamedParts job redirect: " + actual);
            HttpDownload get = new HttpDownload(actual, new ByteArrayOutputStream());
            get.setFollowRedirects(false);
            get.run();
            if (get.getThrowable() != null)
                throw (Exception) get.getThrowable();

            Assert.assertEquals("redirect", 303, get.getResponseCode());
            actual = get.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testPOS_NamedParts redirect: " + actual);

            String query = actual.getQuery();
            Assert.assertNotNull("query", query);

            String[] qps = query.split("&");
            boolean foundCutout = false;
            for (String p : qps)
            {
                String[] pv = p.split("=");
                Assert.assertEquals("param=value", 2, pv.length);
                if ("cutout".equals(pv[0]))
                {
                    foundCutout = true;
                    String val = NetUtil.decode(pv[1]);
                    int i = val.indexOf('[');
                    int j = val.indexOf(']');
                    String partName = val.substring(i+1,j);
                    Assert.assertEquals("Part.name", "AMP_0_1", partName);
                }
            }
            Assert.assertTrue("foundCutout", foundCutout);

        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    */

    /*
    //@Test
    public void testBAND_TiledChunk()
    {
        log.debug("testBAND_TiledChunk");
        try
        {
            URI uri = new URI(TILED_CHUNK_URI);

            // first tile: 1.0/369.093200684/2724.0/375.080413818
            // last tile: 66060.0/495.225585938/70999.0/510.2114868

            Map<String,Object> params = new HashMap<String,Object>();
            params.put("ID", uri);
            params.put("BAND", "371.0e-9 372.0e-9");

            log.debug("POST: " + httpResourceURL + " params: " + params.size());
            HttpPost hp = new HttpPost(httpResourceURL, params, false);
            hp.run();

            if (hp.getThrowable() != null)
                throw (Exception) hp.getThrowable();

            Assert.assertEquals("redirect", 303, hp.getResponseCode());
            URL actual = hp.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testBAND_TiledChunk job redirect: " + actual);
            HttpDownload get = new HttpDownload(actual, new ByteArrayOutputStream());
            get.setFollowRedirects(false);
            get.run();
            if (get.getThrowable() != null)
                throw (Exception) get.getThrowable();

            Assert.assertEquals("redirect", 303, get.getResponseCode());
            actual = get.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testBAND_TiledChunk redirect: " + actual);

            String query = actual.getQuery();
            Assert.assertNotNull("query", query);

            String[] qps = query.split("&");
            boolean foundCutout = false;
            for (String p : qps)
            {
                String[] pv = p.split("=");
                Assert.assertEquals("param=value", 2, pv.length);
                if ("cutout".equals(pv[0]))
                {
                    foundCutout = true;
                    String val = NetUtil.decode(pv[1]);
                    Assert.assertEquals("[0][1:2724,1:2]", val); // first tile,WAVE:flux
                }
            }
            Assert.assertTrue("foundCutout", foundCutout);

        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    */

    /*
    //@Test
    public void testBAND_TiledMultiChunk()
    {
        log.debug("testBAND_TiledMultiChunk");
        try
        {
            URI uri = new URI(TILED_MULTICHUNK_URI);

            // first tile: 1.0/369.093200684/2724.0/375.080413818
            // last tile: 66060.0/495.225585938/70999.0/510.2114868

            Map<String,Object> params = new HashMap<String,Object>();
            params.put("ID", uri);
            params.put("BAND", "371.0e-9 372.0e-9");

            log.debug("POST: " + httpResourceURL + " params: " + params.size());
            HttpPost hp = new HttpPost(httpResourceURL, params, false);
            hp.run();

            if (hp.getThrowable() != null)
                throw (Exception) hp.getThrowable();

            Assert.assertEquals("redirect", 303, hp.getResponseCode());
            URL actual = hp.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testBAND_TiledChunk job redirect: " + actual);
            HttpDownload get = new HttpDownload(actual, new ByteArrayOutputStream());
            get.setFollowRedirects(false);
            get.run();
            if (get.getThrowable() != null)
                throw (Exception) get.getThrowable();

            Assert.assertEquals("redirect", 303, get.getResponseCode());
            actual = get.getRedirectURL();
            Assert.assertNotNull("redirect", actual);
            log.info("testBAND_TiledMultiChunk redirect: " + actual);

            String query = actual.getQuery();
            Assert.assertNotNull("query", query);

            String[] qps = query.split("&");
            int numCutout = 0;
            for (String p : qps)
            {
                String[] pv = p.split("=");
                Assert.assertEquals("param=value", 2, pv.length);
                if ("cutout".equals(pv[0]))
                {
                    numCutout++;
                    String val = NetUtil.decode(pv[1]);
                    Assert.assertEquals("[0][1:2724,1:3]", val); // first tile,WAVE:flux
                }
                // TODO: runid
            }
            Assert.assertEquals("numCutout", 1, numCutout);

        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    */
}
