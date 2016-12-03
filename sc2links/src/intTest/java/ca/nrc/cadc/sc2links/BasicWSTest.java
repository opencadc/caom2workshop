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
package ca.nrc.cadc.sc2links;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.SSLUtil;
import ca.nrc.cadc.caom2.datalink.ManifestWriter;
import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.tables.votable.VOTableInfo;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.dali.tables.votable.VOTableTable;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 *  scope: we can only test scenarios where we have existing content
 *  In addition to the different content scenarios, we also need tests of single
 *  and multiple uri params being posted to test correct merging of results.
 *  A few IRIS uris would suffice to make this simple. Despite listing the GET
 *  with query string tests in the scenarios, a single test to prove the
 *  equivalence of GET and POST (single and multiple IRIS uris) should suffice.
 *  So, one test class for basic GET and POST, single and multiple URIs
 *  and then one test class per scenario should work.
 *
 * @author jburke
 */
public class BasicWSTest
{
    private static Logger log = Logger.getLogger(BasicWSTest.class);

    protected static final String QUERY_URI1 = "caom:IRIS/f212h000/IRAS-25um";
    protected static final String QUERY_URI2 = "caom:IRIS/f097h000/IRAS-60um";
    
    protected static final String QUERY_PUB1 = "ivo://cadc.nrc.ca/IRIS?f212h000/IRAS-25um";
    
    protected static final String INVALID_URI = "foo:bar";
    protected static final String NOTFOUND_URI = "caom:FOO/notSuchObservation/noSuchPlane";

