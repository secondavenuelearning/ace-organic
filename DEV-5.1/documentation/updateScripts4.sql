-- ACE 4.0 starts here

-- RBG 1/2016 timed assignments

alter table assigned_questions_v4 add entry_date varchar2(50);
alter table hwsets_v5 add duration number(38);

-- RBG 3/2016 moving points per Q and dependencies into hwset_qs 

create table hwset_qs_v2 (
	hw_id number(38) not null,
	group_num number(38) not null,
	group_pick number(38) not null,
	group_bundle_size number(38) not null,
	pb_num_in_group number(38) not null,
	pb_id number(38) not null,
	points number(5,2) not null,
	points_str varchar2(10) not null,
	depends_on_pb_id number(38),
	constraint P_HW_QS2 primary key (hw_id, group_num, pb_num_in_group),
	constraint F_HW_QS2_ASSGT foreign key (hw_id) references hwsets_v5(hw_id));
create index hwset_qs_v2_idx on hwset_qs_v2 (pb_id);

insert into hwset_qs_v2 (
	select hwset_qs_v1.hw_id, 
		group_num, 
		group_pick, 
		group_bundle_size, 
		pb_num_in_group, 
		pb_id, 
		1, 
		'1', 
		independent_pb_id 
	from hwset_qs_v1 
	left join hwset_dependencies_v1 
	on hwset_qs_v1.hw_id = hwset_dependencies_v1.hw_id 
	and pb_id = dependent_pb_id
);

-- [*** populate points field with correct values by pressing 
-- "Add points to assignment questions table" in admin DB update tool ***/

delete from hwset_grading_params_v1 where param_type = 'Q';
drop table hwset_dependencies_v1;
drop table hwset_qs_v1;

-- new, simpler grade calculation functions to use points-per-Q field relocated
-- from hwset_grading_params_v1 to hwset_qs_v2 

create or replace function grade_factor_v3(
	hw_id_in responses_v6.hw_id%type, 
	student_id_in responses_v6.student_id%type, 
	pb_id_in responses_v6.pb_id%type,
	tries_in responses_v6.tries%type,
	date_time_in responses_v6.date_time%type) 
return number is factor number;
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
	select points as factor2
	from hwset_qs_v2 
	where hw_id = hw_id_in and pb_id = pb_id_in
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
begin
	open c1;
	fetch c1 into factor;
	close c1;
	return factor;
end;
/

create or replace function instructor_grade_factor_v2(
	hw_id_in responses_v6.hw_id%type,
	pb_id_in responses_v6.pb_id%type,
	tries_in responses_v6.tries%type,
	date_time_in responses_v6.date_time%type)
return number is factor number;
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
	select points as factor2
	from hwset_qs_v2 
	where hw_id = hw_id_in and pb_id = pb_id_in
) s2, (
	select to_date(date_time_in, 'YYYY/MM/DD HH24:MI:SS') 
			- to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as days_past_due, 
		decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days 
	from hwsets_v5 where hw_id = hw_id_in
) s3
where s3.days_past_due > s1.limit_min * s3.in_days
and s3.days_past_due <= s1.limit_max * s3.in_days; 
begin
	open c1;
	fetch c1 into factor;
	close c1;
	return factor;
end;
/

create or replace procedure recalculate_modified_grades_v2(
	hw_id_in responses_v6.hw_id%type)
