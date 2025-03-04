--- add probtype to problems

create table problems_v2 as select * from problems_v1;

alter table problems_v2 add probtype integer;

create index problems_v2_idx on problems_v2 (
    id,
    set_id
);

update problems_v2
    set probtype = 1
    where id = id;

drop table problems_v1;

--- add probtype to user_problems

create table user_problems_v2 as select * from user_problems_v1;

alter table user_problems_v2 add probtype integer;

create index user_problems_v2_idx on user_problems_v2 (
    id,
    set_id
);

update user_problems_v2
    set probtype = 1
    where id = id;

drop table user_problems_v1;

--- enhance ref_type in references

create table references_v3 (
    pb_id integer not null,
    id integer,
    ref_type char(3) check (ref_type in ('MOL','RXN','IMG','LEW', 'MEC')),
    ref_id integer not null,
    rxn_id integer,
    primary key (pb_id, id)
);

create index references_v3 on references_v3 (
    pb_id
);

insert into references_v3 (pb_id, id, ref_type, ref_id, rxn_id)
    select pb_id, id, ref_type, ref_id, rxn_id from references_v1;

drop table references_v1;

--- enhance ref_type in user_references

create table user_references_v3 (
    user_id VARCHAR2(20),
    pb_id integer not null,
    id integer,
    ref_type char(3) check (ref_type in ('MOL','RXN','IMG','LEW','MEC')),
    ref_id integer not null,
    rxn_id integer,
    primary key (user_id, pb_id, id)
);

create index user_references_v3 on user_references_v3 (
    user_id,
    pb_id
);

insert into user_references_v3 (user_id, pb_id, id, ref_type, ref_id, rxn_id)
    select user_id, pb_id, id, ref_type, ref_id, rxn_id from user_references_v1;

drop table user_references_v1;

--- add cd_id, coded_data, mol_name to answers

create table answers_v2 as select * from answers_v1;

alter table answers_v2 add cd_id integer;

alter table answers_v2 add coded_data varchar2(100);

alter table answers_v2 add mol_name varchar2(100);

update answers_v2 set cd_id = match_id where match_id_group = 'MOL';

update answers_v2 set cd_id = 0 where match_id_group != 'MOL';

update answers_v2 a set coded_data = 
    (select b.data from id_values_v1 b where a.match_id=b.id and
    a.match_id_group=b.id_group) 
    where exists 
        (select b.data from id_values_v1 b where a.match_id=b.id and 
        a.match_id_group=b.id_group);

update answers_v2 a set mol_name = 
	(select b.mol_name from molecules1_v1 b where a.cd_id = b.cd_id )
	where exists 
        (select b.mol_name from molecules1_v1 b where a.cd_id = b.cd_id);

update answers_v2 set coded_data='Y' where match_type='LEW_ISOY' and
	match_id_group='MOL';

update answers_v2 set coded_data='Y' where match_type='EXACT_Y' and
	match_id_group='MOL';

update answers_v2 set coded_data='N' where match_type='EXACT_N' and
	match_id_group='MOL';

update answers_v2 set coded_data='Y' where match_type='EXACT_NS_Y' and
	match_id_group='MOL';

update answers_v2 set coded_data='Y' where match_type='SUB_Y' and
	match_id_group='MOL';

update answers_v2 set coded_data='N' where match_type='SUB_N' and
    match_id_group='MOL';

update answers_v2 set coded_data='Y' where match_type='CONTAINS_Y' and
    match_id_group='MOL';

update answers_v2 set coded_data='N' where match_type='CONTAINS_N' and
    match_id_group='MOL';

update answers_v2 set coded_data='Y' where match_type='SKEL_Y' and
    match_id_group='MOL';

update answers_v2 set coded_data='N' where match_type='SKEL_N' and
	match_id_group='MOL';

update answers_v2 set coded_data='Y' where match_type='SIGMA_Y' and
	match_id_group='MOL';

update answers_v2 set coded_data='N' where match_type='SIGMA_N' and
	match_id_group='MOL';

update answers_v2 set coded_data='Y' where match_type='ENANT_Y' and
	match_id_group='MOL';

update answers_v2 set coded_data='N' where match_type='ENANT_N' and
    match_id_group='MOL';

update answers_v2 set coded_data='Y' where match_type='EXACT_NN_Y' and
    match_id_group='MOL';

update answers_v2 set coded_data='N' where match_type='EXACT_NN_N' and
    match_id_group='MOL';

drop table answers_v1;

--- add cd_id, coded_data, mol_name to user_answers

create table user_answers_v2 as select * from user_answers_v1;

alter table user_answers_v2 add cd_id integer;

alter table user_answers_v2 add coded_data varchar2(100);

alter table user_answers_v2 add mol_name varchar2(100);

update user_answers_v2 set cd_id = match_id where match_id_group = 'MOL';

update user_answers_v2 set cd_id = 0 where match_id_group != 'MOL';

update user_answers_v2 a set coded_data = 
    (select b.data from id_values_v1 b where a.match_id=b.id and
    a.match_id_group=b.id_group) 
    where exists 
        (select b.data from id_values_v1 b where a.match_id=b.id and 
        a.match_id_group=b.id_group);

update user_answers_v2 a set mol_name = 
	(select b.mol_name from molecules1_v1 b where a.cd_id = b.cd_id )
	where exists 
        (select b.mol_name from molecules1_v1 b where a.cd_id = b.cd_id);

