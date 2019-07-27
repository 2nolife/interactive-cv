package com.coldcore.icv

import java.io.File

import core.{Interceptor, Writer}
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

object Config {

  private val conf = ConfigFactory.parseResources("overrides.conf")
    .withFallback(ConfigFactory.parseResources("defaults.conf"))
    .resolve()

  val foo: String = conf.getString("interactive-cv.foo")

  case class Schema(uri: String, nodes: Seq[String])
  val schema: Seq[Schema] =
    conf.getObjectList("interactive-cv.schema").asScala
      .map(_.toConfig)
      .map { s => Schema(s.getString("uri"), s.getStringList("nodes").asScala) }

  val neo_writer: Map[String,String] =
    conf.getStringList("interactive-cv.neo-writer").asScala
      .map(_.split("="))
      .map { case Array(k,v) => k.trim -> v.trim }.toMap

  val label_sameAs: String = conf.getString("interactive-cv.link-nodes.same-as")
  val label_moreLike: String = conf.getString("interactive-cv.link-nodes.more-like")
  val n_count: Int = conf.getInt("interactive-cv.link-nodes.n-count")

  val output_dir: File = new File(conf.getString("interactive-cv.output-dir"))

  lazy val writers: Seq[Writer] =
    conf.getStringList("interactive-cv.writers").asScala
      .map(Class.forName(_).newInstance.asInstanceOf[Writer])

  lazy val interceptors: Seq[Interceptor] =
    conf.getStringList("interactive-cv.interceptors").asScala
      .map(Class.forName(_).newInstance.asInstanceOf[Interceptor])

  val c3_dir: File = new File(conf.getString("interactive-cv.c3-writer.dir"))
  val c3_pattern: String = conf.getString("interactive-cv.c3-writer.pattern")

  lazy val c3_charts: Seq[writer.c3.chart.Chart] =
    conf.getStringList("interactive-cv.c3-writer.charts").asScala
      .map(Class.forName(_).newInstance.asInstanceOf[writer.c3.chart.Chart])
  val c3_chart_TimePerCompany_top: Int = conf.getInt("interactive-cv.c3-writer.chart-TimePerCompany.top")
  val c3_chart_TimePerTechnology_top: Int = conf.getInt("interactive-cv.c3-writer.chart-TimePerTechnology.top")

  val c4_dir: File = new File(conf.getString("interactive-cv.c4-interceptor.c4-dir"))
  val c4_c3_dir: File = new File(conf.getString("interactive-cv.c4-interceptor.c3-dir"))

  val haala_dir: File = new File(conf.getString("interactive-cv.haala-interceptor.dir"))

}
