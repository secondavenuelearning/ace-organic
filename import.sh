# Import the tables in this order to avoid problems with references.

# tables containing user information; must be imported first and in this order
imp aceorg15/groeca99 full=y ignore=y FILE=INSTITUTIONS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=USERS_V4.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=LANGUAGES_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=FORUM_TOPICS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=FORUM_POSTS_V1.dmp

# tables containing question database information; must be imported second and in this order
imp aceorg15/groeca99 full=y ignore=y FILE=CHAPTERS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=PBSETS_V3.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=QUESTIONS_V3.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=QUESTION_DATA_V4.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=IMAGES_V2.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=FIGURES_V5.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=CAPTIONS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=EVALUATORS_V4.dmp

# tables containing course (Tutorials only) and assignment information; must be imported third and in this order
imp aceorg15/groeca99 full=y ignore=y FILE=CW_COURSES_V3.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=HWSETS_V5.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=HWSET_QS_V2.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=HWSET_RXN_CONDNS_V1.dmp

# tables copied in their entirety; can be imported in any order
imp aceorg15/groeca99 full=y ignore=y FILE=CANONICALIZED_UNITS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=FUNCTIONAL_GROUPS_V2.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=HWSETS_FOR_IMPORT_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=IMPOSSIBLE_SMS_V2.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=LANGUAGE_CODES_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=MENU_ONLY_REAGENTS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=REACTION_CONDITIONS_V3.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=TRANSLATIONS_V2.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=UNIT_CONVERSIONS_V1.dmp

# JChem tables; copied in their entirety, or created anew and empty
imp aceorg15/groeca99 full=y ignore=y FILE=JCHEMMETADATATABLE.dmp 
imp aceorg15/groeca99 full=y ignore=y FILE=REACTOR_RESULTS_V4.DMP

# tables none of whose data is copied; can be imported in any order
imp aceorg15/groeca99 full=y ignore=y FILE=ALLOWED_IPS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=ASSIGNED_QUESTIONS_V4.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=BLOCKED_FROM_FORUMS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=COINSTRUCTORS_V2.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=CW_COURSE_ENROLLMENT_V3.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=EXAM_STUDENTS_V2.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=HWSET_EXTENSIONS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=HWSET_GRADING_PARAMS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=MODIFIED_HEADERS_V2.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=PREENROLLMENT_V4.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=RESPONSES_V6.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=RESPONSE_RGROUPS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=TEXTBOOKS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=TEXT_CHAPS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=TEXT_COAUTHORS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=TEXT_CONTENT_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=USER_CAPTIONS_V1.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=USER_EVALUATORS_V4.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=USER_FIGURES_V5.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=USER_IMAGES_V2.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=USER_QUESTIONS_V3.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=USER_QUESTION_DATA_V4.dmp
imp aceorg15/groeca99 full=y ignore=y FILE=WATCHED_FORUM_TOPICS_V1.dmp
