package com.epoch.db.dbConstants;

import com.epoch.courseware.courseConstants.ForumConstants;

/** Fields of table FORUM_TOPICS. */
public class ForumRWConstants implements ForumConstants {

	/** Table for forum topics.  */
	public static final String FORUM_TOPICS = "forum_topics_v1";
		/** Field in FORUM_TOPICS.  Unique ID.  */
		public static final String FTOPIC_ID = "topic_id";
		/** Field in FORUM_TOPICS.  */
		public static final String FTOPIC_COURSE = "course_id";
		/** Field in FORUM_TOPICS.  */
		public static final String FTOPIC_CREATOR = "creator_id";
		/** Field in FORUM_TOPICS.  */
		public static final String FTOPIC_DATE = "date_created";
		/** Field in FORUM_TOPICS.  */
		public static final String FTOPIC_TITLE = "title"; // CLOB
		/** Field in FORUM_TOPICS.  */
		public static final String FTOPIC_STICKY = "sticky";
		/** Field in FORUM_TOPICS.  */
		public static final String FTOPIC_HWID = "hw_id";
		/** Field in FORUM_TOPICS.  */
		public static final String FTOPIC_QID = "pb_id";
	/** Sequencer for forum topics.  */
	public static final String FTOPIC_SEQ = "forum_topic_seq";
	/** Table for forum posts.  */
	public static final String POSTS = "forum_posts_v1";
		/** Field in POSTS.  Unique ID.  */
		public static final String POST_ID = "post_id";
		/** Field in POSTS.  */
		public static final String POST_TOPIC = "topic_id";
		/** Field in POSTS.  */
		public static final String POST_AUTHOR = "user_id";
		/** Field in POSTS.  */
		public static final String POST_DATE = "date_created";
		/** Field in POSTS.  */
		public static final String POST_EDITED = "date_edited";
		/** Field in POSTS.  */
		public static final String POST_FLAGS = "flags";
		/** Field in POSTS.  */
		public static final String POST_TEXT = "text"; // CLOB
		/** Field in POSTS.  */
		public static final String POST_FIGURE = "figure"; // CLOB
		/** Field in POSTS.  */
		public static final String POST_FIGTYPE = "figure_type";
	/** Sequencer for forum topics.  */
	public static final String POST_SEQ = "forum_post_seq";
	/** Table for users blocked fom course forums.  */
	public static final String BLOCKED = "blocked_from_forums_v1";
		/** Field in BLOCKED.  */
		public static final String BLOCKED_COURSE = "course_id";
		/** Field in BLOCKED.  */
		public static final String BLOCKED_USER = "user_id";
	/** Table for watched topics.  */
	public static final String WATCHED_TOPICS = "watched_forum_topics_v1";
		/** Field in BLOCKED.  */
		public static final String WATCHED_ID = "topic_id";
		/** Field in BLOCKED.  */
		public static final String WATCHED_STUDENT = "student_id";

} // ForumRWConstants
