package com.nthportal.convert

import org.scalatest.{FlatSpec, Matchers, OptionValues}

class ConvertTest extends FlatSpec with Matchers with OptionValues {

  import ConvertTest._

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

    c.conversion {
      val res = c.unwrap(parseInt("1"))
      res * 2
    } shouldBe 2

    a[NumberFormatException] should be thrownBy c.conversion {
      val res = c.unwrap(parseInt("not a number"))
      res * 2
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

    c.conversion {
      val res = c.unwrap(parseInt("1"))
      res * 2
    }.value shouldBe 2

    c.conversion {
      val res = c.unwrap(parseInt("not a number"))
      res * 2
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

}
