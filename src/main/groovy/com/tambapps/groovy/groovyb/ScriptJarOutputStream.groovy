package com.tambapps.groovy.groovyb

import groovy.transform.CompileStatic

import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

@CompileStatic
class ScriptJarOutputStream extends JarOutputStream {

  private static Manifest generateManifest(File scriptClassFile) {
    Manifest manifest = new Manifest()
    Attributes mainAttributes = manifest.getMainAttributes()
    mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
    mainAttributes[Attributes.Name.MAIN_CLASS] = scriptClassFile.getName().substring(0, scriptClassFile.getName().indexOf('.'))
    mainAttributes[new Attributes.Name("Created-By")] = "Tambapps"
    mainAttributes[new Attributes.Name("Build-Jdk")] = System.getProperty("java.version")
    return manifest
  }

  private final File scriptClassFile

  ScriptJarOutputStream(File outputFile, File scriptClassFile) throws IOException {
    super(new FileOutputStream(outputFile), generateManifest(scriptClassFile))
    this.scriptClassFile = scriptClassFile
  }

  void write() throws IOException {
    JarEntry entry = new JarEntry(scriptClassFile.getName())
    entry.setTime(scriptClassFile.lastModified())

    putNextEntry(entry)
    writeScriptFile()
    closeEntry()
    flush()
  }

  private void writeScriptFile() throws IOException {
    byte[] buf = new byte[8192]
    int count
    try (InputStream inputStream = new FileInputStream(scriptClassFile)) {
      while(-1 != (count = inputStream.read(buf))) {
        write(buf, 0, count)
      }
    }
  }
}
