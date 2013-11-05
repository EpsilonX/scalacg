package ca.uwaterloo.scalacg.analysis

import scala.collection.mutable.Set

import ca.uwaterloo.scalacg.config.CallGraphWorklists
import ca.uwaterloo.scalacg.plugin.PluginOptions
import ca.uwaterloo.scalacg.util.Lookup
import ca.uwaterloo.scalacg.util.MethodOps
import ca.uwaterloo.scalacg.util.SuperCalls
import ca.uwaterloo.scalacg.util.TreeTraversal
import ca.uwaterloo.scalacg.util.TypeConcretization
import ca.uwaterloo.scalacg.util.Worklist

/**
 * The various analysis options offered by our plugin.
 */
object Analysis extends Enumeration {
  type Analysis = Value

  val Ra = Value("ra") // Name-based analysis
  val Tca = Value("tca") // Trait-composition analysis
  val Tcra = Value("tcra") // TODO: what's that?

  final val Default = Tca // The analysis by default runs TCA
}

trait CallGraphAnalysis extends CallGraphWorklists
  with MethodOps
  with TypeConcretization
  with TreeTraversal
  with SuperCalls
  with Lookup {

  import global._

  // Initialize the worklists
  val instantiatedTypes = new Worklist[Type]
  val callSites = new Worklist[AbstractCallSite]
  val reachableMethods = new Worklist[Symbol]
  val superCalled = new Worklist[Symbol]

  // Initial params
  val entryPoints = Set[Symbol]()
  val callBacks = Set[Symbol]()
  val moduleConstructors = Set[Symbol]()
  val trees = Set[Tree]()

  // Plugin options
  val pluginOptions: PluginOptions

  def initialize = {
    // Get the ASTs from the current run
    trees ++= global.currentRun.units.map(_.body)

    // Karim: The ASTs have to be traversed at the very beginning. DO NOT try to traverse them on demand, doesn't work.
    trees.foreach(traverse(_, List()))

    // Main methods and the primary constructor of main modules are entry points to the call graph.
    entryPoints ++= mainMethods
    entryPoints ++= mainModulesPrimaryConstructors

    // Entry points are initially reachable, main modules are initially instantiated.
    reachableMethods ++= mainMethods
    instantiatedTypes ++= mainModules
    instantiatedTypes ++= modulesInTypes(mainModules)

    //    modulesInCallSites(abstractCallSites).foreach { m =>
    //      println(m + " :: " + types(m) + " :: " + m.getClass + " :: " + m.typeSymbol.companionSymbol.tpe + " :: " + types(m.typeSymbol.companionSymbol.tpe))
    //    }
    //    println
    //    println
    //    modulesInTypes(mainModules).foreach(m => println(m + " :: " + types(m)))
    //    mainModules.foreach(m => println(m + " :: " + types(m)))
    //    types.foreach { t =>
    //      println(t + " :: " + t.getClass)
    //    }
    //    abstractToCallSites.values.flatten.foreach(cs => println(cs.receiver + " :: " + cs.staticTarget + " :: " + cs.enclMethod + " :: " + cs.hasModuleReceiver))

  }

  def buildCallGraph = {
    while (reachableMethods.nonEmpty) {
      // Debugging info
      println(s"Items in work list: ${reachableMethods.size}")

      // Process methods
      processMethods

      // Process types
      processTypes

      // Process super calls
      collectSuperCalled

      // Process new call sites with all types, and use new types to process all call sites
      if (callSites.nonEmpty) {
        println(s"\tFound ${callSites.size} new call sites")
        processCallSites(callSites.newItems, instantiatedTypes.reachableItems)
      }
      if (instantiatedTypes.nonEmpty) {
        println(s"\tFound ${instantiatedTypes.size} new instantiated types")
        processCallSites(callSites.reachableItems, instantiatedTypes.newItems)
      }

      //      println("new types: " + instantiatedTypes.newItems) // TODO

      // Clear call sites and instantiated types to prepare for the next iteration.
      callSites.clear
      instantiatedTypes.clear
    }
  }

  /**
   * Process newly instantiated types.
   */
  private def processTypes = {
    for (tpe <- instantiatedTypes.newItems) {
      // Add constructors
      reachableMethods ++= constructorsOf(tpe)

      // Add callbacks
      val cbs = callBacksOf(tpe)
      callBacks ++= cbs
      reachableMethods ++= cbs

      // Add type concretizations 
      addTypeConcretization(tpe)
    }
  }

  /**
   * Process newly reachable methods.
   */
  private def processMethods = {
    for (method <- reachableMethods.newItems) {
      // Find new call sites
      callSites ++= callSitesInMethod(method)

      // Find new instantiated types
      instantiatedTypes ++= instantiatedTypesInMethod(method)
      instantiatedTypes ++= modulesInTypes(instantiatedTypes.newItems)
      instantiatedTypes ++= modulesInCallSites(callSites.newItems) // TODO
    }

    reachableMethods.clear
  }

  /**
   * Collect all methods that are called via "super".
   */
  private def collectSuperCalled = {
    for (callSite <- callSites.newItems) {
      if (callSite.isSuperCall) {
        superCalled += callSite.staticTarget
      }
    }
  }

  /**
   * Process some call sites given some types.
   */
  private def processCallSites(callSites: Set[AbstractCallSite], types: Set[Type]) = {
    for (callSite <- callSites) {
      val targets = Set[Symbol]()

      if (callSite.isConstructorCall) {
        targets += processConstructorCall(callSite)
      } else if (pluginOptions.doSuperCalls && callSite.isSuperCall) {
        targets ++= processSuperCall(callSite, types)
      } else if (pluginOptions.doThis && callSite.hasThisReceiver) {
        targets ++= processThisCall(callSite, types)
      } else {
        targets ++= processLookup(callSite, types)
      }

      // Add targets to worklist
      reachableMethods ++= targets
    }
  }

  /**
   * If the target method is a constructor, no need to do the lookup.
   */
  private def processConstructorCall(callSite: AbstractCallSite) = {
    addTargets(callSite, Set(callSite.staticTarget))
    callSite.staticTarget
  }

  /**
   * If it's a super call, call lookupSuper with types that contain the enclosing class of the call site
   * in their linearization (i.e., tpe.baseClasses).
   */
  private def processSuperCall(callSite: AbstractCallSite, types: Set[Type]) = {
    val allTargets = Set[Symbol]()

    abstractToCallSites(callSite).foreach { cs =>
      val targets = lookupSuper(cs, types)
      addTargets(cs, targets)
      allTargets ++= targets
    }

    allTargets
  }

  /**
   * IF it's a "this" call, then use
   */
  private def processThisCall(callSite: AbstractCallSite, types: Set[Type]) = {
    val allTargets = Set[Symbol]()

    abstractToCallSites(callSite).foreach { cs =>
      val lookupTypes = filterForThis(cs, types)
      // TODO
      //      if (cs.staticTarget.nameString == "toString" && cs.enclMethod.nameString == "toText") {
      //        println("************************************")
      //        println(cs.receiver + " :: " + signature(cs.enclMethod) + " :: " + signature(cs.thisEnclMethod))
      //        //        println(types)
      //        types.foreach { tpe =>
      //          println(tpe + " :: " + (tpe.members.toSet contains cs.thisEnclMethod) + " :: " + (tpe.typeSymbol.companionSymbol.tpe.members.toSet contains cs.thisEnclMethod))
      //        }
      //        println(lookupTypes)
      //        //        println(targets map signature)
      //        println("************************************\n")
      //        //
      //        //        //        println("************************************")
      //        //        //        types foreach println
      //        //        //        println("====================================")
      //        //        //        lookupTypes foreach println
      //        //        //        println("************************************")
      //      }
      val targets = lookup_<:<(cs, lookupTypes)

      addTargets(cs, targets)
      allTargets ++= targets
    }

    allTargets
  }

  /**
   * Process a normal lookup for the given call site using the given types.
   */
  private def processLookup(callSite: AbstractCallSite, types: Set[Type]) = {
    val targets = lookup_<:<(callSite, types)
    addTargets(callSite, targets)
    targets
  }

  /**
   * Add edges from each real call site abstracted by callSite to the given set of targets.
   */
  private def addTargets(callSite: AbstractCallSite, targets: Set[Symbol]) = {
    abstractToCallSites(callSite).foreach { cs => callGraph(cs) ++= targets }
  }

  /**
   * Add edges from a given call site to some given targets.
   */
  private def addTargets(callSite: CallSite, targets: Set[Symbol]) = {
    callGraph(callSite) ++= targets
  }

  /**
   * Filter the types that will be used later to lookup for methods if the receiver of the call site is "this".
   * TODO: 1) should we make the check vs superCalled.reachableItems or newItems?
   * TODO: 2) filter needs OPT?
   */
  private def filterForThis(callSite: CallSite, types: Set[Type]) = {
    if (callSite.thisEnclMethod == NoSymbol || superCalled.reachableItems.contains(callSite.thisEnclMethod)) types
    else types.filter { tpe =>
      (tpe.members.toSet contains callSite.thisEnclMethod) //|| (tpe.typeSymbol.companionSymbol.tpe.members.toSet contains callSite.thisEnclMethod)
    }
  }
}