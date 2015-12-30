/*
Schema for storing raw ISD-lite hourly weather data.
More can be read about these weather sets here: http://www.ncdc.noaa.gov/oa/climate/isd/
*/

DROP KEYSPACE IF EXISTS isd_weather_data;
CREATE KEYSPACE isd_weather_data WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 2 };

USE isd_weather_data;

/*
Raw weather readings from a single station, hourly.
sky_condition_text text, // Non-coded sky conditions
*/
CREATE TABLE raw_weather_data (
    wsid text,               // Composite of Air Force Datsav3 station number and NCDC WBAN number
    date text,               // yyyy-mm-dd for appropriate partition size purpose
    year int,                // Year collected
    month int,               // Month collected
    day int,                 // Day collected
    hour int,                // Hour collected
    temperature double,      // Air temperature (degrees Celsius)
    dewpoint double,         // Dew point temperature (degrees Celsius)
    pressure double,         // Sea level pressure (hectopascals)
    wind_direction int,      // Wind direction in degrees. 0-359
    wind_speed double,       // Wind speed (meters per second)
    sky_condition int,       // Total cloud cover (coded, see format documentation)
    sky_condition_text text, // Non-coded sky conditions
    one_hour_precip double,  // One-hour accumulated liquid precipitation (millimeters)
    six_hour_precip double,  // Six-hour accumulated liquid precipitation (millimeters)
    PRIMARY KEY ((wsid, date), year, month, day, hour)
) WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC, hour DESC) AND
  compaction = {'class': 'DateTieredCompactionStrategy'};

CREATE INDEX on isd_weather_data.raw_weather_data(wsid);

/*
Monthly aggregation of temperature for one weather station
For batch job demo
*/
CREATE TABLE monthly_aggregate_temperature (
    wsid text,
    year int,
    month int,
    high double,
    low double,
    mean double,
    variance double,
    stdev double,
    update_time timestamp,
    PRIMARY KEY ((wsid), year, month)
) WITH CLUSTERING ORDER BY (year DESC, month DESC) AND
  compaction = {'class': 'DateTieredCompactionStrategy'};

/* 
Hourly moving average for one day and one weather station
For streaming job demo
*/
CREATE TABLE moving_average_temperature (
    wsid text,
    event_time timestamp,
    temperature_moving_avg double,
    PRIMARY KEY ((wsid), event_time)
) WITH compaction = {'class': 'DateTieredCompactionStrategy'};