    protected static URL httpResourceURL;
    protected static URL httpsResourceURL;

    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.caom2.datalink", Level.INFO);
        Log4jInit.setLevel("ca.nrc.cadc.reg", Level.INFO);
    }

    public BasicWSTest() { }

    @BeforeClass
    public static void before() throws Exception
    {
        try
        {
            File crt = FileUtil.getFileFromResource("x509_CADCRegtest1.pem", BasicWSTest.class);
            SSLUtil.initSSL(crt);
            log.debug("initSSL: " + crt);
        }
        catch(Throwable t)
        {
            throw new RuntimeException("failed to init SSL", t);
        }

        URI serviceID = new URI(TestUtil.DATALINK_SERVCIE_ID);
        RegistryClient rc = new RegistryClient();
        httpResourceURL = rc.getServiceURL(serviceID, Standards.DATALINK_LINKS_10, AuthMethod.ANON);
        httpsResourceURL = rc.getServiceURL(serviceID, Standards.DATALINK_LINKS_10, AuthMethod.CERT);
        log.info("anon URL: " + httpResourceURL);
        log.info("cert URL: " + httpsResourceURL);
    }

    @Test
    public void testSingleUriManifestFormat() throws Exception
    {
        log.debug("testSingleUriManifestFormat");
        try
        {
            // POST the parameters.
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("id", QUERY_URI1);
            String resp = TestUtil.post(httpResourceURL, parameters, ManifestWriter.CONTENT_TYPE);
            log.debug("response:\n" + resp);
            Assert.assertNotNull("non-null response", resp);
            Assert.assertFalse("non-empty response", resp.isEmpty());
            String[] lines = resp.split("\n");
            Assert.assertEquals("number of lines", 1, lines.length);
            String[] parts = lines[0].split("\t");
            Assert.assertEquals("number of tokens", 2, parts.length);
            Assert.assertEquals("OK", parts[0]);
            URL url = new URL(parts[1]);
            log.debug("result url: " + url);
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testSinglePlaneURI_HTTP() throws Exception
    {
        log.debug("testSinglePlaneURI_HTTP");
        doSingleURI(httpResourceURL, QUERY_URI1, "http");
    }

    @Test
    public void testSinglePlaneURI_HTTPS() throws Exception
    {
        log.debug("testSinglePlaneURI_HTTPS");
        doSingleURI(httpsResourceURL, QUERY_URI1, "https");
    }
    
    @Test
    public void testSinglePublisherID_HTTP() throws Exception
    {
        log.debug("testSinglePublisherID_HTTP");
        doSingleURI(httpResourceURL, QUERY_PUB1, "http");
    }

    @Test
    public void testSinglePublisherID_HTTPS() throws Exception
    {
        log.debug("testSinglePublisherID_HTTPS");
        doSingleURI(httpsResourceURL, QUERY_PUB1, "https");
    }

    private void doSingleURI(URL resourceURL, String uri, String expectedProtocol)
    {
        try
        {
            // GET the query.
            VOTableDocument getVotable = TestUtil.get(resourceURL, new String[] { "id="+uri });
            VOTableResource gvr = getVotable.getResourceByType("results");
            
            VOTableInfo queryStatus = null;
            for (VOTableInfo info : gvr.getInfos())
            {
                if (info.getName().equals("QUERY_STATUS"))
                    queryStatus = info;
            }
            Assert.assertNotNull("queryStatus", queryStatus);
            Assert.assertEquals("OK", queryStatus.getValue());
            
            VOTableTable gvtab = gvr.getTable();

            // Check the VOTable FIELD's.
            List<VOTableField> getFields = gvtab.getFields();
            Assert.assertNotNull("GET VOTable FIELD's is null", getFields);
            Assert.assertTrue("GET VOTable FIELD's should not be empty", getFields.size() > 0);

            // Get the TABLEDATA.
            TableData getTableData = gvtab.getTableData();
            Assert.assertNotNull("GET VOTable TableData should not be null", getTableData);
            Iterator<List<Object>> iter = getTableData.iterator();
            Integer[] fields = TestUtil.getFieldIndexes(getFields);
            boolean foundThis = false;
            boolean foundCutout = false;
            while ( iter.hasNext() )
            {
                List<Object> row = iter.next();
                Object oid = row.get(fields[0]);
                Object ourl = row.get(fields[1]);
                Object odef = row.get(fields[2]);
                Object oerr = row.get(fields[7]);
                Object osem = row.get(fields[4]);
                Assert.assertNull("expect null error_message", oerr);
                Assert.assertEquals(uri, oid);
                if (ourl != null)
                {
                    Assert.assertEquals("#this", osem);
                    foundThis = true;
                }
                else if (odef != null)
                {
                    Assert.assertEquals("#cutout", osem);
                    foundCutout = true;
                }
            }
            Assert.assertTrue("found #this", foundThis);
            Assert.assertTrue("found #cutout", foundCutout);
            
            // POST the parameters.
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REQUEST", "getLinks");
            parameters.put("id", uri);
            VOTableDocument postVotable = TestUtil.post(resourceURL, parameters);
            VOTableResource pvr = postVotable.getResourceByType("results");
            VOTableTable pvtab = pvr.getTable();

            // Check the VOTable FIELD's.
            List<VOTableField> postFields = pvtab.getFields();
            Assert.assertNotNull("VOTable FIELD's is null", postFields);
            Assert.assertTrue("VOTable FIELD's should not be empty", postFields.size() > 0);

            // Get the TABLEDATA.
            TableData postTableData = pvtab.getTableData();
            Assert.assertNotNull("VOTable TableData should not be null", postTableData);

            // Compare the GET and POST FIELD.
            TestUtil.compareFields(getFields, postFields);

            // Compare the GET and POST TABLEDATA.
            int urlCol = TestUtil.getFieldIndexes(getFields)[1];
            int sdfCol = TestUtil.getFieldIndexes(getFields)[2];
            TestUtil.compareTableData(getTableData, postTableData, urlCol, sdfCol);

            TestUtil.checkContent(gvtab, expectedProtocol);

            log.debug("testSingleUri passed");
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testMultipleUri() throws Exception
    {
        log.debug("testMultipleUri");
        try
        {
            // GET the query.
            VOTableDocument getVotable = TestUtil.get(httpResourceURL, new String[] { "id="+QUERY_URI1, "id="+QUERY_URI2 });
            VOTableResource gvr = getVotable.getResourceByType("results");
            
            VOTableInfo queryStatus = null;
            for (VOTableInfo info : gvr.getInfos())
            {
                if (info.getName().equals("QUERY_STATUS"))
                    queryStatus = info;
            }
            Assert.assertNotNull("queryStatus", queryStatus);
            Assert.assertEquals("OK", queryStatus.getValue());
            
            VOTableTable gvtab = gvr.getTable();

            // Check the VOTable FIELD's.
            List<VOTableField> getFields = gvtab.getFields();
            Assert.assertNotNull("VOTable FIELD's is null", getFields);
            Assert.assertTrue("VOTable FIELD's should not be empty", getFields.size() > 0);

            // Get the TABLEDATA.
            TableData getTableData = gvtab.getTableData();
            Assert.assertNotNull("VOTable TableData should not be null", getTableData);

            // POST the parameters.
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("REQUEST", "getLinks");
            parameters.put("id", QUERY_URI1);
            parameters.put("id", QUERY_URI2);
            VOTableDocument postVotable = TestUtil.post(httpResourceURL, parameters);
            VOTableResource pvr = postVotable.getResourceByType("results");
            VOTableTable pvtab = pvr.getTable();

            // Check the VOTable FIELD's.
            List<VOTableField> postFields = pvtab.getFields();
            Assert.assertNotNull("VOTable FIELD's is null", postFields);
            Assert.assertTrue("VOTable FIELD's should not be empty", postFields.size() > 0);

            // Get the TABLEDATA.
            TableData postTableData = pvtab.getTableData();
            Assert.assertNotNull("VOTable TableData should not be null", postTableData);

            // Compare the GET and POST FIELD.
            TestUtil.compareFields(getFields, postFields);

            // Compare the GET and POST TABLEDATA --- this compare assumes row-order is the same put POST params
            // may be handled in any order so we can't verify the tables are the same
            //int urlCol = getFieldIndexes(getFields)[1];
            //TestUtil.compareTableData(getTableData, postTableData, urlCol);

            log.debug("testMultipleUri passed");
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testUsageFault_noID() throws Exception
    {
        log.debug("testUsageFault_noID");
        try
        {
            // GET the query.
            VOTableDocument getVotable = TestUtil.get(httpResourceURL, new String[] { }, 400);
            VOTableResource gvr = getVotable.getResourceByType("results");
            VOTableInfo queryStatus = null;
            for (VOTableInfo info : gvr.getInfos())
            {
                if (info.getName().equals("QUERY_STATUS"))
                    queryStatus = info;
            }
            Assert.assertNotNull("queryStatus", queryStatus);
            Assert.assertEquals("ERROR", queryStatus.getValue());
            Assert.assertNotNull(queryStatus.content);
            Assert.assertTrue(queryStatus.content.startsWith("UsageFault"));
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testUsageFault_badID() throws Exception
    {
        log.debug("testUsageFault_badID");
        try
        {
            // GET the query.
            VOTableDocument getVotable = TestUtil.get(httpResourceURL, new String[] { "ID="+INVALID_URI }, 200);
            VOTableResource gvr = getVotable.getResourceByType("results");
            VOTableInfo queryStatus = null;
            for (VOTableInfo info : gvr.getInfos())
            {
                if (info.getName().equals("QUERY_STATUS"))
                    queryStatus = info;
            }
            Assert.assertNotNull("queryStatus", queryStatus);
            Assert.assertEquals("OK", queryStatus.getValue());
            
            
            VOTableTable gvtab = gvr.getTable();

            // Check the VOTable FIELD's.
            List<VOTableField> getFields = gvtab.getFields();
            Assert.assertNotNull("VOTable FIELD's is null", getFields);
            Assert.assertTrue("VOTable FIELD's should not be empty", getFields.size() > 0);

            // Get the TABLEDATA.
            TableData getTableData = gvtab.getTableData();
            Assert.assertNotNull("VOTable TableData should not be null", getTableData);
            
            Iterator<List<Object>> rows = getTableData.iterator();
            Assert.assertTrue(rows.hasNext()); // one row
            List<Object> row1 = rows.next();
            Assert.assertFalse(rows.hasNext()); // exactly one row

            Integer[] index = TestUtil.getFieldIndexes(getFields);
            String id = (String) row1.get(index[0]);
            String emsg = (String) row1.get(index[7]);
            Assert.assertEquals(INVALID_URI, id);
            Assert.assertTrue(emsg.startsWith("UsageFault"));
            Assert.assertNull( row1.get(index[1])); // access_url
            Assert.assertNull( row1.get(index[2])); // service_def
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testNotFoundFault() throws Exception
    {
        log.debug("testNotFoundFault");
        try
        {
            // GET the query.
            VOTableDocument getVotable = TestUtil.get(httpResourceURL, new String[] { "ID="+NOTFOUND_URI }, 200);
            VOTableResource gvr = getVotable.getResourceByType("results");
            VOTableInfo queryStatus = null;
            for (VOTableInfo info : gvr.getInfos())
            {
                if (info.getName().equals("QUERY_STATUS"))
                    queryStatus = info;
            }
            Assert.assertNotNull("queryStatus", queryStatus);
            Assert.assertEquals("OK", queryStatus.getValue());
            
            
            VOTableTable gvtab = gvr.getTable();

            // Check the VOTable FIELD's.
            List<VOTableField> getFields = gvtab.getFields();
            Assert.assertNotNull("VOTable FIELD's is null", getFields);
            Assert.assertTrue("VOTable FIELD's should not be empty", getFields.size() > 0);

            // Get the TABLEDATA.
            TableData getTableData = gvtab.getTableData();
            Assert.assertNotNull("VOTable TableData should not be null", getTableData);
            
            Iterator<List<Object>> rows = getTableData.iterator();
            Assert.assertTrue(rows.hasNext()); // one row
            List<Object> row1 = rows.next();
            Assert.assertFalse(rows.hasNext()); // exactly one row

            Integer[] index = TestUtil.getFieldIndexes(getFields);
            String id = (String) row1.get(index[0]);
            String emsg = (String) row1.get(index[7]);
            Assert.assertEquals(NOTFOUND_URI, id);
            Assert.assertTrue(emsg.startsWith("NotFoundFault"));
            Assert.assertNull( row1.get(index[1])); // access_url
            Assert.assertNull( row1.get(index[2])); // service_def
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
}
