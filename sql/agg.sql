SELECT
  count(*) as `total` ,
  sum(virtual) as `online` ,
  source 
FROM
  event 
GROUP BY
  source;
