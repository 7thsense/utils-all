package examples

import cats.implicits._
import com.theseventhsense.utils.types.SSDateTime
import com.theseventhsense.utils.types.SSDateTime.Instant
import org.scalatest.{MustMatchers, WordSpec}

/**
  * Created by erik on 5/19/17.
  */
class ExampleSpec extends WordSpec with MustMatchers {
  /** 
    * Roughly equivalant to MyAbstractClass 
    */
  trait MyTrait {
    def age: Int
  }
  
  /** 
    * Class must be abstract because age has no implementation 
    */
  abstract class Vehicle extends MyTrait {
    override def age: Int
  }
  
  class NewCar extends Vehicle {
    override def age: Int = 1
  }
  
  class OlderCar extends Vehicle {
    override def age: Int = 2
  }
  
  class FunkyOldTruck(override val age: Int) extends Vehicle {
    def gender: String = "male"
  }
  
  lazy val myInstance: Vehicle = new NewCar()
  lazy val myOtherInstance: Vehicle = new OlderCar()
  lazy val myTruck: Vehicle = new FunkyOldTruck(77)
  
  "the datatime library" should {
    "teach us how to create an abstract class" in {
      assert(true)
    }
    "be able to create an instance of MyConcreteClass" in {
      myInstance mustBe a[NewCar]
      myInstance mustBe a[Vehicle]
    }
    "instances of MyAbstrctClass should alwayshave an age method" in {
      myInstance.age must not equal myOtherInstance.age
    }
    "instants should be constructable" in {
      val instant1: SSDateTime.Instant = new SSDateTime.Instant(1L)
      val instant3: SSDateTime.Instant = SSDateTime.Instant.apply(1L)
      val instant2: SSDateTime.Instant = SSDateTime.Instant(1L)
      val instant4: Either[Instant.ParseError, SSDateTime.Instant] = SSDateTime.Instant.parse("1235098345")
      val instant5: Either[Instant.ParseError, SSDateTime.Instant] = SSDateTime.Instant.parse("now is the time for all good me")
      val instant6 = SSDateTime.Instant.parse("2017-01-01T00:01:02Z")
      instant6.toOption.value  
    }
  }

}
