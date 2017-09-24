package com.nthportal.convert

import com.nthportal.convert.ConvertTest._
import org.scalatest.{FlatSpec, Matchers, OptionValues}

class ConvertTest extends FlatSpec with Matchers with OptionValues {


  behavior of "Convert.Valid"

  it should "fail" in {
    val c = Convert.Valid

    an[IllegalStateException] should be thrownBy c.conversion { c.fail(new IllegalStateException()) }
  }

  it should "wrap exceptions" in {
    val c = Convert.Valid

    c.conversion { c.wrapException[NumberFormatException, Int] { "2".toInt } } shouldBe 2

    a[NumberFormatException] should be thrownBy c.conversion {
      c.wrapException[NumberFormatException, Int] { "not a number".toInt }
    }

    a[NonWrappedException] should be thrownBy c.conversion {
      c.wrapException[NumberFormatException, Nothing] { throw new NonWrappedException }
    }
  }

  it should "unwrap results" in {
    implicit val c = Convert.Valid

    c.conversion { c.unwrap(parseInt("1")) * 2 } shouldBe 2

    a[NumberFormatException] should be thrownBy c.conversion {
      c.unwrap(parseInt("not a number")) * 2
    }
  }

  it should "auto-unwrap results" in {
    implicit val c = Convert.Valid

    c.conversion {
      import c.AutoUnwrap.autoUnwrap
      parseInt("1") * 2
    } shouldBe 2

    a[NumberFormatException] should be thrownBy c.conversion {
      import c.AutoUnwrap.autoUnwrap
      parseInt("not a number") * 2
    }
  }

  it should "require something" in {
    val c = Convert.Valid

    an[IllegalArgumentException] should be thrownBy c.conversion { c.require(impossibleRequirement) }
    an[IllegalArgumentException] should be thrownBy c.conversion { c.require(impossibleRequirement, "message") }

    noException should be thrownBy c.conversion { c.require(fulfilledRequirement) }
    noException should be thrownBy c.conversion { c.require(fulfilledRequirement, "message") }
  }

  behavior of "Convert.Any"

  it should "fail" in {
    val c = Convert.Any

    c.conversion { c.fail(new IllegalStateException()) } shouldBe empty
  }

  it should "wrap exceptions" in {
    val c = Convert.Any

    c.conversion { c.wrapException[NumberFormatException, Int] { "2".toInt } }.value shouldBe 2
    c.conversion { c.wrapException[NumberFormatException, Int] { "not a number".toInt } } shouldBe empty

    a[NonWrappedException] should be thrownBy c.conversion {
      c.wrapException[NumberFormatException, Nothing] { throw new NonWrappedException }
    }
  }

  it should "unwrap results" in {
    implicit val c = Convert.Any

    c.conversion { c.unwrap(parseInt("1")) * 2 }.value shouldBe 2

    c.conversion { c.unwrap(parseInt("not a number")) * 2 } shouldBe empty
  }

  it should "auto-unwrap results" in {
    implicit val c = Convert.Any

    c.conversion {
      import c.AutoUnwrap.autoUnwrap
      parseInt("1") * 2
    }.value shouldBe 2

    c.conversion {
      import c.AutoUnwrap.autoUnwrap
      parseInt("not a number") * 2
    } shouldBe empty
  }

  it should "require something" in {
    val c = Convert.Any

    c.conversion {
      c.require(impossibleRequirement)
      true
    } shouldBe empty

    var initialized = false
    c.conversion {
      c.require(impossibleRequirement, "message")
      initialized = true
      true
    } shouldBe empty
    initialized shouldBe false

    c.conversion {
      c.require(fulfilledRequirement)
      true
    }.value shouldBe true

    c.conversion {
      c.require(fulfilledRequirement, "message")
      initialized = true
      true
    }.value shouldBe true
    initialized shouldBe true
  }

  behavior of "Convert companion object"

  it should "synthesize Conversions" in {
    val parseBoolean = Convert.synthesize(parseBooleanThrowing, parseBooleanAsOption)

    locally {
      import Convert.Valid.Implicit.ref

      parseBoolean("true") shouldBe true
      parseBoolean("false") shouldBe false
      an[IllegalArgumentException] should be thrownBy { parseBoolean("not a boolean") }
    }

    locally {
      import Convert.Any.Implicit.ref

      parseBoolean("true").value shouldBe true
      parseBoolean("false").value shouldBe false
      parseBoolean("not a boolean") shouldBe empty
    }
  }
}

object ConvertTest {
  val fulfilledRequirement = true
  val impossibleRequirement = false

  class NonWrappedException extends Exception

  def parseInt(s: String)(implicit c: Convert): c.Result[Int] = {
    c.conversion {
      c.wrapException[NumberFormatException, Int](s.toInt)
    }
  }

  def parseBooleanThrowing(s: String): Boolean = {
    s.toLowerCase match {
      case "true" => true
      case "false" => false
      case _ => throw new IllegalArgumentException
    }
  }

  def parseBooleanAsOption(s: String): Option[Boolean] = {
    s.toLowerCase match {
      case "true" => Some(true)
      case "false" => Some(false)
      case _ => None
    }
  }
}
