package com.coldcore.icv
package core

import core.Triple._
import core.Graph._
import org.scalatest.{FlatSpec, Matchers}

class GraphSpec extends FlatSpec with Matchers {

  "verticles" should "have unique hash" in {
    val (a, b, c) = (Verticle(ValueNode("foo")), Verticle(ValueNode("foo")), Verticle(ValueNode("bar")))

    a.hashCode should not equal b.hashCode
    a.hashCode should not equal c.hashCode

    a should matchPattern { case v @ Verticle(ValueNode("foo")) if v.hashCode == a.hashCode => }
  }

  "verticles with blank node" should "have the same hash" in {
    val (a, b, c, d, e) = (
      Verticle(BlankNode("1")), Verticle(BlankNode("1")),
      Verticle(BlankNode("2")), Verticle(BlankNode("2", "foo")), Verticle(BlankNode("2", "foo")))

    a.hashCode shouldEqual b.hashCode
    a.hashCode should not equal c.hashCode
    c.hashCode should not equal d.hashCode
    d.hashCode shouldEqual e.hashCode

    a should matchPattern { case v @ Verticle(BlankNode("1", "")) if v.hashCode == a.hashCode => }
  }

}
