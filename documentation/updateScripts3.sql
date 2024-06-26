-- update from 3.0 to 3.1 starts here.

-- RBG 1/2011 implementing reordering of Qs; need to set current values of
-- serial numbers back to 0 so that default ordering is by qId

update problems_v2 set sequence_id = 0;
update user_problems_v2 set sequence_id = 0;

-- RBG 1/2011 fix previously inconsequential data inconsistencies

update cw_user_v2 set enabled = 'Y' where enabled = 'N' and role = 'S';
update bb_users set translator = 'N' where translator != 'Y' or translator is null;
update hwsets_v3 set rxn_cond_id = null where rxn_cond_id = 'null';

-- RBG 1/2011 moving bb_users data and cw_users_v2.enabled into new flags field in cw_users_v2

alter table cw_user_v2 add flags number(3);
update cw_user_v2 set flags = 0 where enabled = 'N' and 
	(user_id in (select user_id from bb_users where master_access = 'N' and translator = 'N') 
	or user_id not in (select user_id from bb_users));
update cw_user_v2 set flags = 1 where enabled = 'Y' and 
	(user_id in (select user_id from bb_users where master_access = 'N' and translator = 'N') 
	or user_id not in (select user_id from bb_users));
update cw_user_v2 set flags = 2 where enabled = 'N' and user_id in 
	(select user_id from bb_users where master_access = 'Y' and translator = 'N');
update cw_user_v2 set flags = 3 where enabled = 'Y' and user_id in 
	(select user_id from bb_users where master_access = 'Y' and translator = 'N');
update cw_user_v2 set flags = 4 where enabled = 'N' and user_id in 
	(select user_id from bb_users where master_access = 'N' and translator = 'Y');
update cw_user_v2 set flags = 5 where enabled = 'Y' and user_id in 
	(select user_id from bb_users where master_access = 'N' and translator = 'Y');
update cw_user_v2 set flags = 6 where enabled = 'N' and user_id in 
	(select user_id from bb_users where master_access = 'Y' and translator = 'Y');
update cw_user_v2 set flags = 7 where enabled = 'Y' and user_id in 
	(select user_id from bb_users where master_access = 'Y' and translator = 'Y');

-- RBG 3/2011 database cleanup of deleted instructors and orphaned images

delete from languages_v1 where user_id not in (select user_id from cw_user_v2);
delete from image_table1 where pic_id not in 
	(select ref_id from references_v4 where ref_type = 'IMG');
delete from user_image_table1 where pic_id in 
	(select ref_id from user_references_v4 where ref_type = 'IMG' 
		and user_id not in (select user_id from cw_user_v2));
delete from user_references_v4 where user_id not in (select user_id from cw_user_v2);
delete from user_question_data where user_id not in (select user_id from cw_user_v2);
delete from user_answers_v2 where user_id not in (select user_id from cw_user_v2);
delete from user_problems_v2 where user_id not in (select user_id from cw_user_v2);
delete from modified_headers_v1 where user_id not in (select user_id from cw_user_v2);
delete from pbsets_v2 where user_id is not null and user_id not in (select user_id from cw_user_v2);
delete from assignment_questions_v1 where assignment_id not in (select hw_id from hwsets_v3);
delete from responses_v4 where hw_id not in (select hw_id from hwsets_v3);

-- RBG 2/2011 moving varchar2s to CLOBS in cases where an artificial limit 
-- on length makes no sense; 
-- also dropping redundant field hwsets_v4.instructor_id,
-- adding default number-of-decimals flag (value 1) to cw_courses_v2.flags.
-- Will shortly convert BLOBs in evaluators_v3, user_evaluators_v3, figures_v5, 
-- and user_figures_v5 to CLOBs 

create table cw_courses_v3 (
	id number(38),
	instructor_id varchar2(40),
	name CLOB,
	description CLOB,
	homepage CLOB,
	notes CLOB,
	book varchar2(30),
	time_zone CLOB,
	uniq_id_label varchar2(50),
	flags number(38),
	allowed_ips CLOB,
	password_hash raw(150),
	constraint P_COURSES3 primary key (id),
	constraint F_CRS3_INSTRUCTOR foreign key (instructor_id) references cw_user_v2(user_id));
insert into cw_courses_v3 
	(select id, 
	instructor_id, 
	name, 
	description, 
	homepage, 
	notes, 
	book, 
	TO_LOB(time_zone), 
	uniq_id_label, 
	flags + 64, 
	allowed_ips,
	''
	from cw_courses_v2);

create table cw_course_enrollment_v3 (
	course_id number(38) not null,
	student_id varchar2(40) not null,
	isTA char(1),
	constraint P_ENROLL3 primary key (course_id, student_id),
	constraint F_ENROLL3_COURSE foreign key (course_id) references cw_courses_v3(id),
	constraint F_ENROLL3_STUDENT foreign key (student_id) references cw_user_v2(user_id));
insert into cw_course_enrollment_v3
	(select 
	course_id,
	student_id,
	isTA
	from cw_course_enrollment_v2);

create table cw_course_preenrollment_v3 (
	course_id number(38) not null,
	student_num varchar2(20) not null,
	student_name varchar2(80),
	institution varchar2(200),
	constraint P_COURSE_PREENROLL3 primary key (course_id, student_num, institution), 
	constraint F_PREENROLL3_COURSE foreign key (course_id) references cw_courses_v3(id));
insert into cw_course_preenrollment_v3
	(select 
	course_id,
	student_num,
	student_name,
	'University of Kentucky'
	from cw_course_preenrollment_v2);

create table exam_students_v2 (
	user_id varchar2(40) not null, 
	course_id number(38),
	created varchar2(25),
	constraint P_EXAM_USER2 primary key (user_id),
	constraint F_EXAM2_USER foreign key (user_id) references cw_user_v2(user_id),
	constraint F_EXAM2_COURSE foreign key (course_id) references cw_courses_v3(id));
insert into exam_students_v2 
	(select
	user_id,
	course_id,
	created
	from exam_students_v1);

create table pbsets_v3 (
	id number(38), 
	chap_id number(38) not null, 
	name varchar2(200) not null, 
	author varchar2(100), 
	header CLOB, 
	remarks CLOB, 
	user_id varchar2(20), 
	header_id number(38),
	constraint P_QSETS3 primary key (id),
	constraint F_QSETS3_CHAP foreign key (chap_id) references chapters_v1(id),
	constraint F_QSETS3_USER foreign key (user_id) references cw_user_v2(user_id));
insert into pbsets_v3 
	(select
	id, 
	chap_id, 
	name, 
	author, 
	header, 
	remarks, 
	user_id, 
	header_id
	from pbsets_v2);

create table modified_headers_v2 (
	user_id varchar2(40), 
	pbset_id number(38), 
	header CLOB,
	constraint P_MODHEADERS2 primary key (user_id, pbset_id),
	constraint F_MODHEADERS2_USER foreign key (user_id) references cw_user_v2(user_id),
	constraint F_MODHEADERS2_SET foreign key (pbset_id) references pbsets_v3(id));
insert into modified_headers_v2 
	(select 
	user_id, 
	pbset_id, 
	header
	from modified_headers_v1);

create table questions_v3 (
	id number(38),
	set_id number(38) not null,
	serial_no number(38),
	statement CLOB,
	book varchar2(50),
	chapter varchar2(50),
	remarks CLOB,
	probtype number(38),
	keywords CLOB,
	statement_id number(38),
	created varchar2(25),
	last_modified varchar2(25),
	constraint P_QS3 primary key (id),
	constraint F_QS3_SET foreign key (set_id) references pbsets_v3(id));
insert into questions_v3 
	(select
	id,
	set_id,
	sequence_id,
	statement,
	book,
	chapter,
	remarks,
	probtype,
	keywords,
	statement_id,
	to_char(sysdate, 'YYYY/MM/DD HH24:MI:SS'),
	to_char(sysdate, 'YYYY/MM/DD HH24:MI:SS')
	from problems_v2);

create table user_questions_v3 (
	user_id varchar2(20),
	id number(38),
	set_id number(38) not null,
	serial_no number(38),
	statement CLOB,
	book varchar2(50),
	chapter varchar2(50),
	remarks CLOB,
	probtype number(38),
	keywords CLOB,
	created varchar2(25),
	last_modified varchar2(25),
	constraint P_USERQS3 primary key (user_id, id),
	constraint F_USERQS3_USER foreign key (user_id) references cw_user_v2(user_id),
	constraint F_USERQS3_SET foreign key (set_id) references pbsets_v3(id));
insert into user_questions_v3 (select 
	user_id, 
	id, 
	set_id, 
	sequence_id, 
	statement, 
	book, 
	chapter, 
	remarks, 
	probtype, 
	keywords,
	to_char(sysdate, 'YYYY/MM/DD HH24:MI:SS'),
	to_char(sysdate, 'YYYY/MM/DD HH24:MI:SS')
	from (select 
		user_id, 
		id, 
		set_id, 
		sequence_id, 
		statement, 
		book, 
		chapter, 
		remarks, 
		probtype, 
		keywords, 
		row_number() over (partition by user_id, id order by user_id, id) rn 
		from user_problems_v2) 
	where rn = 1);

create table figures_v5 (
	pb_id number(38) not null,
	serial_no number(38),
	fig_type char(3),
	fig_id number(38) not null,
	rxn_data CLOB,
	mol_structure BLOB,
	constraint P_FIGS5 primary key (pb_id, serial_no),
	constraint F_FIGS5_Q foreign key (pb_id) references questions_v3(id));
insert into figures_v5 (select
	pb_id,
	id,
	ref_type,
	ref_id,
	rxn_data,
	mol_structure
	from references_v4);

create table user_figures_v5 (
	user_id varchar2(20),
	pb_id number(38) not null,
	serial_no number(38),
	fig_type char(3),
	fig_id number(38) not null,
	rxn_data CLOB,
	mol_structure BLOB,
	constraint P_USERFIGS5 primary key (user_id, pb_id, serial_no),
	constraint F_USERFIGS5_USER foreign key (user_id) references cw_user_v2(user_id));
insert into user_figures_v5 (select 
	user_id,
	pb_id,
	id,
	ref_type,
	ref_id,
	rxn_data,
	mol_structure from (select 
		user_id,
		pb_id,
		id,
		ref_type,
		ref_id,
		rxn_data,
		mol_structure,
		row_number() over (partition by user_id, pb_id, id order by user_id, pb_id, id) rn 
		from user_references_v4) 
	where rn = 1);

create table evaluators_v3 (
	pb_id number(38),
	major_id number(38),
	minor_id number(38),
	subexp varchar2(100),
	match_type varchar2(10),
	feedback CLOB,
	grade number(3,2),
	coded_data varchar2(200),
	mol_name CLOB,
	mol_structure BLOB,
	feedback_id number(38),
	constraint P_EVALS3 primary key (pb_id, major_id, minor_id),
	constraint F_EVALS3_Q foreign key (pb_id) references questions_v3(id));
insert into evaluators_v3 (select
	pb_id,
	major_id,
	minor_id,
	subexp,
	match_type,
	feedback,
	grade,
	coded_data,
	mol_name,
	mol_structure,
	feedback_id
	from answers_v2);

create table user_evaluators_v3 (
	user_id varchar2(20),
	pb_id number(38),
	major_id number(38),
	minor_id number(38),
	subexp varchar2(100),
	match_type varchar2(10),
	feedback CLOB,
	grade number(3,2),
	coded_data varchar2(200),
	mol_name CLOB,
	mol_structure BLOB,
	constraint P_USEREVALS3 primary key (user_id, pb_id, major_id, minor_id),
	constraint F_USEREVALS3_USER foreign key (user_id) references cw_user_v2(user_id));
