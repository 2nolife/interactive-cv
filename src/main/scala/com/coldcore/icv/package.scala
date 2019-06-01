package com.coldcore

package object icv {

  type JMap[A,B] = java.util.Map[A,B]
  type JHashMap[A,B] = java.util.HashMap[A,B]
  type JSet[A] = java.util.Set[A]

  def withOpen[R <: { def close(): Unit }, T](r: R)(f: R => T) =
    try { f(r) } finally { r.close }

}