update user_answers_v2 set coded_data='Y' where match_type='LEW_ISOY' and
	match_id_group='MOL';

update user_answers_v2 set coded_data='Y' where match_type='EXACT_Y' and
	match_id_group='MOL';

update user_answers_v2 set coded_data='N' where match_type='EXACT_N' and
	match_id_group='MOL';

update user_answers_v2 set coded_data='Y' where match_type='EXACT_NS_Y' and
	match_id_group='MOL';

update user_answers_v2 set coded_data='Y' where match_type='SUB_Y' and
	match_id_group='MOL';

update user_answers_v2 set coded_data='N' where match_type='SUB_N' and
    match_id_group='MOL';

update user_answers_v2 set coded_data='Y' where match_type='CONTAINS_Y' and
    match_id_group='MOL';

update user_answers_v2 set coded_data='N' where match_type='CONTAINS_N' and
    match_id_group='MOL';

update user_answers_v2 set coded_data='Y' where match_type='SKEL_Y' and
    match_id_group='MOL';

update user_answers_v2 set coded_data='N' where match_type='SKEL_N' and
	match_id_group='MOL';

update user_answers_v2 set coded_data='Y' where match_type='SIGMA_Y' and
	match_id_group='MOL';

update user_answers_v2 set coded_data='N' where match_type='SIGMA_N' and
	match_id_group='MOL';

update user_answers_v2 set coded_data='Y' where match_type='ENANT_Y' and
	match_id_group='MOL';

update user_answers_v2 set coded_data='N' where match_type='ENANT_N' and
    match_id_group='MOL';

update user_answers_v2 set coded_data='Y' where match_type='EXACT_NN_Y' and
    match_id_group='MOL';

update user_answers_v2 set coded_data='N' where match_type='EXACT_NN_N' and
    match_id_group='MOL';

drop table user_answers_v1;

--- combine LEW_ISO_Y; not really needed for Prentice-Hall data.

update answers_v2 set coded_data=coded_data||'/E/3' where
	match_id_group='LEW_OUT_Y' and coded_data not like '%/%/%/%/%';

update user_answers_v2 set coded_data=coded_data||'/E/3' where
	match_id_group='LEW_OUT_Y' and coded_data not like '%/%/%/%/%';

--- add rxn_data to and remove rxn_id from references 

create table references_v4 as select * from references_v3;

alter table references_v4 add rxn_data varchar2(100);

update references_v4 a set rxn_data = (select b.data from id_values_v1 b where
	a.rxn_id = b.id and a.rxn_id != 0 and b.id_group='RXN');

alter table references_v4 drop column rxn_id;

drop table references_v3;

--- add rxn_data to and remove rxn_id from user_references 

create table user_references_v4 as select * from user_references_v3;

alter table user_references_v4 add rxn_data varchar2(100);

update user_references_v4 a set rxn_data = (select b.data from
	user_id_values_v1 b where
	a.rxn_id = b.id and a.rxn_id != 0 and b.id_group='RXN');

alter table user_references_v4 drop column rxn_id;

drop table user_references_v3;

--- all information from id_values_v1 and user_id_values_v1 is safely collected

drop table id_values_v1;
drop table user_id_values_v1;

--- populate grade for answers

update answers_v2 set grade=0.00 where ans_type='W';

update answers_v2 set grade=1.00 where ans_type='C';

--- populate grade for user_answers

update user_answers_v2 set grade=0.00 where ans_type='W';

update user_answers_v2 set grade=1.00 where ans_type='C';

--- upgrade the JChem property file

update JChemProperties
	set prop_name='table.ACEORG15.MOLECULES1_V1.version'
	where prop_name = 'table.EPOCH.MOLECULES1_V1.version';
update JChemProperties
	set prop_name='table.ACEORG15.MOLECULES1_V1.absoluteStereo'
	where prop_name='table.EPOCH.MOLECULES1_V1.absoluteStereo';
update JChemProperties
	set prop_name='table.ACEORG15.MOLECULES1_V1.fingerprint.numberOfBits'
	where prop_name='table.EPOCH.MOLECULES1_V1.fingerprint.numberOfBits';
update JChemProperties
	set prop_name='table.ACEORG15.MOLECULES1_V1.fingerprint.numberOfOnes'
	where prop_name='table.EPOCH.MOLECULES1_V1.fingerprint.numberOfOnes';
update JChemProperties
	set prop_name='table.ACEORG15.MOLECULES1_V1.fingerprint.numberOfEdges'
	where prop_name='table.EPOCH.MOLECULES1_V1.fingerprint.numberOfEdges';
update JChemProperties
	set prop_name='table.ACEORG15.USER_MOLECULES1_V1.version'
	where prop_name='table.EPOCH.USER_MOLECULES1_V1.version';
update JChemProperties
	set prop_name='table.ACEORG15.USER_MOLECULES1_V1.absoluteStereo'
	where prop_name='table.EPOCH.USER_MOLECULES1_V1.absoluteStereo';
update JChemProperties
	set prop_name='table.ACEORG15.USER_MOLECULES1_V1.fingerprint.numberOfBits'
	where prop_name='table.EPOCH.USER_MOLECULES1_V1.fingerprint.numberOfBits';
update JChemProperties
	set prop_name='table.ACEORG15.USER_MOLECULES1_V1.fingerprint.numberOfOnes'
	where prop_name='table.EPOCH.USER_MOLECULES1_V1.fingerprint.numberOfOnes';
