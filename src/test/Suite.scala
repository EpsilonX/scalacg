package test

import org.junit.Test
import scala.tools.nsc
import org.junit.Assert

class Suite {
  def runTest(filename: String) = {
    val settings = new nsc.Settings
    settings.d.value = "junit/bin"
    settings.plugin.value = List("jar/callgraph-plugin.jar")
    settings.bootclasspath.append("junit/bin")
    settings.bootclasspath.append("bin")
    val g = new nsc.Global(settings)
    println("==============================")
    println(filename)
    println("==============================")
    try {
      new (g.Run).compile(List("junit/src/tests/" + filename + ".scala", "junit/src/tests/target.scala"))
    } catch {case ex: Throwable =>
      ex.printStackTrace
      throw ex
    }
    println("")
  }
  
  @Test def testTest1 = runTest("Test1")
  @Test def testTest2 = runTest("Test2")
  @Test def testTest3 = runTest("Test3")

  @Test def testTraits1 = runTest("Traits1")
  @Test def testTraits2 = runTest("Traits2")
  @Test def testTraits3 = runTest("Traits3")
  @Test def testTraits4 = runTest("Traits4")
  @Test def testTraits5 = runTest("Traits5")
  @Test def testTraits6 = runTest("Traits6")
  @Test def testTraits7 = runTest("Traits7")
  @Test def testTraits8 = runTest("Traits8")
  @Test def testTraits9 = runTest("Traits9")
  @Test def testTraits10 = runTest("Traits10")
  @Test def testTraits11 = runTest("Traits11")
  @Test def testTraits12 = runTest("Traits12")
  
  @Test def testThisType1 = runTest("ThisType1")
  @Test def testThisType2 = runTest("ThisType2")
  @Test def testThisType3 = runTest("ThisType3")

  @Test def testOverriding1 = runTest("Overriding1")
  @Test def testOverloading = runTest("Overloading")
  
  @Test def testSimple = runTest("Simple")

  @Test def testCaseClass1 = runTest("CaseClass1")
  @Test def testCaseClass2 = runTest("CaseClass2")
  
  @Test def testClosures1 = runTest("Closures1")
  @Test def testClosures2 = runTest("Closures2")
  
  @Test def testPathTypes1 = runTest("PathTypes1") 
  @Test def testPathTypes2 = runTest("PathTypes2") 
  @Test def testPathTypes3 = runTest("PathTypes3") 
  @Test def testPathTypes4 = runTest("PathTypes4") 
  @Test def testPathTypes5 = runTest("PathTypes5") 
  @Test def testPathTypes6 = runTest("PathTypes6") 
  @Test def testPathTypes7 = runTest("PathTypes7") 
  
  @Test def testAbstractTypes1 = runTest("AbstractTypes1") 
  @Test def testAbstractTypes2 = runTest("AbstractTypes2") 
  @Test def testAbstractTypes3 = runTest("AbstractTypes3") 
  @Test def testAbstractTypes4 = runTest("AbstractTypes4") 
  @Test def testAbstractTypes5 = runTest("AbstractTypes5") 
  @Test def testAbstractTypes6 = runTest("AbstractTypes6") 
  @Test def testAbstractTypes7 = runTest("AbstractTypes7") 
  @Test def testAbstractTypes8 = runTest("AbstractTypes8") 
  @Test def testAbstractTypes9 = runTest("AbstractTypes9") 
  @Test def testAbstractTypes10 = runTest("AbstractTypes10") 
  @Test def testAbstractTypes11 = runTest("AbstractTypes11") 
  @Test def testAbstractTypes13 = runTest("AbstractTypes13") 
  @Test def testAbstractTypes14 = runTest("AbstractTypes14") 

  @Test def testGenerics2 = runTest("Generics2") 
  @Test def testGenerics3 = runTest("Generics3") 
  @Test def testGenerics4 = runTest("Generics4") 
  @Test def testGenerics5 = runTest("Generics5") 
  @Test def testGenerics6 = runTest("Generics6") 
  @Test def testGenerics7 = runTest("Generics7") 
  @Test def testGenerics8 = runTest("Generics8") 
  @Test def testGenerics9 = runTest("Generics9") 
  @Test def testGenerics10 = runTest("Generics10") 
  @Test def testGenerics11 = runTest("Generics11") 
  @Test def testGenerics12 = runTest("Generics12") 
  @Test def testGenerics15 = runTest("Generics15") 
  
  @Test def testSuperCall = runTest("SuperCall") 
   
  @Test def testImplicits1 = runTest("Implicits1") 
  @Test def testImplicits2 = runTest("Implicits2") 
  @Test def testImplicits3 = runTest("Implicits3") 
  @Test def testImplicitArguments1 = runTest("ImplicitsArguments1") 
   
  @Test def testGetterMethod1 = runTest("GetterMethod1") 
  @Test def testGetterMethod2 = runTest("GetterMethod2") 
  @Test def testSealed1 = runTest("Sealed1") 
   
}
