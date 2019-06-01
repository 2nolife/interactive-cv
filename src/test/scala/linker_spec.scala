package com.coldcore.icv
package link

import core.{Triple, Graph}
import core.Triple._
import core.Graph._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class LinkerSpec extends FlatSpec with BeforeAndAfter with Matchers {

  var linker: Linker = _

  after {
    linker.close()
  }

  it should "build simple graph ending with text node" in {
    // Sylvanas --title-> Ms
    val it = Iterator.single[Triple](Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("title"), TextNode("Ms")))
    linker = Linker(it)
    linker.nextGraph().get shouldBe
      Graph(
        Verticle(PersonNode("Sylvanas", "Sylvanas")),
        Arc(
          Verticle(PersonNode("Sylvanas", "Sylvanas")),
          Route("title", Verticle(TextNode("Ms"))) :: Nil))
    linker.nextGraph() shouldBe None
  }

  it should "build simple graph ending with blank node" in {
    // Sylvanas --about-> blank
    val it = Iterator.single[Triple](Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("about"), BlankNode("?", "about")))
    linker = Linker(it)
    linker.nextGraph().get shouldBe
      Graph(
        Verticle(PersonNode("Sylvanas", "Sylvanas")),
        Arc(
          Verticle(PersonNode("Sylvanas", "Sylvanas")),
          Route("about", Verticle(BlankNode("?", "about"))) :: Nil))
    linker.nextGraph() shouldBe None
  }

  it should "build graph of 2 levels deep" in {
    /*
      Sylvanas +-title---> Ms
               +-contact-> blank +-email-> sylvanas@undercity.azeroth
                                 +-phone-> 0800-000123
    */
    val it = List(
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("title"), TextNode("Ms")),
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("contact"), BlankNode("?")),
      Triple(BlankNode("?"), ValueNode("email"), TextNode("sylvanas@undercity.azeroth")),
      Triple(BlankNode("?"), ValueNode("phone"), TextNode("0800-000123"))
    ).iterator

    linker = Linker(it)
    linker.nextGraph().get shouldBe
      Graph(
        Verticle(PersonNode("Sylvanas", "Sylvanas")),
        Arc(
          Verticle(PersonNode("Sylvanas", "Sylvanas")),
          Seq(
            Route("title", Verticle(TextNode("Ms"))),
            Route("contact",
              Arc(
                Verticle(BlankNode("?", "contact")),
                Seq(
                  Route("email", Verticle(TextNode("sylvanas@undercity.azeroth"))),
                  Route("phone", Verticle(TextNode("0800-000123")))))))))
  }

  it should "build graph of 3 levels deep" in {
    /*
      Sylvanas +-title-> Ms
               +-has---> blank +-contact-> blank +-email-> sylvanas@undercity.azeroth
                                                 +-phone-> 0800-000123
    */
    val it = List(
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("title"), TextNode("Ms")),
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("has"), BlankNode("a")),
      Triple(BlankNode("a"), ValueNode("contact"), BlankNode("b")),
      Triple(BlankNode("b"), ValueNode("email"), TextNode("sylvanas@undercity.azeroth")),
      Triple(BlankNode("b"), ValueNode("phone"), TextNode("0800-000123"))
    ).iterator

    linker = Linker(it)
    linker.nextGraph().get shouldBe
      Graph(
        Verticle(PersonNode("Sylvanas", "Sylvanas")),
        Arc(
          Verticle(PersonNode("Sylvanas", "Sylvanas")),
          Seq(
            Route("title", Verticle(TextNode("Ms"))),
            Route("has",
              Arc(
                Verticle(BlankNode("a", "has")),
                Seq(
                  Route("contact",
                    Arc(
                      Verticle(BlankNode("b", "contact")),
                      Seq(
                        Route("email", Verticle(TextNode("sylvanas@undercity.azeroth"))),
                        Route("phone", Verticle(TextNode("0800-000123"))))))))))))
  }

  it should "link end nodes together" in {
    /*                                                     : generated link
      Sylvanas +-works-> Trade Fleets -type-> Organization : -same as-+
               |                                           :          |-> blank
               +-works-> Venture Co   -type-> Organization : -same as-+
    */
    val it = List(
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("works"), CompanyNode("Trade Fleets")),
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("works"), CompanyNode("Venture Co")),
      Triple(CompanyNode("Trade Fleets"), ValueNode("type"), ValueNode("Organization")),
      Triple(CompanyNode("Venture Co"), ValueNode("type"), ValueNode("Organization"))
    ).iterator

    linker = new Linker(it, idgen = () => "?")
    linker.nextGraph().get shouldBe
      Graph(
        Verticle(PersonNode("Sylvanas", "Sylvanas")),
        Arc(
          Verticle(PersonNode("Sylvanas", "Sylvanas")),
          Seq(
            Route("works",
              Arc(
                Verticle(CompanyNode("Trade Fleets")),
                Seq(
                  Route("type",
                    Arc(
                      Verticle(ValueNode("Organization")),
                      Route("same as", Verticle(BlankNode("?", "same as"))) :: Nil))))),
            Route("works",
              Arc(
                Verticle(CompanyNode("Venture Co")),
                Seq(
                  Route("type",
                    Arc(
                      Verticle(ValueNode("Organization")),
                      Route("same as", Verticle(BlankNode("?", "same as"))) :: Nil))))))))
  }

  it should "link company nodes together" in {
    /*                                                  : generated link
      Sylvanas +-works-> Venture Co +-name-> Venture Co :
               |                    +------------------ : -same as-+
               |                                        :          |-> blank
               |                    +------------------ : -same as-+
               +-knows-> Venture Co +-name-> Venture Co :

    */
    val it = List(
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("works"), CompanyNode("Venture Co")),
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("knows"), CompanyNode("Venture Co")),
      Triple(CompanyNode("Venture Co"), ValueNode("name"), TextNode("Venture Co"))
    ).iterator

    linker = new Linker(it, idgen = () => "?")
    linker.nextGraph().get shouldBe
      Graph(
        Verticle(PersonNode("Sylvanas", "Sylvanas")),
        Arc(
          Verticle(PersonNode("Sylvanas", "Sylvanas")),
          Seq(
            Route("works",
              Arc(
                Verticle(CompanyNode("Venture Co")),
                Seq(
                  Route("name", Verticle(TextNode("Venture Co"))),
                  Route("same as", Verticle(BlankNode("?", "same as")))))),
            Route("knows",
              Arc(
                Verticle(CompanyNode("Venture Co")),
                Seq(
                  Route("name", Verticle(TextNode("Venture Co"))),
                  Route("same as", Verticle(BlankNode("?", "same as")))))))))
  }

  it should "link company nodes together without shared node" in {
    /*                                                  : generated link
      Sylvanas +-works-> Venture Co + (no shared node)  :
               |                    +------------------ : -same as-+
               |                                        :          |-> blank
               |                    +------------------ : -same as-+
               +-knows-> Venture Co + (no shared node)  :

    */
    val it = List(
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("works"), CompanyNode("Venture Co")),
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("knows"), CompanyNode("Venture Co"))
    ).iterator

    linker = new Linker(it, idgen = () => "?")
    linker.nextGraph().get shouldBe
      Graph(
        Verticle(PersonNode("Sylvanas", "Sylvanas")),
        Arc(
          Verticle(PersonNode("Sylvanas", "Sylvanas")),
          Seq(
            Route("works",
              Arc(
                Verticle(CompanyNode("Venture Co")),
                Route("same as", Verticle(BlankNode("?", "same as"))) :: Nil)),
            Route("knows",
              Arc(
                Verticle(CompanyNode("Venture Co")),
                Route("same as", Verticle(BlankNode("?", "same as"))) :: Nil)))))
  }

  it should "build graph per group" in {
    /*
      Sylvanas --title-> Ms
      Sylvanas --about-> blank
    */
    val it = List(
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("title"), TextNode("Ms")),
      Triple(Separator, Separator, Separator),
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("about"), BlankNode("?", "about"))
    ).iterator

    linker = Linker(it)
    linker.nextGraph().get shouldBe
      Graph(
        Verticle(PersonNode("Sylvanas", "Sylvanas")),
        Arc(
          Verticle(PersonNode("Sylvanas", "Sylvanas")),
          Route("title", Verticle(TextNode("Ms"))) :: Nil))
    linker.nextGraph().get shouldBe
      Graph(
        Verticle(PersonNode("Sylvanas", "Sylvanas")),
        Arc(
          Verticle(PersonNode("Sylvanas", "Sylvanas")),
          Route("about", Verticle(BlankNode("?", "about"))) :: Nil))
    linker.nextGraph() shouldBe None
  }

  it should "connect graphs with each other" in {
    /*                                                      : generated link                : generated connection
      Sylvanas +-works-> Trade Fleets --type-> Organization : -same as-+                    :
               |                                            :          |-> blank("same as") : -more like- +
               +-works-> Venture Co   +-type-> Organization : -same as-+                    :             |
                                      |                     :                               :             |----> blank("more like")
                                      +-------------------- : - - - - - - - - - - - - - - - : -more like- | -+
                                                            :                               :             |  |
      Varian   +-knows-> Venture Co   +-type-> Organization : ----------------------------- : -more like--+  |
                                      |                     :                               :                |-> blank("more like")
                                      +-------------------- : - - - - - - - - - - - - - - - : -more like-----+
                                                            :                               :                |
      Tiffin   +-knows-> Venture Co   --------------------- : - - - - - - - - - - - - - - - : -more like-----+
    */
    val it = List(
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("works"), CompanyNode("Trade Fleets")),
      Triple(PersonNode("Sylvanas", "Sylvanas"), ValueNode("works"), CompanyNode("Venture Co")),
      Triple(CompanyNode("Trade Fleets"), ValueNode("type"), ValueNode("Organization")),
      Triple(CompanyNode("Venture Co"), ValueNode("type"), ValueNode("Organization")),
      Triple(Separator, Separator, Separator),
      Triple(PersonNode("Varian", "Varian"), ValueNode("knows"), CompanyNode("Venture Co")),
      Triple(CompanyNode("Venture Co"), ValueNode("type"), ValueNode("Organization")),
      Triple(Separator, Separator, Separator),
      Triple(PersonNode("Tiffin", "Tiffin"), ValueNode("knows"), CompanyNode("Venture Co"))
    ).iterator

    val ids = Iterator.from(1).map(_.toString)
    linker = new Linker(it, idgen = () => ids.next())

    val graphS = linker.nextGraph().get
    graphS shouldBe
      Graph(
        Verticle(PersonNode("Sylvanas", "Sylvanas")),
        Arc(
          Verticle(PersonNode("Sylvanas", "Sylvanas")),
          Seq(
            Route("works",
              Arc(Verticle(CompanyNode("Trade Fleets")),
                Seq(
                  Route("type",
                    Arc(Verticle(ValueNode("Organization")),
                      Seq(
                        Route("same as", Verticle(BlankNode("3", "same as"))))))))),
            Route("works",
              Arc(Verticle(CompanyNode("Venture Co")),
                Seq(
                  Route("type",
                    Arc(Verticle(ValueNode("Organization")),
                      Seq(
                        Route("same as", Verticle(BlankNode("3", "same as"))))))))))))

    val graphV = linker.nextGraph().get
    graphV shouldBe
      Graph(
        Verticle(PersonNode("Varian", "Varian")),
        Arc(
          Verticle(PersonNode("Varian", "Varian")),
          Seq(
            Route("knows",
              Arc(Verticle(CompanyNode("Venture Co")),
                Seq(
                  Route("type", Verticle(ValueNode("Organization")))))))))

    val graphT = linker.nextGraph().get
    graphT shouldBe
      Graph(
        Verticle(PersonNode("Tiffin", "Tiffin")),
        Arc(
          Verticle(PersonNode("Tiffin", "Tiffin")),
          Seq(
            Route("knows", Verticle(CompanyNode("Venture Co"))))))

    val (ventureS: Verticle, sameS: Verticle) = graphS match {
      case Graph(_, Arc(_, Seq(_, Route(_, Arc(v1, Seq(Route(_, Arc(_, Seq(Route(_, v2)))))))))) => (v1, v2)
    }

    val (ventureV: Verticle, organizationV: Verticle) = graphV match {
      case Graph(_, Arc(_, Seq(Route(_, Arc(v1, Seq(Route(_, v2))))))) => (v1, v2)
    }

    val ventureT: Verticle = graphT match {
      case Graph(_, Arc(_, Seq(Route(_, v @ Verticle(_))))) => v
    }

    sameS.node shouldBe BlankNode("3", "same as")
    ventureS.node shouldBe CompanyNode("Venture Co")
    ventureS.node shouldBe ventureV.node
    ventureS.node shouldBe ventureT.node
    organizationV.node shouldBe ValueNode("Organization")

    val arcs = ConnectIterator(linker).toList
    val find = (f: PartialFunction[Arc,_]) => arcs.collectFirst(f)
    val message = "Arc not found in:\n\t"+arcs.mkString("\n\t")

    arcs.size shouldBe 5

    find { case Arc(v @ Verticle(BlankNode("3", "same as")), Route("more like", Verticle(BlankNode(_, "more like"))) :: Nil) if v.hashCode == sameS.hashCode => } orElse fail(message)
    find { case Arc(v @ Verticle(ValueNode("Organization")), Route("more like", Verticle(BlankNode(_, "more like"))) :: Nil) if v.hashCode == organizationV.hashCode => } orElse fail(message)
    find { case Arc(v @ Verticle(CompanyNode("Venture Co")), Route("more like", Verticle(BlankNode(_, "more like"))) :: Nil) if v.hashCode == ventureS.hashCode => } orElse fail(message)
    find { case Arc(v @ Verticle(CompanyNode("Venture Co")), Route("more like", Verticle(BlankNode(_, "more like"))) :: Nil) if v.hashCode == ventureT.hashCode => } orElse fail(message)
    find { case Arc(v @ Verticle(CompanyNode("Venture Co")), Route("more like", Verticle(BlankNode(_, "more like"))) :: Nil) if v.hashCode == ventureV.hashCode => } orElse fail(message)

  }

}