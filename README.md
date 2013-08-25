Intro
=====
Ultimate Geo is the final definitive geocoding app. The motivation behind it was:
We have different CSVs full of address in the most crazy combinations. Some of them has the `street`, `number`, `site`, `phone`. Other CSVs have data in a different order, etc. This led us to keep creating (actually adjusting) our geocode scripts. Now it is over!
 
Usage
=====
Run it with `lein run -h` and you'll see:
 
    Usage:
    Switches               Default       Desc                                                                                            
    --------               -------       ----                                                                                            
    -in, --in-fields                     A string that describes how to mapp each parsed field: "_ :city :name setreet _ _ :id"         
    -out, --out-format                   A string that be written to the output file. :lat and :lng are also available: ":id, :lat, :ln"
    -query, --maps-query                 The query that will be actually submitted to google maps. `-in` used if not supplied                                        
    -d, --delimiter        ,             A csv delimiter. Defaults to ,                                                                  
    -h, --no-help, --help  false         Show this help.                                                                                 
    -t, --target           ./geo-target  Target file.                                                                                    
    -s, --source           ./geo-source  Target file.


Suppose you have the given csv file:

    Shopping Ibirapuera, Av. Ibirapuera, 3103, São Paulo, Moema
    Shopping Barra, Av. Centenário, 2992, Salvador, Chame-Chame

Just call the ultimate geocoder like this:

    lein run -in ":name :street :number :city :area" \
             -out ":name, :street, :number, :city, :area, :lat, :lng" \
             -query ":street, :number, :area, :city - Brasil" \ 
             -s my.csv

This will generate a second file with the following content:

    Shopping Ibirapuera has the following geodata -23.60972690 -46.66777130
    Shopping Barra has the following geodata -12.98904470 -38.50801550


It works like this: 
   
![ultimate-geo](http://github.com/paulosuzart/ultimate-geo/raw/master/ultimate.png)

   1. Maps every column by position to the specified `in-fields` variables
   1. For each line, replaces the `-query` with the appropriate variables and submit it to google
   1. After geocoding, writes the `-out` to the `-t` file. Providing two more variables that are `:lat` and `:lng`


Notice the column maps works just like variables in HQL queries.

Mapping `_` for a input column, means that it is not important, so you are not going to use them in the output. You need to map them though, so the input fields are correctly mapped according to their order.

Some times there are not enough information to geocode, so Ultimate Geo will put an `"unavailable"`. If google answers with an error or query over limit, Ultimage Geo will put an `"retry"` for lat and long.

Todo
====

   * Make input mapping easier passing just the keywords. **OK**
   * Make contry as a parameter. **OK**
   * Make out-format easier to write like: -o ":id,:lat,:lng", or -o ":id-latitude-:lat-lingitude:lng". instead of providing two arguments to format the output. **OK**
   * Add the possibility to also return zip code
   * Upgrade client to use Google Geocode V3 **OK**
   * Validate `output-format` according to `in-fields`. This will avoid exceptions and wrong usage

