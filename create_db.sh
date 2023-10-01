##############################################
# Cleanup
##############################################

DB_FILE="app.db"; 
echo " Nuking previous DB"
rm $DB_FILE;
sqlite3 $DB_FILE ";"

##############################################
# Create Tables
##############################################

echo " Creating EVENT TABLE "
sqlite3 $DB_FILE "CREATE TABLE EVENT("\
"ID INTEGER PRIMARY KEY AUTOINCREMENT,"\
"EVENT_ID TEXT NOT NULL,"\
"SOURCE STRING NOT NULL,"\
"NAME TEXT NOT NULL,"\
"DESCRIPTION TEXT,"\
"START_TIME TEXT NOT NULL,"\
"LOCATION_NAME TEXT,"\
"COUNTRY TEXT,"\
"REGION TEXT,"\
"LOCALITY TEXT,"\
"STREET_ADDRESS TEXT,"\
"LONGITUDE REAL,"\
"LATITUDE REAL,"\
"ORGANIZER TEXT,"\
"URL TEXT,"\
"VIRTUAL INTEGER);"


# create tables
echo " Creating EVENTBRITE_EVENT TABLE "
sqlite3 $DB_FILE "CREATE TABLE EVENTBRITE_EVENT("\
"ID INTEGER PRIMARY KEY AUTOINCREMENT,"\
"EVENT_ID TEXT NOT NULL,"\
"JSON STRING NOT NULL);"
