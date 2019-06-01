package com.coldcore.icv
package interceptor.haala

import java.io.File

import core.FreemarkerUtil.fmTemplate
import core.Interceptor
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.util.Zip4jConstants
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

/** Package C3 output files as HAALA feed
  * https://bitbucket.org/nolife/haala-project
  */
class PackageC3 extends Interceptor {

  private val log = LoggerFactory.getLogger(classOf[PackageC3])

  override def after() {
    var dir = new File(Config.output_dir, "haala")
    val (dir_c3, file_icv, file_zip) = (
      new File(dir, "c3"),
      new File(dir, "icv-files.json"),
      new File(dir, "icv-feed.zip"))

    log.debug("Packaging C3 files")
    FileUtils.copyDirectory(new File(Config.output_dir, "c3"), dir_c3)

    val json = descriptor(dir_c3)
    FileUtils.write(file_icv, json, "UTF-8")
    //log.debug("Descriptor \n"+json) // uncomment to log descriptor

    val zipfile = new ZipFile(file_zip)
    val parameters = new ZipParameters
    parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE)
    parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA)
    zipfile.addFolder(dir_c3, parameters)
    zipfile.addFile(file_icv, parameters)

    log.debug("Cleaning")
    FileUtils.deleteDirectory(dir_c3)
    file_icv.delete()
  }

  private def descriptor(input: File): String = {
    val content = ondir(root = input, dir = input).trim
    fmTemplate(
      new File(Config.haala_dir, "descriptor.json.ftl").getPath,
      Map("content" -> content))
  }

  private def ondir(root: File, dir: File): String = {
    val subDir = if (root == dir) "" else dir.getAbsolutePath.substring(root.getAbsolutePath.length+1)
    val files = dir.listFiles.toSeq.filter(_.isFile).filter(_.getName.head != '.') // .DS_Store
    val s =
      if (files.isEmpty) ""
      else "{ "+(if (root == dir) "" else s""" "subDir": "$subDir", "subFolder": "$subDir", """)+""" "files": [ """+"\n"+
           files.map(file => "     "+s""" "${file.getName}" """.trim).mkString(", \n")+" ]}"
    (s :: dir.listFiles.toList.filter(_.isDirectory).map(ondir(root, _))).filter("" !=).mkString(", \n\n")
  }

}