update JChemProperties
	set prop_name='table.ACEORG15.USER_MOLECULES1_V1.fingerprint.numberOfEdges'
	where prop_name='table.EPOCH.USER_MOLECULES1_V1.fingerprint.numberOfEdges';
update JChemProperties
	set prop_name='table.ACEORG15.MISC_MOLECULES_V1.version'
	where prop_name='table.EPOCH.MISC_MOLECULES_V1.version';
update JChemProperties
	set prop_name='table.ACEORG15.MISC_MOLECULES_V1.absoluteStereo'
	where prop_name='table.EPOCH.MISC_MOLECULES_V1.absoluteStereo';
update JChemProperties
	set prop_name='table.ACEORG15.MISC_MOLECULES_V1.fingerprint.numberOfBits'
	where prop_name='table.EPOCH.MISC_MOLECULES_V1.fingerprint.numberOfBits';
update JChemProperties
	set prop_name='table.ACEORG15.MISC_MOLECULES_V1.fingerprint.numberOfOnes'
	where prop_name='table.EPOCH.MISC_MOLECULES_V1.fingerprint.numberOfOnes';
update JChemProperties
	set prop_name='table.ACEORG15.MISC_MOLECULES_V1.fingerprint.numberOfEdges'
	where prop_name='table.EPOCH.MISC_MOLECULES_V1.fingerprint.numberOfEdges';
update JChemProperties
	set prop_name='table.ACEORG15.USER_MISC_MOLECULES_V1.version'
	where prop_name='table.EPOCH.USER_MISC_MOLECULES_V1.version';
update JChemProperties
	set prop_name='table.ACEORG15.USER_MISC_MOLECULES_V1.absoluteStereo'
	where prop_name='table.EPOCH.USER_MISC_MOLECULES_V1.absoluteStereo';
update JChemProperties
	set
	prop_name='table.ACEORG15.USER_MISC_MOLECULES_V1.fingerprint.numberOfBits'
	where
	prop_name='table.EPOCH.USER_MISC_MOLECULES_V1.fingerprint.numberOfBits';
update JChemProperties
	set
	prop_name='table.ACEORG15.USER_MISC_MOLECULES_V1.fingerprint.numberOfOnes'
	where
	prop_name='table.EPOCH.USER_MISC_MOLECULES_V1.fingerprint.numberOfOnes';
update JChemProperties
	set
	prop_name='table.ACEORG15.USER_MISC_MOLECULES_V1.fingerprint.numberOfEdges'
	where
	prop_name='table.EPOCH.USER_MISC_MOLECULES_V1.fingerprint.numberOfEdges';

--- raphael 6/2006: relax some limits

alter table hwsets_v2 modify fixedSet varchar2(1000) ;
alter table hwsets_v2 modify randomSets varchar2(1000) ;

--- mallika 7/2006 question data to facilitate multiple choice and mechanisms

create table question_data (
	data_id integer,
	question_id integer not null,
	serial_no integer,
	data_type varchar2(10),
	data blob,
	name varchar2(40),
	primary key (data_id)
);

--- added by Samuel Dost (08/2006)

create table user_question_data (
	data_id integer,
	user_id varchar2(20),
	question_id integer not null,
	serial_no integer,
	data_type varchar2(10),
	data blob,
	name varchar2(40),
	primary key (data_id)
);

--- convert responses LONG to CLOB: raphael 9/2006

create table responses_v3 (
    hw_id integer,
    student_id varchar2(50),
    pb_id integer,
    status char check (status in ('W','P','C')),
    tries integer,
    grade  number(3,2),
    feedback varchar2(800),
    response CLOB,
    primary key (hw_id, student_id, pb_id)
);

create index responses_v3_idx on responses_v3 (
	hw_id,
	student_id
);

--- full copy into responses_v3
insert into responses_v3
    select hw_id, student_id, pb_id, status, tries, grade, feedback,
            to_lob(response)
	from responses_v2;

--- add password field to cw_coursedoc_v2
alter table cw_coursedoc_v2 add password_hash raw(150);

--- add timezone
alter table cw_courses_v2 add time_zone long;

--- add fields to problems_v2
alter table problems_v2 add (sequence_id integer, keywords CLOB);
alter table user_problems_v2 add (sequence_id integer, keywords CLOB);
update problems_v2 set sequence_id = id;
update user_problems_v2 set sequence_id = id;
update problems_v2 p
    set keywords = book || ' chapter' || chapter || ' ' ||
		(select name from chapters_v1
			where id = (select chap_id from pbsets_v2
				where id = p.set_id)) || 
		' ' ||
		(select name from pbsets_v2 
			where id = p.set_id);
update user_problems_v2 p
    set keywords = book || ' chapter' || chapter || ' ' ||
		(select name from chapters_v1
			where id = (select chap_id from pbsets_v2
				where id = p.set_id)) || 
		' ' ||
		(select name from pbsets_v2 
			where id = p.set_id);

--- increase size of question_data.name.  Raphael 12/31/2006

alter table question_data modify name varchar2(100);
alter table user_question_data modify name varchar2(100);

