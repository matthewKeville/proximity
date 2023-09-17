select 
  source,
  virtual as 'online',
  substring(name,0,30) as 'name',
  -- substring(description,0,30) as 'desc',
  substring(location_name,0,30) as 'location',
  latitude as 'lat',
  longitude as 'lon',
  substring(organizer,0,30) 'org',
  country,
  region,
  locality,
  start_time as 'start'
from 
  event 
order by 
  source,
  start_time;
