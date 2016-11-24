
create table uws.Job
(
    jobID                   varchar(16)     not null,
    runID                   varchar,

-- suitable column when using the ACIIdentityManager from cadc-access-control-identity
    ownerID                 integer,

    executionPhase          varchar(16)     not null,
    executionDuration       bigint          not null,
    destructionTime         timestamp,
    quote                   timestamp,
    creationTime            timestamp       not null,
    startTime               timestamp,
    endTime                 timestamp,
    error_summaryMessage    varchar,
    error_type              varchar(16),
    error_documentURL       varchar,
   
    requestPath             varchar,
    remoteIP                varchar,

-- jobInfo is not used in TAP so make these small
    jobInfo_content         varchar,
    jobInfo_contentType     varchar,
    jobInfo_valid           smallint,

    deletedByUser           smallint        default 0,
    lastModified            timestamp       not null,

    primary key (jobID)
)
;

create table uws.JobDetail
(
    jobID                   varchar(16)     not null,
    type                    char(1)         not null,
    name                    varchar         not null,
    value                   varchar,

    foreign key (jobID) references uws.Job (jobID)
)
;

create index uws_param_i1 on uws.JobDetail(jobID)
;

-- indices to support UWS-1.1 job listing
create index uws_jobIndex_ownerID on uws.Job(ownerID)
;

create index uws_jobIndex_creationTime on uws.Job(creationTime)
;