--- increase size of answers_v2.mol_name.  Raphael 1/17/2007
alter table answers_v2 modify mol_name varchar2(1000);
alter table user_answers_v2 modify mol_name varchar2(1000);
alter table MOLECULES1_V1 modify mol_name varchar2(1000);
alter table MISC_MOLECULES_V1 modify mol_name varchar2(1000);
alter table USER_MOLECULES1_V1 modify mol_name varchar2(1000);
alter table USER_MISC_MOLECULES_V1 modify mol_name varchar2(1000);

--- clarify and simplify evaluators.  Raphael and Mallika, 2/17/2007
update answers_v2 set match_type='CHARGE' where match_type = 'CHARGE_Y';
update answers_v2 set match_type='LEW_ISO' where match_type = 'LEW_ISOY';
update answers_v2 set match_type='FORMULA', coded_data='true/' || coded_data
	where match_type='FORMULA_Y';
update answers_v2 set match_type='FORMULA', coded_data='false/' || coded_data
	where match_type='FORMULA_N';
update answers_v2 set match_type='GROUP', coded_data='true/' || coded_data
	where match_type='GROUP_Y';
update answers_v2 set match_type='GROUP', coded_data='false/' || coded_data
	where match_type='GROUP_N';

--- modify how students are allowed extensions.  Raphael and Bob, 2/18/2007
alter table cw_coursedoc_v2 add extensionees varchar2(1000);
alter table cw_coursedoc_v2 drop column password_hash;

--- clarify and simplify evaluators.  Raphael and Mallika, 2/17/2007

update answers_v2 set match_type='IS', coded_data='2/1'
	where match_type='ENANT_Y';
update answers_v2 set match_type='IS', coded_data='1/1'
	where match_type='ENANT_N';
update answers_v2 set match_type='IS', coded_data='2/2'
	where match_type='EXACT_NN_Y';
update answers_v2 set match_type='IS', coded_data='1/2'
	where match_type='EXACT_NN_N';
update answers_v2 set match_type='IS', coded_data='2/0'
	where match_type='EXACT_Y';
update answers_v2 set match_type='IS', coded_data='1/0'
	where match_type='EXACT_N';
update answers_v2 set match_type='IS', coded_data='2/8'
	where match_type='SIGMA_Y';
update answers_v2 set match_type='IS', coded_data='1/8'
	where match_type='SIGMA_N';
update answers_v2 set match_type='IS', coded_data='4/0'
	where match_type='CONTAINS_Y';
update answers_v2 set match_type='IS', coded_data='1/0'
	where match_type='CONTAINS_N';
update answers_v2 set match_type='WT', coded_data=coded_data || '/0/1'
	where match_type='MOLWT_Y';
update answers_v2 set match_type='WT', coded_data=coded_data || '/3/1'
	where match_type='MOLWT_N';
update answers_v2 set match_type='WT', coded_data=coded_data || '/0/0'
	where match_type='EXACTWT_Y';
update answers_v2 set match_type='WT', coded_data=coded_data || '/3/0'
	where match_type='EXACTWT_N';
update answers_v2 set match_type='CONTAIN', coded_data='4/2'
	where match_type='SKEL_Y';
update answers_v2 set match_type='CONTAIN', coded_data='1/2' 
	where match_type='SKEL_N';
update answers_v2 set match_type='CONTAIN', coded_data='4/1' 
	where match_type='SUB_Y';
update answers_v2 set match_type='CONTAIN', coded_data='1/1' 
	where match_type='SUB_N';

--- add grading field to cw_coursedoc_v2.  Raphael and Bob 2/22/2007

alter table cw_coursedoc_v2 add grading varchar2(1000);

--- increase size of several fields.  Raphael 3/27/2007

alter table pbsets_v2 modify header varchar2(1000);
alter table problems_v2 modify statement varchar2(1600);

--- same as earlier, but also for locally authored questions
update user_answers_v2 set match_type='IS', coded_data='2/1'
	where match_type='ENANT_Y';
update user_answers_v2 set match_type='IS', coded_data='1/1'
	where match_type='ENANT_N';
update user_answers_v2 set match_type='IS', coded_data='2/2'
	where match_type='EXACT_NN_Y';
update user_answers_v2 set match_type='IS', coded_data='1/2'
	where match_type='EXACT_NN_N';
update user_answers_v2 set match_type='IS', coded_data='2/0'
	where match_type='EXACT_Y';
update user_answers_v2 set match_type='IS', coded_data='1/0'
	where match_type='EXACT_N';
update user_answers_v2 set match_type='IS', coded_data='2/8'
	where match_type='SIGMA_Y';
update user_answers_v2 set match_type='IS', coded_data='1/8'
	where match_type='SIGMA_N';
update user_answers_v2 set match_type='IS', coded_data='4/0'
	where match_type='CONTAINS_Y';
update user_answers_v2 set match_type='IS', coded_data='1/0'
	where match_type='CONTAINS_N';
update user_answers_v2 set match_type='WT', coded_data=coded_data || '/0/1'
	where match_type='MOLWT_Y';
update user_answers_v2 set match_type='WT', coded_data=coded_data || '/3/1'
	where match_type='MOLWT_N';
update user_answers_v2 set match_type='WT', coded_data=coded_data || '/0/0'
	where match_type='EXACTWT_Y';
update user_answers_v2 set match_type='WT', coded_data=coded_data || '/3/0'
	where match_type='EXACTWT_N';
update user_answers_v2 set match_type='CONTAIN', coded_data='4/2'
	where match_type='SKEL_Y';