insert into user_evaluators_v3 (select 
	user_id,
	pb_id,
	major_id,
	minor_id,
	subexp,
	match_type,
	feedback,
	grade,
	coded_data,
	mol_name,
	mol_structure from (select 
		user_id,
		pb_id,
		major_id,
		minor_id,
		subexp,
		match_type,
		feedback,
		grade,
		coded_data,
		mol_name,
		mol_structure,
		row_number() over 
			(partition by user_id, pb_id, major_id, minor_id 
			order by user_id, pb_id, major_id, minor_id) rn 
		from user_answers_v2) 
	where rn = 1);

create table functional_groups_v2 (
	group_id number(38),
	name varchar2(80),
	definition CLOB,
	category varchar2(40),
	sortkey varchar2(100),
	constraint P_FNALGRPS2 primary key (group_id));
insert into functional_groups_v2 (select
	group_id,
	name,
	definition,
	category,
	sortkey
	from functional_groups_v1);

create table r_group_class_v2 (
	r_group_class_id number(38),
	name varchar(100),
	members CLOB,
	constraint P_R_GRP_CLASS2 primary key (r_group_class_id));
insert into r_group_class_v2 (select
	r_group_class_id,
	name,
	members
	from r_group_class_v1);

create table impossible_SMs_v2 (
	name varchar2(80) not null,
	definition CLOB,
	sortkey varchar2(100) not null,
	constraint P_IMPOSSIBLESMS2 primary key (name));
insert into impossible_SMs_v2 (select
	name,
	definition,
	sortkey
	from impossible_SMs_v1);

create table hwsets_v4 (
	hw_id number(38),
	course_id number(38),
	serial_no number(38),
	name CLOB,
	remarks CLOB,
	date_created varchar2(50),
	date_due varchar2(50),
	tries number(38),
	question_points CLOB, 
	attempt_grading CLOB,
	time_grading CLOB,
	dependencies CLOB,
	extensionees CLOB,
	flags number(38),
	rxn_cond_id CLOB,
	qlist CLOB,
	constraint P_HWS4 primary key (hw_id),
	constraint F_HWS4_COURSE foreign key (course_id) references cw_courses_v3(id));
insert into hwsets_v4
	(select
	hw_id,
	course_id,
	serial_no,
	name,
	remarks,
	date_created,
	date_due,
	tries,
	EMPTY_CLOB(),
	attempt_grading,
	time_grading,
	EMPTY_CLOB(),
	extensionees,
	flags,
	rxn_cond_id,
	qlist
	from hwsets_v3);

create table assignment_questions_v2 (
	user_id varchar2(40) not null, 
	assignment_id number(38) not null, 
	qlist CLOB, 
	constraint P_ASSGT_QS2 primary key (user_id, assignment_id),
	constraint F_ASSGT_QS2_USER foreign key (user_id) references cw_user_v2(user_id),
	constraint F_ASSGT2_QS_ASSGT foreign key (assignment_id) references hwsets_v4(hw_id));
insert into assignment_questions_v2 
	(select 
	user_id, 
	assignment_id, 
	qlist 
	from assignment_questions_v1);

create table responses_v5 (
	hw_id number(38) not null,
	student_id varchar2(50) not null,
	pb_id number(38) not null,
	tries number(38) not null,
	grade number(3,2),
	response CLOB,
	r_groups CLOB,
	date_time varchar2(25),
	status char(1),
	instructor_comment CLOB,
	constraint P_RESPONSES5 primary key (hw_id, student_id, pb_id, tries),
	constraint F_RESP5_STUDENT foreign key (student_id) references cw_user_v2(user_id),
	constraint F_RESP5_ASSGT foreign key (hw_id) references hwsets_v4(hw_id));
insert into responses_v5 
	(select
	hw_id,
	student_id,
	pb_id,
	tries,
	grade,
	response,
	r_groups,
	date_time,
	status,
	instructor_comment
	from responses_v4);

-- RBG 2/2011 create new tables in preparation for moving BLOBs to CLOBs; 
-- newly created tables evaluators_v3, user_evaluators_v3, figures_v5, 
-- and user_figures_v5 will also have BLOBs converted

create table question_data_v2 as select * from question_data;
alter table QUESTION_DATA_V2 
	add constraint P_QDATA2 primary key (DATA_ID)
	add constraint F_QDATA2_q foreign key (QUESTION_ID) references QUESTIONS_V3(ID);
create table user_question_data_v2 as select * from user_question_data;
alter table USER_QUESTION_DATA_V2 
	add constraint P_USERQDATA2 primary key (DATA_ID)
	add constraint F_USERQDATA2_USER foreign key (user_id) references cw_user_v2(user_id);
create table reaction_conditions_v3 as select * from reaction_conditions_v2;
alter table REACTION_CONDITIONS_v3 add constraint P_RXNCONDS3 primary key (RXN_COND_ID);
create table translations_v2 as select * from translations_v1;

-- RBG 2/2011 convert BLOBs to CLOBs everywhere except images,
-- update match codes and coded data in archaic formats in evaluator tables

--- [*** run "Move BLOB data to CLOBs" in admin tool ***]
--- [*** run "Convert synthesis format" in admin tool ***]

-- RBG 3/2011 convert obsolete figure type MEC to MOL

update figures_v5 set fig_type = 'MOL' where fig_type = 'MEC';
update user_figures_v5 set fig_type = 'MOL' where fig_type = 'MEC';

-- add primary and foreign keys where they are missing

create table coinstructors_v2 (
	course_id number(38) not null,
	instructor_id varchar2(40) not null, 
	constraint P_COINSTRUCTORS2 primary key (course_id, instructor_id),
	constraint F_COINSTR2_COURSE foreign key (course_id) references cw_courses_v3(id),
	constraint F_COINSTR2_INSTRUCTOR foreign key (instructor_id) references cw_user_v2(user_id)
);
insert into coinstructors_v2 (select
	course_id,
	instructor_id
	from coinstructors_v1);

alter table CW_USER_V2 add constraint P_USERS2 primary key (USER_ID);
alter table LANGUAGES_V1 
	add constraint P_LANGUAGE1 primary key (USER_ID, LANGUAGE) 
	add constraint F_LANG1_USER foreign key (user_id) references cw_user_v2(user_id);
alter table UNIT_CONVERSIONS_V1 add constraint P_UNITS1 primary key (UNIT_FROM, UNIT_TO);
alter table CHAPTERS_V1 add constraint P_CHAPS1 primary key (ID);

-- add indices for fields that are not primary keys but that we search on
-- (but cannot make index on LOB)

create index cw_user_v2_idx on cw_user_v2 (role, institution);
create index cw_courses_v3_idx on cw_courses_v3 (instructor_id);
create index pbsets_v3_idx on pbsets_v3 (chap_id, user_id, header_id);
create index questions_v3_idx on questions_v3 (set_id, statement_id);
create index user_questions_v3_idx on user_questions_v3 (set_id);
create index question_data_v2_idx on question_data_v2 (question_id, text_id);
create index user_question_data_v2_idx on user_question_data_v2 (question_id);
create index evaluators_v3_idx on evaluators_v3 (feedback_id);
create index hwsets_v4_idx on hwsets_v4 (course_id);
create index responses_v5_idx on responses_v5 (status);

-- remove redundant fields
-- do not execute until UKY-3.0 is upgraded!

drop table bb_users;
drop table cw_courses_v2;
drop table cw_course_enrollment_v2;
drop table cw_course_preenrollment_v2;
drop table question_data;
drop table user_question_data;
drop table reaction_conditions_v2;
drop table translations_v1;
drop table pbsets_v2;
drop table modified_headers_v1;
drop table problems_v2;
drop table user_problems_v2;
drop table references_v4;
drop table user_references_v4;
drop table answers_v2;
drop table user_answers_v2;
drop table functional_groups_v1;
drop table r_group_class_v1;
drop table impossible_SMs_v1;
drop table hwsets_v3;
drop table assignment_questions_v1;
drop table responses_v4;
drop table coinstructors_v1;
drop table exam_students_v1;

-- ACE 3.1 ends here ---
-- ACE 3.2 starts here ---

-- RBG 5/2011 switch from MOL to MRV format for Lewis structures

--- [*** run "Convert Lewis structure format" in admin tool;
--- however, previous versions of ACE will not be able to read 
--- the Lewis structures in their new MRV format ***]

-- RBG 9/2011 add security questions to cw_user, drop redundant field

alter table cw_user_v2 drop column enabled; 
alter table cw_user_v2 add 
	(security_questions varchar2(10),
	security_answer1 CLOB, 
	security_answer2 CLOB);

-- RBG 9/2011 update coded data of question data that use CYCLES evaluators

update question_data_v2 set data = regexp_replace(data, 'CYCLES/', 'CYCLES/N/') || '/N=/0' 
	where data_type = 'synthOkSM' and data like 'CYCLES/%'
	and dbms_lob.substr(data, 100, 1) not like '%/%/%/%/%/%';
update user_question_data_v2 set data = regexp_replace(data, 'CYCLES/', 'CYCLES/N/') || '/N=/0' 
	where data_type = 'synthOkSM' and data like 'CYCLES/%'
	and dbms_lob.substr(data, 100, 1) not like '%/%/%/%/%/%';

-- ACE 3.2 ends here ---
-- ACE 3.3 starts here ---

-- RBG 9/2011 add parameter to map property evaluators

update evaluators_v3 set coded_data = coded_data || '/N' 
	where match_type = 'MAP_PRP' and coded_data not like '%/%/%/%';
update user_evaluators_v3 set coded_data = coded_data || '/N' 
	where match_type = 'MAP_PRP' and coded_data not like '%/%/%/%';

-- 9/2010 RBG add sortkey for users that accounts for character entity
-- references, and convert to name and address fields to CLOBS

create table cw_users_v3 (
	user_id varchar2(40) not null,
	password_hash raw(150) not null,
	role char(1) not null,
	first_name CLOB,
	last_name CLOB,
	middle_name CLOB,
	student_num varchar2(20),
	email varchar2(100),
	institution varchar2(3600),
	contact_address CLOB,
	phone varchar2(25),
	registration_date varchar2(25),
	flags number(3),
	security_questions varchar2(10),
	security_answer1 CLOB,
	security_answer2 CLOB,
	sortkey varchar2(400),
	constraint P_USERS_V3 primary key (user_id));
insert into cw_users_v3 (select
	user_id,
	password_hash,
	role,
	first_name,
	last_name,
	middle_name,
	student_num,
	email,
	institution,
	contact_address,
	phone,
	registration_date,
	flags,
	security_questions,
	security_answer1,
	security_answer2,
	nls_upper(last_name || ', ' || first_name || ' ' || middle_name)
	from cw_user_v2);
create index cw_users_v3_idx on cw_users_v3 (role, institution, registration_date);
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#19(3|4|5|6|7);', 'A');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#198;', 'AE');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#199;', 'C');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#20(0|1|2|3);', 'E');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#20(4|5|6|7);', 'I');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#208;', 'Dz');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#209;', 'N');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#20(0|1|2|3|4);', 'O');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#(217|218|219|220);', 'U');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#(221|376);', 'Y');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#222;', 'Tz');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#338;', 'OE');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#352;', 'S');
-- lower case (convert to upper case)
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#223;', 'SS');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#22(4|5|6|7|8|9);', 'A');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#230;', 'AE');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#231;', 'C');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#23(2|3|4|5);', 'E');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#23(6|7|8|9);', 'I');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#240;', 'Dz');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#241;', 'N');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#24(2|3|4|5|6|8);', 'O');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#(249|250|251|252);', 'U');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#25(3|5);', 'Y');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#254;', 'Tz');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#339;', 'OE');
update cw_users_v3 set sortkey = regexp_replace(sortkey, '&#353;', 'S');

drop table cw_users_v2;

-- 9/2011 RBG: add table to convert entered languages into their two-letter
-- code; will use to realphabetize strings according to the user's language's
-- rules

create table language_codes_v1 (
	language varchar2(150) not null,
	code varchar2(2),
	constraint P_LANG_CODES primary key (language)
	);
insert into language_codes_v1 values ('Espa&#241;ol', 'es');
insert into language_codes_v1 values ('Deutsch', 'de');
insert into language_codes_v1 values ('&#1497;&#64285;&#1491;&#1497;&#1513;', 'yi');

