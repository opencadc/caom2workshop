# CAOM web services for the Dec 2016 workshop

## build system

* java + gradle

## dependencies

* using code from many OpenCADC repositories

* gradle pulls all java libraries from the JCenter maven repository

## deployment

* java: 1.7 or 1.8

* application server: tomcat7 with 4 connection pools configured to connect to database

* database: postgresql-9.5 pg-sphere-1.1

## services

### non-CAOM services used in the background

* CADC data web service for archive files

* CANFAR user and group services for authentication and authorization support

* CANFAR VOSpace service for files in there

### sc2repo : CAOM repository service

* metadata creation and curation

* incremental operations via timestamp-ordered listing feature

* authenticated access only

### sc2tap : IVOA TAP service for CAOM

* async and sync execution of ADQL queries

* content: CAOM tables + ivoa.ObsCore

* anonymous and authenticated queries, although the workshop database has no mechanism for granting access to non-public metadata

* authenticated query output to VOSpace with DEST=vos://cadc.nrc.ca~vospace/place/to/store/result

### sc2links : IVOA DataLink service for CAOM

* calls the sc2tap service with Plane publisherID value(s)

* provides direct download links

* provides SODA service descriptors when metadata is sufficient to compute cutouts

### sc2meta : get single CAOM instance

* get a single Observation using observationURI 

* output in XML (default) or JSON using RESPONSEFORMAT=application/json

### sc2pkg : get CAOM data packages

* get data package (all the artifacts) for one or more CAOM planes using publisherID

* output is a tar file with all the artifacts in the specified planes

### sc2soda : IVOA SODA service for CAOM

* async and sync SODA requests

* currently works for CADC archive files and CANFAR VOSpace files