update user_answers_v2 set match_type='CONTAIN', coded_data='1/2' 
	where match_type='SKEL_N';
update user_answers_v2 set match_type='CONTAIN', coded_data='4/1' 
	where match_type='SUB_Y';
update user_answers_v2 set match_type='CONTAIN', coded_data='1/1' 
	where match_type='SUB_N';

--- Raphael 5/22/2007
--- undoing previous change to maintain consistency with production.
--- modifying user_answers_v2 to keep them consistent with earlier change,
--- which was on 2/17/2007

update answers_v2 set match_type='CHARGE_Y' where match_type = 'CHARGE';
update user_answers_v2 set match_type='LEW_ISO' where match_type = 'LEW_ISOY';
update user_answers_v2 set match_type='FORMULA', coded_data='true/' || coded_data
	where match_type='FORMULA_Y';
update user_answers_v2 set match_type='FORMULA', coded_data='false/' || coded_data
	where match_type='FORMULA_N';
update user_answers_v2 set match_type='GROUP', coded_data='true/' || coded_data
	where match_type='GROUP_Y';
update user_answers_v2 set match_type='GROUP', coded_data='false/' || coded_data
	where match_type='GROUP_N';

--- removing obsolete attributes.  Raphael 6/2007
alter table ANSWERS_V2 drop column ANS_TYPE;
alter table ANSWERS_V2 drop column MATCH_ID_GROUP;
alter table ANSWERS_V2 drop column MATCH_ID;
alter table RESPONSES_V3 drop column STATUS;

--- add database relations for synthesis questions.  Raphael 7/2007
create table REACTION_CONDITIONS_V1 (
    RXN_COND_ID integer,
	NAME varchar(40),
	DEFINITION blob,
	CLASS varchar(40),
    primary key (RXN_COND_ID)
);

--- build an id sequence to avoid nastiness of turning off autocommit
--- Raphael 7/2007
--- RBG notes: The sequencer means that new reaction conditions at UK will not 
--- have the same ID numbers as when they are added at Pearson, but this shouldn't 
--- matter, because we do not store reaction condition IDs in questions anymore.

grant CREATE SEQUENCE to aceorg15; --- must execute as DBA:
-- import the contents of the UK database to Pearson *before* executing the next two lines.
create sequence REACTION_CONDITIONS_SEQ START WITH 1;

--- removing more obsolete attributes.  Raphael 8/2007

alter table USER_ANSWERS_V2 drop column ANS_TYPE;
alter table USER_ANSWERS_V2 drop column MATCH_ID_GROUP;
alter table USER_ANSWERS_V2 drop column MATCH_ID;

--- adding reaction conditions to homework sets.  Raphael 8/2007

alter table HWSETS_V2 add RXN_COND_ID varchar(1000);

--- for R-groups
--- RBG notes: The sequencer means that new R group collections at UK will not 
--- have the same ID numbers as when they are added at Pearson, so care will
--- need to be exercised when transferring R-group questions from UK to Pearson.

create table R_GROUP_CLASS_V1 (
	R_GROUP_CLASS_ID integer,
	NAME varchar(500),
	MEMBERS varchar(1000),
	primary key (R_GROUP_CLASS_ID)
);
-- import the contents of the UK database to Pearson *before* executing the next two lines.
create sequence R_GROUP_CLASS_SEQ START WITH 
	(select max(R_GROUP_CLASS_ID) from R_GROUP_CLASS_V1) + 10;

--- increase caution.  Raphael 10/2007

update question_data set data_type = 'text'
    where data_type is null;
update user_question_data set data_type = 'text'
    where data_type is null;
alter table question_data modify data_type varchar2(10) not null;
alter table user_question_data modify data_type varchar2(10) not null;

--- added by Second Avenue

alter table cw_courses_v2 add use_uniq_id char(1);
alter table cw_courses_v2 add uniq_id_label varchar2(50);
alter table cw_course_enrollment_v2 add uniq_id varchar2(50);

--- *** ACE 1.6 ends here ***

--- add R_GROUPS to RESPONSES_V3.  Raphael 1/2008

alter table RESPONSES_V3 add R_GROUPS varchar2(1000);

--- allow local instructors to modify the common problem statement for a problem set
--- Raphael 1/2008

create table MODIFIED_HEADERS_V1 (
	USER_ID varchar2(40),
	PBSET_ID integer,
	HEADER varchar2(1000)
);

create index MODIFIED_HEADERS_IDX on MODIFIED_HEADERS_V1 (
    USER_ID,
    PBSET_ID
);

--- remove obsolete table entries.  Raphael 3/2008

alter table ANSWERS_V2 drop column COMPLEX;
alter table USER_ANSWERS_V2 drop column COMPLEX;
alter table RESPONSES_V3 drop column FEEDBACK;

--- database cleanup.  Raphael 3/2008

delete from BB_USERS where USER_ID not in (
    select USER_ID from CW_USER_V2);

delete from BB_USERS where USER_ID not in (
	select user_id from cw_user_v2 where role = 'I');

delete from PBSETS_V2 where USER_ID not in (
    select user_id from cw_user_v2 where role = 'I');

delete from CW_COURSE_PREENROLLMENT_V2 where COURSE_ID in (
	select id from CW_COURSES_V2 where INSTRUCTOR_ID not in (
		select user_id from cw_user_v2 where role = 'I')
	);

