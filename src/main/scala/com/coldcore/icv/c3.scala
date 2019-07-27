package com.coldcore.icv
package writer.c3

import java.io.File
import java.text.SimpleDateFormat
import java.util.{Calendar, Date, GregorianCalendar}

import core.{Graph, JsonUtil, Writer}
import core.Triple._
import core.FreemarkerUtil._
import core.Graph.{Arc, Route, Verticle}
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/** Write C3 charts as JS and HTML files */
class C3Writer extends Writer {

  private val log = LoggerFactory.getLogger(classOf[C3Writer])
  private val dir = new File(Config.output_dir, "c3")
  private val (gparser, cwriter) = (new GraphParser, new ChartWriter(dir))

  override def write(iterator: Iterator[Graph]) {
    dir.mkdir()

    iterator foreach { graph =>
      graph.verticle.node match {
        case node: PersonNode =>
          log.debug("Graph for "+graph.verticle.node)
          gparser.parseCareer(graph) foreach cwriter.write(node)
        case node => log.warn("Unsupported node: "+node)
      }
    }

    log.debug("Copying web resources")
    FileUtils.copyDirectory(new File(Config.c3_dir, "web"), dir)
  }

  override def connect(iterator: Iterator[Graph.Arc]) {
    // not supported, isolated graphs only
  }

}

/** Create charts with Freemarker templates and Chart classes */
private class ChartWriter(dir: File) {

  private val log = LoggerFactory.getLogger(classOf[ChartWriter])
  private val now = new SimpleDateFormat("dd/MM/yyyy").format(new Date)

  def write(person: PersonNode)(career: model.Career) {
    val js = Config.c3_charts.map { chart =>
      log.debug("Chart "+chart.getClass.getName)
      fmTemplate(new File(Config.c3_dir, "templates/"+chart.template).getPath, chart.build(career, person))
    }.mkString("\n\n")

    val pid = pattern(person)
    val html = fmTemplate(
      new File(Config.c3_dir, "templates/charts.html.ftl").getPath,
      Map(
        "html_title" -> person.value,
        "html_person" -> person.id,
        "html_person_id" -> pid,
        "html_generated" -> now))

    FileUtils.write(new File(dir, pid+".js"), js, "UTF-8")
    FileUtils.write(new File(dir, pid+".html"), html, "UTF-8")
  }

  private def pattern(p: PersonNode): String = {
    def transform(v: String, f: String => String => Boolean): String =
      Config.c3_pattern match {
        case s if f(s)("lowercase") => v.toLowerCase
        case s if f(s)("uppercase") => v.toUpperCase
        case _ => v
      }

    val (first, last) = p.id.split("#") match {
      case Array(a, b, _*) => (a, b)
      case Array(a) => (a, "")
    }
    val name = transform(first, _.startsWith)
    val user = transform(last, _.endsWith)
    val separator = Config.c3_pattern.dropWhile(_.toString.matches("[a-z]")).take(1)
    Seq(name, user).filter("" !=).mkString(separator)
  }

}

/** Extract data from graph which is then used by Chart classes */
private class GraphParser {

  private val log = LoggerFactory.getLogger(classOf[GraphParser])

  def parseCareer(graph: Graph): Option[model.Career] = {
    log.debug("Building career model")
    new model.CareerBuilder().build(graph.arc) match {
      case career @ Some(model.Career(companies))  =>
        log.debug(s"Career model: ${companies.size} companies")
        career
      case _ =>
        log.warn(s"Career model is unavailable")
        None
    }
  }

}

/** Graph parser extractors */
private object Extractor {
  object CareerArc { // blank(career) -> ...
    def unapplySeq(arc: Arc): Option[Seq[Route]] = arc match {
      case Arc(Verticle(BlankNode(_, "career")), routes) => Some(routes)
      case _ => None
    }
  }
  object CompanyArc { // Company -> ...
    def unapply(arc: Arc): Option[(String, Seq[Route])] = arc match {
      case Arc(Verticle(CompanyNode(value)), routes) => Some(value, routes)
      case _ => None
    }
  }
  object CompanyRoute { // label(company) -> Company -> ...
    def unapply(route: Route): Option[Arc] = route match {
      case Route("company", arc @ CompanyArc(_, _)) => Some(arc)
      case _ => None
    }
  }
  object AssignmentArc { // blank(assignment) -> ...
    def unapplySeq(arc: Arc): Option[Seq[Route]] = arc match {
      case Arc(Verticle(BlankNode(_, "assignment")), routes) => Some(routes)
      case _ => None
    }
  }
  object AssignmentRoute { // label(assignment) -> blank(assignment) -> ...
    def unapply(route: Route): Option[Arc] = route match {
      case Route("assignment", arc @ AssignmentArc(_*)) => Some(arc)
      case _ => None
    }
  }
  object StartDateRoute { // label(start year/month/day) -> value
    def unapply(route: Route): Boolean = route match {
      case Route("start year" | "start month"| "start day", Verticle(DateNode(_, _, _))) => true
      case _ => false
    }
  }
  object TechnologyRoute { // label(technology) -> Technology ( -> same as )
    def unapply(route: Route): Option[String] = route match {
      case Route("uses", Verticle(TechnologyNode(value))) => Some(value)
      case Route("uses", Arc(Verticle(TechnologyNode(value)), _)) => Some(value)
      case _ => None
    }
  }
}