-- 10/2011 RBG update coded data of extended evaluators 
-- and of question data that use those same evaluators

update evaluators_v3 set coded_data = coded_data || '/N=/0' 
	where match_type = 'WT' and coded_data not like '%/%/%/%/%/%';
update user_evaluators_v3 set coded_data = coded_data || '/N=/0' 
	where match_type = 'WT' and coded_data not like '%/%/%/%/%/%';
update evaluators_v3 set coded_data = coded_data || '/N/N=/0' 
	where (match_type in ('CHARGE', 'GROUP') and coded_data not like '%/%/%/%/%')
	or (match_type = 'ATOMS' and coded_data not like '%/%/%/%/%/%/%');
update user_evaluators_v3 set coded_data = coded_data || '/N/N=/0' 
	where (match_type in ('CHARGE', 'GROUP') and coded_data not like '%/%/%/%/%')
	or (match_type = 'ATOMS' and coded_data not like '%/%/%/%/%/%/%');

update question_data_v2 set data = data || '/N/N=/0' 
	where data_type = 'synthOkSM' 
	and (data like 'CHARGE/%' or data like 'ATOMS/%' or data like 'GROUP/%')
	and dbms_lob.substr(data, 100, 1) not like '%/%/%/%/%/%';
update user_question_data_v2 set data = data || '/N/N=/0' 
	where data_type = 'synthOkSM' 
	and (data like 'CHARGE/%' or data like 'ATOMS/%' or data like 'GROUP/%')
	and dbms_lob.substr(data, 100, 1) not like '%/%/%/%/%/%';

-- 10/2011 RBG: remove responses to questions/assignments when questions have
-- already been removed from assignments

delete from responses_v5 where (hw_id, pb_id) in 
	(select responses_v5.hw_id, responses_v5.pb_id 
	from responses_v5, hwsets_v4 
	where responses_v5.hw_id = hwsets_v4.hw_id 
	and REGEXP_INSTR(hwsets_v4.qlist, '[@/;]' ||  responses_v5.pb_id || '([/;:]|$)') <= 0);

-- 10/2011 RBG separate assignment_questions qlist into separate Qs

create table hwsets_v5 as select * from hwsets_v4;
alter table HWSETS_V5 
	add constraint P_HWS5 primary key (HW_ID) 
	add constraint F_HWS5_COURSE foreign key (COURSE_ID) references CW_COURSES_V3(ID);

create table assignment_questions_v3 (
	student_id varchar2(40) not null, 
	hw_id number(38) not null, 
	serial_no number(38) not null,
    pb_id number(38) not null,
	constraint P_ASSGT_QS3 primary key (student_id, hw_id, serial_no),
	constraint F_ASSGT_QS3_USER foreign key (student_id) references cw_users_v3(user_id),
	constraint F_ASSGT_QS3_ASSGT foreign key (hw_id) references hwsets_v5(hw_id));
create index assignment_questions_v3_idx on assignment_questions_v3 (pb_id);

INSERT INTO assignment_questions_v3 
	SELECT user_id AS student_id, 
		assignment_id AS hw_id, 
		column_value AS serial_no, 
		TO_NUMBER(REGEXP_SUBSTR(qlist, '[^:]+', 1, column_value)) AS pb_id 
	FROM assignment_questions_v2, 
	TABLE(
		CAST(
			MULTISET(
				SELECT LEVEL FROM dual 
				CONNECT BY LEVEL <= LENGTH(REGEXP_REPLACE(qlist, '[^:]')) + 1
			) AS sys.OdciNumberList
		) 
	) WHERE assignment_id IN (SELECT hw_id FROM hwsets_v5) 
	AND user_id IN (SELECT user_id FROM cw_users_v3);

-- add new tables to separate :- or /-separated assignment description 
-- components into individual rows of their own tables; also, convert
-- question dependencies to use Q ID numbers instead of serial numbers

create table hwset_qs_v1 (
	hw_id number(38) not null,
	group_num number(38) not null,
	group_pick number(38) not null,
	group_bundle_size number(38) not null,
	pb_num_in_group number(38) not null,
	pb_id number(38) not null,
	constraint P_HW_QS1 primary key (hw_id, group_num, pb_num_in_group),
	constraint F_HW_QS1_ASSGT foreign key (hw_id) references hwsets_v5(hw_id));
create index hwset_qs_v1_idx on hwset_qs_v1 (pb_id);

create table hwset_dependencies_v1 (
	hw_id number(38) not null,
	dependent_pb_id number(38) not null,
	independent_pb_id number(38) not null,
	constraint P_HW_DEPS1 primary key (hw_id, dependent_pb_id),
	constraint F_HW_DEPS1_ASSGT foreign key (hw_id) references hwsets_v5(hw_id));

--- [*** run "Make new questions, dependencies tables" in admin tool ***]

create table hwset_grading_params_v1 (
	hw_id number(38) not null,
	param_type char(1) not null,
	limit_max_str varchar2(10) not null,
	factor_str varchar2(10) not null,
	limit_min number(12,2),
	limit_max number(12,2),
	factor number(5,2),
	row_num number(38),
	constraint P_HW_GRADES1 primary key (hw_id, param_type, limit_max_str),
	constraint F_HW_GRADES1_ASSGT foreign key (hw_id) references hwsets_v5(hw_id));

UPDATE hwsets_v5 
	SET attempt_grading = REGEXP_REPLACE(attempt_grading, '(^|/)/', '\19999999999/')  
	WHERE attempt_grading IS NOT null;

INSERT INTO hwset_grading_params_v1 
	SELECT hw_id, 
		'A' AS param_type, 
		REGEXP_SUBSTR(attempt_grading, '[^/]+', 1, column_value * 2 - 1) AS limit_max_str, 
		REGEXP_SUBSTR(attempt_grading, '[^/]+', 1, column_value * 2) AS factor_str,
		0 as limit_min,
		0 as limit_max,
		0 as factor, 
		column_value as row_num
	FROM hwsets_v5, 
	TABLE(
		CAST(
			MULTISET(
				SELECT LEVEL FROM dual 
				CONNECT BY LEVEL <= (LENGTH(REGEXP_REPLACE(attempt_grading, '[^/]')) + 1) / 2
			) AS sys.OdciNumberList
		)
	) WHERE attempt_grading IS NOT null;

UPDATE hwsets_v5 
	SET time_grading = REGEXP_REPLACE(time_grading, '(^|/)/', '\19999999999/')  
	WHERE time_grading IS NOT null;

INSERT INTO hwset_grading_params_v1 
	SELECT hw_id AS hw_id, 
		'T' AS param_type, 
		REGEXP_SUBSTR(time_grading, '[^/]+', 1, column_value * 2 - 1) AS limit_max_str, 
		REGEXP_SUBSTR(time_grading, '[^/]+', 1, column_value * 2) AS factor_str, 
		0 as limit_min,
		0 as limit_max,
		0 as factor, 
		column_value as row_num
	FROM hwsets_v5, 
	TABLE(
		CAST(
			MULTISET(
				SELECT LEVEL FROM dual 
				CONNECT BY LEVEL <= (LENGTH(REGEXP_REPLACE(time_grading, '[^/]')) + 1) / 2
			) AS sys.OdciNumberList
		)
	) WHERE time_grading IS NOT null;

INSERT INTO hwset_grading_params_v1 
	SELECT hw_id, 
		'Q' AS param_type, 
		TO_CHAR(column_value) AS limit_max_str, 
		REGEXP_SUBSTR(question_points, '[^/]+', 1, column_value) AS factor_str, 
		0 as limit_min,
		0 as limit_max,
		0 as factor, 
		column_value as row_num
	FROM hwsets_v5, 
	TABLE(
		CAST(
			MULTISET(
				SELECT LEVEL FROM dual 
				CONNECT BY LEVEL <= LENGTH(REGEXP_REPLACE(question_points, '[^/]')) + 1
			) AS sys.OdciNumberList
		)
	) WHERE question_points IS NOT null;

update hwset_grading_params_v1 set limit_max = to_number(limit_max_str);
update hwset_grading_params_v1 set factor = to_number(factor_str);
update hwset_grading_params_v1 op set limit_min = (
	select limit_max from hwset_grading_params_v1 ip 
	where op.hw_id = ip.hw_id 
	and op.param_type = ip.param_type 
	and op.row_num = ip.row_num + 1
) where param_type != 'Q';
update hwset_grading_params_v1 set limit_min = 0 where row_num = 1 and param_type != 'Q';
update hwset_grading_params_v1 set limit_min = limit_max where param_type = 'Q';
alter table hwset_grading_params_v1 drop column row_num;

create table hwset_extensions_v1 (
	hw_id number(38) not null,
	student_id varchar2(40) not null,
	extension_str varchar2(10) not null,
	extension number(8,2),
	serial_no number(38),
	constraint P_HW_EXTS1 primary key (hw_id, student_id),
	constraint F_HW_EXTS1_ASSGT foreign key (hw_id) references hwsets_v5(hw_id),
	constraint F_HW_EXTS1_STUDENT foreign key (STUDENT_ID) references CW_USERS_V3(USER_ID));

UPDATE hwsets_v5 SET extensionees = '' 
	WHERE DBMS_LOB.SUBSTR(extensionees, 4000, 1)  = 'null';

UPDATE hwsets_v5 
	SET extensionees = DBMS_LOB.SUBSTR(extensionees, LENGTH(extensionees) - 2, 2) 
	WHERE extensionees IS NOT null;

INSERT INTO hwset_extensions_v1 
	SELECT hw_id, 
		REGEXP_SUBSTR(extensionees, '[^/]+', 1, column_value * 2 - 1) AS student_id, 
		REGEXP_SUBSTR(extensionees, '[^/]+', 1, column_value * 2) AS extension_str, 
		0 as extension,
		column_value AS serial_no 
	FROM hwsets_v5, 
	TABLE(
		CAST(
			MULTISET(
				SELECT LEVEL FROM dual 
				CONNECT BY LEVEL <= (LENGTH(REGEXP_REPLACE(extensionees, '[^/]')) + 1) / 2
			) AS sys.OdciNumberList
		)
	) WHERE length(extensionees) > 0 
	AND DBMS_LOB.SUBSTR(REGEXP_SUBSTR(extensionees, '[^/]+', 1, column_value * 2 - 1), 50, 1)  
		IN (SELECT user_id FROM cw_users_v3);
update hwset_extensions_v1 set extension = to_number(extension_str);

delete from hwset_extensions_v1 
	where student_id not in (
		select student_id from cw_course_enrollment_v3 
		where course_id in (
			select course_id from hwsets_v5 where hw_id = hwset_extensions_v1.hw_id
		)
	);

create table hwset_rxn_condns_v1 (
	hw_id number(38) not null,
	rxn_cond_id number(38),
	constraint P_HW_RXNCONDS1 primary key (hw_id, rxn_cond_id),
	constraint F_HW_RXNCONDS1_ASSGT foreign key (hw_id) references hwsets_v5(hw_id),
	constraint F_HW_RXNCONDS1_RXNCOND foreign key (rxn_cond_id) 
			references reaction_conditions_v3(rxn_cond_id));

INSERT INTO hwset_rxn_condns_v1 
	SELECT hw_id, 
		TO_NUMBER(REGEXP_SUBSTR(rxn_cond_id, '[^:]+', 1, column_value)) AS rxn_cond_id 
	FROM hwsets_v5, 
	TABLE(
		CAST(
			MULTISET(
				SELECT LEVEL FROM dual 
				CONNECT BY LEVEL <= LENGTH(REGEXP_REPLACE(rxn_cond_id, '[^:]')) + 1
			) AS sys.OdciNumberList
		)
	) WHERE rxn_cond_id IS NOT null;

alter table hwsets_v5 drop (
	qlist, 
	dependencies, 
	question_points, 
	attempt_grading, 
	time_grading, 
	extensionees,
	rxn_cond_id);

-- 11/2011 RBG: move R groups from responses to new table