delete from CW_COURSE_ENROLLMENT_V2 where COURSE_ID in (
	select id from CW_COURSES_V2 where INSTRUCTOR_ID not in (
		select user_id from cw_user_v2 where role = 'I')
	);

delete from CW_COURSEDOC_V2 where COURSE_ID in (
    select id from CW_COURSES_V2 where INSTRUCTOR_ID not in (
        select user_id from cw_user_v2 where role = 'I')
    );


--- submission-time dependent grading: 4/2008

alter table responses_v3 add when varchar2(25);
	update responses_v3 set when = to_char(sysdate, 'YYYY/MM/DD HH24:MI:SS');

alter table cw_coursedoc_v2 add time_grading varchar2(1000);

-- for compatibility with SMS: add further identifiers of students, flags to
-- courses; these updates have already been applied to Second Avenue & Pearson's
-- installations! 4/2008

alter table cw_course_enrollment_v2 add uniq_id varchar2(20);
alter table cw_courses_v2 add use_uniq_id varchar2(1);
	update cw_courses_v2 set use_uniq_id = 'N';
alter table cw_courses_v2 add uniq_id_label varchar2(50);

-- in SMS, move data from redundant field in table enrollment to table users,
-- then drop redundant field; does not need to be done at UK! 4/2008

-- Modified from http://publib.boulder.ibm.com/infocenter/iseries/v5r3/index.jsp?topic=/sqlp/rbafycorrs.htm

-- Each row of cw_user_v2 is checked to see if it has a corresponding row in 
-- cw_course_enrollment_v2. If it does have such a matching 
-- row, the COALESCE function is used to return a value for uniq_id. If uniq_id 
-- in cw_course_enrollment_v2 has a non-null value, that value is
-- used to update student_num in cw_user_v2. If uniq_id in cw_course_enrollment_v2 is 
-- NULL, cw_user_v2.student_num is updated with its own value.

UPDATE cw_course_enrollment_v2 SET uniq_id = '' WHERE uniq_id = '-1';
UPDATE cw_user_v2 X
	SET student_num = (
		SELECT UNIQUE COALESCE (Y.uniq_id, X.student_num)
			FROM cw_course_enrollment_v2 Y
			WHERE X.user_id = Y.student_id)
		WHERE X.user_id IN (
			SELECT student_id
			FROM cw_course_enrollment_v2
	);
alter table cw_course_enrollment_v2 drop column uniq_id;

-- increase size of reaction_conditions_V1 class

alter table reaction_conditions_v1 modify class varchar2(200);

-- add column to responses to denote nature of stored response

alter table responses_v3 add status char(1);
update responses_v3 set status = 'E';
update responses_v3 set status = 'I' where tries = 0;
update responses_v3 set status = 'S' where tries = -1;

-- move molecules from molecules1_v1 to answers_v2
-- Second Avenue should validate and test this SQL before executing it on the
-- Pearson database!

alter table answers_v2 add mol_structure BLOB;
alter table user_answers_v2 add mol_structure BLOB;
update answers_v2 set mol_structure =
	(select molecules1_v1.mol_structure from molecules1_v1
		where molecules1_v1.mol_id = answers_v2.cd_id)
	where exists
		(select * from molecules1_v1
		where molecules1_v1.mol_id = answers_v2.cd_id);

update user_answers_v2 set mol_structure =
	(select user_molecules1_v1.mol_structure from user_molecules1_v1
		where user_molecules1_v1.mol_id = user_answers_v2.cd_id)
	where exists
		(select * from user_molecules1_v1
		where user_molecules1_v1.mol_id = user_answers_v2.cd_id);

-- move molecules from misc_molecules_v1 to references_v4
-- Second Avenue should validate and test this SQL before executing it on the
-- Pearson database!

alter table references_v4 add mol_structure BLOB;
alter table user_references_v4 add mol_structure BLOB;
update references_v4 set mol_structure = 
	(select misc_molecules_v1.mol_structure from misc_molecules_v1 
			where misc_molecules_v1.mol_id = references_v4.ref_id)
	where ref_type != 'IMG' and exists 
		(select * from misc_molecules_v1 
				where misc_molecules_v1.mol_id = references_v4.ref_id);

update user_references_v4 set mol_structure = 
	(select user_misc_molecules_v1.mol_structure from user_misc_molecules_v1 
			where user_misc_molecules_v1.mol_id = user_references_v4.ref_id) 
	where ref_type != 'IMG' and exists 
		(select * from user_misc_molecules_v1 
				where user_misc_molecules_v1.mol_id = user_references_v4.ref_id);

-- consolidate booleans in cw_coursedoc_v2 into one flag

alter table cw_coursedoc_v2 add flags number(38);
update cw_coursedoc_v2 set flags = 0 where visible = 'N' and show_after_due = 'N';
update cw_coursedoc_v2 set flags = 1 where visible = 'Y' and show_after_due = 'N';
update cw_coursedoc_v2 set flags = 2 where visible = 'N' and show_after_due = 'Y';
update cw_coursedoc_v2 set flags = 3 where visible = 'Y' and show_after_due = 'Y';
alter table cw_coursedoc_v2 drop column visible;
alter table cw_coursedoc_v2 drop column show_after_due;

-- add sequencers

create sequence problem_seq start with (select max(id) from 
	(select id from user_problems_v2 union select id from problems_v2)) + 10;
create sequence reference_seq start with 
	(select max(ref_id) from references_v4) + 10;
