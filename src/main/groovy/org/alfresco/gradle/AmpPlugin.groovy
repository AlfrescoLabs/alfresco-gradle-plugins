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

import com.yahoo.platform.yui.compressor.CssCompressor
import com.yahoo.platform.yui.compressor.JavaScriptCompressor
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskInstantiationException
import org.gradle.api.tasks.bundling.Zip
import org.apache.tools.ant.filters.ReplaceTokens

/**
 * Plugin which adds an <code>amp</code> task to projects which assembles the
 * resources in a build directory then zips the file into a standard AMP.
 */
class AmpPlugin implements Plugin<Project> {
	
	int lineBreak = -1
	boolean munge
	boolean verbose
	boolean preserveAllSemiColons
	boolean disableOptimizations
	
	final String CSS_FILE_EXTENSION = '.css';
	final String JAVASCRIPT_FILE_EXTENSION = '.js';
	
	void apply(Project project) {
		
		// Set default properties if they're not set
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
			project.ext.sourceWebDir = 'src/main/webapp'
		}
		if (!project.hasProperty('dependencyLibsDir')) {
			project.ext.dependencyLibsDir = 'lib'
		}
		if (!project.hasProperty('licensesDir')) {
			project.ext.licensesDir = 'licenses'
		}
		if (!project.hasProperty('springVersion')) {
			project.ext.springVersion = '3.0.0.RELEASE'
		}
		if (!project.hasProperty('alfrescoVersion')) {
			project.ext.alfrescoVersion = '4.0.d'
		}
        if (!project.hasProperty('alfrescoEdition')) {
            project.ext.alfrescoEdition = "community"
        }
		if (!project.hasProperty('assembleAmpDir')) {
			project.ext.assembleAmpDir = "${project.buildDir}/amp"
		}
		if (!project.hasProperty('ampFile')) {
			project.ext.ampFile = "${project.buildDir}/${project.distsDirName}/${project.name}-${project.version}.amp"
		}
		
		// Add common dependencies
        if (project.alfrescoEdition == 'enterprise') {
            project.dependencies.add("compile", "org.alfresco.enterprise:alfresco-repository:${project.alfrescoVersion}")
            project.dependencies.add("compile", "org.alfresco.enterprise:alfresco-core:${project.alfrescoVersion}")
            project.dependencies.add("compile", "org.alfresco.enterprise:alfresco-data-model:${project.alfrescoVersion}")
        } else {
            project.dependencies.add("compile", "org.alfresco:alfresco-repository:${project.alfrescoVersion}")
            project.dependencies.add("compile", "org.alfresco:alfresco-core:${project.alfrescoVersion}")
            project.dependencies.add("compile", "org.alfresco:alfresco-data-model:${project.alfrescoVersion}")
        }
		project.dependencies.add("compile", "org.springframework:spring-core:${project.springVersion}")
		project.dependencies.add("compile", "org.springframework:spring-beans:${project.springVersion}")
		project.dependencies.add("compile", "org.springframework:spring-context:${project.springVersion}")
		project.dependencies.add("compile", project.fileTree(dir: 'lib', include: '**/*.jar'))
		project.dependencies.add("testCompile", "junit:junit:4.+")
		project.configurations.create("ampLib")
		project.configurations.ampLib.transitive = false
        project.configurations.create("ampConfig")
        project.dependencies.add("ampConfig", project.files("${project.sourceConfigDir}"))
        project.configurations.create("ampConfigModule")
        project.dependencies.add("ampConfigModule", project.files("${project.sourceConfigModuleDir}"))
        project.configurations.create("ampWeb")
        project.dependencies.add("ampWeb", project.files("${project.sourceWebDir}"))
		
		
		// Sets the project build number from the project's last changed SVN revision
		project.task('setBuildNumberFromSvnRevision') << {
			if (!project.hasProperty('buildNumber')) {
				new ByteArrayOutputStream().withStream { os ->
					def result = project.exec {
						executable = 'svn'
						args = ['info']
						standardOutput = os
						ignoreExitValue = true
					}
					if (result.getExitValue()==0) {
						def outputAsString = os.toString()
	                    if (outputAsString.contains('Last Changed Rev')) {
	                        def matchLastChangedRev = outputAsString =~ /Last Changed Rev: (\d+)/
	                        project.ext.buildNumber = matchLastChangedRev[0][1]
	                    } else {
	                        project.ext.buildNumber = '0'
	                    }
					} else {
						project.ext.buildNumber = '0'
					}
				}
				logger.lifecycle "Set buildNumber from SVN revision: ${project.buildNumber}"
			}
		}
		
