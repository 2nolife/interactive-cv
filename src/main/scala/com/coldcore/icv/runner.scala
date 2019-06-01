package com.coldcore.icv

import java.io.File

import core.{Defaults, Interceptor, Writer}
import link.{ConnectIterator, GraphIterator, Linker}
import org.apache.commons.io.FileUtils
import rdf.{TripleIterator, TripleReader}
import org.slf4j.LoggerFactory

object Runner {
  def main(args: Array[String]): Unit = new Runner(args.toSeq)
}

class Runner(val args: Seq[String]) {

  private val log = LoggerFactory.getLogger(classOf[Runner])
  private lazy val rdf: File = new File(args.head)

  private def validArgs: Option[String] =
    if (args.size != 1) Some("""Use: sbt "run <file_or_directory>" """)
    else if (!rdf.exists) Some("Not found: "+rdf.getAbsolutePath)
    else None

  private def validOutput: Option[String] = {
    val (dir, marker) = (Config.output_dir, new File(Config.output_dir, "marker.tmp"))
    if (dir.exists && !marker.exists) Some("Output directory may contain other data: "+dir.getAbsolutePath)
    else None
  }

  validArgs orElse validOutput map log.info getOrElse run()

  def run() {
    val files = if (rdf.isFile) rdf :: Nil else rdf.listFiles.filter(_.getName.endsWith(".nt")).toList
    log.info(s"Feed on ${files.size} files: "+rdf.getAbsolutePath)
    new Processor(files, Config.writers, Config.interceptors)
    log.info("Done! cd "+Config.output_dir.getAbsolutePath)
  }

}

class Processor(files: Seq[File], writers: Seq[Writer], interceptors: Seq[Interceptor]) {

  private val log = LoggerFactory.getLogger(classOf[Processor])
  private val (output, marker) = (Config.output_dir, new File(Config.output_dir, "marker.tmp"))

  Defaults.set()

  FileUtils.deleteDirectory(output)
  output.mkdirs()
  FileUtils.touch(marker)

  interceptors foreach before
  writers foreach write
  interceptors foreach after

  private def write(writer: Writer): Unit =
    withOpen( TripleReader(files) ) { reader =>
      withOpen( Linker(TripleIterator(reader)) ) { linker =>
        withOpen( writer ) { w =>
          log.debug("Using writer "+w.getClass.getName)
          w.write(GraphIterator(linker))
          w.connect(ConnectIterator(linker)) }}}

  private def before(interceptor: Interceptor): Unit = {
    log.debug("Using interceptor "+interceptor.getClass.getName)
    interceptor.before()
  }

  private def after(interceptor: Interceptor): Unit = {
    log.debug("Using interceptor "+interceptor.getClass.getName)
    interceptor.after()
  }

}