create sequence question_data_seq start with 
	(select max(data_id) from question_data) + 10;
create sequence chapters_seq start with (select max(id) from chapters_v1) + 10; 
create sequence pbsets_seq start with (select max(id) from pbsets_v2) + 10; 
create sequence hwsets_seq start with (select max(id) from hwsets_v2) + 10; 
create sequence cw_courses_seq start with (select max(id) from cw_courses_v2) + 10; 
create sequence CW_COURSEDOC_SEQ start with (select max(id) from CW_COURSEDOC_V2) + 10; 

-- make bb_users user_id same length as other fields that point to cw_user_v2.user_id

alter table bb_users modify user_id varchar2(40);

-- add TA flag to enrollment list

alter table cw_course_enrollment_v2 add isTA char(1);

-- add Assignment Questions table

create table assignment_questions_v1 (
	user_id varchar(40) not null,
	assignment_id number(38) not null,
	qlist varchar(2000),
	primary key (user_id, assignment_id)
);

-- add coinstructors table

create table coinstructors_v1 (
	course_id number(38) not null,
	instructor_id varchar2(40) not null 
);

-- add field for three-component reactions flag, make field for name bigger

alter table reaction_conditions_V1 add threeComponent char(1);
update reaction_conditions_V1 set threeComponent = 'N';
alter table reaction_conditions_v1 modify name varchar2(200);

-- stop storing permissible reaction condition lists for multistep synthesis questions
-- (stored only in assignments now)

delete from question_data where data_type = 'rxn_condn';
delete from user_question_data where data_type = 'rxn_condn';

-- store student IDs created just for purpose of exams

create table exam_students_v1 (
	user_id varchar2(40) not null, 
	course_id number(38),
	created varchar2(25),
	primary key (user_id),
	foreign key (user_id) references cw_user_v2(user_id),
	foreign key (course_id) references cw_courses_v2(id)
);

-- convert boolean in cw_courses_v2 into flag

alter table cw_courses_v2 add flags number(38);
update cw_courses_v2 set flags = 0 where enabled = 'N';
update cw_courses_v2 set flags = 1 where enabled = 'Y';
alter table cw_courses_v2 drop column enabled;

-- clean up unused columns

alter table bb_users drop column comments;
alter table cw_course_preenrollment_v2 drop column student_email;
alter table image_table1 drop column pic_name;
alter table user_image_table1 drop column pic_name;
alter table chapters_v1 drop column book;

-- store functional groups; populate this table by dumping from UK database
--- RBG notes: The sequence means that new functional groups at UK will not 
--- have the same ID numbers as when they are added at Pearson, so care will
--- need to be exercised when transferring questions from UK to Pearson.

create table functional_groups_v1 (
	group_id number(38) not null, 
	name varchar2(80) not null,
	category varchar2(40) not null,
	definition varchar2(500) not null,
	sortkey varchar2(100) not null,
	primary key (group_id, name)
);
-- import the contents of the UK database to Pearson *before* executing the next two lines.
create sequence functional_groups_seq 
	start with (select max(group_id) from functional_groups_v1) + 10; 

-- store impossible synthetic starting materials; populate this table by dumping
-- from UK database

create table impossible_SMs_v1 (
	name varchar2(80) not null,
	definition varchar2(500) not null,
	sortkey varchar2(100) not null,
	primary key (name)
);

-- there are two enol ether definitions with different group IDs; replace one
-- with the other

update answers_v2 set coded_data = 'true/74' where coded_data = 'true/21';
update answers_v2 set coded_data = 'false/74' where coded_data = 'false/21';
update user_answers_v2 set coded_data = 'true/74' where coded_data = 'true/21';
update user_answers_v2 set coded_data = 'false/74' where coded_data = 'false/21';

-- synchronize UK reaction conditions database with Pearson
drop sequence REACTION_CONDITIONS_SEQ;
-- import the contents of the UK database to Pearson *before* executing the next two lines.
create sequence REACTION_CONDITIONS_SEQ 
	START WITH (select max(RXN_COND_ID) from REACTION_CONDITIONS_V1) + 10; 

-- create table to store user language preferences

create table languages_v1 (
	user_id varchar2(40) not null, 
	language varchar2(150) not null,
	preference number(4),
	primary key (user_id, language)
);
create index languages_v1_idx on languages_v1 (
    user_id
);

-- create table to store translated phrases

create table translations_v1 (
	phrase_id number(38) not null, 
	language varchar2(150) not null,
	translation BLOB, 
	primary key (phrase_id, language)
);
create sequence TRANSLATIONS_SEQ START WITH (select max(phrase_id) from translations_v1) + 10; 


-- add columns for ID numbers for student-visible phrases to be translated; 
-- all will use same sequence

alter table pbsets_v2 add header_id number(38);
alter table problems_v2 add statement_id number(38);
alter table answers_v2 add feedback_id number(38);
alter table question_data add text_id number(38);

-- add translator permission for instructor

alter table bb_users add translator char(1);

--- drop obsolete tables

drop table BB_ACCESSLIST;
drop table JCHEMPROPERTIES;
drop table AMS;
drop table USER_MISC_MOLECULES_V1;
drop table USER_MISC_MOLECULES_V2;
drop table USER_MISC_MOLECULES_V1_UL;
drop table USER_MOLECULES1_V1;
drop table USER_MOLECULES1_V1_UL;
drop table USER_MISC_VALUES_V1;
drop table MISC_MOLECULES_V1;
drop table MISC_MOLECULES_V2;
drop table MISC_MOLECULES_V1_UL;
drop table MOLECULES1_V1;
drop table MOLECULES1_V1_UL;
drop table MISC_VALUES_V1;

