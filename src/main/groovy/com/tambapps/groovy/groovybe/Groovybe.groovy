package com.tambapps.groovy.groovybe

import com.tambapps.groovy.groovybe.arguments.Arguments
import com.tambapps.groovy.groovybe.arguments.OutputType
import com.tambapps.groovy.groovybe.io.SourceDependencyGrabber
import com.tambapps.groovy.groovybe.io.GroovyCompiler
import com.tambapps.groovy.groovybe.io.GroovyDepsFetcher
import com.tambapps.groovy.groovybe.io.Jpackage
import com.tambapps.groovy.groovybe.io.stream.JarMergingOutputStream
import com.tambapps.groovy.groovybe.util.Utils

import java.nio.file.Path

Arguments arguments = Arguments.parseArgs(args)
if (!arguments) {
  return
}

File tempDir = File.createTempDir('groovybe')

try {
  GroovyDepsFetcher groovyDepsFetcher = new GroovyDepsFetcher()
  SourceDependencyGrabber sourceDependencyGrabber = new SourceDependencyGrabber()

  // extract @Grab artifacts if any
  File transformedScriptFile = new File(tempDir, arguments.scriptFile.name)
  transformedScriptFile.text = sourceDependencyGrabber.transform(arguments.scriptFile.readLines())

  // Fetch dependencies. They will constitute the classpath used for compilation
  List<File> dependencyJars =
      groovyDepsFetcher.fetch(arguments.version, arguments.subProjects,
          sourceDependencyGrabber.grabbedArtifacts) + arguments.additionalJars

  // compile class
  GroovyCompiler compiler = new GroovyCompiler(tempDir, dependencyJars)
  File classFile = compiler.compile(transformedScriptFile)
  String className = Utils.nameWithExtension(classFile, '')

  // compile executable jar
  File jarWithDependencies = new File(tempDir, "${className}-exec.jar")
  try (JarMergingOutputStream os = new JarMergingOutputStream(new FileOutputStream(jarWithDependencies), className)) {
    os.writeClass(classFile)
    for (dependencyJar in dependencyJars) {
      os.writeJar(dependencyJar)
    }
    os.flush()
  }

  File outputFile
  // now export to provided format
  switch (arguments.outputType) {
    case OutputType.JAR:
      outputFile = new File(arguments.outputDir, jarWithDependencies.name)
      jarWithDependencies.renameTo(outputFile)
      break
    case OutputType.APPIMAGE:
      Jpackage jpackage = arguments.jpackageFile != null ? new Jpackage(arguments.jpackageFile)
          : Jpackage.newInstance()
      outputFile = jpackage.run(tempDir, jarWithDependencies, className, arguments.outputDir)
      break
    default:
      return
  }

  Path normalizedPath = outputFile.toPath().toAbsolutePath().normalize()
  if (outputFile.directory) {
    println "Files were generated in $normalizedPath"
  } else {
    println "$normalizedPath was generated"
  }
} finally {
  // cleaning
  tempDir.deleteDir()
}
