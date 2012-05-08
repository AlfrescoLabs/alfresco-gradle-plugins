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
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskInstantiationException
import org.gradle.api.tasks.bundling.Zip
import org.apache.tools.ant.filters.ReplaceTokens

/**
 * Plugin which adds dependencies from a specified exploded war file.
 * 
 */
class DependenciesFromWarPlugin implements Plugin<Project> {
	void apply(Project project) {
		
		if (!project.hasProperty('warFile')) {
			project.ext.warFile = 'alfresco.war'
		}
		if (!project.hasProperty('dependenciesExplodedDir')) {
			project.ext.dependenciesExplodedDir = 'explodedDependencies'
		}
		if (!project.hasProperty('dependenciesExplodedLibsDir')) {
			project.ext.dependenciesExplodedLibsDir = "${project.dependenciesExplodedDir}/lib"
		}
		if (!project.hasProperty('dependenciesExplodedConfigDir')) {
			project.ext.dependenciesExplodedConfigDir = "${project.dependenciesExplodedDir}/config"
		}
		
		project.dependencies.add("compile", project.fileTree(dir: project.dependenciesExplodedLibsDir, include: '*.jar'))
		
		// Extracts the jars and configs from the war
		project.task('explodeWarDependencies',
				group: 'Build',
				description: 'Extracts the jars and configs from the WAR file specified in warFile.') << {
			def explodedDir = project.file(project.dependenciesExplodedDir)
			def explodedLibDir = project.file(project.dependenciesExplodedLibsDir)
			def dependenciesExplodedConfigDir = project.file(project.dependenciesExplodedConfigDir)
			def warFileObj = project.file(project.warFile)
		
			if (warFileObj.exists() == true) {
				
				logger.lifecycle "${project.warFile} was found.  Checking dependancies ..."
				
				if (explodedDir.exists() == false) {
					println(" ... creating destination dir ${explodedDir}")
					explodedDir.mkdir()
				}
				
				if (isUnpacked(explodedLibDir) == false) {
				
					println(" ... unpacking libs into ${explodedLibDir}")
					
					ant.unzip(src: warFileObj, dest: explodedLibDir) {
						ant.patternset {
							ant.include(name: 'WEB-INF/lib/*.jar')
						}
						ant.mapper(type: 'flatten')
					}
				}
				
				if (isUnpacked(dependenciesExplodedConfigDir) == false) {
				
					println(" ... unpacking config into ${dependenciesExplodedConfigDir}")
					
					ant.unzip(src: warFileObj, dest: explodedDir) {
						ant.patternset {
							ant.include(name: 'WEB-INF/classes/**/*')
						}
					}
					
					project.copy {
						from "${explodedDir}/WEB-INF/classes"
						into dependenciesExplodedConfigDir
					}
					
					// TODO understand why this doesn't delete the folder as expected
					ant.delete(includeEmptyDirs: 'true') {
						ant.fileset(dir: "${explodedDir}/WEB-INF", includes: '**/*')
					}
				}
			}
			else {
				logger.error "Dependant WAR file ${project.warFile} can not be found.  Please place it in ${warFileObj.getPath()} to continue."
				throw new TaskInstantiationException("Dependant WAR file ${project.warFile} can not be found.  Please place it in ${warFileObj.getPath()} to continue.")
			}
		}
		
		project.tasks.compileJava.dependsOn project.tasks.explodeWarDependencies
		project.tasks.explodeWarDependencies.outputs.dir project.file("${project.dependenciesExplodedDir}")
		
	}
	
	/** Utility function - indicates whether the provided dir is unpacked (i.e. exists and has some contents) */
	Boolean isUnpacked(dir) {
		if (dir.exists() == true && dir.list().length > 0) {
			return true
		}
		else {
			return false
		}
	}
}