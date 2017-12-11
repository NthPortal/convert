package com.nthportal.convert

import com.nthportal.convert.SpecializationTypes.specTypes

import scala.language.{higherKinds, implicitConversions}
import scala.reflect.ClassTag
import scala.util.control.{ControlThrowable, NonFatal}

/** An object for handling a conversion between two types.
  *
  * [[Convert.Throwing]] returns the result of a conversion as is,
  * and throws an exception if the conversion fails.
  *
  * [[Convert.AsOption]] returns the result of a conversion wrapped in
  * an [[scala.Option Option]], and returns [[scala.None None]] if
  * the conversion fails.
  *
  * @example
  * {{{
  * /* Convert a string to a boolean */
  * def parseBoolean(s: String)(implicit c: Convert): c.Result[Boolean] = {
  *   c.conversion {
  *     s.toLowerCase match {
  *       case "true" => true
  *       case "false" => false
  *       case _ => c.fail(new IllegalArgumentException(s"'\$s' is not 'true' or 'false'"))
  *     }
  *   }
  * }
  *
  * val res1: Boolean = parseBoolean("true")(Convert.Throwing)
  * val res2: Option[Boolean] = parseBoolean("true")(Convert.AsOption)
  * }}}
  * @define withinConversion This method MUST be called within a conversion
  *                          block from this Convert instance.
  */
sealed trait Convert {
  self =>

  /** A function which takes the result type of a conversion as input,
    * and yields the return type of the conversion block.
    *
    * For example, if converting a String to a Boolean, this is a function
    * which takes `Boolean` as input, and yields some type `Result[Boolean]`
    * (`Boolean` for Convert.Throwing, and `Option[Boolean]` for
    * Convert.AsOption).
    */
  type Result[+T]

  /** Performs a conversion.
    *
    * Conversion operations MUST take place within this block
    * (a 'conversion block').
    *
    * @example
    * {{{
    * conversion {
    *   val res = ??? // do conversion
    *   if (cond) fail(new IllegalArgumentException("invalid input")) // fail if something goes wrong
    *   res // return a result
    * }
    * }}}
    *
    * @param body the body of the conversion
    * @tparam T the type of the conversion's result
    * @return the result of the conversion, if it did not fail
    */
  def conversion[@specialized(specTypes) T](body: => T): Result[T]

  /** Fails a conversion with the specified exception.
    *
    * $withinConversion
    *
    * @param ex an exception to throw, if this Convert throws exceptions
    */
  def fail(ex: => Exception): Nothing

  /** Unwraps the [[Result]] of another conversion.
    *
    * $withinConversion
    *
    * @param result the result of another conversion
    * @tparam T the type of the `Result[T]`
    * @return the result of the other conversion, not wrapped as a `Result`
    */
  def unwrap[@specialized(specTypes) T](result: Result[T]): T

  /** Tests an expression, failing the conversion if false. Analogous to
    * [[scala.Predef.require(requirement:<?>):Unit* Predef.require]], except
    * that it fails the conversion instead of always throwing an exception.
    *
    * $withinConversion
    *
    * @param requirement the expression to test
    */
  @inline
  final def require(requirement: Boolean): Unit = {
    if (!requirement) fail(new IllegalArgumentException("requirement failed"))
  }

  /** Tests an expression, failing the conversion if false. Analogous to
    * [[scala.Predef.require(requirement:<?>,message:<?>):Unit* Predef.require]],
    * except that it fails the conversion instead of always throwing an exception.
    *
    * $withinConversion
    *
    * @param requirement the expression to test
    * @param message     a String to include in the failure message
    */
  @inline
  final def require(requirement: Boolean, message: => Any): Unit = {
    if (!requirement) fail(new IllegalArgumentException("requirement failed: " + message))
  }

  /** Wraps a block of code which throws an exception, failing the
    * conversion if the block throws an exception of the specified
    * type.
    *
    * $withinConversion
    *
    * @param body the block of code which may throw an exception
    * @tparam E the type of exceptions that should be wrapped
    * @tparam T the type of the result of `body`
    * @return the result of `body`, if it did not fail
    */
  final def wrapException[E <: Exception : ClassTag, @specialized(specTypes) T](body: => T): T = {
    wrapException(implicitly[ClassTag[E]].runtimeClass.isInstance(_))(body)
  }

  /** Wraps a block of code which throws an exception, failing the
    * conversion if the block throws an exception, and the exception
    * matches the specified predicate.
    *
    * $withinConversion
    *
    * @param matches a predicate to match exceptions that should be wrapped
    * @param body    the block of code which may throw an exception
    * @tparam T the type of the result of `body`
    * @return the result of `body`, if it did not fail
    */
  final def wrapException[@specialized(specTypes) T](matches: Exception => Boolean)(body: => T): T = {
    try {
      body
    } catch {
      case NonFatal(e: Exception) if matches(e) => fail(e)
    }
  }