-- move course.use_uniq_id into flags

update cw_courses_v2 set flags = (flags + 2) where use_uniq_id = 'Y';
alter table cw_courses_v2 drop column use_uniq_id;

-- make user_problems_v2.statement same length as problems_v2.statement

alter table user_problems_v2 modify statement varchar2(1600);

-- transfer data from fixedset to qlist, removing whitespace and reformatting
update hwsets_v2
set qlist='1@'||replace(replace(fixedset, ':', ':1@'), ' ', '') 
where fixedset is not null and qlist is null;

-- change column name so it is not a keyword

alter table responses_v3 rename column when to date_time;

-- RBG 12/2009 relax some limits in light of CERs

alter table answers_v2 modify coded_data varchar2(200) ;
alter table user_answers_v2 modify coded_data varchar2(200) ;

-- increase size of rxn_data field in references_v4 to accommodate Jmol scripts

alter table references_v4 modify rxn_data varchar2(500);
alter table user_references_v4 modify rxn_data varchar2(500);

-- RBG 4/2010 add field for instuctor comments on student responses

alter table responses_v3 add instructor_comment varchar2(500);

-- RBG 6/2010 add table for conversion of units 

create table unit_conversions_v1 (
    unit_from VARCHAR2(20) not null,
    unit_to VARCHAR2(20) not null,
	factor number,
	power number,
    primary key (unit_from, unit_to)
);

-- RF 7/2010 forgot to build this before!

create index references_v4_idx on references_v4 (pb_id);

-- RBG 11/2010 merging coursedocs and hwsets tables
create table hwsets_v3 (
	hw_id number(38),
	course_id number(38),
	instructor_id varchar2(50),
	serial_no number(38),
	name varchar2(80),
	remarks varchar2(500),
	date_created varchar2(50),
	date_due varchar2(50),
	tries number(38),
	attempt_grading varchar2(1000),
	time_grading varchar2(1000),
	extensionees varchar2(1000),
	flags number(38),
	rxn_cond_id varchar2(1000),
	qlist varchar2(2000),
	primary key (hw_id)
);

create index hwsets_v3_idx on hwsets_v3 (
    hw_id,
    course_id,
	instructor_id
);

INSERT INTO hwsets_v3 (
	hw_id, 
	course_id, 
	instructor_id, 
	serial_no, 
	name, 
	remarks, 
	date_created, 
	date_due, 
	tries, 
	attempt_grading, 
	time_grading, 
	extensionees, 
	flags, 
	rxn_cond_id, 
	qlist
) SELECT hwsets_v2.id, 
	cw_coursedoc_v2.course_id, 
	hwsets_v2.user_id, 
	cw_coursedoc_v2.seq_num, 
	hwsets_v2.name, 
	hwsets_v2.remarks, 
	cw_coursedoc_v2.date_created, 
	cw_coursedoc_v2.date_due, 
	hwsets_v2.tries, 
	cw_coursedoc_v2.grading, 
	cw_coursedoc_v2.time_grading, 
	cw_coursedoc_v2.extensionees, 
	cw_coursedoc_v2.flags, 
	hwsets_v2.rxn_cond_id, 
	hwsets_v2.qlist 
FROM cw_coursedoc_v2 
INNER JOIN hwsets_v2 
ON cw_coursedoc_v2.external_id = hwsets_v2.id;

drop table hwsets_v2;
drop table cw_coursedoc_v2;
drop sequence hwsets_seq;
drop sequence cw_coursedoc_seq;
create sequence hwsets_seq start with (select max(hw_id) from hwsets_v3) + 10; 

-- RBG 12/2010 
-- moving to JChem 5.4.0, need reaction definitions
-- that handle reaction stereochemistry ChemAxon's way

-- copy UK's reaction_conditions_v2 table, or:

create table reaction_conditions_v2 (
    rxn_cond_id integer,
	name varchar(200),
	definition blob,
	class varchar(200),
	threeComponent char(1),
    primary key (rxn_cond_id)
);

-- and then copy data from UK's reaction_conditions_v2

drop table reaction_conditions_v1;

-- RBG 12/2010 make it possible to store multiple tries in database

create table responses_v4 (
    hw_id number(38),
    student_id varchar2(50),
    pb_id number(38),
    tries number(38),
    grade  number(3,2),
    response CLOB,
	r_groups varchar2(1000),
	date_time varchar2(25),
	status char(1),
	instructor_comment varchar2(500),
    primary key (hw_id, student_id, pb_id, tries)
);

--- full copy into responses_v4
insert into responses_v4
    select hw_id, student_id, pb_id, tries, grade, 
           response, r_groups, date_time, status, instructor_comment
	from responses_v3;

drop table responses_v3;

-- RBG 12/2010 add registration date field to cw_user_v2

alter table cw_user_v2 add registration_date varchar2(25);
	update cw_user_v2 set registration_date = to_char(sysdate, 'YYYY/MM/DD HH24:MI:SS');

-- RBG 12/2010 add allowed IP addresses field to cw_courses_v2

alter table cw_courses_v2 add allowed_ips varchar2(600);


