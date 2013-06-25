package tests.ra

import callgraph.annotation.target
import callgraph.annotation.invocations

object Operator1 {

  class C(x : Int) {
    @target("::") def :: (y : Int): C = new C(x * y)
    override def toString = x.toString
  }

  @invocations("16: ::")
  def main(args: Array[String]) {
    val c = new C(10)
    val d = 10 :: c
    val e =  { "::"; d}.::(10)
    println(e)
  }
}