/** JSON serializers used in Freemarker templates */
object ChartJson {
  class Columns(data: Array[Array[Any]]) {
    def json: String = JsonUtil.toJson(data, classOf[Array[Array[Any]]])
  }
  class Groups(data: Array[Array[String]]) {
    def json: String = JsonUtil.toJson(data, classOf[Array[Array[String]]])
  }
  class Colors(data: Map[String,String]) {
    def json: String = JsonUtil.toJson(data.asJava, classOf[JMap[String,String]])
  }
  class LegendHide(data: Array[String]) {
    def json: String = JsonUtil.toJson(data, classOf[Array[String]])
  }
}

object Util {
  def months(date1: Date, date2: Date, inclusive: Boolean = true): Int = {
    val (c1, c2) = (new GregorianCalendar, new GregorianCalendar)
    c1.setTime(date1)
    c2.setTime(date2)
    val y = c1.get(Calendar.YEAR)-c2.get(Calendar.YEAR)
    val m = c1.get(Calendar.MONTH)-c2.get(Calendar.MONTH)
    math.abs(y*12+m)+(if (inclusive) 1 else -1)
  }
}

/** Model and builders used by Chart classes */
package model {

  case class Career(companies: Seq[Company])
  case class Company(name: String, assignments: Seq[Assignment])
  case class Assignment(start: Date, end: Date, technologies: Seq[String])

  class CareerBuilder {

    private val log = LoggerFactory.getLogger(classOf[CareerBuilder])

    def build(input: Arc): Option[model.Career] =
      input match {
        case Extractor.CareerArc(routes @ _*)
          if routes.collectFirst { case Extractor.CompanyRoute(_) => }.isDefined =>

          val companies: Seq[model.Company] =
            routes.collect { case Extractor.CompanyRoute(arc) => arc }.flatMap(new CompanyBuilder(_).company)

          if (companies.isEmpty) {
            log.warn("Not enough data for career model")
            None
          } else {
            Some(model.Career(companies))
          }

        case Arc(v, routes) => routes.collect { case Route(_, arc @ Arc(_, _)) => build(arc) }.flatten.headOption
        case _ => None // ignore
      }

  }

  private class CompanyBuilder(input: Arc) {

    private val log = LoggerFactory.getLogger(classOf[CompanyBuilder])

    val company: Option[model.Company] = input match {
      case Extractor.CompanyArc(companyName, routes)
        if routes.collectFirst { case Extractor.AssignmentRoute(_) => }.isDefined =>

        val assignments: Seq[model.Assignment] =
          routes.collect { case Extractor.AssignmentRoute(arc) => arc }.flatMap(new AssignmentBuilder(_).assignment)

        val name: String =
          routes.collectFirst { case Route("name", Verticle(ValueNode(value))) => value } getOrElse companyName

        if (assignments.isEmpty) {
          log.warn("Not enough data: "+input)
          None
        } else {
          Some(model.Company(name, assignments))
        }

      case _ =>
        log.warn("Unsupported combination: "+input)
        None
    }

  }

  private class AssignmentBuilder(input: Arc) {

    private val log = LoggerFactory.getLogger(classOf[AssignmentBuilder])

    val assignment: Option[model.Assignment] = input match {
      case Extractor.AssignmentArc(routes @ _*)
        if routes.collectFirst { case Extractor.StartDateRoute() => }.isDefined &&
          routes.collectFirst { case Extractor.TechnologyRoute(_) => }.isDefined =>

        val (startYear, startMonth, endYear, endMonth) = {
          val f = (s: String) =>
            routes.collectFirst { case Route(label, Verticle(DateNode(v1, v2, _))) if label == s => (v1, v2) }
          (f("start year").flatMap(_._1), f("start month").flatMap(_._2), f("end year").flatMap(_._1), f("end month").flatMap(_._2))
        }

        val technologies: Seq[String] =
          routes.collect { case Extractor.TechnologyRoute(value) => value }

        val startDate = // first day of the month
          for (y <-  startYear; m <- startMonth) yield {
            val cal = new GregorianCalendar
            cal.clear()
            cal.set(y, m-1, 1)
            cal.getTime
          }

        val endDate = // last day of the month
          for (y <-  endYear; m <- endMonth) yield {
            val cal = new GregorianCalendar
            cal.clear()
            cal.set(y, m-1, 1)
            cal.set(Calendar.DAY_OF_MONTH, cal.getMaximum(Calendar.DAY_OF_MONTH))
            cal.getTime
          }

        val eom = { // end of this month
          val cal = new GregorianCalendar
          cal.set(Calendar.DAY_OF_MONTH, cal.getMaximum(Calendar.DAY_OF_MONTH))
          cal.set(Calendar.HOUR_OF_DAY, 23)
          cal.set(Calendar.MINUTE, 59)
          cal.set(Calendar.SECOND, 59)
          cal.set(Calendar.MILLISECOND, 0)
          cal.getTime
        }

        if (startDate.isEmpty || technologies.isEmpty) {
          log.warn("Not enough data: "+input)
          None
        } else {
          Some(model.Assignment(startDate.get, endDate.getOrElse(eom), technologies))
        }

      case _ =>
        log.warn("Unsupported combination: "+input)
        None
    }

  }

}

