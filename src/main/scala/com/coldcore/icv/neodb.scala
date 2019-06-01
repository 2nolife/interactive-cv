package com.coldcore.icv
package writer.neodb

import java.io.File

import core.{DbMapToolkit, Graph, Writer}
import core.Graph._
import core.Triple._
import org.slf4j.LoggerFactory
import org.neo4j.unsafe.batchinsert.{BatchInserter, BatchInserters}
import org.neo4j.graphdb.{Label, RelationshipType}

import scala.collection._
import scala.collection.JavaConverters._

/** Write graphs into Neo4j directory */
class NeoWriter extends Writer {

  private val log = LoggerFactory.getLogger(classOf[NeoWriter])

  private val dbmapToolkit = new DbMapToolkit
  private val dir = new File(Config.output_dir, "graph.db")

  private lazy val dbmap = dbmapToolkit.db
  private lazy val verticles = dbmapToolkit.newMap[Verticle,Long](dbmap) // large map to connects graphs with each other (verticle -> database PK)
  private lazy val inserter = BatchInserters.inserter(dir, Config.neo_writer.asJava)
  private lazy val gwriter = new GraphWriter(inserter, verticles)

  override def write(iterator: Iterator[Graph]) {
    log.debug("Saving to "+dir.getAbsolutePath)
    val gcount =
      iterator count { graph => gwriter.write(graph); true }
    log.debug(s"Saved $gcount graphs")
  }

  override def connect(iterator: Iterator[Arc]) {
    log.debug("Connecting graphs")
    val acount =
      iterator count { arc => gwriter.write(arc); true }
    log.debug(s"Connected graphs with $acount arcs")
  }

  override def close() {
    inserter shutdown()
    dbmap.close()
  }

}

/** Write graph in Neo4j format and fills verticles to connects it with other graphs */
private class GraphWriter(inserter: BatchInserter, verticles: JMap[Verticle,Long]) {

  private val log = LoggerFactory.getLogger(classOf[GraphWriter])
  private val verticlePkCache = mutable.Map[Verticle,Long]() // verticles of a single graph (or connect arc)

  def write(graph: Graph) {
    log.debug("Graph for "+graph.verticle.node)
    write(graph.verticle, graph.arc)
    verticlePkCache.clear() 
  }

  def write(arc: Arc): Boolean = {
    arc match {
      case Arc(v, Seq(Route(label, linkTo @ Verticle(_)))) =>
        Option(verticles.get(v)) match { 
          case Some(pkA) =>
            addRelationship(pkA, pkB = addVerticle(linkTo, isolated = false), label)
            if (verticlePkCache.size != 1) verticlePkCache.clear() // only save last "linkTo" verticle 
            true
          case _ =>
            log.warn("No verticle for arc: "+arc)
            false
        }
      case a =>
        log.warn("Unsupported combination: "+a)
        false
    }
  }

  private def write(verticle: Verticle, term: Term): Unit =
    term match {
      case Arc(v, routes) =>
        routes.foreach(write(v, _))
      case Route(label, v @ Verticle(_)) =>
        addRelationship(pkA = addVerticle(verticle), pkB = addVerticle(v), label)
      case Route(label, arc @ Arc(v, routes)) =>
        addRelationship(pkA = addVerticle(verticle), pkB = addVerticle(v), label)
        write(v, arc)
      case t =>
        log.warn("Unsupported combination: "+t)
    }

  private def addVerticle(verticle: Verticle, isolated: Boolean = true): Long =
    verticlePkCache.getOrElseUpdate(verticle, // fast lookup (isolated graph)
      (if (isolated) None else Option(verticles.get(verticle))).getOrElse { // slow lookup (all verticles)
        val props = nodeProps(verticle.node)
        val label = props.get("type").map(Label.label(_) :: Nil).getOrElse(Nil)
        val pk = inserter.createNode(cypherMap(props.filter(_._1 != "type")), label: _*)
        verticles.put(verticle, pk)
        pk
      })

  private def addRelationship(pkA: Long, pkB: Long, label: String) =
    inserter.createRelationship(pkA, pkB, RelationshipType.withName(label), emptyCypherMap)

  private val emptyCypherMap = Map.empty[String, AnyRef].asJava
  private val cypherMap = (map: Map[String, AnyRef]) => map.asJava

  private def nodeProps(node: Node): Map[String,String] =
    node match {
      case TextNode(value, language) if language.nonEmpty =>
        Map("type" -> "Text", "value" -> value, "lng" -> language)
      case TextNode(value, _) =>
        Map("type" -> "Text", "value" -> value)
      case ValueNode(value) =>
        Map("type" -> "Other", "value" -> value)
      case BlankNode(_, value) =>
        Map("type" -> "Blank", "value" -> value)
      case DateNode(year, month, day) =>
        Map("type" -> "Date", "value" -> Seq(year, month, day).flatten.mkString("-"))
      case PersonNode(id, value) =>
        Map("type" -> "Person", "value" -> value, "user" -> id)
      case TechnologyNode(value) =>
        Map("type" -> "Technology", "value" -> value)
      case TeamNode(value) =>
        Map("type" -> "Team", "value" -> value)
      case CompanyNode(value) =>
        Map("type" -> "Company", "value" -> value)
      case n =>
        log.warn("Unsupported node: "+n)
        Map.empty
    }

}
