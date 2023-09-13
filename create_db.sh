

# app db

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
"URL TEXT,"\
"VIRTUAL INTEGER);"

# create sample data
echo " Creating sample events "

sqlite3 $DB_FILE "INSERT INTO EVENT (EVENT_ID,SOURCE,NAME,DESCRIPTION,START_TIME,CITY,STATE)"\
"VALUES ('asd43', 'DEBUG','yoga','sunset yoga in bradley beach','2023-08-30T21:00:00Z','Bradley','NJ')"

# this should fail
#sqlite3 $DB_FILE "INSERT INTO EVENT (DESCRIPTION) VALUES ('knitting with the boys')"


# eventbrite db
EB_DB_FILE="eventbrite.db"; 
echo " Nuking previous DB"
# delete and create new db file
rm $EB_DB_FILE;
sqlite3 $EB_DB_FILE ";"

# create tables
echo " Creating EVENTBRITE EVENT TABLE "
sqlite3 $EB_DB_FILE "CREATE TABLE EVENT("\
"ID INTEGER PRIMARY KEY AUTOINCREMENT,"\
"EVENT_ID TEXT NOT NULL,"\
"JSON STRING NOT NULL);"

echo " Creating EVENTBRITE VENUE TABLE "
sqlite3 $EB_DB_FILE "CREATE TABLE VENUE("\
"ID INTEGER PRIMARY KEY AUTOINCREMENT,"\
"VENUE_ID TEXT NOT NULL,"\
"JSON STRING NOT NULL);"