create table responses_v6 as 
	(select
	hw_id,
	student_id,
	pb_id,
	tries,
	grade,
	response,
	date_time,
	status,
	instructor_comment
	from responses_v5);
alter table responses_v6 
	add constraint P_RESPONSES6 primary key (STUDENT_ID, HW_ID, PB_ID, TRIES) 
	add constraint F_RESP6_ASSGT foreign key (HW_ID) references HWSETS_V5(HW_ID) 
	add constraint F_RESP6_STUDENT foreign key (STUDENT_ID) references CW_USERS_V3(USER_ID);
create index responses_v6_idx on responses_v6 (status);

create table response_rgroups_v1 (
	hw_id number(38) not null,
	student_id varchar2(50) not null,
	pb_id number(38) not null,
	r_group_num number(2) not null,
	r_group varchar2(10),
	constraint P_RESP_RGRPS1 primary key (hw_id, student_id, pb_id, r_group_num),
	constraint F_RESP_RGRPS1_STUDENT foreign key (student_id) references cw_users_v3(user_id),
	constraint F_RESP_RGRPS1_ASSGT foreign key (hw_id) references hwsets_v5(hw_id));

INSERT INTO response_rgroups_v1 
	SELECT hw_id, 
		student_id, 
		pb_id,
		column_value AS r_group_num, 
		REGEXP_SUBSTR(r_groups, '[^_]+', 1, column_value) AS r_group 
	FROM responses_v5, 
	TABLE(
		CAST(
			MULTISET(
				SELECT LEVEL FROM dual 
				CONNECT BY LEVEL <= LENGTH(REGEXP_REPLACE(r_groups, '[^_]'))
			) AS sys.OdciNumberList
		) 
	) WHERE hw_id IN (SELECT hw_id FROM hwsets_v5) 
	AND student_id IN (SELECT user_id FROM cw_users_v3)
	AND r_groups is not null;

drop table responses_v5;

-- 11/2011 RBG: separate comma-separated R groups into separate records 

create table r_group_class_v3 (
	id number(38),
	name varchar(100) not null,
	member_num number(38),
	member varchar2(10) not null,
	constraint P_R_GRP_CLASS3 primary key (id, member_num));

insert into r_group_class_v3 
	select r_group_class_id AS id,
		name,
		column_value AS member_num,
		REGEXP_SUBSTR(members, '[^,]+', 1, column_value) AS member 
	FROM r_group_class_v2, 
	TABLE(
		CAST(
			MULTISET(
				SELECT LEVEL FROM dual 
				CONNECT BY LEVEL <= LENGTH(REGEXP_REPLACE(members, '[^,]')) + 1
			) AS sys.OdciNumberList
		)
	);

-- change constraints to point to cw_users_v3

alter table COINSTRUCTORS_V2 drop constraint F_COINSTR_INSTRUCTOR; 
alter table COINSTRUCTORS_V2 
	add constraint F_COINSTR2_INSTRUCTOR foreign key (INSTRUCTOR_ID) 
	references CW_USERS_V3(USER_ID); 
alter table CW_COURSES_V3 drop constraint F_CRS_INSTRUCTOR; 
alter table CW_COURSES_V3 
	add constraint F_CRS3_INSTRUCTOR foreign key (INSTRUCTOR_ID) 
	references CW_USERS_V3(USER_ID); 
alter table CW_COURSE_ENROLLMENT_V3 drop constraint F_ENROLL_STUDENT; 
alter table CW_COURSE_ENROLLMENT_V3 
	add constraint F_ENROLL3_STUDENT foreign key (STUDENT_ID) 
	references CW_USERS_V3(USER_ID); 
alter table EXAM_STUDENTS_V2 drop constraint F_EXAM_USER; 
alter table EXAM_STUDENTS_V2 
	add constraint F_EXAM2_USER foreign key (USER_ID) 
	references CW_USERS_V3(USER_ID); 
alter table LANGUAGES_V1 drop constraint F_LANG_USER; 
alter table LANGUAGES_V1 
	add constraint F_LANG1_USER foreign key (USER_ID) 
	references CW_USERS_V3(USER_ID); 
alter table MODIFIED_HEADERS_V2 drop constraint F_MODHEADERS_USER; 
alter table MODIFIED_HEADERS_V2 
	add constraint F_MODHEADERS2_USER foreign key (USER_ID) 
	references CW_USERS_V3(USER_ID); 
alter table PBSETS_V3 drop constraint F_QSETS_USER; 
alter table PBSETS_V3 
	add constraint F_QSETS3_USER foreign key (USER_ID) 
	references CW_USERS_V3(USER_ID); 
alter table USER_EVALUATORS_V3 drop constraint F_USEREVALS_USER; 
alter table USER_EVALUATORS_V3 
	add constraint F_USEREVALS3_USER foreign key (USER_ID) 
	references CW_USERS_V3(USER_ID); 
alter table USER_FIGURES_V5 drop constraint F_USERFIGS_USER; 
alter table USER_FIGURES_V5 
	add constraint F_USERFIGS5_USER foreign key (USER_ID) 
	references CW_USERS_V3(USER_ID); 
alter table USER_QUESTIONS_V3 drop constraint F_USERQS_USER; 
alter table USER_QUESTIONS_V3 
	add constraint F_USERQS3_USER foreign key (USER_ID) 
	references CW_USERS_V3(USER_ID); 
alter table USER_QUESTION_DATA_V2 drop constraint F_USERQDATA_USER; 
alter table USER_QUESTION_DATA_V2 
	add constraint F_USERQDATA2_USER foreign key (USER_ID) 
	references CW_USERS_V3(USER_ID); 

-- 11/2011 RBG: set all user_id fields to same length 

alter table cw_users_v3 modify user_id varchar2(50);
alter table coinstructors_v2 modify instructor_id varchar2(50);
alter table cw_courses_v3 modify instructor_id varchar2(50);
alter table exam_students_v2 modify user_id varchar2(50);
alter table languages_v1 modify user_id varchar2(50);
alter table modified_headers_v2 modify user_id varchar2(50);
alter table pbsets_v3 modify user_id varchar2(50);
alter table user_questions_v3 modify user_id varchar2(50);
alter table user_figures_v5 modify user_id varchar2(50);
alter table user_question_data_v2 modify user_id varchar2(50);
alter table user_evaluators_v3 modify user_id varchar2(50);
alter table cw_course_enrollment_v3 modify student_id varchar2(50);
alter table assignment_questions_v3 modify student_id varchar2(50);
alter table hwset_extensions_v1 modify student_id varchar2(50);
alter table responses_v6 modify student_id varchar2(50);
alter table response_rgroups_v1 modify student_id varchar2(50);

-- 11/2011 RBG: separate IP addresses into separate table

create table allowed_ips_v1 (
	course_id number(38) not null,
	address varchar2(32) not null,
	constraint F_IPS1_COURSE foreign key (course_id) references cw_courses_v3(id));
create index allowed_ips_v1_idx on allowed_ips_v1 (course_id);

insert into allowed_ips_v1
	select id AS course_id,
		REGEXP_SUBSTR(allowed_ips, '[^,]+', 1, column_value) AS address
	FROM cw_courses_v3,
	TABLE(
		CAST(
			MULTISET(
				SELECT LEVEL FROM dual 
				CONNECT BY LEVEL <= LENGTH(REGEXP_REPLACE(allowed_ips, '[^,]')) + 1
			) AS sys.OdciNumberList
		)
	)
	WHERE allowed_ips is not null;
alter table cw_courses_v3 drop column allowed_ips;

-- 12/2011 RBG: Correct and make consistent format of dates

update hwsets_v5 set date_due = 
	to_char(to_date(date_due, 'MON DD, YYYY HH12:MI:SS AM'), 'YYYY/MM/DD HH24:MI:SS');
update hwsets_v5 set date_created = 
	to_char(to_date(date_created, 'MON DD, YYYY HH12:MI:SS AM'), 'YYYY/MM/DD HH24:MI:SS');
update responses_v6 set date_time = 
	to_char(to_date(date_time, 'YYYY/DD/MM HH24:MI:SS'), 'YYYY/MM/DD HH24:MI:SS') 
	where date_time >= '2009/13';

-- 12/2011 RBG: add modified grade field to responses so they don't have to be
-- calculated every time, and fill with Oracle-calculated grades

alter table responses_v6 add modified_grade number(8,4);
update responses_v6 set modified_grade = grade
	where hw_id not in (select hw_id from hwset_grading_params_v1);
update responses_v6 set modified_grade = (
	select s5.grade * s0.factor0 * s1.factor1 * s2.factor2 from (
		select hw_id, 
			limit_max, 
			factor as factor0
		from hwset_grading_params_v1 
		where param_type = 'Q' 
		union select hw_id, serial_no, 1 
		from assignment_questions_v3 
		where hw_id not in (
			select hw_id 
			from hwset_grading_params_v1 
			where param_type = 'Q')
	) s0, (
		select hw_id, 
			limit_min,
			limit_max, 
			factor as factor1 
		from hwset_grading_params_v1 
		where param_type = 'A' 
		union select hw_id, 0, 9999999999, 1 from hwsets_v5 
		where hw_id not in (
			select hw_id 
			from hwset_grading_params_v1 
			where param_type = 'A')
	) s1, (
		select hw_id, 
			limit_min, 
			limit_max, 
			factor as factor2 
		from hwset_grading_params_v1 
		where param_type = 'T' 
		union select hw_id, -9999999999, 0, 1 from hwsets_v5 
		where hw_id in (
			select hw_id 
			from hwset_grading_params_v1 
			where param_type = 'T'
		)
		union select hw_id, -9999999999, 9999999999, 1 from hwsets_v5 
		where hw_id not in (
			select hw_id 
			from hwset_grading_params_v1 
			where param_type = 'T'
		)
	) s2, (
		select hw_id, 
			to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as date_due,
			decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days 
		from hwsets_v5 
	) s3, (
		select hw_id, student_id, extension 
		from hwset_extensions_v1
		union select hw_id, student_id, 0 from responses_v6 
		where (hw_id, student_id) not in (
			select hw_id, student_id from hwset_extensions_v1
		)
	) s4, ( 
		select responses_v6.hw_id, 
			responses_v6.student_id, 
			responses_v6.pb_id, 
			serial_no, 
			tries, 
			grade, 
			to_date(date_time, 'YYYY/MM/DD HH24:MI:SS') as response_time
		from responses_v6 
		join assignment_questions_v3 
		on responses_v6.hw_id = assignment_questions_v3.hw_id 
		and responses_v6.student_id = assignment_questions_v3.student_id 
		and responses_v6.pb_id = assignment_questions_v3.pb_id
		where grade > 0
		and responses_v6.hw_id in (select hw_id from hwset_grading_params_v1)
	) s5
	where s5.hw_id = s0.hw_id 
	and s5.serial_no = s0.limit_max
	and s5.hw_id = s1.hw_id 
	and s5.tries > s1.limit_min
	and s5.tries <= s1.limit_max
	and s5.hw_id = s2.hw_id 
	and s5.hw_id = s3.hw_id
	and s5.hw_id = s4.hw_id
	and s5.student_id = s4.student_id
	and s5.response_time - s3.date_due - s4.extension * s3.in_days > s2.limit_min * s3.in_days
	and s5.response_time - s3.date_due - s4.extension * s3.in_days <= s2.limit_max * s3.in_days 
	and responses_v6.hw_id = s5.hw_id
	and responses_v6.student_id = s5.student_id
	and responses_v6.pb_id = s5.pb_id
	and responses_v6.tries = s5.tries
) 
where grade > 0
and hw_id in (select hw_id from hwset_grading_params_v1);

-- RBG 12/2011 create procedure for calculating modified grades for insertions
-- of single responses; question number determined from instantiated assignment

CREATE OR REPLACE FUNCTION grade_factor(
	hw_id_in responses_v6.hw_id%type, 
	student_id_in responses_v6.student_id%type, 
	pb_id_in responses_v6.pb_id%type,
	tries_in responses_v6.tries%type,
	date_time_in responses_v6.date_time%type) 
