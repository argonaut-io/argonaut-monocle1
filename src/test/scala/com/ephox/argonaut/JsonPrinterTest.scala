package com.ephox.argonaut

import org.scalacheck.Properties
import org.scalacheck.Prop._
import JsonParser._
import JsonPrinter._
import Data._

object JsonPrinterTest extends Properties("JsonPrinter") {
  /*
  // todo failing tests
  property("json prints consistently, compact -> pretty") =
          forAll({(j: Json) =>
              pretty(j) == pretty(parse(compact(j)).get)
            })
  
  property("json prints consistently, pretty -> compact") =
          forAll({(j: Json) =>
              compact(j) == compact(parse(pretty(j)).get)
            })
  */
}
