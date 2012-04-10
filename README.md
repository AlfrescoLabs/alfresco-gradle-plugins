Overview
--------

Adds tasks to [Gradle](http://gradle.org) projects which help in compiling, assembling,
and packaging extensions to Alfresco, particularly those deployed as
[Alfresco AMP](http://wiki.alfresco.com/wiki/AMP_Files) projects.

### Requirements

The project currently requires that Gradle and [Maven](http://maven.apache.org) be installed.

### Installing this Project

To build this plugins project and install in your local maven repo run:

	cd alfresco-gradle-plugins
	gradle install
	

### Adding the Plugins to Your Project

In the `build.gradle` file of your project add:

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

```groovy
apply plugin: 'alfresco-war-dependencies'
apply plugin: 'amp'
```



Task: `amp`
-----------


The `amp` task packages an Alfresco AMP for deployment via the Module Management Tool.

### Running the `amp` Task

To compile and package run the following from your AMP project:

	cd *my-amp-project*
	gradle amp
	
The packed AMP will be available at `build/distributions/*moduleId*-*version*.amp` by default.

### Project Layout

The standard Gradle Java plugin [project layout](http://gradle.org/docs/current/userguide/java_plugin.html#N11D6B)
is assumed and `project.name` is used as your AMP module id by default. You can override these properties in `gradle.properties`.

### Properties

<table>
	<tr>
		<td><strong>Property name</strong></td>
		<td><strong>Type</strong></td>
		<td><strong>Default value</strong></td>
		<td><strong>Description</strong></td>
	</tr>
	<tr>
		<td><code>moduleId</code></td>
		<td>String</td>
		<td><code>project.name</code></td>
		<td>The AMP module id</td>
	</tr>
	<tr>
		<td><code>sourceConfigDir</code></td>
		<td>String</td>
		<td><code>src/main/config</code></td>
		<td>The source directory for configuration files</td>
	</tr>
	<tr>
		<td><code>sourceConfigModuleDir</code></td>
		<td>String</td>
		<td><code><em>sourceConfigDir</em>/alfresco/module/<em>moduleId</em></code></td>
		<td>The source directory for AMP module-specific configuration files</td>
	</tr>
	<tr>
		<td><code>sourceWebDir</code></td>
		<td>String</td>
		<td><code>src/main/web</code></td>
		<td>The source directory for web files such as JSP, CSS, and JavaScript files</td>
	</tr>
	<tr>
		<td><code>dependencyLibsDir</code></td>
		<td>String</td>
		<td><code>lib</code></td>
		<td>Directory containing jar files that should be copied into the AMP for deployment</td>
	</tr>
	<tr>
		<td><code>licensesDir</code></td>
		<td>String</td>
		<td><code>licenses</code></td>
		<td>Directory containing license files that should be copied into the AMP for deployment</td>
	</tr>
	<tr>
		<td><code>fromMavenArchetype</code></td>
		<td>Boolean</td>
		<td><code>false</code></td>
		<td>Whether or not this project was create from the Alfresco Maven archetypes</td>
	</tr>
</table>

The plugin also uses many of the [convention properties](http://gradle.org/docs/current/userguide/java_plugin.html#N11E90) from the Java plugin.

### Coming From Maven Archetypes

If your project was created by the Alfresco Maven archetypes you can set `fromMavenArchetype`
to true in your gradle.properties file and maintain the same project structure and files
you've used before.  The archetype's file mapping during assembly of the AMP 
(i.e. `src/main/config/context` -> `config/alfresco/module/*moduleId*` within the AMP) will be
maintined and velocity template paramters will also be expanded within your 
`module.properties` and `module-context.xml` files.

### Related Tasks

<table>
	<tr>
		<td><strong>Task</strong></td>
		<td><strong>Description</strong></td>
	</tr>
	<tr>
		<td><code>setBuildNumberFromSvnRevision</code></td>
		<td>Uses Subversion to set <code>project.buildNumber</code> to the last revision if available, 
		otherwise zero</td>
	</tr>
	<tr>
		<td><code>assembleAmp</code></td>
		<td>Does the work of assembling the AMP structure but does not zip into a deployable .amp file</td>
	</tr>
</table>

### Example Project

Checkout the [amp-gradle-example](https://github.com/Alfresco/amp-gradle-example) project for an example on how to use the Gradle amp plugin.




Task: `explodeWarDependencies`
------------------------------

The `explodeWarDependencies` task extracts the jars and configs from the WAR file specified in warFile
to be used as dependencies in the project.  A `compile` dependency on all jars in `explodedLibsDir` is 
also added to the project.

### Running the `explodeWarDependencies` Task

To extract the configs and jars from `warFile` place the WAR in the root of your project and run the following:

	cd *my-amp-project*
	gradle explodeWarDependencies
	
By default the task is looking for `alfresco.war` and the extracted files will be available at 
`explodedDependencies`.  You can override these defaults in `gradle.properties`.
		
### Properties

<table>
	<tr>
		<td><strong>Property nam</strong>e</td>
		<td><strong>Type</strong></td>
		<td><strong>Default value</strong></td>
		<td><strong>Description</strong></td>
	</tr>
	<tr>
		<td><code>warFile</code></td>
		<td>String</td>
		<td><code>alfresco.war</code></td>
		<td>The path to the WAR file that should be used as a dependencies source</td>
	</tr>
	<tr>
		<td><code>explodedDependenciesDir</code></td>
		<td>String</td>
		<td><code>explodedDependencies</code></td>
		<td>The path to the directory where the extracted dependencies should be placed</td>
	</tr>
	<tr>
		<td><code>explodedLibsDir</code></td>
		<td>String</td>
		<td><code><em>explodedDependenciesDir</em>/lib</code></td>
		<td>The path to the directory where the extracted jars should be placed</td>
	</tr>
	<tr>
		<td><code>explodedConfigDir</code></td>
		<td>String</td>
		<td><code><em>explodedDependenciesDir</em>/config</code></td>
		<td>The path to the directory where the extracted configuration files should be placed</td>
	</tr>
</table>

### Related Tasks

<table>
	<tr>
		<td><strong>Task</strong></td>
		<td><strong>Description</strong></td>
	</tr>
	<tr>
		<td><code>cleanExplodeWarDependencies</code></td>
		<td>Deletes the jars and configs extracted from the WAR file</td>
	</tr>
</table>


License
-------

Copyright (C) 2005-2012 Alfresco Software Limited.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

