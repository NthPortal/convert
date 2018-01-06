# convert
A Scala library for handling conversions between types by throwing exceptions or returning Options containing the results

[![Build Status](https://travis-ci.org/NthPortal/convert.svg?branch=master)](https://travis-ci.org/NthPortal/convert)
[![Coverage Status](https://coveralls.io/repos/github/NthPortal/convert/badge.svg?branch=master)](https://coveralls.io/github/NthPortal/convert?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/com.nthportal/convert_2.12.svg)](https://mvnrepository.com/artifact/com.nthportal/convert_2.12)
[![Versioning](https://img.shields.io/badge/versioning-semver%202.0.0-blue.svg)](http://semver.org/spec/v2.0.0.html)
[![Docs](https://www.javadoc.io/badge/com.nthportal/convert_2.12.svg?color=blue&label=docs)](https://www.javadoc.io/doc/com.nthportal/convert_2.12)


## Add as a Dependency

### SBT (Scala 2.11 and 2.12)

```sbtshell
"com.nthportal" %% "convert" % "0.5.0"
```

### Maven

**Scala 2.12**

```xml
<dependency>
  <groupId>com.nthportal</groupId>
  <artifactId>convert_2.12</artifactId>
  <version>0.5.0</version>
</dependency>
```

**Scala 2.11**

```xml
<dependency>
  <groupId>com.nthportal</groupId>
  <artifactId>convert_2.11</artifactId>
  <version>0.5.0</version>
</dependency>
```

## Usage: As a Client

Suppose you wish to convert the a `String` to a `BigDecimal` using the following
method:

```scala
import com.nthportal.convert.Convert

def parseBigDecimal(s: String)(implicit c: Convert): c.Result[BigDecimal] = ???
```

If you would like the method to throw an exception if the string does not
represent a valid `BigDecimal`, invoke the method as follows (using
`"2.718281828"` as an example string):

```scala
val e: BigDecimal = parseBigDecimal("2.718281828")(Convert.Throwing)
```

When using `Convert.Throwing`, the return type `c.Result[BigDecimal]` in the
above method is just `BigDecimal`.

If you would like the method to return an `Option` containing the result if
the string represents a valid `BigDecimal`, and `None` otherwise, invoke the
method as follows (again, using `"2.718281828"` as an example string):

```scala
val e: Option[BigDecimal] = parseBigDecimal("2.718281828")(Convert.AsOption)
```

When using `Convert.AsOption`, the return type `c.Result[BigDecimal]` in the
above method is `Option[BigDecimal]`.

Alternatively, the `Convert` with the desired behaviour can be imported as
an implicit, as in the following two examples:

```scala
import com.nthportal.convert.Convert.Throwing.Implicit.ref

val e: BigDecimal = parseBigDecimal("2.718281828")
```

```scala
import com.nthportal.convert.Convert.AsOption.Implicit.ref

val e: Option[BigDecimal] = parseBigDecimal("2.718281828")
```

## Usage: In a Library

### Basics

The core methods of `Convert` are `conversion` and `fail`. Additionally, `unwrap`
is essential when chaining multiple conversions together.

To demonstrate how to use the first two of these methods, let's write a simple
method to parse `Boolean`s from `String`s

```scala
import com.nthportal.convert.Convert

def parseBoolean(s: String)(implicit c: Convert): c.Result[Boolean] = {
  ??? // do parsing here
}
```

When using `Convert`, it is imperative that all parsing or conversion operations
take place inside a `conversion` block

```scala
import com.nthportal.convert.Convert

def parseBoolean(s: String)(implicit c: Convert): c.Result[Boolean] = {
  c.conversion {
    ??? // do parsing here
  }
}
```

Now, let us implement the conversion by just checking if the (lowercase) string
is equal to `"true"` or `"false"`

```scala
import com.nthportal.convert.Convert

def parseBoolean(s: String)(implicit c: Convert): c.Result[Boolean] = {
  c.conversion {
    s.toLowerCase match {
      case "true" => true
      case "false" => false
      case _ => ??? // it's not a valid boolean - now what? 
    }
  }
}
```

The final thing to do for this parsing method is to handle invalid inputs; to
'fail' the conversion. In Java, one would usually throw an exception.
With `Convert`, instead of `throw`ing the exception, you call the method `fail`
with the exception you would have thrown

```scala
import com.nthportal.convert.Convert

def parseBoolean(s: String)(implicit c: Convert): c.Result[Boolean] = {
  c.conversion {
    s.toLowerCase match {
      case "true" => true
      case "false" => false
      case _ => c.fail(new IllegalArgumentException(s"$s wasn't 'true' or 'false'"))
    }
  }
}
```

What if you need to use the result of another conversion inside of yours?
Suppose you want to be able to parse a comma-separated pair of `Boolean`s.
Let us write a simple method to do so which uses `unwrap` (note: `unwrap` MUST
be called *inside* of the `conversion` block)

```scala
import com.nthportal.convert.Convert

def parseBoolean(s: String)(implicit c: Convert): c.Result[Boolean] = ??? // The same as defined above

def parseBooleanPair(s: String)(implicit c: Convert): c.Result[(Boolean, Boolean)] = {
  c.conversion {
    s split ',' match {
      case Array(a, b) => (c.unwrap(parseBoolean(a)), c.unwrap(parseBoolean(b)))
      case _ => c.fail(new IllegalArgumentException("Not a pair"))
    }
  }
}
```

Now you know how to write a conversion, and how to chain multiple conversions
together!

### Utility Methods

#### `require`

A common method used when parsing is `require` (defined in `Predef`).
Unfortunately, `Predef.require` always throws an exception when it fails, which
is not desirable if a caller wants an `Option` back. To solve this, `Convert`
defines two `require` methods with signatures identical to the signatures of
those in `Predef`. They can be used as drop-in replacements for latter (but
MUST be called *inside* of a `conversion` block).

For example, let us write a simple method to convert a `List` of two elements
to a `Tuple2`

```scala
import com.nthportal.convert.Convert

def list2Tuple[A](list: List[A])(implicit c: Convert): c.Result[(A, A)] = {
  c.conversion {
    c.require(list.size == 2, "List did not have exactly two elements")
    (list.head, list.tail.head)
  }
}
```

#### `wrapException`

Sometimes you may wish to use a conversion function which always throws
exceptions, because you either cannot or do not want to re-implement it. For
example, you may not wish to implement integer parsing, but instead use Java's
`Integer.parseInt`. This can be accomplished easily using `wrapException` (which
MUST be called *inside* of a `conversion` block) in either of the following ways:

```scala
import com.nthportal.convert.Convert

def parseInt1(s: String)(implicit c: Convert): c.Result[Int] = {
  c.conversion {
    c.wrapException[NumberFormatException, Int](Integer.parseInt(s))
  }
}

def parseInt2(s: String)(implicit c: Convert): c.Result[Int] = {
  c.conversion {
    c.wrapException(_.isInstanceOf[NumberFormatException])(Integer.parseInt(s))
  }
}
```

### Synthesizing Conversions

Two functions, one of which throws exceptions, and one of which returns
`Option`s, can be synthesized into a single conversion function using
`Convert.synthesize`. For example, suppose the following two methods to parse
`Boolean`s from `String`s are defined:

```scala
def parseBooleanThrowing(s: String): Boolean = ???
def parseBooleanAsOption(s: String): Option[Boolean] = ???
```

They can be combined into a conversion function with `Convert.synthesize`

```scala
import com.nthportal.convert.Convert

val booleanParser: Convert.Conversion[String, Boolean] =
  Convert.synthesize(parseBooleanThrowing, parseBooleanAsOption)
```

### Automatic Unwrapping (USE WITH CARE)

Sometimes, an excess of calls to `Convert.unwrap` can reduce code readability.
Readability can be improved by importing `Convert.AutoUnwrap.autoUnwrap` - an
implicit conversion from `Convert#Result[T]` to `T`. However, use of this
implicit conversion is not without risks.

**USE THIS IMPLICIT CONVERSION AT YOUR OWN RISK**

This implicit conversion can improve code readability; however, it
should be used with care. As with all implicit conversions, it may
be difficult to tell when it is applied by the compiler, leading to
unexpected behavior which is difficult to debug.

Additionally, because `unwrap` MUST be called within a conversion block,
this implicit conversion MUST be called within a conversion block as well.
However, if imported in the wrong scope, the compiler may insert a call
to this implicit conversion *outside* of any conversion block.

Use this implicit conversion with care.

If you have decided to use automatic unwrapping, its benefits can be seen by
revisiting the `parseBooleanPair` method from earlier. We originally defined
the method as

```scala
import com.nthportal.convert.Convert

def parseBoolean(s: String)(implicit c: Convert): c.Result[Boolean] = ??? // The same as defined above

def parseBooleanPair(s: String)(implicit c: Convert): c.Result[(Boolean, Boolean)] = {
  c.conversion {
    s split ',' match {
      case Array(a, b) => (c.unwrap(parseBoolean(a)), c.unwrap(parseBoolean(b)))
      case _ => c.fail(new IllegalArgumentException("Not a pair"))
    }
  }
}
```

Using `AutoUnwrap` allows us to tidy it up a little bit

```scala
def parseBooleanPair(s: String)(implicit c: Convert): c.Result[(Boolean, Boolean)] = {
  c.conversion {
    import c.AutoUnwrap.autoUnwrap
    s split ',' match {
      case Array(a, b) => (parseBoolean(a), parseBoolean(b))
      case _ => c.fail(new IllegalArgumentException("Not a pair"))
    }
  }
}
```

`AutoUnwrap` makes it more clear that we are just returning a tuple from the
results of calling `parseBoolean` on the two elements.
