package com.coldcore.icv
package rdf

import java.io.File
import java.net.URLDecoder
import java.nio.file.{Files, Paths}

import core.{DefaultIterator, Triple}
import core.Triple._
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.jena.query._
import org.apache.jena.rdf.model.{Model, RDFNode}
import org.apache.jena.tdb.{TDBFactory, TDBLoader}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

class TripleIterator(supplier: TripleReader) extends DefaultIterator(() => supplier.nextTriple())

object TripleIterator {
  def apply(supplier: TripleReader): TripleIterator = new TripleIterator(supplier)
}

object TripleReader {
  def apply(files: Seq[File]): TripleReader = new TripleReader(files)
}

/** Extract triples from RDF files */
class TripleReader(files: Seq[File]) {

  private val log = LoggerFactory.getLogger(classOf[TripleReader])
  private val toolkit = new TdbToolkit
  private val converter = new NodeConverter
  private val dir = Files.createTempDirectory("icv-tdb-").toFile
  private val dataset = toolkit.newDataset(dir)

  private val fileIt = files.iterator
  private var rs = parseFile(fileIt.next())

  @tailrec final def nextTriple(): Option[Triple] = {
    if (!rs.hasNext && fileIt.hasNext) {
      rs = parseFile(fileIt.next())
      Some(Triple(Separator, Separator, Separator))
    }
    else if (!rs.hasNext) None
    else toolkit.makeTriple(rs.next).flatMap(converter.convert) match {
      case triple@Some(_) => triple
      case _ => nextTriple()
    }
  }

  def close() {
    toolkit.closeDataset(dataset)
    FileUtils.deleteDirectory(dir)
  }

  private def parseFile(file: File): ResultSet = {
    val (path, model) = (Paths.get(file.getAbsolutePath), "20190509")
    log.debug("Parsing "+file.getName)
    toolkit.deleteModel(dataset, model)
    toolkit.loadModel(dataset, path.toUri.toString, model)
    log.debug(s"Parsed ${toolkit.countTriples(dataset, model)} triples")
    toolkit.readTriples(dataset, model)
  }
}

/** Triple database toolkit */
private class TdbToolkit {

  private val log = LoggerFactory.getLogger(classOf[TdbToolkit])

  def newDataset(dir: File): Dataset = TDBFactory.createDataset(dir.getAbsolutePath)

  def closeDataset(dataset: Dataset) = dataset.close()

  def dsmodel(dataset: Dataset, modelName: String = ""): Model =
    if (modelName.isEmpty) dataset.getDefaultModel else dataset.getNamedModel(modelName)

  def loadModel(dataset: Dataset, url: String, modelName: String = "") =
    TDBLoader.loadModel(dsmodel(dataset, modelName), url)

  def deleteModel(dataset: Dataset, modelName: String = "") =
    dataset.removeNamedModel(modelName)

  def countTriples(dataset: Dataset, modelName: String = ""): Long = {
    val strQuery= "SELECT (count(*) AS ?count) { ?s ?p ?o }"
    val query = QueryFactory.create(strQuery)
    val qexe = QueryExecutionFactory.create(query, dsmodel(dataset, modelName))
    val rs = qexe.execSelect
    rs.next().getLiteral("count").getLong
  }

  def readTriples(dataset: Dataset, modelName: String = ""): ResultSet = {
    val strQuery= s"SELECT ?s ?p ?o { ?s ?p ?o }"
    val query = QueryFactory.create(strQuery)
    val qexe = QueryExecutionFactory.create(query, dsmodel(dataset, modelName))
    val rs = qexe.execSelect
    rs
  }

