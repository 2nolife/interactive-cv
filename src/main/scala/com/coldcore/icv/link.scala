package com.coldcore.icv
package link

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import core.Graph._
import core.Triple._
import core.{DbMapToolkit, DefaultIterator, Graph, Triple}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection._

class GraphIterator(supplier: Linker) extends DefaultIterator(() => supplier.nextGraph())
class ConnectIterator(supplier: Linker) extends DefaultIterator(() => supplier.nextConnect())

object GraphIterator {
  def apply(linker: Linker): GraphIterator = new GraphIterator(linker)
}
object ConnectIterator {
  def apply(linker: Linker): ConnectIterator = new ConnectIterator(linker)
}

object Linker {
  def apply(iterator: Iterator[Triple], idgen: () => String = () => UUID.randomUUID.toString): Linker = new Linker(iterator, idgen)
}

/** Connect nodes together to form a graph per each group of triples */
class Linker(iterator: Iterator[Triple], idgen: () => String) {

  private val log = LoggerFactory.getLogger(classOf[Linker])

  private val triplesBySubject = mutable.Map[Node,List[Triple]]()
  private val personNodes = mutable.Set[PersonNode]()
  private val blankValues = mutable.Map[String,String]() // id -> value

  private val sameNodes = mutable.Map[Node, (BlankNode,Int)]() // nodes in the same graph: node("same as") -> (link to, adjacent count)
  private val moreNodes = mutable.Map[Node, (BlankNode,Int)]() // nodes between graphs: node("more like") -> (link to, adjacent count)

  private val dbmapToolkit = new DbMapToolkit
  private val dbmap = dbmapToolkit.db
  private val connected = mutable.Map[Node, JSet[Verticle]]() // large sets of Company/Technology/Value/etc verticles to connects graphs with each other

  private val gbuilder = new GraphBuilder(triplesBySubject, blankValues, sameNodes, moreNodes, connected)
  private var graphIt = buildGraph()

  def nextGraph(): Option[Graph] =
    if (graphIt.hasNext) Some(graphIt.next())
    else if (iterator.hasNext) {
      graphIt = buildGraph()
      Some(graphIt.next())
    } else None

  private def buildGraph(): Iterator[Graph] = {
    val it = iterator.takeWhile(_.s != Separator)
    new NodeCollector(it, triplesBySubject, blankValues, personNodes, sameNodes, moreNodes, idgen)
    moreNodes.keySet.foreach(connected.getOrElseUpdate(_, dbmapToolkit.newSet[Verticle](dbmap)))

    log.debug("Building graphs")
    val graphs = personNodes.map(gbuilder.build)
    log.debug("Built "+graphs.size+" graphs")
    graphs.iterator
  }

  private val cbuilder = new ConnectBuilder(connected, idgen)
  private var cfirst = true

  def nextConnect(): Option[Arc] = {
    if (cfirst) log.debug("Building connect arcs")
    cbuilder.buildNext() match {
      case arc @ Some(_) =>
        cfirst = false
        arc
      case _ =>
        log.debug(if (cfirst) "Nothing to connect" else "Built connect arcs")
        cfirst = false
        None
    }
  }

  def close(): Unit = dbmap.close()
  
}

/** Collect and organise nodes */
private class NodeCollector(it: Iterator[Triple],
                            triplesBySubject: mutable.Map[Node,List[Triple]],
                            blankValues: mutable.Map[String,String],
                            personNodes: mutable.Set[PersonNode],
                            sameNodes: mutable.Map[Node,(BlankNode,Int)],
                            moreNodes: mutable.Map[Node,(BlankNode,Int)],
                            idgen: () => String) {

  private val log = LoggerFactory.getLogger(classOf[NodeCollector])

  private object Linkable { // node to link with alike
    def unapply(n: Node): Option[Node] = n match {
      case CompanyNode(_) | TechnologyNode(_) | TeamNode(_) | ValueNode(_) => Some(n)
      case _ => None
    }
  }

  log.debug("Collecting nodes")

  triplesBySubject.clear()
  blankValues.clear()
  personNodes.clear()
  sameNodes.clear()

  it.foreach { triple =>
    triplesBySubject.put(triple.s, triple :: triplesBySubject.getOrElse(triple.s, Nil))
    triple match {
      case Triple(_, ValueNode(value), BlankNode(id, _)) =>
        blankValues += (id -> blankValues.get(id).map(_ :: Nil).getOrElse(Nil).:+(value).mkString(" or "))
      case _ =>
    }
    triple match {
      case Triple(n @ PersonNode(_, _), _, _) =>
        personNodes += n
      case _ =>
    }
    triple match {
      case Triple(_, _, Linkable(n)) =>
        val (linkTo, count) = sameNodes.get(n).map(x => (x._1, x._2)).getOrElse(BlankNode(idgen(), Config.label_sameAs), 0)
        sameNodes += (n -> (linkTo, count+1))
      case _ =>
    }
  }

  moreNodes ++= sameNodes

  sameNodes.collect { case x @ (_, (_, 0 | 1)) => x }.foreach(sameNodes -= _._1)

  log.debug(
    s"""Collected nodes:
       | ${triplesBySubject.size} subjects,
       | ${personNodes.size} people,
       | ${blankValues.size} valued blanks,
       | ${sameNodes.size} links
       |""".stripMargin.replace("\n", ""))

}

