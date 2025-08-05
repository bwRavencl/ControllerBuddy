import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.code
import kotlinx.html.h1
import kotlinx.html.html
import kotlinx.html.p
import kotlinx.html.stream.createHTML
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.tr
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.w3c.dom.Element
import org.w3c.dom.NodeList

plugins {
  application
  id("com.diffplug.spotless") version "7.2.1"
  id("com.github.spotbugs") version "6.2.3"
  id("net.ltgt.errorprone") version "4.3.0"
  id("org.gradlex.extra-java-module-info") version "1.13"
}

buildscript { dependencies { classpath("org.jetbrains.kotlinx:kotlinx-html-jvm:0.12.0") } }

val versionProvider =
    providers
        .exec { commandLine("git", "describe", "--long", "--dirty=.dirty") }
        .standardOutput
        .asText
        .map { it.trim().replaceFirst("-", ".").replaceFirst("-g", "-") }

version = versionProvider.get()

base { archivesName = application.applicationName.lowercase() }

application {
  mainModule = "de.bwravencl.controllerbuddy"
  mainClass = "de.bwravencl.controllerbuddy.gui.Main"
}

val javaLanguageVersion = JavaLanguageVersion.of(24)

val launcher = javaToolchains.launcherFor { languageVersion = javaLanguageVersion }
val javaHome = launcher.map { it.metadata.installationPath }

val os = DefaultNativePlatform.getCurrentOperatingSystem()!!

val moduleInfoFile = "$projectDir/src/main/java/module-info.java"
val constantsFile =
    "$projectDir/src/main/java/de/bwravencl/controllerbuddy/constants/Constants.java"

val resourcesDir = "$projectDir/src/main/resources"
val tmpDir = layout.buildDirectory.dir("tmp")
val runtimeDir = tmpDir.map { it.dir("runtime") }

val mainModule: String = project.application.mainModule.get()
val commonJvmArgs =
    listOf("-XX:+UseSerialGC", "-Xms96m", "-Xmx96m", "--enable-native-access=$mainModule")
val windowsJvmArgs =
    listOf(
        "--add-opens=java.desktop/java.awt=$mainModule",
        "--add-opens=java.desktop/sun.awt.windows=$mainModule")
val linuxJvmArgs =
    listOf(
        "--add-opens=java.desktop/sun.awt=$mainModule",
        "--add-opens=java.desktop/sun.awt.X11=$mainModule")

val gamecontrollerdbGitFile = "$projectDir/SDL_GameControllerDB/gamecontrollerdb.txt"
val gamecontrollerdbResFile = "$resourcesDir/gamecontrollerdb.txt"

val arch: Architecture = DefaultNativePlatform.getCurrentArchitecture()
val distAppendix = "${os.toFamilyName()}-${arch.name}"

repositories {
  mavenCentral()
  maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
}

java {
  toolchain {
    languageVersion = javaLanguageVersion
    vendor = JvmVendorSpec.AZUL
  }
}

