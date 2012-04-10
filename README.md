Overview
========

Adds tasks to [Gradle](http://gradle.org) projects which help in compiling, assembling,
and packaging extensions to Alfresco, particularly those deployed as
[Alfresco AMP](http://wiki.alfresco.com/wiki/AMP_Files) projects.

###Installing this Project

To build this project and install in your local maven repo run:

		gradle install
	

###Adding the Plugins to Your Project

In your build.gradle add:

```groovy
buildscript {
	repositories {
		mavenLocal()
	}
	dependencies {
		classpath group: 'org.alfresco.gradle', name: 'amp-plugin', version: '1.0-SNAPSHOT'
	}
}
buildscript {
   configurations.classpath.resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
}
```


then add each plugin you want to use:

		apply plugin: 'alfresco-war-dependencies'
		apply plugin: 'amp'



`amp` Task
==========

The `amp` task packages an Alfresco AMP for deployment via the Module Management Tool.

Running the `amp` Task
----------------------

To compile and package your AMP project run:

		gradle amp

Project Layout
--------------

The standard [Gradle Java plugin project layout](http://gradle.org/docs/current/userguide/java_plugin.html#N11D6B)
is assumed and `project.name` is used as your AMP module id by default.  

You can override these in `gradle.properties`.

Properties
----------

<table>
	<tr>
		<td>Property name</td>
		<td>Type</td>
		<td>Default value</td>
		<td>Description</td>
	</tr>
	<tr>
		<td>`moduleId`</td>
		<td>String</td>
		<td>`project.name`</td>
		<td>The AMP module id</td>
	</tr>
	<tr>
		<td>`sourceConfigDir`</td>
		<td>String</td>
		<td>`src/main/config`</td>
		<td>The source directory for configuration files</td>
	</tr>
	<tr>
		<td>`sourceConfigModuleDir`</td>
		<td>String</td>
		<td>`*sourceConfigDir*/alfresco/module/*moduleId*`</td>
		<td>The source directory for AMP module-specific configuration files</td>
	</tr>
	<tr>
		<td>`sourceWebDir`</td>
		<td>String</td>
		<td>`src/main/web`</td>
		<td>The source directory for web files such as JSP, CSS, and JavaScript files</td>
	</tr>
	<tr>
		<td>`dependencyLibsDir`</td>
		<td>String</td>
		<td>`lib`</td>
		<td>Directory containing jar files that should be copied into the AMP for deployment</td>
	</tr>
	<tr>
		<td>`licensesDir`</td>
		<td>String</td>
		<td>`licenses`</td>
		<td>Directory containing license files that should be copied into the AMP for deployment</td>
	</tr>
	<tr>
		<td>`fromMavenArchetype`</td>
		<td>Boolean</td>
		<td>`false`</td>
		<td>Whether or not this project was create from the Alfresco Maven archetypes</td>
	</tr>
</table>

Coming From Maven Archetypes
----------------------------

If your project was created by the Alfresco Maven archetypes you can set `fromMavenArchetype`
to true in your gradle.properties file and you can maintain the same project structure and files
you've used before.  The velocity template paramters will also be expanded within your 
`module.properties` and `module-context.xml` files.

Related Tasks
-------------

<table>
	<tr>
		<td>Task</td>
		<td>Description</td>
	</tr>
	<tr>
		<td>`setBuildNumberFromSvnRevision`</td>
		<td>Uses Subversion to set `project.buildNumber` to the last revision if available, otherwise zero</td>
	</tr>
	<tr>
		<td>`buildAmp`</td>
		<td>Does the work of assembling the AMP structure but does not zip into a deployable .amp file</td>
	</tr>
</table>




`explodeWarDependencies` Task
=============================

The `explodeWarDependencies` task extracts the jars and configs from the WAR file specified in warFile
to be used as dependencies in the project.

Running the `explodeWarDependencies` Task
----------------------

To extract the configs and jars from a war file run:

		gradle explodeWarDependencies
		
Properties
----------

<table>
	<tr>
		<td>Property name</td>
		<td>Type</td>
		<td>Default value</td>
		<td>Description</td>
	</tr>
	<tr>
		<td>`warFile`</td>
		<td>String</td>
		<td>`alfresco.war`</td>
		<td>The path to the WAR file that should be used as a dependencies source</td>
	</tr>
	<tr>
		<td>`explodedDependenciesDir`</td>
		<td>String</td>
		<td>`explodedDependencies`</td>
		<td>The path to the directory where the extracted dependencies should be placed</td>
	</tr>
	<tr>
		<td>`explodedLibsDir`</td>
		<td>String</td>
		<td>`*explodedDependenciesDir*/lib`</td>
		<td>The path to the directory where the extracted jars should be placed</td>
	</tr>
	<tr>
		<td>`explodedConfigDir`</td>
		<td>String</td>
		<td>`*explodedDependenciesDir*/config`</td>
		<td>The path to the directory where the extracted configuration files should be placed</td>
	</tr>
</table>

Related Tasks
-------------

<table>
	<tr>
		<td>Task</td>
		<td>Description</td>
	</tr>
	<tr>
		<td>`cleanWarDependencies`</td>
		<td>Deletes the jars and configs extracted from the WAR file</td>
	</tr>
</table>


License
=======

Apache 2