/** Build a complete graph per Person and fills verticles to connects it with other graphs */
private class GraphBuilder(triplesBySubject: mutable.Map[Node,List[Triple]],
                           blankValues: mutable.Map[String,String],
                           sameNodes: mutable.Map[Node,(BlankNode,Int)],
                           moreNodes: mutable.Map[Node,(BlankNode,Int)],
                           connected: mutable.Map[Node,JSet[Verticle]]) {

  private val log = LoggerFactory.getLogger(classOf[GraphBuilder])

  def build(person: PersonNode): Graph = {
    log.debug("Graph for "+person.id)
    val verticle = Verticle(person)
    val arc = Arc(verticle, build(person, Nil))
    Graph(verticle, arc)
  }

  private def build(node: Node, routes: Seq[Route]): Seq[Route] =
    routes ++ triplesBySubject.getOrElse(node, Nil).reverse.flatMap {
      case Triple(_, ValueNode(label), End(n)) if sameNodes.contains(n) =>
        val linkTo = sameNodes(n)._1
        val v = Verticle(linkTo)
        connect(n, v)
        Some(Route(label, Arc(Verticle(n), Route(linkTo.value, v) :: Nil)))
      case Triple(_, ValueNode(label), End(n)) =>
        val v = Verticle(n)
        connect(n, v)
        Some(Route(label, v))
      case Triple(_, ValueNode(label), More(n)) if sameNodes.contains(n) =>
        val linkTo = sameNodes(n)._1
        val v = Verticle(linkTo)
        connect(n, v)
        val r2 = Route(linkTo.value, v)
        deeper(n, label, Verticle(n)) match {
          case route @ Route(_, arc @ Arc(_, r)) => Some(route.copy(term = arc.copy(routes = r :+ r2)))
          case route @ Route(_, verticle @ Verticle(_)) => Some(route.copy(term = Arc(verticle, r2 :: Nil)))
          case route => Some(route)
        }
      case Triple(_, ValueNode(label), More(n)) =>
        val v = Verticle(n)
        connect(n, v)
        Some(deeper(n, label, v))
      case Triple(_, ValueNode(label), n @ BlankNode(id, _)) =>
        Some(deeper(n, label, Verticle(BlankNode(id, blankValues.getOrElse(id, "")))))
      case t =>
        log.warn("Unsupported combination: "+t)
        None
    }

  private def deeper(n: Node, label: String, v: Verticle): Route =
    build(n, Nil) match {
      case r @ Seq(_, _*) => Route(label, Arc(v, r))
      case _ => Route(label, v)
    }

  private object End { // node is route end
    def unapply(n: Node): Option[Node] = n match {
      case TextNode(_, _) | ValueNode(_) | DateNode(_, _, _) | TechnologyNode(_) | TeamNode(_) => Some(n)
      case _ => None
    }
  }

  private object More { // node has more routes
    def unapply(n: Node): Option[Node] = n match {
      case PersonNode(_, _) | CompanyNode(_) => Some(n)
      case _ => None
    }
  }

  private def connect(node: Node, verticle: Verticle) =
      connected.get(node).foreach(_.add(verticle))

}

/** Build acrs to connect graphs with each other */
private class ConnectBuilder(connected: mutable.Map[Node,JSet[Verticle]], idgen: () => String) {

  private val log = LoggerFactory.getLogger(classOf[ConnectBuilder])
  private val nv = mutable.Map[Node, Verticle]() // node -> verticle of node("more like")
  private val (nodeCount, count) = (Config.n_count, new AtomicInteger)

  @tailrec final def buildNext(): Option[Arc] =
    connected.headOption match {
      case Some((node, verticles)) if nv.contains(node) && !verticles.isEmpty || verticles.size > 1 =>
        if (!nv.contains(node)) log.debug("Arcs for "+node)
        val linkTo = () => nv.getOrElseUpdate(node, Verticle(BlankNode(idgen(), Config.label_moreLike)))
        val verticle =
          if (count.incrementAndGet() >= nodeCount && nodeCount > 1 && verticles.size > 1) { // pagination
            count.set(0)
            nv.remove(node).get
          } else {
            val v = verticles.iterator.next()
            verticles.remove(v)
            v
          }
        Some(Arc(verticle, Route("more like", linkTo()) :: Nil))
      case None =>
        None
      case Some((node, _)) =>
        count.set(0)
        connected -= node
        buildNext()
    }

}