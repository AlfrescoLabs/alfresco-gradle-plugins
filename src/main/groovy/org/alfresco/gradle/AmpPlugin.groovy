/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfresco.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.bundling.Zip
import org.apache.tools.ant.filters.ReplaceTokens

/**
 * Plugin which adds an <code>amp</code> task to projects which assembles the
 * resources in a build directory then zips the file into a standard AMP.
 */
class AmpPlugin implements Plugin<Project> {
	void apply(Project project) {
		
		if (!project.hasProperty('moduleId')) {
			project.ext.moduleId = project.name
		}
		if (!project.hasProperty('sourceConfigDir')) {
			project.ext.sourceConfigDir = 'src/main/config'
		}
		if (!project.hasProperty('sourceConfigModuleDir')) {
			project.ext.sourceConfigModuleDir = "${project.sourceConfigDir}/alfresco/module/${project.moduleId}"
		}
		if (!project.hasProperty('sourceWebDir')) {
			project.ext.sourceWebDir = 'src/main/web'
		}
		if (!project.hasProperty('dependencyLibsDir')) {
			project.ext.dependencyLibsDir = 'lib'
		}
		if (!project.hasProperty('licensesDir')) {
			project.ext.licensesDir = 'licenses'
		}
		
		// Sets the project build number from the project's last changed SVN revision
		project.task('setBuildNumberFromSvnRevision') << {
			new ByteArrayOutputStream().withStream { os ->
				def result = project.exec {
					executable = 'svn'
					args = ['info']
					standardOutput = os
					ignoreExitValue = true
				}
				if (result.getExitValue()==0) {
					def outputAsString = os.toString()
					def matchLastChangedRev = outputAsString =~ /Last Changed Rev: (\d+)/
					project.ext.buildNumber = matchLastChangedRev[0][1]
				} else {
					project.ext.buildNumber = '0'
				}
			}
			logger.lifecycle "Set buildNumber from SVN revision: ${project.buildNumber}"
		}
		
		// Sets the project build number from the project's last changed SVN revision
		project.task('setupMavenProperties', dependsOn: 'setBuildNumberFromSvnRevision') << {
			onlyIf {
				isFromMavenArchetype(project)
			}
			project.ext.artifactId = project.name
			project.ext.noSnapshotVersion = project.version
		}
		
		// Assembles the resources for an Alfresco AMP in the build dir using the 
		// same hierarchical mapping the Alfresco Maven archetype does
		project.task('assembleAmpFromMavenArchetype', type: Copy, dependsOn: 'setupMavenProperties') {
			onlyIf {
				isFromMavenArchetype(project)
			}
			into("${project.buildDir}/amp")
			exclude '**/*README*'
			into('./') {
				from('./') {
					include 'module.properties'
					expand(project.properties)
				}
			}
			into('config/alfresco/module/' + project.name + '/context') {
				from "${project.sourceConfigDir}/context"
			}
			into('config/alfresco/module/' + project.name) {
				from("${project.sourceConfigDir}") {
					include 'module-context.xml'
					expand(project.properties)
				}
			}
		}
		
		// Assembles the resources for an Alfresco AMP in the build dir.
		project.task('assembleAmp', type: Copy, dependsOn: ['jar', 'assembleAmpFromMavenArchetype']) {
			into("${project.buildDir}/amp")
			exclude '**/*README*'
			from("${project.buildDir}/libs") {  // contains the result of the jar task
				into 'lib'
			}
			from("${project.dependencyLibsDir}") {
				into 'lib'
			}
			from("${project.licensesDir}") {
				into 'licenses'
			}
			into('web') {
				from "${project.sourceWebDir}"
			}
			if (!isFromMavenArchetype(project)) {
				into('config') {
					from "${project.sourceConfigDir}"
					exclude '**/module.properties'
					exclude '**/module-context.xml'
					exclude '**/file-mapping.properties'
				}
				into("config/alfresco/module/${project.moduleId}") {
					from ("${project.sourceConfigModuleDir}") {
						include 'module-context.xml'
						expand(project.properties)
					}
				}
				into('./') {
					from ("${project.sourceConfigModuleDir}") {
						include 'module.properties'
						expand(project.properties)
					}
					from ("${project.sourceConfigModuleDir}") {
						include 'file-mapping.properties'
						expand(project.properties)
					}
				}
			}
		}
		project.tasks.assembleAmp.outputs.dir project.file("${project.buildDir}/amp")
		
		project.task('amp', type: Zip, dependsOn: 'assembleAmp', 
				group: 'Build', 
				description: 'Packages an Alfresco AMP for deployment via the Module Management Tool.') {
			from "${project.buildDir}/amp"
			extension = 'amp'
		}
//		project.tasks.amp.outputs.file project.file("${project.buildDir}/${project.distsDirName}/${project.name}-${project.version}.amp")
		
	    // TODO - Move this to MMT once it supports exploded WARs, file-mapping.properties is ignored
		project.task('deployDevelopmentAmp', type: Copy, dependsOn: ['assembleAmp']) {
			if (project.hasProperty('developmentExplodedWar')) {
				into("${project.developmentExplodedWar}")
				exclude '**/*README*'
				from("${project.buildDir}/libs") {  // contains the result of the jar task
					into 'WEB-INF/lib'
				}
				from("${project.dependencyLibsDir}") {
					into 'WEB-INF/lib'
				}
				into('./') {
					from "${project.sourceWebDir}"
				}
				into('WEB-INF/classes') {
					from "${project.sourceConfigDir}"
					exclude '**/module.properties'
					exclude '**/module-context.xml'
					exclude '**/file-mapping.properties'
				}
				into("WEB-INF/classes/alfresco/module/${project.moduleId}") {
					from ("${project.sourceConfigModuleDir}") {
						include 'module-context.xml'
						expand(project.properties)
					}
				}
			}
		}
	}
	
	boolean isFromMavenArchetype(Project project) {
		return (project.hasProperty('fromMavenArchetype') && project.fromMavenArchetype)
	}
}