  /** An object containing an implicit conversion from `Result[T]` to `T`.
    * The implicit conversion allows the automatic unwrapping of
    * [[Result Results]], without explicitly calling [[unwrap]].
    *
    * '''USE THIS IMPLICIT CONVERSION AT YOUR OWN RISK'''
    *
    * This implicit conversion can improve code readability; however, it
    * should be used with care. As with all implicit conversions, it may
    * be difficult to tell when it is applied by the compiler, leading to
    * unexpected behavior which is difficult to debug.
    *
    * Additionally, because [[unwrap]] MUST be called within a conversion block,
    * this implicit conversion MUST be called within a conversion block as well.
    * However, if imported in the wrong scope, the compiler may insert a call
    * to this implicit conversion ''outside'' of any conversion block.
    *
    * Use this implicit conversion with care.
    */
  object AutoUnwrap {
    /** Automatically unwraps the [[Result]] of another conversion.
      *
      * $withinConversion
      *
      * @param result the result of another conversion
      * @tparam T the type of the `Result[T]`
      * @return the result of the other conversion, not wrapped as a `Result`
      */
    implicit def autoUnwrap[T](result: Result[T]): T = unwrap(result)
  }

  /** A wrapper object for an implicit reference to the enclosing [[Convert]]. */
  object Implicit {
    /** Import to bring the enclosing [[Convert]] into scope as an implicit. */
    implicit val ref: self.type = self
  }

}

object Convert {
  /** Synthesizes a [[Conversion]] from two conversion functions: one which throws
    * exceptions on failure, and one which returns an [[scala.Option Option]].
    *
    * @param throwing a conversion function which throws exceptions on failure
    * @param asOption a conversion function which returns an Option
    * @tparam T the input type of the conversion
    * @tparam R the output type of the conversion
    */
  def synthesize[T, R](throwing: T => R, asOption: T => Option[R]): Conversion[T, R] = {
    new Conversion[T, R] {
      override def apply(t: T)(implicit c: Convert): c.Result[R] = c match {
        case Throwing => throwing(t).asInstanceOf[c.Result[R]]
        case AsOption => asOption(t).asInstanceOf[c.Result[R]]
      }
    }
  }

  /** A conversion from `T` to `R`.
    *
    * Alternatively, a function `T => R` where there is an implicit
    * `Convert` in scope.
    *
    * @tparam T the input type of the conversion
    * @tparam R the output type of the conversion
    */
  trait Conversion[-T, +R] {
    /** Converts `t` to an `R`.
      *
      * @param t the thing to convert
      * @param c the `Convert` to use
      * @return `t` as an `R`
      */
    def apply(t: T)(implicit c: Convert): c.Result[R]
  }

  /** Type member alias for [[Convert]]. */
  type Aux[R[X]] = Convert { type Result[T] = R[T] }

  /** Utility object containing type aliases/functions. */
  object Type {
    /** Identity type function. */
    type Id[T] = T

    /** Type alias for the type of [[Convert.Throwing]]. */
    type Throwing = Aux[Id]

    /** Type alias for the type of [[Convert.AsOption]]. */
    type AsOption = Aux[Option]
  }

  /**
    * A [[Convert]] which returns the result of a conversion as is,
    * and throws an exception if the conversion fails.
    */
  object Throwing extends Convert {
    self: Type.Throwing =>

    override type Result[+T] = T

    override def conversion[@specialized(specTypes) T](res: => T): T = res

    /** Throws the specified exception.
      *
      * $withinConversion
      *
      * @param ex the exception to throw
      */
    override def fail(ex: => Exception): Nothing = throw ex

    /** Returns the result of another conversion.
      *
      * $withinConversion
      *
      * @param result the result of another conversion
      * @tparam T the type of the result
      * @return the result of the other conversion
      */
    override def unwrap[@specialized(specTypes) T](result: T): T = result
  }

  /**
    * A [[Convert]] which returns the result of a conversion wrapped in
    * an [[scala.Option Option]], and returns [[scala.None None]] if
    * the conversion fails.
    */
  object AsOption extends Convert {
    self: Type.AsOption =>

    override type Result[+T] = Option[T]

    override def conversion[@specialized(specTypes) T](res: => T): Option[T] = {
      try {
        Some(res)
      } catch {
        case FailControl => None
      }
    }

    /** Terminates the enclosing conversion block with [[scala.None None]].
      *
      * $withinConversion
      *
      * @param ex ignored
      */
    override def fail(ex: => Exception): Nothing = throw FailControl

    /** Unwraps the result of another conversion (an [[scala.Option Option]]).
      *
      * If the result was [[scala.None None]], this terminates the enclosing
      * conversion block with None; otherwise, it returns the value of the Option.
      *
      * $withinConversion
      *
      * @param result the result of another conversion
      * @tparam T the type of the `Option[T]`
      * @return the result of the other conversion, not wrapped in an Option
      */
    override def unwrap[@specialized(specTypes) T](result: Option[T]): T = result match {
      case Some(t) => t
      case None => throw FailControl
    }
  }

  private case object FailControl extends ControlThrowable

}
