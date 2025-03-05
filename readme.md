**Disclaimer**

The instructions for building and running this application assume that you are using a Linux distribution. If you are using a non-linux OS, you may want to use a VM such as WSL to simplify the build process.

Additionally, this application is intended to be used with an Oracle database. MySQL dump files can be found in the _database/sql_dumps_ directory, and some instructions are provided to utilize a SQL database; however, keep in mind that this application has not been fully tested using a MySQL database, and you will have to make code changes to accommodate the change in database connection.

**Initial setup**

To assist in directory navigation, a file named settings.sh.copy has been provided in /DEV-5.1/ to automatically create environment variables. Make a copy of this file and remove the “copy” at the end of the file name. Then edit the file to change environment variables to reflect your values.

A description of each environment variable declared in settings.sh.copy is as follows:

- BASE => The base level of the application
- WHO => The current version of the application. Default is DEV-5.1
- JVERS => The version of jchem being used. Default is 20.16
- ACE => The location of the ace directory
- WEB => The location of the web directory
- CATALINA_HOME => The location of the tomcat9 installation
- CATALINA_BASE => The location of the tomcat9 installation
- JRE_HOME => The location of your java installation
- sv => The current version of the application. Default is DEV-5.1
- s => The location of the ace directory
- sj => The location of the web directory
- st => The location of the epoch directory
- sl => The location you want to put any tomcat logs

You will also need a copy of Tomcat to host the application. The Tomcat installation should be at /opt/tomcat9.

**Dependencies**

This application uses the following third-party dependencies. Different versions of these dependencies have not been tested. Once you have these packages, place them in their corresponding directory.

- commons-lang-2.4.jar => /usr/share/java
- commons-math-2.0.jar => /usr/share/java
- commons-collections4-4.0.jar => /usr/share/java
- commons-fileupload-1.2.1.jar => /usr/share/java
- guava-15.0-rc1.jar => /usr/share/java
- unbescape-1.1.6.RELEASE.jar => /usr/share/java
- stanford-parser-2011-09-14 => $BASE
- jchem20.16 => $BASE

Additionally, once you have placed the jchem20.16 directory in $BASE, you will want to create a symlink in $BASE named jchemDEV-5.1 that points to the jchem20.16 directory.

**Building**

Once you are ready to build the app, you’ll need to set up some symlinks using the configureJChem.pl script. Before you run this script, you must first edit the $base variable in that script to match your applications base directory. Additionally, you’ll need to go to $WEB/WEB-INF/lib/ and delete all of these placeholder .jar files.

Now you can run the configureJchem script. To run this script, go to $ACE/marvin-extra/scrips directory and run the following command:

_./configureJChem.pl $WHO jchem$JVERS_

Now that the symlinks are created, you’ll need to make some changes to the Makefile.master, which is located in $st. You’ll need to update the HOME, WS, JCHEMDIR, FINALDEST, FD, and LOCKFILE variables to reflect the location of your ace directory.

To test the compilation of the application, run _make testCompile_ while in the $st directory. If you don’t encounter any errors, you can run the build and compile scripts by running the following command:

_make unlock lock compileAll publish unlock_

**Setting up database connection**

Before running in tomcat, you will have to set up a database using the dump files provided. You may either use an Oracle database with the data dump files provided, or a MySQL database. Keep in mind that the application has not been fully tested with a MySQL database, so problems may arise.

Depending on your choice of database, you will need a jdbc driver. Once you have the necessary driver, put it in /opt/tomcat9/lib.

In order to run the application in Tomcat, you’ll need to first go to $WEB/META-INF where you will find a template for the tomcat context named context.xml.copy. Rename that file context.xml and fill in the values for name, driverClassName, url, username, password, and maxTotal. These values should reflect the database and JDBC appropriate for your setup.

Navigate to $WEB/WEB-INF and rename web.xml.copy to web.xml. Go into that file and uncomment the resource-ref under the comment stating, “Input your db connection resource here”. Fill in the values for each field that correspond to the resource you created in the previous step.

While in the WEB-INF directory, edit the epoch.properties file to reflect your database url and user, as well as the correct location of your ace directory. You should also add values for admin email, verifier name, verifier email, and default institution.

Next, you’ll want to edit the AppConfig.java file located in $st. You will want to update the jdbc_driverclass attribute to reflect the jdbc driver that you are using.

Finally, you will want to edit the DBCommon.java file located at $st/db to reference your database resource. To do this, replace any use of “jdbc/acepool” with the resource name you declared in context.xml.

To make sure the java files are updated, you will need to re-compile the project.

**Running in Tomcat**

To assist in running the application in tomcat, there is a server.xml file found in $BASE/tomcat-conf. Edit this file by replacing “YOUR HOST NAME” in the engine declaration and the host declaration with what you would like to name your application. Once that is updated, you can copy server.xml and put it in /opt/tomcat9/conf, replacing the existing server.xml file

**Mysql troubleshooting**

If you are trying to use MySQL rather than oracle, you will run into some issues regarding oracle specific functions. What follows is a non-exhaustive list of some of the problems you may encounter

- DBMS_LOB.SUBSTR
  - This can be replaced by using “SUBSTRING”. Most instances of this can be found in DBCommon.java.
- BITAND
  - This can be replaced by the MySQL “&” operator.

**Directory structure**

- /
  - database
    - sql_dumps
      - contains database dumps for mysql
    - contains database dumps for oracle db
  - DEV-5.1
    - ace
      - contains all code for the ace application, including build files
    - documentation
      - contains some instructions on how to set up and build the application, as well as information about previous version updates
    - marvin-extra
      - contains an evaluator xml file used for configuration, as well as some other miscellaneous configuration files
      - scripts
        - contains perl scripts that are used to set up the application