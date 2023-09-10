# a single event within the html 

{\"@context\":\"https://schema.org\",\"@type\":\"Event\",\"name\":\"Monthly Meetup for Ocean County Women's Book Club\",\"url\":\"https://www.meetup.com/ocean-county-womens-book-club/events/295738204/\",\"description\":\"This will be the initial meeting for this book club; this will give everyone a chance to meet and to get to know one another. We will also select the book we will read during the month of September that will be discussed at the October meeting. For the preliminary meeting, a selection of 5 books will be provided and the group can vote on which book they would to read. Whichever book receives the majority votes will be selected for September's reading.\\n\\nThis group is open to any woman over the age of 18 who wants to join. We do not discriminate on race, creed, or sexual identity.\\n\\nThere is no fee to join however each member is responsible for acquiring their own copy of the book of the month. Whether this is through your favorite bookstore or local library is entirely your discretion.\",\"startDate\":\"2023-09-12T23:00:00.000Z\",\"endDate\":\"2023-09-13T01:00:00.000Z\",\"eventStatus\":\"https://schema.org/EventScheduled\",\"image\":\"https://secure-content.meetupstatic.com/images/classic-events/\",\"eventAttendanceMode\":\"https://schema.org/OfflineEventAttendanceMode\",\"location\":{\"@type\":\"Place\",\"name\":\"Panera Bread\",\"address\":{\"@type\":\"PostalAddress\",\"addressLocality\":\"Toms River\",\"addressRegion\":\"NJ\",\"addressCountry\":\"us\",\"streetAddress\":\"2 NJ-37, Toms River, NJ\"},\"geo\":{\"@type\":\"GeoCoordinates\",\"latitude\":39.96429,\"longitude\":-74.203316}},\"organizer\":{\"@type\":\"Organization\",\"name\":\"Ocean County Women's Book Club\",\"url\":\"https://www.meetup.com/ocean-county-womens-book-club/\"},\"performer\":\"Ocean County Women's Book Club\"}

- these entries are in a list [] that is each event is an object {} in the list [{},...,{}]

- this data exists in a <script> see <script type=\"application/ld+json\">[]</script>

- there is an early match on above, we want the match preceeded with 

<link rel=\"dns-prefetch\" href=\"https://www.googletagmanager.com\"/>

that is

<link rel=\"dns-prefetch\" href=\"https://www.googletagmanager.com\"/><script type=\"application/ld+json\">[]</script>

- this does seem to isolate the event data, but the json has escaped characters ..

- here is an example event from this section

{\"@context\":\"https://schema.org\",\"@type\":\"Event\",\"name\":\"Monthly Meetup for Ocean County Women's Book Club\",\"url\":\"https://www.meetup.com/ocean-county-womens-book-club/events/295738204/\",\"description\":\"This will be the initial meeting for this book club; this will give everyone a chance to meet and to get to know one another. We will also select the book we will read during the month of September that will be discussed at the October meeting. For the preliminary meeting, a selection of 5 books will be provided and the group can vote on which book they would to read. Whichever book receives the majority votes will be selected for September's reading.\\n\\nThis group is open to any woman over the age of 18 who wants to join. We do not discriminate on race, creed, or sexual identity.\\n\\nThere is no fee to join however each member is responsible for acquiring their own copy of the book of the month. Whether this is through your favorite bookstore or local library is entirely your discretion.\",\"startDate\":\"2023-09-12T23:00:00.000Z\",\"endDate\":\"2023-09-13T01:00:00.000Z\",\"eventStatus\":\"https://schema.org/EventScheduled\",\"image\":\"https://secure-content.meetupstatic.com/images/classic-events/\",\"eventAttendanceMode\":\"https://schema.org/OfflineEventAttendanceMode\",\"location\":{\"@type\":\"Place\",\"name\":\"Panera Bread\",\"address\":{\"@type\":\"PostalAddress\",\"addressLocality\":\"Toms River\",\"addressRegion\":\"NJ\",\"addressCountry\":\"us\",\"streetAddress\":\"2 NJ-37, Toms River, NJ\"},\"geo\":{\"@type\":\"GeoCoordinates\",\"latitude\":39.96429,\"longitude\":-74.203316}},\"organizer\":{\"@type\":\"Organization\",\"name\":\"Ocean County Women's Book Club\",\"url\":\"https://www.meetup.com/ocean-county-womens-book-club/\"},\"performer\":\"Ocean County Women's Book Club\"}

- a version with \" replaced with " is

- id this with this regex `sed 's/\\\"/\"/g'`  

{"@context":"https://schema.org","@type":"Event","name":"Monthly Meetup for Ocean County Womens Book Club","url":"https://www.meetup.com/ocean-county-womens-book-club/events/295738204/","description":"This will be the initial meeting for this book club; this will give everyone a chance to meet and to get to know one another. We will also select the book we will read during the month of September that will be discussed at the October meeting. For the preliminary meeting, a selection of 5 books will be provided and the group can vote on which book they would to read. Whichever book receives the majority votes will be selected for Septembers reading.\n\nThis group is open to any woman over the age of 18 who wants to join. We do not discriminate on race, creed, or sexual identity.\n\nThere is no fee to join however each member is responsible for acquiring their own copy of the book of the month. Whether this is through your favorite bookstore or local library is entirely your discretion.","startDate":"2023-09-12T23:00:00.000Z","endDate":"2023-09-13T01:00:00.000Z","eventStatus":"https://schema.org/EventScheduled","image":"https://secure-content.meetupstatic.com/images/classic-events/","eventAttendanceMode":"https://schema.org/OfflineEventAttendanceMode","location":{"@type":"Place","name":"Panera Bread","address":{"@type":"PostalAddress","addressLocality":"Toms River","addressRegion":"NJ","addressCountry":"us","streetAddress":"2 NJ-37, Toms River, NJ"},"geo":"@type":"GeoCoordinates"},"organizer":{"@type":"Organization","name":"Ocean NJ"},"geo":"latitude":39.96429},"organizer":{"@type":"Organization","name":"Ocean NJ"},"geo":"longitude":-74.203316},"organizer":{"@type":"Organization","name":"Ocean County Womens Book Club","url":"https://www.meetup.com/ocean-county-womens-book-club/"},"performer":"Ocean County Womens Book Club"}

but this fails json validation around "geo" : "@type": "GeoCoordinates"

me thinks this regex is bad because \"geo\":{\"@type\":\"GeoCoordinates\", is losing the object starting symbol '{'


# cat www.meetup.com.har | grep -A5 -B5 -oP "(?<=googletagmanager.com\\\\\\\"/><script type=\\\\\"application/ld\+json\\\\\">).*?(?=</script>)" 

working regex 
