package ca.uwaterloo.scalacg.plugin

import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.PluginComponent

import ca.uwaterloo.scalacg.util.Annotations
import ca.uwaterloo.scalacg.util.Timer

/**
 * Phase that annotates each method with @callgraph.annotation.MethodUID(serial number)
 */
abstract class MethodAnnotation extends PluginComponent with Annotations {

  def newPhase(prevPhase: Phase) = {
    println(s"Starting phase $phaseName ...")
    Timer.start
    new AnnotationPhase(prevPhase)
  }

  class AnnotationPhase(prevPhase: Phase) extends StdPhase(prevPhase) {
    var serialNumber = 1

    var classesCount = 0
    var anonfunCount = 0
    var modulesCount = 0
    var traitsCount = 0
    var mixinsCount = 0
    var closuresCount = 0
    var methodsCount = 0

    override def run = {
      super.run
      println(s"Finished $phaseName in ${Timer.elapsed} seconds.")
      println(s"# classes  : $classesCount")
      println(s"# anonfun  : $anonfunCount")
      println(s"# modules  : $modulesCount")
      println(s"# traits   : $traitsCount")
      println(s"# mixins   : $mixinsCount")
      println(s"# closures : $closuresCount")
      println(s"# methods  : $methodsCount")
    }

    // We are overriding apply because we want this phase to run on each file separately.
    def apply(unit: global.CompilationUnit) = {
      import global._

      def getReceiver(tree: Tree): Option[Tree] = tree match {
        case a: Apply => getReceiver(a.fun)
        case s: Select => Some(s.qualifier)
        case t: TypeApply => getReceiver(t.fun)
        case i: Ident => Some(i)
        case _ => assert(assertion = false, message = "getReceiver on unexpected tree " + tree + " of type " + tree.getClass); null
      }

      def normalizeMultipleParameters(tree: Apply): Apply = tree.fun match {
        case a: Apply => normalizeMultipleParameters(a)
        case _ => tree
      }

      unit.body.foreach { node =>
        node match {
          case dd: DefDef => {
            methodsCount += 1

            val defdef = dd.symbol // NSC marks this as OPT, so just call it once

            // Add the serial number annotation
            addMethodUID(defdef, serialNumber)
            methodToId += (defdef -> serialNumber)
            serialNumber += 1

            // Compile a list of methods that have @reachable annotation
            if (hasReachableAnnotation(defdef)) {
              expectedReachables += defdef
            }

            // Compile a list of methods that have @notreachable annotation
            if (hasNotReachableAnnotation(defdef)) {
              expectedNotReachables += defdef
            }
          }
          case _: ClassDef | _: ModuleDef => {
            val sym = node.symbol

            if (sym.isTrait) traitsCount += 1
            else if (sym.isModuleOrModuleClass) modulesCount += 1
            else if (sym.isAnonymousFunction) anonfunCount += 1
            else if (sym.isClass) classesCount += 1

            if (sym.mixinClasses.nonEmpty) mixinsCount += 1
          }
          case nw: New => if (nw.tpt.tpe.dealias.typeSymbol.mixinClasses.nonEmpty) mixinsCount += 1
          case vd: ValDef if definitions.isFunctionType(vd.symbol.tpe) && vd.rhs.isEmpty => closuresCount += 1
          case _ => // don't care
        }
      }
    }
  }
}