is begin
update responses_v6 set modified_grade =
(select s4.grade
	* s0.factor0
	* s1.factor1
from (
	select limit_min, limit_max, factor as factor0
	from hwset_grading_params_v1
	where param_type = 'A' and hw_id = hw_id_in
	union all select 0, 9999999999, 1 from dual
	where hw_id_in not in (
		select hw_id from hwset_grading_params_v1 where param_type = 'A'
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
	select to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as date_due,
		decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days
	from hwsets_v5 where hw_id = hw_id_in
) s2, (
	select student_id, extension
	from hwset_extensions_v1 where hw_id = hw_id_in
	union all select unique student_id, 0 from responses_v6
	where hw_id = hw_id_in and student_id not in (
		select student_id from hwset_extensions_v1 where hw_id = hw_id_in
	)
) s3, (
	select student_id,
		responses_v6.pb_id,
		tries,
		grade * points as grade,
		to_date(date_time, 'YYYY/MM/DD HH24:MI:SS') as date_time
	from responses_v6
	join hwset_qs_v2
	on responses_v6.hw_id = hwset_qs_v2.hw_id
	and responses_v6.pb_id = hwset_qs_v2.pb_id
	where grade != 0
	and responses_v6.hw_id = hw_id_in
) s4
where s4.tries > s0.limit_min
and s4.tries <= s0.limit_max
and s4.date_time
	- s2.date_due
	- s3.extension * s2.in_days
	> s1.limit_min * s2.in_days
and s4.date_time
	- s2.date_due
	- s3.extension * s2.in_days
	<= s1.limit_max * s2.in_days
and s4.student_id = s3.student_id
and responses_v6.student_id = s4.student_id
and responses_v6.pb_id = s4.pb_id
and responses_v6.tries = s4.tries
)
where hw_id = hw_id_in
and grade != 0;
end;
/

create or replace procedure recalc_1_student_grades_v2(
	hw_id_in responses_v6.hw_id%type,
	student_id_in responses_v6.student_id%type)
is begin
update responses_v6 set modified_grade =
(select s4.grade
	* s0.factor0
	* s1.factor1
from (
	select limit_min, limit_max, factor as factor0
	from hwset_grading_params_v1
	where param_type = 'A' and hw_id = hw_id_in
	union all select 0, 9999999999, 1 from dual
	where hw_id_in not in (
	select hw_id from hwset_grading_params_v1 where param_type = 'A'
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
	select to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as date_due,
		decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days
	from hwsets_v5 where hw_id = hw_id_in
) s2, (
	select extension from hwset_extensions_v1
	where hw_id = hw_id_in and student_id = student_id_in
	union all select 0 from dual
	where not exists (
		select extension from hwset_extensions_v1
		where hw_id = hw_id_in and student_id = student_id_in
	)
) s3, (
	select responses_v6.pb_id,
		tries,
		grade * points as grade,
		to_date(date_time, 'YYYY/MM/DD HH24:MI:SS') as date_time
	from responses_v6
	join hwset_qs_v2
	on responses_v6.hw_id = hwset_qs_v2.hw_id
	and responses_v6.pb_id = hwset_qs_v2.pb_id
	where grade != 0
	and responses_v6.hw_id = hw_id_in
	and student_id = student_id_in
) s4
where s4.tries > s0.limit_min
and s4.tries <= s0.limit_max
and s4.date_time
	- s2.date_due
	- s3.extension * s2.in_days
	> s1.limit_min * s2.in_days
and s4.date_time
	- s2.date_due
	- s3.extension * s2.in_days
	<= s1.limit_max * s2.in_days
and responses_v6.pb_id = s4.pb_id
and responses_v6.tries = s4.tries
)
where hw_id = hw_id_in
and grade != 0
and responses_v6.student_id = student_id_in;
end;
/

drop function grade_factor_v2;
drop function grade_factor_with_q_num_v1;
drop function instructor_grade_factor_v1;
drop procedure recalculate_modified_grades_v1;
drop procedure recalc_1_student_grades_v1;

-- ACE 4.0 ends here
-- ACE 4.1 starts here

-- RBG 8/2016
-- after upgrade to MarvinSketch 16.8.1, users can no longer use Java applet for
-- mechanisms or syntheses, so set all users to use MarvinJS by default 

update users_v4 set flags = bitand(flags, power(2, 9) - 1 - power(2, 5));

-- RBG 9/2016
-- remove nullability of password so linkblue password can be used instead

alter table users_v4 modify password_hash raw(150) null;

-- RBG 10/2016
-- make highlighting of table cells in certain evaluators optional

update evaluators_v4 set coded_data = coded_data || '/Y' 
	where match_type in ('TAB_TXT', 'TAB_NUM');

-- RBG 11/2016
-- make new table for captions and labels in complete-the-table and energy 
-- diagram questions

create table captions_v1 (
	question_id number(38),
	type_num number(38),
	type char(1),
	serial_no number(38),
	text CLOB);
insert into captions_v1 (
	select question_id, serial_no, 'R', 0, data from question_data_v3
	where question_id in (select id from questions_v3 where q_type = 'TABLE')
	and serial_no <= 2);
update captions_v1 set type = 'C' where type_num = 2;
alter table captions_v1 drop column type_num;
insert into captions_v1 (
	select question_id, 'L', 0, data from question_data_v3
	where question_id in (
		select id from questions_v3 where q_type in ('ORBITAL_DIAG', 'RXN_COORD')
	) and serial_no = 1);
-- *** y-axis scale; distinguish from rows/labels info by serial_no = -2
insert into captions_v1 (
	select question_id, 'Y', -2, data from question_data_v3
	where question_id in (
		select id from questions_v3 where q_type in ('ORBITAL_DIAG', 'RXN_COORD')
	) and serial_no = 2);
alter table captions_v1 
	add constraint P_CAPTS1 primary key (question_id, type, serial_no)
	add constraint F_CAPTS1_Q foreign key (QUESTION_ID) references QUESTIONS_V3(ID);

-- *** drop size fields from character-separated lists 
update captions_v1 
	set text = TRIM(DBMS_LOB.SUBSTR(text, length(text), REGEXP_INSTR(text, '\|') + 1)),
	serial_no = -1
	where question_id in (select id from questions_v3 where q_type = 'TABLE');
update captions_v1
	set text = TRIM(DBMS_LOB.SUBSTR(text, length(text), REGEXP_INSTR(text, chr(9)) + 1)),
	serial_no = -1
	where question_id in (
		select id from questions_v3 where q_type in ('ORBITAL_DIAG', 'RXN_COORD')
	) and serial_no != -2;
update captions_v1
	set text = TRIM(DBMS_LOB.SUBSTR(text, length(text), REGEXP_INSTR(text, chr(9)) + 1))
	where question_id in (select id from questions_v3 where q_type = 'RXN_COORD')
	and serial_no != -2;

create table user_captions_v1 (
	question_id number(38),
	user_id varchar2(50),
	type_num number(38),
	type char(1),
	serial_no number(38),
	text CLOB);
insert into user_captions_v1 (
	select question_id, user_id, serial_no, 'R', 0, data from user_question_data_v3
	where question_id in (select id from user_questions_v3 where q_type = 'TABLE')
	and serial_no <= 2);
update user_captions_v1 set type = 'C' where type_num = 2;
alter table user_captions_v1 drop column type_num;
insert into user_captions_v1 (
	select question_id, user_id, 'L', 0, data from user_question_data_v3
	where question_id in (
		select id from user_questions_v3 where q_type in ('ORBITAL_DIAG', 'RXN_COORD')
	) and serial_no = 1);
-- *** y-axis scale; distinguish from rows/labels info by serial_no = -2
insert into user_captions_v1 (
	select question_id, user_id, 'Y', -2, data from user_question_data_v3
	where question_id in (
		select id from user_questions_v3 where q_type in ('ORBITAL_DIAG', 'RXN_COORD')
	) and serial_no = 2);
alter table user_captions_v1 
	add constraint P_USERCAPTS1 primary key (question_id, user_id, type, serial_no)
	add constraint F_USERCAPTS1_USER foreign key (user_id) references users_v4(user_id);

update user_captions_v1 
	set text = TRIM(DBMS_LOB.SUBSTR(text, length(text), REGEXP_INSTR(text, '\|') + 1)),
	serial_no = -1
	where question_id in (select id from user_questions_v3 where q_type = 'TABLE');
update user_captions_v1
	set text = TRIM(DBMS_LOB.SUBSTR(text, length(text), REGEXP_INSTR(text, chr(9)) + 1)),
	serial_no = -1
	where question_id in (
		select id from user_questions_v3 where q_type in ('ORBITAL_DIAG', 'RXN_COORD')
	) and serial_no != -2;
update user_captions_v1
	set text = TRIM(DBMS_LOB.SUBSTR(text, length(text), REGEXP_INSTR(text, chr(9)) + 1))
	where question_id in (select id from user_questions_v3 where q_type = 'RXN_COORD')
	and serial_no != -2;

-- *** run "Split caption strings into separate entries" 
-- in admin tool before doing next step ***

delete from captions_v1 where serial_no < 0;
delete from user_captions_v1 where serial_no < 0;

create table question_data_v4 as select * from question_data_v3;
alter table QUESTION_DATA_V4 
	add constraint P_QDATA4 primary key (DATA_ID)
	add constraint F_QDATA4_Q foreign key (QUESTION_ID) references QUESTIONS_V3(ID);
create index question_data_v4_idx on question_data_v4 (question_id, text_id);
update question_data_v4 
	set data = TRIM(DBMS_LOB.SUBSTR(data, REGEXP_INSTR(data, '\|') - 1)) 
	where question_id in (select id from questions_v3 where q_type = 'TABLE') 
	and serial_no <= 2;
update question_data_v4 
	set data = TRIM(DBMS_LOB.SUBSTR(data, REGEXP_INSTR(data, chr(9)) - 1)) 
	where question_id in (select id from questions_v3 where q_type = 'ORBITAL_DIAG') 
	and serial_no = 1;
update question_data_v4 
	set data = TRIM(DBMS_LOB.SUBSTR(data, REGEXP_INSTR(data, chr(9), 1, 2) - 1)) 
	where question_id in (select id from questions_v3 where q_type = 'RXN_COORD') 
	and serial_no = 1;
-- *** y-axis scale
delete from question_data_v4
	where question_id in (
		select id from questions_v3 where q_type in ('ORBITAL_DIAG', 'RXN_COORD')
	) and serial_no = 2;
update question_data_v4 set data_type = 'substn' where data_type in ('rgroup', 'value');

create table user_question_data_v4 as select * from user_question_data_v3;
alter table USER_QUESTION_DATA_V4 
	add constraint P_USERQDATA4 primary key (DATA_ID)
	add constraint F_USERQDATA4_USER foreign key (user_id) references users_v4(user_id);
create index user_question_data_v4_idx on user_question_data_v4 (question_id);
update user_question_data_v4 
	set data = TRIM(DBMS_LOB.SUBSTR(data, REGEXP_INSTR(data, '\|') - 1)) 
	where question_id in (select id from user_questions_v3 where q_type = 'TABLE') 
	and serial_no <= 2;
update user_question_data_v4 
	set data = TRIM(DBMS_LOB.SUBSTR(data, REGEXP_INSTR(data, chr(9)) - 1)) 
	where question_id in (select id from questions_v3 where q_type = 'ORBITAL_DIAG') 
	and serial_no = 1;
update user_question_data_v4 
	set data = TRIM(DBMS_LOB.SUBSTR(data, REGEXP_INSTR(data, chr(9), 1, 2) - 1)) 
	where question_id in (select id from questions_v3 where q_type = 'RXN_COORD') 
	and serial_no = 1;
-- *** y-axis scale
delete from user_question_data_v4
	where question_id in (
		select id from user_questions_v3 where q_type in ('ORBITAL_DIAG', 'RXN_COORD')
	) and serial_no = 2;
update user_question_data_v4 set data_type = 'substn' where data_type in ('rgroup', 'value');

delete from translations_v2 where phrase_id in (
	select text_id from question_data_v4 
	where text_id != 0 
	and (
		question_id in (
			select id from questions_v3 
			where q_type in ('ORBITAL_DIAG', 'RXN_COORD')
		) or (
			question_id in (
				select id from questions_v3 
				where q_type = 'TABLE'
			) and serial_no <= 2
		)
	)
);

drop table question_data_v3;
drop table user_question_data_v3;

-- ACE 4.1 ends here
-- ACE 4.2 starts here

-- RBG 4/2017 add text message emails to users_v4 and add watched topics

alter table users_v4 add text_msg_number CLOB;
create table watched_forum_topics_v1 (
	student_id varchar2(50) not null,
	topic_id number(38) not null,
	constraint P_WATCHTOPIC1 primary key (student_id, topic_id),
	constraint F_WATCHTOPIC1_STUDENT foreign key (student_id) references users_v4(user_id),
	constraint F_WATCHTOPIC1_TOPIC foreign key (topic_id) references forum_topics_v1(topic_id));

-- ACE 4.2 ends here
-- ACE 4.3 starts here

-- no changes in database in ACE 4.3

-- ACE 4.4 starts here

-- no changes in database in ACE 4.4

-- ACE 4.5 starts here

-- RBG 3/2020 add payment information to users_v4

alter table users_v4 add payment_transaction_num varchar2(100);
alter table institutions_v1 add grace_days number(5,2);

-- RBG 3/2020 add record of translator

alter table translations_v2 add translator varchar2(50);

-- RBG 5/2020 add record of last login

alter table users_v4 add last_login_date varchar2(25);
update users_v4 set last_login_date = to_char(sysdate, 'YYYY/MM/DD HH24:MI:SS');