RETURN number IS factor number;
cursor c1 is 
select s0.factor0 * s1.factor1 * s2.factor2 
from (
	select factor as factor0
	from hwset_grading_params_v1
	where param_type = 'A' and hw_id = hw_id_in
	and limit_min < tries_in and tries_in <= limit_max
	union all select 1 from dual 
	where not exists (
		select factor
		from hwset_grading_params_v1
		where param_type = 'A' and hw_id = hw_id_in
		and limit_min < tries_in and tries_in <= limit_max
	)
) s0, (
	select limit_min, limit_max, factor as factor1
	from hwset_grading_params_v1 
	where param_type = 'T' and hw_id = hw_id_in
	union all select -9999999999, 0, 1 from dual 
	where hw_id_in in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
	union all select -9999999999, 9999999999, 1 from dual 
	where hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
) s1, (
	select limit_max, factor as factor2
	from hwset_grading_params_v1 
	where param_type = 'Q' and hw_id = hw_id_in
	union all select unique serial_no, 1 
	from assignment_questions_v3 
	where hw_id = hw_id_in and hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'Q'
	)
) s2, (
	select to_date(date_time_in, 'YYYY/MM/DD HH24:MI:SS')
		- to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as days_past_due, 
	decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days 
	from hwsets_v5 where hw_id = hw_id_in
) s3, (
	select student_id, extension 
	from hwset_extensions_v1 
	where hw_id = hw_id_in and student_id = student_id_in
	union all select student_id_in, 0 from dual 
	where student_id_in not in (
		select student_id from hwset_extensions_v1
		where hw_id = hw_id_in
	)
) s4, (
	select serial_no 
	from assignment_questions_v3 
	where hw_id = hw_id_in
	and student_id = student_id_in
	and pb_id = pb_id_in
) s5
where s5.serial_no = s2.limit_max
and s3.days_past_due - s4.extension * s3.in_days > s1.limit_min * s3.in_days
and s3.days_past_due - s4.extension * s3.in_days <= s1.limit_max * s3.in_days;
BEGIN
	open c1;
	fetch c1 into factor;
	close c1;
	return factor;
END;
/

-- RBG 1/2012 rename constraints to include version numbers where not already
-- done

alter table coinstructors_v2 
	rename constraint f_coinstr_course to f_coinstr2_course;
alter table cw_course_enrollment_v3 
	rename constraint f_enroll_course to f_enroll3_course;
alter table cw_course_preenrollment_v3 
	rename constraint f_preenroll_course to f_preenroll3_course;
alter table evaluators_v3 rename constraint f_evals_q to f_evals3_q;
alter table exam_students_v2 
	rename constraint f_exam_course to f_exam2_course;
alter table figures_v5 rename constraint f_fig_q to f_fig5_q;
alter table modified_headers_v2 
	rename constraint f_modheaders_set to f_modheaders2_set;
alter table pbsets_v3 rename constraint f_qsets_chap to f_qsets3_chap;
alter table questions_v3 rename constraint f_qs_set to f_qs3_set;
alter table user_questions_v3 rename constraint f_userqs_set to f_userqs3_set;

alter table chapters_v1 rename constraint p_chaps to p_chaps1;
alter table coinstructors_v2 rename constraint p_coinstructors to p_coinstructors2;
alter table cw_courses_v3 rename constraint p_courses to p_courses3;
alter table cw_course_enrollment_v3 rename constraint p_enroll to p_enroll3;
alter table cw_course_preenrollment_v3 
	rename constraint p_course_preenroll to p_course_preenroll3;
alter table cw_users_v3 rename constraint p_users_v3 to p_users3;
alter table evaluators_v3 rename constraint p_evals to p_evals3;
alter table exam_students_v2 rename constraint p_exam_user to p_exam_user2;
alter table figures_v5 rename constraint p_figs to p_figs5;
alter table functional_groups_v2 rename constraint p_fnalgrps to p_fnalgrps2;
alter table impossible_sms_v2 rename constraint p_impossiblesms to p_impossiblesms2; 
alter table languages_v1 rename constraint p_language to p_language1;
alter table modified_headers_v2 rename constraint p_modheaders to p_modheaders2;
alter table pbsets_v3 rename constraint p_qsets to p_qsets3;
alter table question_data_v2 rename constraint p_qdata to p_qdata2;
alter table questions_v3 rename constraint p_qs to p_qs3;
alter table unit_conversions_v1 rename constraint p_units to p_units1;
alter table user_evaluators_v3 rename constraint p_userevals to p_userevals3;
alter table user_figures_v5 rename constraint p_userfigs to p_userfigs5;
alter table user_question_data_v2 rename constraint p_userqdata to p_userqdata2;
alter table user_questions_v3 rename constraint p_userqs to p_userqs3;

-- RBG 1/2012 add "most recent response" field to responses table

alter table responses_v6 add most_recent char(1);
update responses_v6 set most_recent = 'N';
update responses_v6 set most_recent = 'Y' where (student_id, hw_id, pb_id, tries) in (
	select student_id, hw_id, pb_id, tries from (
		select student_id, hw_id, pb_id, tries, row_number() over (
			partition by student_id, hw_id, pb_id order by tries desc
		) as rn from responses_v6
	) where rn = 1
);
drop index responses_v6_idx;
create index responses_v6_idx on responses_v6 (status, most_recent);

-- RBG 1/2012 create forums
-- we don't make foreign keys to user_id in two of the tables because we want to
-- preserve forum entries even if user is deleted

create table forum_topics_v1 (
	topic_id number(38) not null,
	course_id number(38) not null,
	creator_id varchar2(50) not null,
	date_created varchar2(25),
	title CLOB,
	sticky char(1),
	hw_id number(38),
	pb_id number(38),
	constraint P_FORUMTOPIC1 primary key (topic_id),
	constraint F_FORUMTOPIC1_COURSE foreign key (course_id) references cw_courses_v3(id)
	);
create index forum_topics_v1_idx on forum_topics_v1 (course_id, creator_id, date_created);
create sequence FORUM_TOPIC_SEQ start with 1;

create table forum_posts_v1 (
	post_id number(38) not null,
	topic_id number(38) not null,
	user_id varchar2(50) not null,
	date_created varchar2(25),
	date_edited varchar2(25),
	text CLOB,
	figure CLOB,
	figure_type char(3),
	constraint P_FORUMPOST1 primary key (post_id),
	constraint F_FORUMPOST1_TOPIC foreign key (topic_id) references forum_topics_v1(topic_id) 
	);
create index forum_posts_v1_idx on forum_posts_v1 (topic_id, user_id, date_created, date_edited);
create sequence FORUM_POST_SEQ start with 1;

create table blocked_from_forums_v1 (
	course_id number(38) not null,
	user_id varchar2(50) not null,
	constraint P_BLOCKED1 primary key (course_id, user_id),
	constraint F_BLOCKED1_COURSE foreign key (course_id) references cw_courses_v3(id),
	constraint F_BLOCKED1_USER foreign key (user_id) references USERS_V4(USER_ID) 
	);

alter table image_table1 add constraint P_IMGS1 primary key (pic_id);
alter table user_image_table1 add constraint P_USERIMGS1 primary key (pic_id);

-- remove orphaned images; number of rows removed should be very small

delete from image_table1 where pic_id not in 
	(select fig_id from figures_v5 where fig_type = 'IMG');
delete from user_image_table1 where pic_id not in 
	(select fig_id from user_figures_v5 where fig_type = 'IMG');

-- ACE 3.3 ends here
-- ACE 3.4 begins here

-- RBG 1/2012 simplify synthesis combination expressions 
-- and switch to 1-based serial numbers

select question_id from question_data_v2 where data_type = 'SMExpr' 
	and length(regexp_replace(regexp_replace(data, '\d'), ':')) = 0;

select question_id from user_question_data_v2 where data_type = 'SMExpr' 
	and length(regexp_replace(regexp_replace(data, '\d'), ':')) = 0;

-- [*** If either of the above queries gives results, then before you proceed,
-- have an author go to each returned question in the ACE 3.3 or prior authoring 
-- tool, edit the expression on how to combine the permissible starting material
-- conditions, and save the question.  After the author does this, the queries 
-- above should return no records. ***]

create table question_data_v3 as select * from question_data_v2;
alter table QUESTION_DATA_V3 
	add constraint P_QDATA3 primary key (DATA_ID)
	add constraint F_QDATA3_Q foreign key (QUESTION_ID) references QUESTIONS_V3(ID);
create index question_data_v3_idx on question_data_v3 (question_id, text_id);

create table user_question_data_v3 as select * from user_question_data_v2;
alter table USER_QUESTION_DATA_V3 
	add constraint P_USERQDATA3 primary key (DATA_ID)
	add constraint F_USERQDATA3_USER foreign key (user_id) references cw_users_v3(user_id);
create index user_question_data_v3_idx on user_question_data_v3 (question_id);

-- [*** run "Convert synthesis starting material combination expressions" in
-- admin tool ***]

-- RBG 1/2012 move menu-only reagents into database

-- [*** get menu_only_reagents_v1 table from UK; we ran,
-- 	create table menu_only_reagents_v1 (
--		definition varchar2(80),
--		name varchar2(80) not null,
--		constraint P_MENURGTS1 primary key (definition));
-- and then entered the contents manually through the authoring tool ***]

-- RBG 1-2/2012 create separate table for institutions, use institution ID numbers
-- in users and preenrollment tables, convert more users fields to CLOB

create table institutions_v1 (
	id number(38) not null,
	name CLOB,
	uniq_id_label CLOB,
	constraint P_INSTNS1 primary key (id));
insert into institutions_v1 (select rownum, name, 'student ID number' from (
	select unique dbms_lob.substr(institution, length(institution), 1) as name from cw_users_v3));
select max(id) + 1 from institutions_v1;
create sequence INSTITUTIONS_SEQ start with (*** result of previous query***);
update institutions_v1 set uniq_id_label = 'UK ID' 
	where dbms_lob.substr(name, length(name), 1) = 'University of Kentucky';
alter table cw_courses_v3 drop column uniq_id_label;

create table users_v4 (
	user_id varchar2(40) not null,
	password_hash raw(150) not null,
	role char(1) not null,
	first_name CLOB,
	last_name CLOB,
	middle_name CLOB,
	student_num CLOB,
	email CLOB,
	institution CLOB,
	contact_address CLOB,
	phone varchar2(25),
	registration_date varchar2(25),
	flags number(3),
	security_questions varchar2(10),
	security_answer1 CLOB,
	security_answer2 CLOB,
	sortkey varchar2(400),
	constraint P_USERS4 primary key (user_id));
insert into users_v4 (select
	user_id,
	password_hash,
	role,
	first_name,
	last_name,
	middle_name,
	student_num,
	email,
	institution,
	contact_address,
	phone,
	registration_date,
	flags,
	security_questions,
	security_answer1,
	security_answer2,
	sortkey
	from cw_users_v3);

alter table users_v4 add institution_id number(38);
update users_v4 set institution_id = (
	select id from institutions_v1 where 
	dbms_lob.substr(users_v4.institution, length(users_v4.institution), 1) =
	dbms_lob.substr(name, length(name), 1));
create index users_v4_idx on users_v4 (role, institution_id);
alter table users_v4 
	add constraint F_USERS4_INSTN_ID foreign key (institution_id) 
	references institutions_v1(id); 
alter table users_v4 drop column institution;

create table preenrollment_v4 (
	course_id number(38) not null,
	student_num CLOB,
	student_name CLOB,
	institution CLOB,
	constraint F_PREENROLL4_COURSE foreign key (course_id) references cw_courses_v3(id));
insert into preenrollment_v4 
	(select 
	course_id,
	student_num,
	student_name,
	institution
	from cw_course_preenrollment_v3);