  def makeTriple(x: QuerySolution): Option[Triple] = {
    val node = (n: RDFNode) =>
      if (n.isLiteral) {
        val x = n.asLiteral
        Some(Option(x.getDatatypeURI) match {
          case Some(uri) if !Set("#string", "#langString").exists(uri.endsWith) =>
            TypeLiteralNode(x.getString, x.getDatatypeURI)
          case _ => PlainLiteralNode(x.getString, x.getLanguage)
        })
      }
      else if (n.isURIResource) Some(UriNode(n.asResource.getURI))
      else if (n.isAnon) Some(AnonNode(n.asResource.getId.toString))
      else {
        log.warn("Unsupported node: "+n)
        None
      }

    (node(x.get("s")), node(x.get("p")), node(x.get("o"))) match {
      case (s, p, o) if s.isDefined && p.isDefined && o.isDefined => Some(Triple(s.get, p.get, o.get))
      case _ => None  
    }
  }

}

/** Convert nodes in a triple to the ones supported by the application */
private class NodeConverter {

  /** convert raw nodes in a triple */
  def convert(triple: Triple): Option[Triple] = {
    val Triple(s, p, o) = triple
    for (s2 <- convert(s); p2 <- convert(p); o2 <- convert(o)) yield Triple(s2, p2, o2)
  }

  private val log = LoggerFactory.getLogger(classOf[NodeConverter])

  private val schema = (uri: String) => // configured schema by URI
    Config.schema.collectFirst { case s if uri.startsWith(s.uri) => s }

  private val decode = (uri: String) => URLDecoder.decode(uri, "UTF-8").trim

  private val parseURI = (prefix: String, uri: String) => //...Person/Foo_Bar#abc -> (Foo_Bar#abc,Foo Bar)
    (uri.drop(prefix.length), decode(uri).drop(prefix.length).takeWhile('#' !=).replace("_", " "))

  private val parseValue = (prefix: String, uri: String) => //.../Foo_barAbc -> Foo bar abc
    decode(uri).drop(prefix.length).replace("_", " ").split("\\s").map { token =>
      val s = StringUtils.splitByCharacterTypeCamelCase(token).mkString(" ")
      if (s.head == s.head.toLower) s.toLowerCase else s
    }.mkString(" ")

  /** convert a raw node to supported type */
  private def convert(node: Node): Option[Node] =
    node match {
      case UriNode(uri) if schema(uri).exists(_.uri.endsWith("/Person/")) =>
        val (id, value) = parseURI(schema(uri).get.uri, uri)
        Some(PersonNode(id, value))
      case UriNode(uri) if schema(uri).exists(_.uri.endsWith("/Technology/")) =>
        val (_, value) = parseURI(schema(uri).get.uri, uri)
        Some(TechnologyNode(value))
      case UriNode(uri) if schema(uri).exists(_.uri.endsWith("/Team/")) =>
        val (_, value) = parseURI(schema(uri).get.uri, uri)
        Some(TeamNode(value))
      case UriNode(uri) if schema(uri).exists(_.uri.endsWith("/Company/")) =>
        val (_, value) = parseURI(schema(uri).get.uri, uri)
        Some(CompanyNode(value))
      case UriNode(uri) if schema(uri).exists(s => s.nodes.contains(uri.drop(s.uri.length))) => // only configured nodes
        val value = parseValue(schema(uri).get.uri, uri)
        Some(ValueNode(value))
      case AnonNode(id) =>
        Some(BlankNode(id))
      case PlainLiteralNode(value: String, language: String) =>
        Some(TextNode(value, language))
      case TypeLiteralNode(value: String, uri: String) if schema(uri).isDefined && uri.endsWith("#gYear") =>
        Some(DateNode(Some(value.toInt), None, None))
      case TypeLiteralNode(value: String, uri: String) if schema(uri).isDefined && uri.endsWith("#gMonth") =>
        Some(DateNode(None, Some(value.drop(2).toInt), None))
      case TypeLiteralNode(value: String, uri: String) if schema(uri).isDefined && uri.endsWith("#gDay") =>
        Some(DateNode(None, None, Some(value.drop(3).toInt)))
      case TypeLiteralNode(value: String, uri: String) if schema(uri).isDefined && uri.endsWith("#date") =>
        Some(DateNode(Some(value.take(4).toInt), Some(value.slice(5, 7).toInt), Some(value.drop(8).toInt)))
      case n =>
        log.warn("Unsupported node: "+n)
        None
    }

}
