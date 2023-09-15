##############################################
# Application DB
##############################################

DB_FILE="app.db"; 
echo " Nuking previous DB"
rm $DB_FILE;
sqlite3 $DB_FILE ";"

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

echo " Creating sample events "

# minimal event
sqlite3 $DB_FILE "INSERT INTO EVENT (EVENT_ID,SOURCE,NAME,DESCRIPTION,START_TIME)"\
"VALUES ('sdfda3', 'DEBUG','fishing','fly fishing at prosper town lake','2023-08-30T21:00:00Z')"

# maximal event
sqlite3 $DB_FILE "INSERT INTO EVENT (EVENT_ID,SOURCE,NAME,DESCRIPTION,START_TIME,"\
"LOCATION_NAME,COUNTRY,REGION,LOCALITY,STREET_ADDRESS,LATITUDE,LONGITUDE,ORGANIZER,URL,VIRTUAL)"\
"VALUES ('asd43', 'DEBUG','yoga','sunset yoga in bradley beach','2023-08-30T21:00:00Z',"\
"'Bradley Pavillon','us','nj','bradley beach','23 fake st','234.23','235.43','matthewKeville','https://google.com','0')"

##############################################
# Eventbrite DB
##############################################

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