alter table preenrollment_v4 add institution_id number(38);
update preenrollment_v4 set institution_id = (
	select id from institutions_v1 where 
	dbms_lob.substr(preenrollment_v4.institution, length(preenrollment_v4.institution), 1) =
	dbms_lob.substr(name, length(name), 1));
create index preenrollment_v4_idx on preenrollment_v4 (course_id, institution_id);
alter table preenrollment_v4 
	add constraint F_PREENRL4_INSTN_ID foreign key (institution_id) 
	references institutions_v1(id); 
alter table preenrollment_v4 drop column institution;

-- change constraints to point to users_v4

alter table COINSTRUCTORS_V2 drop constraint F_COINSTR2_INSTRUCTOR; 
alter table COINSTRUCTORS_V2 
	add constraint F_COINSTR2_INSTRUCTOR foreign key (INSTRUCTOR_ID) 
	references USERS_V4(USER_ID); 
alter table CW_COURSES_V3 drop constraint F_CRS3_INSTRUCTOR; 
alter table CW_COURSES_V3 
	add constraint F_CRS3_INSTRUCTOR foreign key (INSTRUCTOR_ID) 
	references USERS_V4(USER_ID); 
alter table CW_COURSE_ENROLLMENT_V3 drop constraint F_ENROLL3_STUDENT; 
alter table CW_COURSE_ENROLLMENT_V3 
	add constraint F_ENROLL3_STUDENT foreign key (STUDENT_ID) 
	references USERS_V4(USER_ID); 
alter table EXAM_STUDENTS_V2 drop constraint F_EXAM2_USER; 
alter table EXAM_STUDENTS_V2 
	add constraint F_EXAM2_USER foreign key (USER_ID) 
	references USERS_V4(USER_ID); 
alter table LANGUAGES_V1 drop constraint F_LANG1_USER; 
alter table LANGUAGES_V1 
	add constraint F_LANG1_USER foreign key (USER_ID) 
	references USERS_V4(USER_ID); 
alter table MODIFIED_HEADERS_V2 drop constraint F_MODHEADERS2_USER; 
alter table MODIFIED_HEADERS_V2 
	add constraint F_MODHEADERS2_USER foreign key (USER_ID) 
	references USERS_V4(USER_ID); 
alter table PBSETS_V3 drop constraint F_QSETS3_USER; 
alter table PBSETS_V3 
	add constraint F_QSETS3_USER foreign key (USER_ID) 
	references USERS_V4(USER_ID); 
alter table USER_EVALUATORS_V3 drop constraint F_USEREVALS3_USER; 
alter table USER_EVALUATORS_V3 
	add constraint F_USEREVALS3_USER foreign key (USER_ID) 
	references USERS_V4(USER_ID); 
alter table USER_FIGURES_V5 drop constraint F_USERFIGS5_USER; 
alter table USER_FIGURES_V5 
	add constraint F_USERFIGS5_USER foreign key (USER_ID) 
	references USERS_V4(USER_ID); 
alter table USER_QUESTIONS_V3 drop constraint F_USERQS3_USER; 
alter table USER_QUESTIONS_V3 
	add constraint F_USERQS3_USER foreign key (USER_ID) 
	references USERS_V4(USER_ID); 
alter table USER_QUESTION_DATA_V3 drop constraint F_USERQDATA3_USER; 
alter table USER_QUESTION_DATA_V3 
	add constraint F_USERQDATA3_USER foreign key (USER_ID) 
	references USERS_V4(USER_ID); 
alter table HWSET_EXTENSIONS_V1 drop constraint F_HW_EXTS1_STUDENT; 
alter table HWSET_EXTENSIONS_V1 
	add constraint F_HW_EXTS1_STUDENT foreign key (STUDENT_ID) 
	references USERS_V4(USER_ID); 
alter table RESPONSES_V6 drop constraint F_RESP6_STUDENT; 
alter table RESPONSES_V6 
	add constraint F_RESP6_STUDENT foreign key (STUDENT_ID) 
	references USERS_V4(USER_ID); 
alter table RESPONSE_RGROUPS_V1 drop constraint F_RESP_RGRPS1_STUDENT; 
alter table RESPONSE_RGROUPS_V1 
	add constraint F_RESP_RGRPS1_STUDENT foreign key (STUDENT_ID) 
	references USERS_V4(USER_ID); 

drop table cw_users_v3;
drop table cw_course_preenrollment_v3;

-- RBG 1/2012 make another function for calculating modified grade where we 
-- already know the question number

CREATE OR REPLACE FUNCTION grade_factor_with_q_num_v1(
	hw_id_in responses_v6.hw_id%type,
	student_id_in responses_v6.student_id%type,
	pb_id_in responses_v6.pb_id%type,
	tries_in responses_v6.tries%type,
	date_time_in responses_v6.date_time%type,
	q_num_in responses_v6.pb_id%type)
RETURN number IS factor number;
cursor c1 is
select s0.factor0 * s1.factor1 * s2.factor2
from (
	select factor as factor0
	from hwset_grading_params_v1
	where param_type = 'A' and hw_id = hw_id_in
	and limit_min < tries_in and tries_in <= limit_max
	union all select 1 from dual where not exists (
		select factor
		from hwset_grading_params_v1
		where param_type = 'A' and hw_id = hw_id_in
		and limit_min < tries_in and tries_in <= limit_max
	)
) s0, (
	select limit_min, limit_max, factor as factor1
	from hwset_grading_params_v1
	where param_type = 'T' and hw_id = hw_id_in
	union all select -9999999999, 0, 1 from dual
	where hw_id_in in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
	union all select -9999999999, 9999999999, 1 from dual
	where hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
) s1, (
	select factor as factor2
	from hwset_grading_params_v1
	where param_type = 'Q' and hw_id = hw_id_in 
	and limit_max = q_num_in
	union all select 1 from dual where not exists (
		select factor
		from hwset_grading_params_v1
		where param_type = 'Q' and hw_id = hw_id_in 
		and limit_max = q_num_in
	)
) s2, (
	select to_date(date_time_in, 'YYYY/MM/DD HH24:MI:SS')
		- to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as days_past_due,
	decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days
	from hwsets_v5 where hw_id = hw_id_in
) s3, (
	select student_id, extension
	from hwset_extensions_v1
	where hw_id = hw_id_in and student_id = student_id_in
	union all select student_id_in, 0 from dual
	where student_id_in not in (
		select student_id from hwset_extensions_v1
		where hw_id = hw_id_in
	)
) s4
where s3.days_past_due - s4.extension * s3.in_days > s1.limit_min * s3.in_days
and s3.days_past_due - s4.extension * s3.in_days <= s1.limit_max * s3.in_days;
BEGIN
	open c1;
	fetch c1 into factor;
	close c1;
	return factor;
END;
/

-- RBG 2/2012 rename assigned questions table

create table assigned_questions_v4 as select * from assignment_questions_v3;
alter table assigned_questions_v4 
	add constraint P_ASSGD_QS4 primary key (student_id, hw_id, serial_no)
	add constraint F_ASSGD_QS4_USER foreign key (student_id) references users_v4(user_id)
	add constraint F_ASSGD_QS4_ASSGT foreign key (hw_id) references hwsets_v5(hw_id);
create index assigned_questions_v4_idx on assigned_questions_v4 (pb_id);
drop table assignment_questions_v3;

-- correct grade_factor function for new name of assigned questions table

CREATE OR REPLACE FUNCTION grade_factor_v2(
	hw_id_in responses_v6.hw_id%type, 
	student_id_in responses_v6.student_id%type, 
	pb_id_in responses_v6.pb_id%type,
	tries_in responses_v6.tries%type,
	date_time_in responses_v6.date_time%type) 
RETURN number IS factor number;
cursor c1 is 
select s0.factor0 * s1.factor1 * s2.factor2 
from (
	select factor as factor0
	from hwset_grading_params_v1
	where param_type = 'A' and hw_id = hw_id_in
	and limit_min < tries_in and tries_in <= limit_max
	union all select 1 from dual 
	where not exists (
		select factor
		from hwset_grading_params_v1
		where param_type = 'A' and hw_id = hw_id_in
		and limit_min < tries_in and tries_in <= limit_max
	)
) s0, (
	select limit_min, limit_max, factor as factor1
	from hwset_grading_params_v1 
	where param_type = 'T' and hw_id = hw_id_in
	union all select -9999999999, 0, 1 from dual 
	where hw_id_in in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
	union all select -9999999999, 9999999999, 1 from dual 
	where hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
) s1, (
	select limit_max, factor as factor2
	from hwset_grading_params_v1 
	where param_type = 'Q' and hw_id = hw_id_in
	union all select unique serial_no, 1 
	from assigned_questions_v4 
	where hw_id = hw_id_in and hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'Q'
	)
) s2, (
	select to_date(date_time_in, 'YYYY/MM/DD HH24:MI:SS')
			- to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as days_past_due, 
		decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days 
	from hwsets_v5 where hw_id = hw_id_in
) s3, (
	select student_id, extension 
	from hwset_extensions_v1 
	where hw_id = hw_id_in and student_id = student_id_in
	union all select student_id_in, 0 from dual 
	where student_id_in not in (
		select student_id from hwset_extensions_v1
		where hw_id = hw_id_in
	)
) s4, (
	select serial_no from assigned_questions_v4 
	where hw_id = hw_id_in and student_id = student_id_in
	and pb_id = pb_id_in
) s5
where s5.serial_no = s2.limit_max
and s3.days_past_due - s4.extension * s3.in_days > s1.limit_min * s3.in_days
and s3.days_past_due - s4.extension * s3.in_days <= s1.limit_max * s3.in_days;
BEGIN
	open c1;
	fetch c1 into factor;
	close c1;
	return factor;
END;
/

-- RBG 2/2012 make function for calculating modified grade of instructor's
-- response 

create or replace function instructor_grade_factor_v1(
	hw_id_in responses_v6.hw_id%type,
	q_num_in responses_v6.pb_id%type,
	tries_in responses_v6.tries%type,
	date_time_in responses_v6.date_time%type)
RETURN number IS factor number;
cursor c1 is
select s0.factor0 * s1.factor1 * s2.factor2 
from (
	select factor as factor0
	from hwset_grading_params_v1
	where param_type = 'A' and hw_id = hw_id_in
	and limit_min < tries_in and limit_max >= tries_in
	union all select 1 from dual 
	where not exists (
		select factor from hwset_grading_params_v1
		where param_type = 'A' and hw_id = hw_id_in
		and limit_min < tries_in and limit_max >= tries_in
	)
) s0, (
	select limit_min, limit_max, factor as factor1
	from hwset_grading_params_v1 
	where param_type = 'T' and hw_id = hw_id_in
	union all select -9999999999, 0, 1 from dual 
	where hw_id_in in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
	union all select -9999999999, 9999999999, 1 from dual 
	where hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
) s1, (
	select factor as factor2
	from hwset_grading_params_v1 
	where param_type = 'Q' and hw_id = hw_id_in and limit_max = q_num_in
	union all select 1 from dual 
	where not exists (
		select factor from hwset_grading_params_v1 
		where param_type = 'Q' and hw_id = hw_id_in and limit_max = q_num_in
	)
) s2, (
	select to_date(date_time_in, 'YYYY/MM/DD HH24:MI:SS') 
			- to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as days_past_due, 
		decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days 
	from hwsets_v5 where hw_id = hw_id_in
) s3
where s3.days_past_due > s1.limit_min * s3.in_days
and s3.days_past_due <= s1.limit_max * s3.in_days; 
BEGIN
	open c1;
	fetch c1 into factor;
	close c1;
	return factor;
END;
/

-- RBG 2/2012 make procedure for recalculating student's grades after change of
-- extension

create or replace procedure recalc_1_student_grades_v1(
	hw_id_in responses_v6.hw_id%type,
	student_id_in responses_v6.student_id%type)
