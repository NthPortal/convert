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
"com.nthportal" %% "convert" % "0.4.0"
```

### Maven

**Scala 2.12**

```xml
<dependency>
  <groupId>com.nthportal</groupId>
  <artifactId>convert_2.12</artifactId>
  <version>0.4.0</version>
</dependency>
```

**Scala 2.11**

```xml
<dependency>
  <groupId>com.nthportal</groupId>
  <artifactId>convert_2.11</artifactId>
  <version>0.4.0</version>
</dependency>
```

## Usage

### As a Client

Suppose you wish to convert the a `String` to a `BigDecimal` using the following
method:

```scala
import com.nthportal.convert.Convert

def parseBigDecimal(s: String)(c: Convert): c.Result[BigDecimal] = ???
```

If you would like the method to throw an exception if the string does not
represent a valid `BigDecimal`, invoke the method as follows (using
`"2.718281828"` as an example string):

```scala
import com.nthportal.convert.Convert

val e: BigDecimal = parseBigDecimal("2.718281828")(Convert.Throwing)
```

If you would like the method to return an `Option` containing the result if
the string represents a valid `BigDecimal`, and `None` otherwise, invoke the
method as follows (again, using `"2.718281828"` as an example string):

```scala
import com.nthportal.convert.Convert

val e: Option[BigDecimal] = parseBigDecimal("2.718281828")(Convert.AsOption)
```

### In a Library
