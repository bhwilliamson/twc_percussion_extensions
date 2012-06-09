Setup:
-Unpack this archive to some working location.
-Open Eclipse and from the menu select File -> Import.
-From the dialog select General -> Existing Projects Into Workspace.
-Browse to location you unpacked this archive and finish wizard.
-Copy all jars from a Rhythmyx install at:
	Rhythmyx/AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/lib
	to the lib directory in this project.
-Select this project in Package Explorer, right click and select Properties
	From the dialog select Java Build Path and on the Libraries tab remove
	all jars and then add all jars in the project's lib directory via 'Add Jars...' button.



Build Jar:
-Ant is used to build the extensions jar.  
-Update build.properties with the location to a jdk
-Open ant view in eclipse and add the build.xml in the root of this project
-Run the default target (jar) to compile and build the extension jar

Deploy Jar:
-Copy the jar ant creates in the jar directory to:
-Rhythmyx/AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/lib

To Run JUnit Tests:
-Tests are located in com.weather.percussion.extensions.test
-To run in eclipse, right click on .java test file in package explorer and 
	select Run As -> JUnit Test