is begin
update responses_v6 set modified_grade =
(select srch_result5.grade
	* srch_result0.factor0
	* srch_result1.factor1
	* srch_result2.factor2
from (
	select limit_min, limit_max, factor as factor0
	from hwset_grading_params_v1
	where param_type = 'A' and hw_id = hw_id_in
	union all select 0, 9999999999, 1 from dual
	where hw_id_in not in (
	select hw_id from hwset_grading_params_v1 where param_type = 'A'
	)
) srch_result0, (
	select limit_min, limit_max, factor as factor1
	from hwset_grading_params_v1
	where param_type = 'T' and hw_id = hw_id_in
	union all select -9999999999, 0, 1 from dual
	where hw_id_in in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
	union all select -9999999999, 9999999999, 1 from dual
	where hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
) srch_result1, (
	select limit_max as serial_no, factor as factor2
	from hwset_grading_params_v1
	where param_type = 'Q' and hw_id = hw_id_in
	union all select unique serial_no, 1
	from assigned_questions_v4
	where hw_id = hw_id_in and hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'Q'
	)
) srch_result2, (
	select to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as date_due,
		decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days
	from hwsets_v5 where hw_id = hw_id_in
) srch_result3, (
	select extension from hwset_extensions_v1
	where hw_id = hw_id_in and student_id = student_id_in
	union all select 0 from dual
	where not exists (
		select extension from hwset_extensions_v1
		where hw_id = hw_id_in and student_id = student_id_in
	)
) srch_result4, (
	select responses_v6.pb_id,
		serial_no,
		tries,
		grade,
		to_date(date_time, 'YYYY/MM/DD HH24:MI:SS') as date_time
	from responses_v6
	join assigned_questions_v4
	on responses_v6.hw_id = assigned_questions_v4.hw_id
	and responses_v6.student_id = assigned_questions_v4.student_id
	and responses_v6.pb_id = assigned_questions_v4.pb_id
	where grade != 0
	and responses_v6.hw_id = hw_id_in
	and responses_v6.student_id = student_id_in
) srch_result5
where srch_result5.tries > srch_result0.limit_min
and srch_result5.tries <= srch_result0.limit_max
and srch_result5.date_time
	- srch_result3.date_due
	- srch_result4.extension * srch_result3.in_days
	> srch_result1.limit_min * srch_result3.in_days
and srch_result5.date_time
	- srch_result3.date_due
	- srch_result4.extension * srch_result3.in_days
	<= srch_result1.limit_max * srch_result3.in_days
and srch_result5.serial_no = srch_result2.serial_no
and responses_v6.pb_id = srch_result5.pb_id
and responses_v6.tries = srch_result5.tries
)
where hw_id = hw_id_in
and grade != 0
and responses_v6.student_id = student_id_in;
end;
/

-- RBG 2/2012 make procedure for recalculating students' grades after change of
-- assignment grading parameters

create or replace procedure recalculate_modified_grades_v1(
	hw_id_in responses_v6.hw_id%type)
is begin
update responses_v6 set modified_grade =
(select srch_result5.grade
	* srch_result0.factor0
	* srch_result1.factor1
	* srch_result2.factor2
from (
	select limit_min, limit_max, factor as factor0
	from hwset_grading_params_v1
	where param_type = 'A' and hw_id = hw_id_in
	union all select 0, 9999999999, 1 from dual
	where hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'A'
	)
) srch_result0, (
	select limit_min, limit_max, factor as factor1
	from hwset_grading_params_v1
	where param_type = 'T' and hw_id = hw_id_in
	union all select -9999999999, 0, 1 from dual
	where hw_id_in in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
	union all select -9999999999, 9999999999, 1 from dual
	where hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	)
) srch_result1, (
	select limit_max as serial_no, factor as factor2
	from hwset_grading_params_v1
	where param_type = 'Q' and hw_id = hw_id_in
	union all select unique serial_no, 1
	from assigned_questions_v4
	where hw_id = hw_id_in and hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'Q'
	)
) srch_result2, (
	select to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as date_due,
		decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days
	from hwsets_v5 where hw_id = hw_id_in
) srch_result3, (
	select student_id, extension
	from hwset_extensions_v1 where hw_id = hw_id_in
	union all select unique student_id, 0 from responses_v6
	where hw_id = hw_id_in and student_id not in (
		select student_id from hwset_extensions_v1 where hw_id = hw_id_in
	)
) srch_result4, (
	select responses_v6.student_id,
		responses_v6.pb_id,
		serial_no,
		tries,
		grade,
		to_date(date_time, 'YYYY/MM/DD HH24:MI:SS') as date_time
	from responses_v6
	join assigned_questions_v4
	on responses_v6.hw_id = assigned_questions_v4.hw_id
	and responses_v6.student_id = assigned_questions_v4.student_id
	and responses_v6.pb_id = assigned_questions_v4.pb_id
	where grade != 0
	and responses_v6.hw_id = hw_id_in
) srch_result5
where srch_result5.tries > srch_result0.limit_min
and srch_result5.tries <= srch_result0.limit_max
and srch_result5.date_time
	- srch_result3.date_due
	- srch_result4.extension * srch_result3.in_days
	> srch_result1.limit_min * srch_result3.in_days
and srch_result5.date_time
	- srch_result3.date_due
	- srch_result4.extension * srch_result3.in_days
	<= srch_result1.limit_max * srch_result3.in_days
and srch_result5.serial_no = srch_result2.serial_no
and srch_result5.student_id = srch_result4.student_id
and responses_v6.student_id = srch_result5.student_id
and responses_v6.pb_id = srch_result5.pb_id
and responses_v6.tries = srch_result5.tries
)
where hw_id = hw_id_in
and grade != 0;
end;
/

-- RBG 2/2012 convert Newman projection Qs to 3D Qs

update questions_v3 set probtype = probtype + power(2, 8) - power(2, 26) 
	where id in (select id from questions_v3 where decode(bitand(probtype, power(2, 26)), 0, 0, 1) = 1) 
	and id not in (select id from questions_v3 where decode(bitand(probtype, power(2, 8)), 0, 0, 1) = 1);

-- RBG 2/2012 add textbooks

create table textbooks_v1 (
	id number(38) not null,
	name CLOB,
	user_id varchar2(50),
	flags number(38),
	lock_holder varchar2(50),
	constraint p_texts1 primary key (id, user_id),
	constraint f_texts1_user foreign key (user_id) references users_v4(user_id),
	constraint f_texts1_lock foreign key (lock_holder) references users_v4(user_id));
create index textbooks_v1_idx on textbooks_v1 (user_id, flags);
create sequence textbooks_seq start with 1; 
create table text_chaps_v1 (
	id number(38) not null,
	text_id number(38) not null,
	serial_num number(38) not null,
	name CLOB,
	constraint p_textchaps1 primary key (id),
	constraint f_textchaps1_text foreign key (text_id) references textbooks_v1(id));
create index text_chaps_v1_idx on text_chaps_v1 (text_id);
create sequence text_chaps_seq start with 1; 
create table text_content_v1 (
	id number(38) not null,
	chap_id number(38) not null,
	serial_num number(38) not null,
	data_type varchar2(3) not null,
	data CLOB,
	caption CLOB,
	extra_data CLOB,
	constraint p_textcontent1 primary key (id),
	constraint f_textcontent1_chap foreign key (chap_id) references text_chaps_v1(id));
create index text_content_v1_idx on text_content_v1 (chap_id);
create sequence text_content_seq start with 1; 
create table text_coauthors_v1 (
	book_id number(38) not null,
	user_id varchar2(50) not null,
	constraint p_coauths1 primary key (book_id, user_id),
	constraint f_coauths1_book foreign key (book_id) references textbooks_v1(id),
	constraint f_coauths1_user foreign key (user_id) references users_v4(user_id));

alter table cw_courses_v3 add ace_text_id number(38);
alter table cw_courses_v3 add constraint f_crs3_text 
	foreign key (ace_text_id) references textbooks_v1(id);

-- RBG 4/2012 set null values for translation phraseIds to 0

update questions_v3 set statement_id = 0 where statement_id is null;
update pbsets_v3 set header_id = 0 where header_id is null;
update evaluators_v3 set feedback_id = 0 where feedback_id is null;
update question_data_v3 set text_id = 0 where text_id is null;

-- RBG 4/2012 add parameter to formula evaluator 

update evaluators_v3 set coded_data = coded_data || '/Y' 
	where match_type = 'FORMULA';
update user_evaluators_v3 set coded_data = coded_data || '/Y' 
	where match_type = 'FORMULA';
update question_data_v3 set data = data || '/Y' 
	where data_type = 'synthOkSM' and data like 'FORM%';
update user_question_data_v3 set data = data || '/Y' w
	here data_type = 'synthOkSM' and data like 'FORM%';

-- RBG 7/2012 add enabling date for courses

alter table cw_courses_v3 add enable_date varchar2(25);
update cw_courses_v3 
	set enable_date = to_char(sysdate, 'YYYY/MM/DD HH24:MI:SS')
	where bitand(flags, 1) = 1; 

-- RBG 8/2012 add parameter for SYNTH_EQUALS coded data 

update evaluators_v3 set coded_data = coded_data || '/Y' 
	where match_type = 'SYN_EQ' and length(coded_data) = 1;
update user_evaluators_v3 set coded_data = coded_data || '/Y' 
	where match_type = 'SYN_EQ' and length(coded_data) = 1;

-- ACE 3.4 ends here
-- ACE 3.5 starts here

