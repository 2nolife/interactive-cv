package com.coldcore.icv

package writer.c4.chart {

  import core.Triple._

  class Neovis extends writer.c3.chart.Chart {
    override val template: String = "neovis.ftl"

    override def build(career: writer.c3.model.Career, person: PersonNode): Map[String, Any] =
      Map("js_person" -> person.id)
  }

}

package interceptor.c4 {

  import java.io.File

  import com.coldcore.icv.core.Interceptor
  import org.apache.commons.io.FileUtils
  import org.slf4j.LoggerFactory

  /** Copy C3 and C4 resources */
  class Default extends Interceptor {

    private val log = LoggerFactory.getLogger(classOf[Default])

    override def before() {
      log.debug("Copying resources")
      var (dir_web, dir_templates) = (
        new File(Config.output_dir, "c4-resources/web"),
        new File(Config.output_dir, "c4-resources/templates"))
      FileUtils.copyDirectory(new File(Config.c4_c3_dir, "web"), dir_web)
      FileUtils.copyDirectory(new File(Config.c4_c3_dir, "templates"), dir_templates)
      FileUtils.copyDirectory(new File(Config.c4_dir, "web"), dir_web)
      FileUtils.copyDirectory(new File(Config.c4_dir, "templates"), dir_templates)
    }

  }

}