/** Chart classes */
package chart {

  trait Chart {
    val template: String
    def build(career: model.Career, person: PersonNode): Map[String,Any]
  }

  class TimePerCompany extends Chart {
    override val template: String = "chart1.ftl"

    override def build(career: model.Career, person: PersonNode): Map[String,Any] = {
      val chartColumns = career.companies
        .map(company => Array(company.name, company.assignments.map(a => Util.months(a.start, a.end)).sum))
        .sortBy { case Array(_, n: Int) => n }.reverse // order

      val top = Config.c3_chart_TimePerCompany_top
      Map(
        "js_columns" -> new ChartJson.Columns(chartColumns.toArray).json,
        "js_columns_top" -> new ChartJson.Columns(chartColumns.toArray.take(top)).json,
        "top" -> top
      )
    }
  }

  class TimePerTechnology extends Chart {
    override val template: String = "chart3.ftl"

    override def build(career: model.Career, person: PersonNode): Map[String,Any] = {
      val tuples = career.companies.flatMap(_.assignments.flatMap(a => a.technologies.map(_ -> Util.months(a.start, a.end))))
      val chartColumns = tuples
        .groupBy(_._1)
        .map { case (name, seq) => name -> seq.map(_._2).sum }
        .map { case (name, n) => Array(name, n) }
        .toSeq.sortBy { case Array(_, n: Int) => n }.reverse // order

      val top = Config.c3_chart_TimePerTechnology_top
      Map(
        "js_columns" -> new ChartJson.Columns(chartColumns.toArray).json,
        "js_columns_top" -> new ChartJson.Columns(chartColumns.toArray.take(top)).json,
        "top" -> top
      )
    }
  }

  class Timelapse extends Chart {
    override val template: String = "chart2.ftl"

    override def build(career: model.Career, person: PersonNode): Map[String,Any] = {
      case class CompanyAssignment(name: String, assignment: model.Assignment)
      val gap = (from: Date, to: Date, n: Int) => // make gap if needed
        if (Util.months(from, to, inclusive = false) > 0)
          Some(CompanyAssignment(s"(gap $n)", model.Assignment(start = from, end = to, technologies = Nil)))
        else None
      val gapN = (x: Seq[CompanyAssignment]) => // next gap number
        x.filter(_.name.startsWith("(gap ")).lastOption.map(_.name.drop(5).dropRight(1).toInt).getOrElse(0)+1

      val assignments = career.companies
        .flatMap(company => company.assignments.map(CompanyAssignment(company.name, _)))
        .sortBy(_.assignment.start)
      val assignmentsWithGaps = assignments.foldLeft(List.empty[CompanyAssignment]) { case (x, ca) =>
        x ::: List(if (x.isEmpty) None else gap(x.last.assignment.end, ca.assignment.start, gapN(x)), Some(ca)).flatten
      } match {
        case x @ Seq(_*) => x ::: List(gap(x.last.assignment.end, new Date, gapN(x))).flatten // add final gap
      }

      val chartColumns = assignmentsWithGaps
        .map(ca => Array(ca.name, Util.months(ca.assignment.start, ca.assignment.end, inclusive = !ca.name.startsWith("(gap "))))
      val chartGroups = assignmentsWithGaps.map(_.name)
      val chartColors = assignmentsWithGaps.filter(_.name.startsWith("(gap ")).map(_.name -> "#FFFFFF")
      val chartLegendHide = assignmentsWithGaps.filter(_.name.startsWith("(gap ")).map(_.name)

      Map(
        "js_columns" -> new ChartJson.Columns(chartColumns.toArray).json,
        "js_groups" -> new ChartJson.Groups(Array(chartGroups.toArray)).json,
        "js_colors" -> new ChartJson.Colors(chartColors.toMap).json,
        "js_legend_hide" -> new ChartJson.LegendHide(chartLegendHide.toArray).json
      )
    }
  }

}
