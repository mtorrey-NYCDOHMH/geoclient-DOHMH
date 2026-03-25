README-this-fork.md
Last modified: 2026-03-25 13:38
Mark Torrey (mtorrey@health.nyc.gov) & Shohruzbek Abdumuminov

# Geoclient DOHMH: README-this-fork
This fork implements lookups for NTA name, UHF code, and UHF names (geographies commonly used by DOHMH). These additional lookups are added as flat lookup tables to the repo, and code is added to inject the results into the json returned by geoclient.

Additional notes below are just details from compiling and running and apply to the original geoclient repo as well.

## Additonal compiling notes
* geoclient source documentation: https://mlipper.github.io/geoclient/docs/current/user-guide/#building-from-source
    * these notes give examples that assume you have cloned geoclient into your home directory `~/geoclient/` 
        * note, if you cloned this forked repo instead of the upstream geoclient, the folder will be `geoclient-DOHMH`
    * these notes give examples that assume you unzipped your geosupport download in `/usr/share/R/library/geocoding_tests/`, like on the DOHMH R server
* Check java version: `java -version` and `javac -version` should both be 17+.
	* "17+" except it didn't compile with java 21 on my Fedora system
    * on Fedora, you can use `sdkman` to change the java version
    * on Arch, you can use `archlinux-java`
    * (I'm sure there's some similar equivalent on deb/ubuntu)
* "Gradle 8.7+" is listed among the requirements, but the `gradlew` file included with the source will automatically download and run the correct version of gradle. So you don't need to install it on the system.
* You need to download geosupport and configure LIBRARY_PATH, LD_LIBRARY_PATH, and GEOFILES to tell the gradlew compiler where geosupport files are. Set in bash with something like:
	* `export LIBRARY_PATH=/usr/share/R/library/geocoding_tests/version-26a_26.1/lib`
		* (this one is only needed for compiling. the next two you need to set for running as well) 
	* `export GEOFILES=/usr/share/R/library/geocoding_tests/version-26a_26.1/fls/`
		* (note trailing / required by geosupport) 
	* `export LD_LIBRARY_PATH=/usr/share/R/library/geocoding_tests/version-26a_26.1/lib:$LD_LIBRARY_PATH`
* build with `./gradlew build` 
	* But in trying to build locally, some (but not all) of the integration tests failed.
	* So, you might have to compile without integration tests: `./gradlew build -x integrationTest`
	* you can do `gradlew clean` to clean, or `gradlew clean build` to clean before building
* after BUILD SUCCESSFUL, search for geoclient.jar in the source diretory, that's your executable.

## Additional running notes
* `geoclient.jar` includes a Spring Boot embedded Tomcat web server, so that is your executable file:
	* You still need to configure your environment variables first, in bash:
		* `export GEOFILES=/usr/share/R/library/geocoding_tests/version-24d_24.4/fls/`
		* `export LD_LIBRARY_PATH=/usr/share/R/library/geocoding_tests/version-24d_24.4/lib:$LD_LIBRARY_PATH`
		* (if you don't do this correctly, you will get a big mess of `Application run failed` errors, the key line being `libgeoclientjni.so: libgeo.so: cannot open shared object file: No such file or directory`)
	* Then run the java executable, like: `java -jar ~/geoclient/geoclient-service/build/libs/geoclient.jar`
		* If it runs correctly, you will see a `geoclient v2` ascii logo 
		* It's possible you might need to run it with java 17 instead of whatever is native on the system you are currently running on, in that case (if you used sdk) use something like: `~/.sdkman/candidates/java/17.0.11-tem/bin/java -jar ~/geoclient/geoclient-service/build/libs/geoclient.jar`
* Check that it is running and geocoding in a browser or with `curl -v "http://address.goes.here/"`:
	* Check you get a response for version: `http://localhost:8080/geoclient/v2/version`
	* Check an address: `http://localhost:8080/geoclient/v2/address?houseNumber=280&street=Riverside+Drive&borough=Manhattan`
	* This one does natural language searches: `http://localhost:8080/geoclient/v2/search?input=350+Fifth+Avenue+Manhattan`
* You may need to look up the path (after the 8080): If it got compiled as something other than geoclient/v2/, there's not really any way to know this from the documentation.
	* If you aren't sure about the endpoint, try grepping for the path with: `grep -r "context-path" ~/geoclient --include="*.yml" --include="*.properties"`


