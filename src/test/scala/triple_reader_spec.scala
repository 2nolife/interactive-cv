package com.coldcore.icv
package rdf

import java.io.File
import core.Triple
import core.Triple._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class TripleReaderSpec extends FlatSpec with BeforeAndAfter with Matchers {

  var reader: TripleReader = _

  after {
    reader.close()
  }

  val resources = "src/test/resources/"
  val (aristotle_nt, valid_nodes_nt, invalid_nodes_nt) = (
    new File(resources+"aristotle.nt"),
    new File(resources+"valid-nodes.nt"),
    new File(resources+"invalid-nodes.nt")
  )

  it should "consume n-triple file" in {
    reader = TripleReader(aristotle_nt :: Nil)
  }

  it should "read all valid nodes" in {
    reader = TripleReader(valid_nodes_nt :: Nil)
    val triples = TripleIterator(reader).toList
    val find = (f: PartialFunction[Triple,_]) => triples.collectFirst(f)
    val message = "Triple not found in:\n\t"+triples.mkString("\n\t")

    triples.size shouldBe 44

    // <http://coldcore.com/schema/cv/Person/Sylvanas_Windrunner> <http://coldcore.com/schema/cv/term#about> _:1
    find { case Triple(PersonNode("Sylvanas_Windrunner", "Sylvanas Windrunner"), ValueNode("about"), BlankNode(_, _)) => } orElse fail(message)

    // <http://coldcore.com/schema/cv/Person/Varian_Wrynn#king> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person>
    // <http://coldcore.com/schema/cv/Person/Tiffin_Wrynn#queen> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person>
    find { case Triple(PersonNode("Varian_Wrynn#king", "Varian Wrynn"), ValueNode("type"), ValueNode("Person")) => } orElse fail(message)
    find { case Triple(PersonNode("Tiffin_Wrynn#queen", "Tiffin Wrynn"), ValueNode("type"), ValueNode("Person")) => } orElse fail(message)

    // _:1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person>
    find { case Triple(BlankNode(_, _), ValueNode("type"), ValueNode("Person")) => } orElse fail(message)

    // _:1 <http://xmlns.com/foaf/0.1/name> "Sylvanas Windrunner"
    // _:1 <http://xmlns.com/foaf/0.1/name> "Сильвана Ветрокрылая"@ru
    find { case Triple(BlankNode(_, _), ValueNode("name"), TextNode("Sylvanas Windrunner", "")) => } orElse fail(message)
    find { case Triple(BlankNode(_, _), ValueNode("name"), TextNode("Сильвана Ветрокрылая", "ru")) => } orElse fail(message)

    // _:1 <http://dbpedia.org/ontology/birthDate> "1919-09-10"^^<http://www.w3.org/2001/XMLSchema#date>
    find { case Triple(BlankNode(_, _), ValueNode("birth date"), DateNode(Some(1919), Some(9), Some(10))) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#startYear> "2016"^^<http://www.w3.org/2001/XMLSchema#gYear>
    // _:1 <http://coldcore.com/schema/cv/term#endYear> "2016"^^<http://www.w3.org/2001/XMLSchema#gYear>
    // _:1 <http://coldcore.com/schema/cv/term#startMonth> "--09"^^<http://www.w3.org/2001/XMLSchema#gMonth>
    // _:1 <http://coldcore.com/schema/cv/term#endMonth> "--09"^^<http://www.w3.org/2001/XMLSchema#gMonth>
    // _:1 <http://coldcore.com/schema/cv/term#startDay> "---15"^^<http://www.w3.org/2001/XMLSchema#gDay>
    // _:1 <http://coldcore.com/schema/cv/term#endDay> "---15"^^<http://www.w3.org/2001/XMLSchema#gDay>
    find { case Triple(BlankNode(_, _), ValueNode("start year"), DateNode(Some(2016), None, None)) => } orElse fail(message)
    find { case Triple(BlankNode(_, _), ValueNode("end year"), DateNode(Some(2016), None, None)) => } orElse fail(message)
    find { case Triple(BlankNode(_, _), ValueNode("start month"), DateNode(None, Some(9), None)) => } orElse fail(message)
    find { case Triple(BlankNode(_, _), ValueNode("end month"), DateNode(None, Some(9), None)) => } orElse fail(message)
    find { case Triple(BlankNode(_, _), ValueNode("start day"), DateNode(None, None, Some(15))) => } orElse fail(message)
    find { case Triple(BlankNode(_, _), ValueNode("end day"), DateNode(None, None, Some(15)) ) => } orElse fail(message)

    // _:1 <http://xmlns.com/foaf/0.1/title> "Ms"
    find { case Triple(BlankNode(_, _), ValueNode("title"), TextNode("Ms", "")) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#has> _:a
    find { case Triple(BlankNode(_, _), ValueNode("has"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#nationality> _:a
    find { case Triple(BlankNode(_, _), ValueNode("nationality"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#available> _:a
    find { case Triple(BlankNode(_, _), ValueNode("available"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#work> _:a
    find { case Triple(BlankNode(_, _), ValueNode("work"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#location> _:a
    find { case Triple(BlankNode(_, _), ValueNode("location"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#contact> _:a
    find { case Triple(BlankNode(_, _), ValueNode("contact"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#email> _:a
    find { case Triple(BlankNode(_, _), ValueNode("email"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#phone> _:a
    find { case Triple(BlankNode(_, _), ValueNode("phone"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#education> _:a
    find { case Triple(BlankNode(_, _), ValueNode("education"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#study> _:a
    find { case Triple(BlankNode(_, _), ValueNode("study"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#hobby> _:a
    find { case Triple(BlankNode(_, _), ValueNode("hobby"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#likes> _:a
    find { case Triple(BlankNode(_, _), ValueNode("likes"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#reference> _:a
    find { case Triple(BlankNode(_, _), ValueNode("reference"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#referee> _:a
    find { case Triple(BlankNode(_, _), ValueNode("referee"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#skill> _:a
    find { case Triple(BlankNode(_, _), ValueNode("skill"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#knows> <http://coldcore.com/schema/cv/Technology/Dagger>
    // _:1 <http://coldcore.com/schema/cv/term#knows> <http://coldcore.com/schema/cv/Technology/Bow/Arrow>
    find { case Triple(BlankNode(_, _), ValueNode("knows"), TechnologyNode("Dagger")) => } orElse fail(message)
    find { case Triple(BlankNode(_, _), ValueNode("knows"), TechnologyNode("Bow/Arrow")) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#knows> <http://coldcore.com/schema/cv/Team/Pathfinder>
    find { case Triple(BlankNode(_, _), ValueNode("knows"), TeamNode("Pathfinder")) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#contribution> _:a
    find { case Triple(BlankNode(_, _), ValueNode("contribution"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#url> _:a
    find { case Triple(BlankNode(_, _), ValueNode("url"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#career> _:a
    find { case Triple(BlankNode(_, _), ValueNode("career"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#works> _:a
    find { case Triple(BlankNode(_, _), ValueNode("works"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#works> <http://coldcore.com/schema/cv/Company/Trade_Fleets>
    find { case Triple(BlankNode(_, _), ValueNode("works"), CompanyNode("Trade Fleets")) => } orElse fail(message)

    // _:1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Organization>
    find { case Triple(BlankNode(_, _), ValueNode("type"), ValueNode("Organization")) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#assignment> _:a
    find { case Triple(BlankNode(_, _), ValueNode("assignment"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#group> _:a
    find { case Triple(BlankNode(_, _), ValueNode("group"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#role> _:a
    find { case Triple(BlankNode(_, _), ValueNode("role"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#project> _:a
    find { case Triple(BlankNode(_, _), ValueNode("project"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#uses> _:a
    find { case Triple(BlankNode(_, _), ValueNode("uses"), BlankNode(_, _)) => } orElse fail(message)

    // _:1 <http://coldcore.com/schema/cv/term#company> _:a .
    find { case Triple(BlankNode(_, _), ValueNode("company"), BlankNode(_, _)) => } orElse fail(message)
  }

  it should "ignore invalid nodes" in {
    reader = TripleReader(invalid_nodes_nt :: Nil)
    val triples = TripleIterator(reader).toList

    triples.size shouldBe 2

    triples should contain only (
      Triple(PersonNode("Sylvanas_Windrunner", "Sylvanas Windrunner"), ValueNode("type"), ValueNode("Person")),
      Triple(PersonNode("Sylvanas_Windrunner", "Sylvanas Windrunner"), ValueNode("title"), TextNode("Ms")))
  }

}