		// Sets up aliases for maven named properties
		project.task('setupMavenProperties') << {
			project.ext.artifactId = project.name
			project.ext.noSnapshotVersion = project.version
		}
		
		// Assembles the resources for an Alfresco AMP in the build dir using the 
		// same hierarchical mapping the Alfresco Maven archetype does
		project.task('assembleAmpFromMavenArchetype', type: Copy, dependsOn: 'setupMavenProperties') {
			onlyIf {
				isFromMavenArchetype(project)
			}
			into("${project.assembleAmpDir}")
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
		project.task('assembleAmp', type: Copy, dependsOn: ['jar', 'setBuildNumberFromSvnRevision', 'assembleAmpFromMavenArchetype']) {
			into("${project.assembleAmpDir}")
			exclude '**/*README*'
			from(project.configurations.ampLib) {
				into 'lib'
			}
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
				from project.configurations.ampWeb
			}
			if (!isFromMavenArchetype(project)) {
				into('config') {
					from project.configurations.ampConfig
					exclude '**/module.properties'
					exclude '**/file-mapping.properties'
				}
				into('./') {
					from project.configurations.ampConfig {
						include 'module.properties'
						expand(project.properties)
					}
					from project.configurations.ampConfig {
						include 'file-mapping.properties'
						expand(project.properties)
					}
				}
			}
		}
		project.tasks.assembleAmp.outputs.dir project.file("${project.assembleAmpDir}")
		
		project.task('compressAmp', dependsOn: 'assembleAmp',
				group: 'Build',
				description: 'Uses YUI Compressor to compress web resources within the assembled AMP.') << {
			FileTree tree = project.fileTree(dir: "${project.assembleAmpDir}")
			tree.include '**/*.js'
			tree.include '**/*.css'
			tree.exclude '**/*-min.js'
			tree.exclude '**/WEB-INF/**'
			tree.exclude '**/tiny_mce/**'
			tree.exclude '**/yui/**'
			tree.exclude '**/site-webscripts/**'
			
			tree.each { File sourceFile ->
				File targetFile = new File(generateDestinationFileName(sourceFile.getParent(), sourceFile))
				logger.debug "compressing ${sourceFile.name}"
				if (sourceFile.name.endsWith(JAVASCRIPT_FILE_EXTENSION))
					compressJsFile(sourceFile, targetFile)
				else if (sourceFile.name.endsWith(CSS_FILE_EXTENSION))
					compressCssFile(sourceFile, targetFile)
				logger.info "${sourceFile.name} with size ${sourceFile.size()} compressed to ${targetFile.name} with size ${targetFile.size()}"
			}
		}
		
		project.task('amp', type: Zip, dependsOn: 'compressAmp', 
				group: 'Build', 
				description: 'Packages an Alfresco AMP for deployment via the Module Management Tool.') {
			from "${project.assembleAmpDir}"
			extension = 'amp'
		}
		project.tasks.amp.outputs.file project.file("${project.ampFile}")
		
		project.task('installAmp', dependsOn: ['amp'],
			description: "Uses MMT to install the packaged AMP into the specified 'warFile'") << {
			def warFileLocation = project.file("${project.warFile}")
			def ampFileLocation = project.file("${project.ampFile}")
			
			def mmt = new org.alfresco.repo.module.tool.ModuleManagementTool()
			mmt.setVerbose(true)
			mmt.installModule(ampFileLocation.getPath(), warFileLocation.getPath(), false, true, false)
		}
		project.tasks.installAmp.doFirst {
			checkForWarFile(project)
			if (!project.hasProperty('warFile')) {
				throw new TaskInstantiationException(
					"Project property 'warFile' must be set for installAmp task"
				);
			}
		}
				
