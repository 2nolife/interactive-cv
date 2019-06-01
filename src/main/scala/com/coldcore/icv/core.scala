package com.coldcore.icv
package core

import java.io.{File, Reader, StringReader, StringWriter}
import java.util.UUID
import org.apache.commons.io.FileUtils
import scala.collection.JavaConverters._

object Defaults {
  import java.util.{Locale, TimeZone}

  def set() {
    Locale.setDefault(Locale.ENGLISH)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }
}

/** Iterator which operates on an endless stream of optional values provided by the function */
class DefaultIterator[A](f: () => Option[A]) extends Iterator[A] {
  private var nextElement: Option[A] = None
  private val acquire = () => { nextElement = f(); nextElement }
  override def hasNext: Boolean = (nextElement orElse acquire()).isDefined
  override def next(): A = (nextElement orElse acquire() orElse Iterator.empty.next()).map { v => nextElement = None; v }.get
}

/** Triple nodes */
object Triple {

  sealed trait Node

  // raw nodes from RDF file
  case class TypeLiteralNode(value: String, datatypeUri: String) extends Node
  case class PlainLiteralNode(value: String, language: String) extends Node
  case class UriNode(uri: String) extends Node
  case class AnonNode(id: String) extends Node

  // nodes from triple reader
  case class ValueNode(value: String) extends Node
  case class TextNode(value: String, language: String = "") extends Node // optional language
  case class TechnologyNode(value: String) extends Node
  case class TeamNode(value: String) extends Node
  case class CompanyNode(value: String) extends Node
  case class PersonNode(id: String, value: String) extends Node
  case class DateNode(year: Option[Int], month: Option[Int], day: Option[Int]) extends Node
  case class BlankNode(id: String, value: String = "") extends Node // optional value

  // separator per group of triples (per parsed file)
  case object Separator extends Node
}

/** Triple contains data for Subject / Predicate / Object */
case class Triple(s: Triple.Node, p: Triple.Node, o: Triple.Node)

object Graph {

  sealed trait Term
  case class Verticle(node: Triple.Node) extends Term {
    private val salt = node match {
      case Triple.BlankNode(id, value) => ""
      case _ => UUID.randomUUID.toString
    }
    override def hashCode: Int = (node, salt).##
  }
  case class Route(label: String, term: Term) extends Term
  case class Arc(verticle: Verticle, routes: Seq[Route]) extends Term

}

/** Graph contains connected arcs for a Verticle */
case class Graph(verticle: Graph.Verticle, arc: Graph.Arc)

/** Large collection toolkit */
class DbMapToolkit {
  import org.mapdb.{DB, DBMaker}

  def db: DB =
    DBMaker.tempFileDB().fileDeleteAfterClose.make

  def newMap[K,V](db: DB): JMap[K,V] =
    db.hashMap(math.random.toString).create().asInstanceOf[JMap[K,V]]

  def newSet[V](db: DB): JSet[V] =
    db.hashSet(math.random.toString).create().asInstanceOf[JSet[V]]

}

/** Writer trait for plugins to output graphs */
trait Writer {
  def write(iterator: Iterator[Graph]) // write isolated graphs
  def connect(iterator: Iterator[Graph.Arc]) // connect graphs after "write"
  def close() {} // close resources
}

/** Before and after writers */
trait Interceptor {
  def before() { }
  def after() { }
}

class FreemarkerEngine {
  import freemarker.template._
  import freemarker.cache._
  import freemarker.ext.beans.BeansWrapper

  private val configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)
  private val wrapper = new DefaultObjectWrapper(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)
  wrapper.setExposureLevel(BeansWrapper.EXPOSE_ALL)
  configuration.setObjectWrapper(wrapper)
  configuration.setDefaultEncoding("UTF-8")
  configuration.setLocalizedLookup(false)
  configuration.setNumberFormat("computer") // http://freemarker.org/docs/ref_directive_setting.html
  configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
  configuration.setTemplateLoader(new Loader)

  def template(name: String): Template = configuration.getTemplate(name)
  def objectWrapper(): ObjectWrapper = configuration.getObjectWrapper

  private class Loader extends TemplateLoader {
    override def findTemplateSource(source: String): AnyRef =
      FileUtils.readFileToString(new File(source), "UTF-8")

    override def getLastModified(templateSource: Any): Long = -1

    override def getReader(templateSource: Any, encoding: String): Reader =
      new StringReader(templateSource.asInstanceOf[String])

    override def closeTemplateSource(templateSource: scala.Any) {}
  }
}

object FreemarkerUtil {

  private val engine = new FreemarkerEngine

  def fmTemplate(path: String, model: Map[String,Any]): String = {
    val template = engine.template(path)
    val writer = new StringWriter
    val env = template.createProcessingEnvironment(model.asJava, writer)
    env.setOutputEncoding("UTF-8")
    env.process()
    writer.toString
  }

}

object JsonUtil {
  import com.google.gson.Gson

  def fromJson[T](content: String, c: Class[T]): T = new Gson().fromJson(content, c)
  def toJson[T](content: T, c: Class[T]): String = new Gson().toJson(content, c)
}