-- [*** create new directories figures and user_figures in ace/web/,
-- set the owner of the directories to the owner of the web application,
-- and press "Write images to disk" in admin DB update tool ***/

create table images_v2 as (
	select pic_id, 
		id || '_' || pic_id || '.' || extension as file_name 
	from questions_v3 join figures_v5 on id = pb_id 
	join image_table1 on fig_id = pic_id 
	where fig_type = 'IMG' 
	union select pic_id, 
		'forumPost' || post_id || '_' || pic_id || '.' || extension 
	from forum_posts_v1 join image_table1 on to_number(figure) = pic_id 
	where figure_type = 'IMG'
);

create table user_images_v2 as (
	select pic_id, 
		id || '_' || pic_id || '.' || extension as file_name
	from user_questions_v3 join user_figures_v5 on id = pb_id 
		and user_questions_v3.user_id = user_figures_v5.user_id 
	join user_image_table1 on fig_id = pic_id 
	where fig_type = 'IMG' 
	union select pic_id, 
		'textContent_' || pic_id || '.' || extension 
	from text_content_v1 join user_image_table1 on to_number(data) = pic_id 
	where data_type = 'IMG'
);

alter table images_v2 add constraint P_IMGS2 primary key (pic_id);
alter table user_images_v2 add constraint P_USERIMGS2 primary key (pic_id);

alter table images_v2 add file_name2 CLOB;
update images_v2 set file_name2 = file_name;
alter table images_v2 drop column file_name;
alter table images_v2 rename column file_name2 to file_name;

alter table user_images_v2 add file_name2 CLOB;
update user_images_v2 set file_name2 = file_name;
alter table user_images_v2 drop column file_name;
alter table user_images_v2 rename column file_name2 to file_name;

-- update click-here coded data

update evaluators_v3 set coded_data = '6' 
	where match_type = 'CLICK_HERE' and coded_data like 'Y';
update evaluators_v3 set coded_data = '1' 
	where match_type = 'CLICK_HERE' and coded_data like 'N';
update user_evaluators_v3 set coded_data = '6' 
	where match_type = 'CLICK_HERE' and coded_data like 'Y';
update user_evaluators_v3 set coded_data = '1' 
	where match_type = 'CLICK_HERE' and coded_data like 'N';

-- convert click-here response to XML

update responses_v6 
	set response = regexp_replace(
		regexp_replace(
			regexp_replace(response, '^', '<xml><mark x="'), 
			':', '" y="'), 
		'$', '" /></xml>') 
	where pb_id in (
		select id from questions_v3 
		where bitand(probtype, 31) = 15
	) and regexp_like(response, '^\d*:\d*$');


update responses_v6 
	set response = regexp_replace(
		regexp_replace(
			regexp_replace(response, '^', '<xml><mark x="'), 
			':', '" y="'), 
		'$', '" /></xml>') 
	where pb_id in (
		select id from user_questions_v3 
		where bitand(probtype, 31) = 15
	) and regexp_like(response, '^\d*:\d*$');

-- *** run "Convert clickable image coordinates to XML" in admin tool ***

-- add IP address field to database

alter table responses_v6 add ip_address varchar2(25);

-- RBG 10/2012 first entry into course to enrollment table

alter table cw_course_enrollment_v3 add first_entry varchar2(25); 

-- RBG 11/2012 unit canonicalization for interconversions

create table canonicalized_units_v1 (
	unit_symbol varchar2(20) not null,
	unit_name varchar2(50) not null,
	what_measures varchar2(50),
	factor_coefficient number not null,
	factor_power10 number(3),
	meter_power number(3),
	kilogram_power number(3),
	second_power number(3),
	ampere_power number(3),
	kelvin_power number(3),
	mole_power number(3),
	candela_power number(3),
	constraint P_CANON_UNIT1 primary key(unit_symbol, unit_name)
);

-- RBG 1/2013 serial numbers for courses
-- "928" represents tutorial course ID number; yours may be different: 
-- value is stored in web/WEB-INF/epoch.properties

alter table cw_courses_v3 add serial_no number(38);
alter table coinstructors_v2 add serial_no number(38);
alter table cw_course_enrollment_v3 add serial_no number(38);
update cw_courses_v3 set serial_no = 1 where id = 928;
update cw_course_enrollment_v3 set serial_no = 1 where course_id = 928;
update coinstructors_v2 set serial_no = 1 where course_id = 928;

-- RBG 2/2013 bigger field for shortcut group names 

alter table r_group_class_v3 modify member varchar2(15);

-- ACE 3.5 ends here
-- ACE 3.6 starts here

-- RBG 3/2013 separate probtype into question type and flags fields

alter table questions_v3 add (
	q_type_num number(38), 
	q_type_name varchar2(15),
	q_flags number(38)
);
alter table user_questions_v3 add (
	q_type_num number(38), 
	q_type_name varchar2(15),
	q_flags number(38)
);
create table q_types (
	name varchar2(15), 
	num number(38), 
	constraint P_QTYPES primary key (num)
);
insert into q_types (name, num)
	select 'OTHER', 0 from dual union all
	select 'SKELETAL', 1 from dual union all
	select 'LEWIS', 2 from dual union all
	select 'MECHANISM', 3 from dual union all
	select 'MULT_CHOICE', 4 from dual union all
	select 'ORDERING', 5 from dual union all
	select 'SYNTHESIS', 6 from dual union all
	select 'FILL_BLANK', 7 from dual union all
	select 'NUMERIC', 8 from dual union all
	select 'TEXT', 9 from dual union all
	select 'TABLE', 10 from dual union all
	select 'ORBITAL_DIAG', 11 from dual union all
	select 'RXN_COORD', 12 from dual union all
	select 'FREE_BODY', 13 from dual union all
	select 'CHOOSE_EXPLN', 14 from dual union all
	select 'CLICK_IMG', 15 from dual union all
	select 'LOGIC_STMTS', 16 from dual union all
	select 'VECTORS', 17 from dual union all
	select 'EQUATIONS', 18 from dual;
update questions_v3 set 
	q_type_num = bitand(probtype, 63), 
	q_flags = bitand(probtype, power(2, 30) - power(2, 6));
update user_questions_v3 set 
	q_type_num = bitand(probtype, 63), 
	q_flags = bitand(probtype, power(2, 30) - power(2, 6));
update questions_v3 set q_type_name = (
	select name from q_types where num = questions_v3.q_type_num
);
update user_questions_v3 set q_type_name = (
	select name from q_types where num = user_questions_v3.q_type_num
);
drop table q_types;

alter table questions_v3 drop column q_type_num;
alter table user_questions_v3 drop column q_type_num;
alter table questions_v3 rename column q_type_name to q_type;
alter table user_questions_v3 rename column q_type_name to q_type;

-- update some obsolete coded data of SynthScheme and SynthEfficiency evaluators

update evaluators_v3 set coded_data = substr(coded_data, 1, 1) 
	where match_type = 'SYN_SCHEME' and length(coded_data) > 1;
update user_evaluators_v3 set coded_data = substr(coded_data, 1, 1) 
	where match_type = 'SYN_SCHEME' and length(coded_data) > 1;
update evaluators_v3 set coded_data = substr(coded_data, 1, 1) 
	where match_type = 'SYN_EFFIC' and length(coded_data) > 1;
update user_evaluators_v3 set coded_data = substr(coded_data, 1, 1) 
	where match_type = 'SYN_EFFIC' and length(coded_data) > 1;

-- remove obsolete table (Raphael 7/2014)

drop table responses_v5;

-- add maximum extensions field to course (RBG 8/2014)

alter table cw_courses_v3 add max_extensions_str varchar(10);
update cw_courses_v3 set max_extensions_str = '0.0';
alter table hwsets_v5 add max_extension_str varchar(10);
update hwsets_v5 set max_extension_str = '-1';

-- ACE 3.8 ends here
-- ACE 3.9 starts here

-- add assignment dependency column (RBG 1/2015)

alter table hwsets_v5 add depends_on number(38);
alter table hwsets_v5 add constraint f_hwsets5_dep 
	foreign key (depends_on) references hwsets_v5(hw_id);

-- add primary language column to institution (RBG 2/2015)

alter table institutions_v1 add primary_language varchar2(150);

-- add text ID fields for translation purposes to tables for locally modified
-- (and authored) questions (RBG 2/2015)

alter table user_questions_v3 add statement_id number(38);
alter table user_question_data_v3 add text_id number(38);
alter table user_evaluators_v3 add feedback_id number(38);

-- now update local tables by copying phrase IDs from master table to those 
-- question statements, evaluator feedbacks, and question data in local table
-- that have already been translated

-- run this query to find translated phrases in master table that are also
-- found in local table

select unique DBMS_LOB.SUBSTR(statement, length(statement), 1) as srch_result 
	from user_questions_v3 
	where DBMS_LOB.SUBSTR(statement, length(statement), 1) in (
		select DBMS_LOB.SUBSTR(statement, length(statement), 1) 
			from questions_v3 
			where statement_id != 0
	) and id > 0 
	and statement_id = 0;

-- run this query repeatedly, replacing [] with each result from previous query,
-- to copy phrase IDs from master table to local table with identical phrase

update user_questions_v3 
	set statement_id = (
		select statement_id 
			from (
				select unique statement_id, rownum 
					from questions_v3 
					where statement like '[]'
			) where rownum = 1
	) where statement like '[]';

-- run this query to find translated phrases in master table that are also
-- found in local table

select unique DBMS_LOB.SUBSTR(feedback, length(feedback), 1) as srch_result 
	from user_evaluators_v3 
	where DBMS_LOB.SUBSTR(feedback, length(feedback), 1) in (
		select DBMS_LOB.SUBSTR(feedback, length(feedback), 1) as srch_result
			from evaluators_v3 
			where feedback_id != 0
	) and pb_id > 0 and feedback_id = 0;

-- run this query repeatedly, replacing [] with each result from previous query,
-- to copy phrase IDs from master table to local table with identical phrase

update user_evaluators_v3 
	set feedback_id = (
		select feedback_id 
			from (
				select unique feedback_id, rownum 
					from evaluators_v3 
					where feedback like '[]'
			) where rownum = 1
	) where feedback like '[]';

-- run this query to find translated phrases in master table that are also
-- found in local table

select unique DBMS_LOB.SUBSTR(data, length(data), 1) as srch_result 
	from user_question_data_v3 
	where data_type = 'text' 
	and DBMS_LOB.SUBSTR(data, length(data), 1) in (
		select DBMS_LOB.SUBSTR(data, length(data), 1) 
			from question_data_v3 
			where text_id != 0 
			and data_type = 'text'
	) and question_id > 0 
	and text_id = 0;

-- run this query repeatedly, replacing [] with each result from previous query,
-- to copy phrase IDs from master table to local table with identical phrase

update user_question_data_v3 
	set text_id = (
		select text_id 
			from (
				select unique text_id, rownum 
					from question_data_v3 
					where data like '[]'
					and data_type = 'text'
			) where rownum = 1
	) where data like '[]'
	and data_type = 'text';

-- run this query to find translated phrases in master table that are also
-- found in local table

select unique DBMS_LOB.SUBSTR(name, length(name), 1) as srch_result 
	from user_question_data_v3 
	where data_type = 'marvin' 
	and DBMS_LOB.SUBSTR(name, length(name), 1) in (
		select DBMS_LOB.SUBSTR(name, length(name), 1) 
			from question_data_v3 
			where text_id != 0 
			and data_type = 'marvin'
	) and question_id > 0 
	and text_id = 0;

-- run this query repeatedly, replacing [] with each result from previous query,
-- to copy phrase IDs from master table to local table with identical phrase

update user_question_data_v3 
	set text_id = (
		select text_id 
			from (
				select unique text_id, rownum 
					from question_data_v3 
					where name like '[]'
					and data_type = 'marvin'
			) where rownum = 1
	) where name like '[]'
	and data_type = 'marvin';

-- RBG 3/2015 add parameter to mapping

update evaluators_v3 
	set coded_data = coded_data || '/Y' 
	where match_type = 'MAP_PRP'
	and length(coded_data) <= 7;
update user_evaluators_v3 
	set coded_data = coded_data || '/Y' 
	where match_type = 'MAP_PRP'
	and length(coded_data) <= 7;

-- RBG 4/2015 new flags field in forum 

alter table forum_posts_v1 add flags number(38);

-- RBG 8/2015 new field for Marvin Live port in courses

alter table cw_courses_v3 add marvin_live_port number(38);

-- ACE 3.8 ends here
-- ACE 3.9 starts here

-- RBG 10/2015 new field for question sets

alter table pbsets_v3 add serial_no number(38);

-- RBG 12/2015 increase size of R group field to accommodate words and
-- arithmetical values 

alter table response_rgroups_v1 modify r_group varchar2(50);

-- tables dropped, indices should have been dropped as well 

drop index assignment_questions_v3_idx;
drop index cw_users_v3_idx;

-- convert coded_data field in evaluators and user_evaluators to CLOB

create table evaluators_v4 (
	pb_id number(38),
	major_id number(38),
	minor_id number(38),
	subexp varchar2(100),
	match_type varchar2(10),
	feedback CLOB,
	grade number(3,2),
	coded_data CLOB,
	mol_name CLOB,
	mol_structure CLOB,
	feedback_id number(38),
	constraint P_EVALS4 primary key (pb_id, major_id, minor_id),
	constraint F_EVALS4_Q foreign key (pb_id) references questions_v3(id));
insert into evaluators_v4 (select
	pb_id,
	major_id,
	minor_id,
	subexp,
	match_type,
	feedback,
	grade,
	coded_data,
	mol_name,
	mol_structure,
	feedback_id
	from evaluators_v3);

create table user_evaluators_v4 (
	pb_id number(38),
	user_id varchar2(20),
	major_id number(38),
	minor_id number(38),
	subexp varchar2(100),
	match_type varchar2(10),
	feedback CLOB,
	grade number(3,2),
	coded_data CLOB,
	mol_name CLOB,
	mol_structure CLOB,
	feedback_id number(38),
	constraint P_USEREVALS4 primary key (user_id, pb_id, major_id, minor_id),
	constraint F_USEREVALS4_USER foreign key (user_id) references users_v4(user_id));
insert into user_evaluators_v4 (select
	pb_id,
	user_id,
	major_id,
	minor_id,
	subexp,
	match_type,
	feedback,
	grade,
	coded_data,
	mol_name,
	mol_structure,
	feedback_id
	from user_evaluators_v3);
create index evaluators_v4_idx on evaluators_v4 (feedback_id);
drop index evaluators_v3_idx;
drop table evaluators_v3;
drop table user_evaluators_v3;

-- ACE 3.9 ends here