	    // TODO - Move this to MMT once it supports exploded WARs, file-mapping.properties is ignored
		project.task('installDevelopmentAmp', type: Copy, dependsOn: ['assembleAmp'],
			    description: "Installs the files that would be packged as an AMP directly into 'warExplodedDir'") {
			checkForWarExplodedDir(project)
			if (project.hasProperty('warExplodedDir')) {
				into("${project.warExplodedDir}")
				exclude '**/*README*'
				from("${project.assembleAmpDir}/lib") {  // contains the result of the jar task
					into 'WEB-INF/lib'
				}
				into('./') {
					from "${project.assembleAmpDir}/web"
				}
				into('WEB-INF/classes') {
					from "${project.assembleAmpDir}/config"
					exclude '**/module.properties'
					exclude '**/file-mapping.properties'
				}
				into("WEB-INF/classes/alfresco/module/${project.moduleId}") {
					from ("${project.sourceConfigModuleDir}") {
						include 'module-properties.xml'
						expand(project.properties)
					}
				}
			}
		}
		// TODO Below runs regardless of task called
//		project.tasks.installDevelopmentAmp.doFirst {
//			checkForWarExplodedDir(project)
//			if (!project.hasProperty('warExplodedDir')) {
//				throw new TaskInstantiationException(
//					"Project property 'warExplodedDir' must be set for installDevelopmentAmp task"
//				);
//			}
//		}
	}
	
	boolean isFromMavenArchetype(Project project) {
		return (project.hasProperty('fromMavenArchetype') && project.fromMavenArchetype)
	}
	
	
	// Much of compressor code based on https://github.com/ecamacho/YUI-Compressor-Gradle-Plugin
	
	String generateDestinationFileName(String targetDir, File sourceFile) {
		String fileNameWithoutExtension = sourceFile.name.lastIndexOf( '.' ).with {
			it != -1 ? sourceFile.name[0..<it] : sourceFile.name
		}
		String fileExtension = sourceFile.name.lastIndexOf( '.' ).with {
			it != -1 ? sourceFile.name[it..sourceFile.name.length() - 1] : ''
		}
		def fileName = "$targetDir/${fileNameWithoutExtension}-min${fileExtension}"
		fileName
	}
	
	void compressJsFile(File sourceFile, File targetFile) {
		sourceFile.withReader{ reader ->
			JavaScriptCompressor compressor = new JavaScriptCompressor(reader, null)
			targetFile.withWriter{ writer ->
				compressor.compress(writer, lineBreak, munge, verbose, preserveAllSemiColons, disableOptimizations)
			}
		}
	}
	
	void compressCssFile(File sourceFile, File targetFile) {
		sourceFile.withReader{ reader ->
			CssCompressor compressor = new CssCompressor(reader)
			targetFile.withWriter{ writer ->
				compressor.compress(writer, lineBreak)
			}
		}
	}
	
	void checkForWarFile(Project project) {
		if (!project.hasProperty('warFile') && System.getenv()['CURRENT_PROJECT'] != '') {
			if (project.name.contains('repo')) {
				project.ext.warFile = System.properties['user.home'] +
					'/Development/projects/' + System.getenv()['CURRENT_PROJECT'] + '/software/tomcat/webapps/alfresco.war'
			} else if (project.name.contains('share')) {
				project.ext.warFile = System.properties['user.home'] +
					'/Development/projects/' + System.getenv()['CURRENT_PROJECT'] + '/software/tomcat-app/webapps/share.war'
			}
		}
	}
	
	void checkForWarExplodedDir(Project project) {
		if (!project.hasProperty('warExplodedDir') && System.getenv()['CURRENT_PROJECT'] != '') {
			if (project.name.contains('repo')) {
				project.ext.warExplodedDir = System.properties['user.home'] +
					'/Development/projects/' + System.getenv()['CURRENT_PROJECT'] + '/software/tomcat/webapps/alfresco'
			} else if (project.name.contains('share')) {
				project.ext.warExplodedDir = System.properties['user.home'] +
					'/Development/projects/' + System.getenv()['CURRENT_PROJECT'] + '/software/tomcat-app/webapps/share'
			}
		}
	}
	
}
