# Summary
Oracle database dumps have been provided in the database directory. Instructions for importing those data dumps into your Oracle database follow.

## Getting started
You will need an Oracle database to be set up. Oracle database options can be found [here](https://www.oracle.com/database/technologies/oracle-database-software-downloads.html) as well as instructions on how to set them up.


## Importing tables into Oracle
Once you have Oracle set up, you should be able to import the data dumps. The `import.sh` script can be used to accomplish this. Before using this script, you should either create a new Oracle user with the following credentials:
- Username: aceorg15
- Password: groeca99

or, if you would like to use your own credentials, you can replace `aceorg15/groeca99` with your username/password in the `import.sh` script.

More details about the `imp` Oracle utility can be found [in the Oracle docs]([https://](https://docs.oracle.com/en/database/oracle/oracle-database/19/sutil/oracle-original-import-utility.html))