dependencies {
  errorprone("com.google.errorprone:error_prone_core:2.41.0")
  spotbugs("com.github.spotbugs:spotbugs:4.9.3")

  val dbusJavaVersion = "5.1.1"
  val lwjglVersion = "3.4.0-SNAPSHOT"

  val lwjglOs =
      when {
        os.isWindows -> "windows"
        os.isMacOsX -> "macos"
        os.isLinux -> "linux"
        else -> throw GradleException("Unsupported operating system ${os.displayName}")
      }

  val lwjglArch =
      when {
        Architectures.X86.isAlias(arch.name) && os.isWindows -> "x86"
        Architectures.X86_64.isAlias(arch.name) -> ""
        Architectures.ARM_V7.isAlias(arch.name) && os.isLinux -> "arm32"
        Architectures.AARCH64.isAlias(arch.name) -> "arm64"
        else -> throw GradleException("Unsupported system architecture ${arch.displayName}")
      }

  val lwjglPlatform = "$lwjglOs${if (lwjglArch.isEmpty()) "" else "-"}$lwjglArch"

  implementation("commons-cli:commons-cli:1.10.0")
  implementation("com.formdev:flatlaf:3.6.1")
  implementation("com.github.hypfvieh:dbus-java-core:$dbusJavaVersion") {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:$dbusJavaVersion") {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  implementation("com.google.code.gson:gson:2.13.1")
  implementation("io.github.classgraph:classgraph:4.8.180")
  implementation("org.apache.xmlgraphics:batik-swing:1.19") {
    exclude(group = "xml-apis", module = "xml-apis")
  }
  implementation("org.lwjgl:lwjgl:$lwjglVersion")
  implementation("org.lwjgl:lwjgl:$lwjglVersion:natives-$lwjglPlatform")
  implementation("org.lwjgl:lwjgl-sdl:$lwjglVersion")
  implementation("org.lwjgl:lwjgl-sdl:$lwjglVersion:natives-$lwjglPlatform")
  implementation("org.slf4j:slf4j-jdk14:2.0.17")
}

extraJavaModuleInfo {
  deriveAutomaticModuleNamesFromFileNames = true
  failOnMissingModuleInfo = false
}

spotless {
  encoding("UTF-8")
  java {
    target("src/main/java/de/bwravencl/**/*.java")
    targetExclude(
        moduleInfoFile.removePrefix("$projectDir/"), constantsFile.removePrefix("$projectDir/"))
    eclipse("4.33").configFile("spotless.eclipseformat.xml")
    formatAnnotations()
    cleanthat()
        .sourceCompatibility(
            project.extensions.getByType(JavaPluginExtension::class).sourceCompatibility.toString())
        .addMutators(listOf("SafeButNotConsensual", "SafeButControversial"))
    importOrderFile("spotless.importorder")
    removeUnusedImports()
    removeWildcardImports()
    licenseHeader(
        $$"""
			/* Copyright (C) $YEAR  Matteo Hausner
			 *
			 * This program is free software: you can redistribute it and/or modify
			 * it under the terms of the GNU General Public License as published by
			 * the Free Software Foundation, either version 3 of the License, or
			 * (at your option) any later version.
			 *
			 * This program is distributed in the hope that it will be useful,
			 * but WITHOUT ANY WARRANTY; without even the implied warranty of
			 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
			 * GNU General Public License for more details.
			 *
			 * You should have received a copy of the GNU General Public License
			 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
			 */


            """
            .trimIndent())
    endWithNewline()
  }
  kotlinGradle {
    target("*.gradle.kts")
    ktfmt("0.56")
    endWithNewline()
  }
  format("xml") {
    target("*.svg")
    eclipseWtp(EclipseWtpFormatterStep.XML)
  }
  format("newlineAndTrailingWhitespace") {
    target(".github/**/*.yml", "$resourcesDir/**/*.properties", "$resourcesDir/**/*.svg")
    endWithNewline()
    trimTrailingWhitespace()
  }
  format("onlyNewline") {
    target("LICENSE", "*.gitignore", "*.md", "*.txt", "$resourcesDir/**/*.txt")
    targetExclude(gamecontrollerdbResFile)
    endWithNewline()
  }
}

spotbugs {
  baselineFile = file("spotbugs-baseline.xml")
  effort = Effort.valueOf("MAX")
  extraArgs = listOf("-maxRank", "20")
  onlyAnalyze = listOf("de.bwravencl.-")
  reportLevel = Confidence.valueOf("LOW")
}

tasks.register<Delete>("cleanConstants") {
  description = "Removes the '$constantsFile' source file"
  delete(constantsFile)
}

tasks.register<Delete>("cleanGameControllerDB") {
  description = "Removes the 'gamecontrollerdb.txt' file from the '$resourcesDir' directory"
  delete(gamecontrollerdbResFile)
}

tasks.register<Delete>("cleanLibsDirectory") {
  description = "Removes the '${base.libsDirectory.get()}' directory"
  delete(base.libsDirectory)
}

tasks.register<Delete>("cleanModuleInfo") {
  description = "Removes the '$moduleInfoFile' source file"
  delete(moduleInfoFile)
}

tasks.register<Delete>("cleanRuntimeDir") {
  description = "Removes the '${runtimeDir.get()}' directory"
  delete(runtimeDir)
}

tasks.register<Delete>("cleanTmpProjectDir") {
  val tmpProjectFile = tmpDir.get().file(project.name)
  description = "Removes the '$tmpProjectFile}' directory"
  delete(tmpProjectFile)
}

tasks.named("clean") {
  dependsOn(
      "cleanConstants",
      "cleanGameControllerDB",
      "cleanLibsDirectory",
      "cleanModuleInfo",
      "cleanRuntimeDir",
      "cleanTmpProjectDir")
}

data class Coordinate(
    val group: String? = null,
    val artifactId: String? = null,
    val version: String? = null
) : Comparable<Coordinate> {
  override fun compareTo(other: Coordinate): Int {
    group?.compareTo(other.group ?: "")?.let { if (it != 0) return it }
    artifactId?.compareTo(other.artifactId ?: "")?.let { if (it != 0) return it }
    return version?.compareTo(other.version ?: "") ?: 0
  }

  override fun toString(): String = listOfNotNull(group, artifactId, version).joinToString(":")
}

data class DependencyMetadata(
    val coordinate: Coordinate,
    val file: File,
    val licenses: MutableList<License> = mutableListOf()
)

data class License(val name: String, val url: String)

fun buildNoLicenseException(coordinate: Coordinate): GradleException =
    GradleException("No license metadata found for dependency: $coordinate")

fun getLicensesForDependency(
    file: File,
    coordinate: Coordinate,
    initialCoordinate: Coordinate = coordinate
): DependencyMetadata {
  val aliases: Map<License, List<Any>> =
      mapOf(
          License("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0") to
              listOf(
                  "Apache-2.0",
                  "Apache License, Version 2.0",
                  "The Apache License, Version 2.0",
                  "The Apache Software License, Version 2.0"),
          License("MIT", "https://opensource.org/license/mit/") to
              listOf("MIT", "MIT License", "The MIT License (MIT)"))
  val dependency = project.dependencies.create("$coordinate@pom")
  val pomConfiguration = project.configurations.detachedConfiguration(dependency)
  val pomFile: File =
      try {
        pomConfiguration.resolve().first()
      } catch (_: ResolveException) {
        throw buildNoLicenseException(coordinate)
      }
  val docBuilder =
      DocumentBuilderFactory.newInstance()
          .apply {
            isNamespaceAware = true
            isValidating = false
          }
          .newDocumentBuilder()
  val document = docBuilder.parse(pomFile)
  val dependencyMetadata = DependencyMetadata(initialCoordinate, file)
  val licensesNodes = document.getElementsByTagName("license")
  for (i in 0 until licensesNodes.length) {
    val licenseElement = licensesNodes.item(i) as? Element ?: continue
    val name = licenseElement.getChildText("name")?.trim() ?: ""
    val url = licenseElement.getChildText("url")?.trim() ?: ""
    var license = License(name, url)
    val aliasEntry =
        aliases.entries.find { (_, aliasNames) ->
          aliasNames.any { aliasElem ->
            when (aliasElem) {
              is String -> aliasElem == license.name
              is License -> aliasElem == license
              else -> false
            }
          }
        }
    if (aliasEntry != null) {
      license = aliasEntry.key
    }
    dependencyMetadata.licenses.add(license)
  }
  if (dependencyMetadata.licenses.isNotEmpty()) {
    return dependencyMetadata
  }
  val parentNodes = document.getElementsByTagName("parent")
  if (parentNodes.length > 0) {
    val parentElement = parentNodes.item(0) as? Element
    val parentGroupId = parentElement?.getChildText("groupId")?.trim()
    val parentArtifactId = parentElement?.getChildText("artifactId")?.trim()
    val parentVersion = parentElement?.getChildText("version")?.trim()

    if (!parentGroupId.isNullOrEmpty() &&
        !parentArtifactId.isNullOrEmpty() &&
        !parentVersion.isNullOrEmpty()) {
      val parentCoordinate = Coordinate(parentGroupId, parentArtifactId, parentVersion)
      return getLicensesForDependency(file, parentCoordinate, initialCoordinate)
    }
  }
  throw buildNoLicenseException(initialCoordinate)
}

private fun Element.getChildText(tagName: String): String? {
  val nodeList: NodeList = this.getElementsByTagName(tagName)
  if (nodeList.length == 0) return null
  return nodeList.item(0)?.textContent
}

val hardcodedDependencyMetadataSet: Set<DependencyMetadata> =
    setOf(
        DependencyMetadata(
            Coordinate(artifactId = "SDL_GameControllerDB"),
            file = file(gamecontrollerdbGitFile),
            listOf(
                    License(
                        "zlib License",
                        "https://raw.githubusercontent.com/mdqinc/SDL_GameControllerDB/master/LICENSE"))
                .toMutableList()))

val dependencyMetadataSet: Set<DependencyMetadata> =
    hardcodedDependencyMetadataSet +
        configurations.runtimeClasspath
            .get()
            .resolvedConfiguration
            .resolvedArtifacts
            .map { resolvedArtifact ->
              val coordinate =
                  Coordinate(
                      group = resolvedArtifact.moduleVersion.id.group,
                      artifactId = resolvedArtifact.moduleVersion.id.name,
                      version = resolvedArtifact.moduleVersion.id.version)
              getLicensesForDependency(resolvedArtifact.file, coordinate)
            }
            .toSet()

tasks.register("generateConstants") {
  description = "Generates the '$constantsFile' source file"
  mustRunAfter("cleanConstants")
  val licenseFile = layout.projectDirectory.file("LICENSE")
  inputs.file(licenseFile)
  val constantsFile = file(constantsFile)
  outputs.file(constantsFile)
  val licenseLines = licenseFile.asFile.readLines().map { it.trim() }
  val applicationName = application.applicationName
  val version = versionProvider.get()
  val licensesHtml =
      createHTML(false).html {
        body {
          h1 { +"$applicationName License:" }
          p {
            licenseLines.forEach { line ->
              +line
              br()
            }
          }
          br()
          h1 { +"Third-Party Licenses:" }
          table {
            attributes["border"] = "1"
            attributes["cellpadding"] = "10"
            tr {
              th {
                attributes["align"] = "left"
                +"Dependency"
              }
              th {
                attributes["align"] = "left"
                +"License"
              }
              th {
                attributes["align"] = "left"
                +"Link"
              }
            }
            dependencyMetadataSet
                .sortedBy { it.coordinate }
                .forEach { entry ->
                  entry.licenses
                      .sortedBy { it.name }
                      .forEach { license ->
                        tr {
                          td {
                            +entry.coordinate.toString()
                            br
                            code { +entry.file.name }
                          }
                          td { +license.name }
                          td {
                            license.url
                                .takeIf { it.isNotBlank() }
                                ?.let { url -> a(href = url) { +"Open" } }
                          }
                        }
                      }
                }
          }
        }
      }
  doLast {
    constantsFile.parentFile.mkdirs()
    constantsFile.writeText(
        """
            package de.bwravencl.controllerbuddy.constants;

            public class Constants {

            	public static final String APPLICATION_NAME = "$applicationName";

            	public static final long BUILD_TIMESTAMP = ${System.currentTimeMillis()}L;

            	public static final String LICENSES_HTML;

            	public static final String VERSION = "$version";

            	static {
            		// noinspection HttpUrlsUsage
            		LICENSES_HTML = "${licensesHtml.replace("\"", "\\\"")}";
            	}
            }

            """
            .trimIndent(),
        StandardCharsets.UTF_8)
  }
}

tasks.register("generateModuleInfo") {
  description = "Generates the '$moduleInfoFile' source file"
  mustRunAfter("cleanModuleInfo")
  val moduleInfoFile = file(moduleInfoFile)
  outputs.file(moduleInfoFile)
  doLast {
    moduleInfoFile.writeText(
        """
            @SuppressWarnings({"requires-automatic", "Java9RedundantRequiresStatement"})
            module de.bwravencl.controllerbuddy {
                exports de.bwravencl.controllerbuddy.gui;

                opens de.bwravencl.controllerbuddy.input to com.google.gson;
                opens de.bwravencl.controllerbuddy.input.action to com.google.gson;

                requires com.google.gson;
                requires com.formdev.flatlaf;
                requires io.github.classgraph;
                requires transitive java.desktop;
                requires java.logging;
                requires java.prefs;
                requires jdk.xml.dom;
                requires org.apache.commons.cli;
                requires org.apache.xmlgraphics.batik.anim;
                requires org.apache.xmlgraphics.batik.bridge;
                requires org.apache.xmlgraphics.batik.constants;
                requires org.apache.xmlgraphics.batik.dom;
                requires org.apache.xmlgraphics.batik.util;
                requires org.apache.xmlgraphics.batik.swing;
                requires org.freedesktop.dbus;
                requires org.lwjgl;
                requires org.lwjgl.natives;
                requires org.lwjgl.sdl;
                requires org.lwjgl.sdl.natives;
                requires xml.apis.ext;
            }

            """
            .trimIndent(),
        StandardCharsets.UTF_8)
  }
}

tasks.withType<JavaCompile>().configureEach {
  dependsOn("generateModuleInfo", "generateConstants")
  mustRunAfter("cleanLibsDirectory")
  options.encoding = "UTF-8"
  gradle.taskGraph.whenReady {
    if (hasTask("jpackage")) {
      options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-preview", "-Werror"))
      options.isDeprecation = true
      options.errorprone.error("MissingBraces")
    } else {
      options.errorprone.isEnabled = false
    }
  }
}

tasks.named<JavaExec>("run") {
  jvmArgs(commonJvmArgs)
  if (os.isWindows) {
    jvmArgs(windowsJvmArgs)
  } else if (os.isLinux) {
    jvmArgs(linuxJvmArgs)
  }
}

tasks.register("checkGameControllerDBSubmodule") {
  description = "Checks that the SDL_GameControllerDB submodule is checked out"
  val gamecontrollerdbGitFile = file(gamecontrollerdbGitFile)
  inputs.file(gamecontrollerdbGitFile)
  doLast {
    if (!gamecontrollerdbGitFile.exists()) {
      throw GradleException("SDL_GameControllerDB submodule not checked out")
    }
  }
}

tasks.register<Copy>("copyGameControllerDB") {
  description = "Places a copy of '$gamecontrollerdbGitFile' into the '$resourcesDir' directory"
  dependsOn("checkGameControllerDBSubmodule")
  mustRunAfter("cleanGameControllerDB")
  from(gamecontrollerdbGitFile)
  into(resourcesDir)
}

tasks.named("processResources") { dependsOn("copyGameControllerDB") }

tasks.register<Copy>("copyLibs") {
  description = "Copies all jar files into a directory called \'libs\' inside the build directory"
  dependsOn("cleanLibsDirectory", "jar")
  from(configurations.runtimeClasspath)
  into(base.libsDirectory)
}

tasks.named<SpotBugsTask>("spotbugsMain") {
  reports {
    create("html").required = true
    create("xml").required = true
  }
}

tasks.named("spotbugsTest") { enabled = false }

tasks.named("spotlessNewlineAndTrailingWhitespace") { dependsOn("copyGameControllerDB") }

tasks.named("spotlessOnlyNewline") { dependsOn("copyGameControllerDB") }

tasks.named("spotlessXml") { dependsOn("copyGameControllerDB") }

tasks.named("test") { enabled = false }

tasks.register<Exec>("jlink") {
  description =
      "Executes the jlink command to create a customized minimal Java runtime inside the build directory"
  dependsOn("check", "jar", "cleanRuntimeDir")
  commandLine(
      "${javaHome.get()}/bin/jlink",
      "--output",
      runtimeDir.get(),
      "--strip-debug",
      "--no-header-files",
      "--no-man-pages",
      "--strip-native-commands",
      "--add-modules",
      "java.desktop,java.management,jdk.unsupported,java.logging,jdk.accessibility,jdk.net,jdk.security.auth,jdk.xml.dom")
}

tasks.register("customizeLoggingProperties") {
  description =
      "Alters the default 'logging.properties' configuration file of the Java runtime to include a FileHandler that logs to a logfile in the system's TEMP directory using SimpleFormatter with custom formatting"
  dependsOn("jlink")
  val loggingPropertiesFile = runtimeDir.get().file("conf/logging.properties")
  val projectName = project.name
  doLast {
    ant.withGroovyBuilder {
      "propertyfile"("file" to loggingPropertiesFile) {
        "entry"(
            "key" to "handlers",
            "value" to "java.util.logging.FileHandler, java.util.logging.ConsoleHandler")
        "entry"("key" to "java.util.logging.FileHandler.pattern", "value" to "%t/$projectName.log")
        "entry"(
            "key" to "java.util.logging.FileHandler.formatter",
            "value" to "java.util.logging.SimpleFormatter")
        "entry"(
            "key" to "java.util.logging.SimpleFormatter.format",
            "value" to $$"[%1$tY-%1$tm-%1$td %1$tk:%1$tM:%1$tS:%1$tL] %3$s: %5$s%6$s%n")
      }
    }
  }
}

tasks.register<Exec>("jpackage") {
  description =
      "Executes the jpackage command to create a standalone application image packaged with a custom minimal Java runtime"
  dependsOn("copyLibs", "customizeLoggingProperties", "cleanTmpProjectDir")
  val commandLineParts =
      mutableListOf(
          "${javaHome.get()}/bin/jpackage",
          "--dest",
          tmpDir.get(),
          "--type",
          "app-image",
          "--name",
          project.name,
          "--runtime-image",
          runtimeDir.get(),
          "--module-path",
          base.libsDirectory.get(),
          "--module",
          "${project.application.mainModule.get()}/${project.application.mainClass.get()}",
          "--app-version",
          versionProvider.map { it.substringBefore("-") }.get(),
          "--icon",
          "$projectDir/icon.${if (os.isWindows) "ico" else "png"}",
          "--copyright",
          "Copyright ${SimpleDateFormat("yyyy").format(Date())} Matteo Hausner",
          "--vendor",
          "Matteo Hausner",
          "--verbose")
  var jvmArgs = commonJvmArgs.toMutableList()
  if (os.isWindows) {
    jvmArgs += windowsJvmArgs
  } else if (os.isLinux) {
    jvmArgs += linuxJvmArgs
  }
  jvmArgs.forEach { commandLineParts.addAll(listOf("--java-options", it)) }
  commandLine(commandLineParts)
}

tasks.named("startScripts") { enabled = false }

tasks.named<Tar>("distTar") {
  dependsOn("jpackage")
  from(tmpDir)
  include("${project.name}${if (os.isMacOsX) ".app" else ""}/**")
  archiveAppendix = distAppendix
  compression = Compression.GZIP
  isPreserveFileTimestamps = true
  @Suppress("UnstableApiUsage") useFileSystemPermissions()
}

tasks.named<Zip>("distZip") {
  dependsOn("jpackage")
  from(tmpDir)
  include("${project.name}${if (os.isMacOsX) ".app" else ""}/**")
  archiveAppendix = distAppendix
  isPreserveFileTimestamps = true
  @Suppress("UnstableApiUsage") useFileSystemPermissions()
}

tasks.named<Sync>("installDist") {
  dependsOn("jpackage")
  from(tmpDir)
  into(layout.buildDirectory.dir("install"))
  include("${project.name}${if (os.isMacOsX) ".app" else ""}/**")
}
