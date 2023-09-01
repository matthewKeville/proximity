DB_FILE="app.db"; 
echo " Nuking previous DB"
# delete and create new db file
rm $DB_FILE;
sqlite3 $DB_FILE ";"

# create tables
echo " Creating EVENT TABLE "
#sqlite3 $DB_FILE "CREATE TABLE EVENT(EVENTID INT NOT NULL PRIMARY KEY, EVENTNAME TEXT);"
sqlite3 $DB_FILE "CREATE TABLE EVENT("\
"ID INTEGER PRIMARY KEY AUTOINCREMENT,"\
"EVENT_ID TEXT NOT NULL,"\
"SOURCE STRING NOT NULL,"\
"NAME TEXT NOT NULL,"\
"DESCRIPTION TEXT,"\
"START_TIME TEXT NOT NULL,"\
"LONGITUDE REAL,"\
"LATITUDE REAL,"\
"CITY TEXT,"\
"STATE TEXT,"\
"URL TEXT);"

# create sample data
echo " Creating sample events "

sqlite3 $DB_FILE "INSERT INTO EVENT (EVENT_ID,SOURCE,NAME,DESCRIPTION,START_TIME,CITY,STATE)"\
"VALUES ('asd43', 'DEBUG','yoga','sunset yoga in bradley beach','2023-08-30T21:00:00Z','Bradley','NJ')"

# this should fail
#sqlite3 $DB_FILE "INSERT INTO EVENT (DESCRIPTION) VALUES ('knitting with the boys')"


# show sample tables
sqlite3 $DB_FILE "SELECT * FROM EVENT;"
