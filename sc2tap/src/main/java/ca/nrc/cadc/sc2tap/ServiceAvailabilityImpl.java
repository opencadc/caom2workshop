/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2016.                            (c) 2016.
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
************************************************************************
*/

package ca.nrc.cadc.sc2tap;


import ca.nrc.cadc.auth.ACIdentityManager;
import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticatorImpl;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.uws.server.RandomStringGenerator;
import ca.nrc.cadc.uws.server.impl.PostgresJobPersistence;
import ca.nrc.cadc.vosi.AvailabilityStatus;
import ca.nrc.cadc.vosi.WebService;
import ca.nrc.cadc.vosi.avail.CheckCertificate;
import ca.nrc.cadc.vosi.avail.CheckDataSource;
import ca.nrc.cadc.vosi.avail.CheckException;
import ca.nrc.cadc.vosi.avail.CheckResource;
import ca.nrc.cadc.vosi.avail.CheckWebService;
import java.io.File;
import java.net.URI;
import org.apache.log4j.Logger;

/**
 *
 * @author pdowler
 */
public class ServiceAvailabilityImpl implements WebService
{
    private static final Logger log = Logger.getLogger(ServiceAvailabilityImpl.class);

    private String UWSDS_TEST = "select jobID from uws.Job limit 1";
    private String TAPDS_TEST = "select schema_name from tap_schema.schemas where schema_name='caom2'";
    
    public ServiceAvailabilityImpl() { }

    @Override
    public AvailabilityStatus getStatus()
    {
        boolean isGood = true;
        String note = "service is accepting queries";
        try
        {
            CheckResource cr;

            cr = AuthenticatorImpl.getAvailabilityCheck();
            cr.check();

            cr = new CheckDataSource("jdbc/uws", UWSDS_TEST);
            cr.check();

            cr = new CheckDataSource("jdbc/tapuser", TAPDS_TEST);
            cr.check();

            // tap_upload test: create and drop a table
            // datasource names are the QueryRunner defaults
            String[] uploadTest = getTapUploadTest();
            // create table
            cr = new CheckDataSource("jdbc/tapuploadadm", uploadTest[0], false);
            cr.check();
            // drop table
            cr = new CheckDataSource("jdbc/tapuploadadm", uploadTest[1], false);
            cr.check();

            // certificate need to store async results
            File cert = new File(System.getProperty("user.home") + "/.ssl/cadcproxy.pem");
            CheckCertificate checkCert = new CheckCertificate(cert);
            checkCert.check();

            RegistryClient reg = new RegistryClient();
            String vos = reg.getServiceURL(URI.create("ivo://cadc.nrc.ca/vospace"), 
                    Standards.VOSI_AVAILABILITY, AuthMethod.ANON).toExternalForm();
            CheckWebService cws = new CheckWebService(vos);
            cws.check();
        }
        catch(CheckException ce)
        {
            // tests determined that the resource is not working
            isGood = false;
            note = ce.getMessage();
        }
        catch (Throwable t)
        {
            // the test itself failed
            log.error("test failed", t);
            isGood = false;
            note = "test failed, reason: " + t;
        }
        return new AvailabilityStatus(isGood, null, null, null, note);
    }

    private String[] getTapUploadTest()
    {
        String id = new RandomStringGenerator(16).getID();
        String name = "tap_upload.avail_" + id;
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(name).append(" (");
        sb.append("c char(1), i integer, d double precision");
        sb.append(")");
        String drop = getTapUploadCleanup(name);
        return new String[] { sb.toString(), drop };
    }
    private String getTapUploadCleanup(String name)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DROP TABLE ").append(name);
        return sb.toString();
    }

    @Override
    public void setState(String string)
    {
        // no-op
    }
}
