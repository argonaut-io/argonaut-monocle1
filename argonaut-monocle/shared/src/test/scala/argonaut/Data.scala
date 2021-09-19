package argonaut

import scalaz._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen.{frequency, listOfN, const => value, oneOf}
import Json._
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Cogen, Gen}

import scala.util.Random.shuffle

object Data {
  val maxJsonStructureDepth = 3

  implicit val bigDecimalEq: Equal[BigDecimal] = Equal.equalA[BigDecimal]
  implicit val bigIntEq: Equal[BigInt] = Equal.equalA[BigInt]

  implicit val jsonNumberCogen: Cogen[JsonNumber] =
    Cogen[BigDecimal].contramap[JsonNumber](_.toBigDecimal)

  implicit val jsonObjectCogen: Cogen[JsonObject] =
    Cogen[List[(String, Json)]].contramap[JsonObject](_.toMap.toList)

  implicit def jsonCogen: Cogen[Json] =
    Cogen((seed: Seed, json: Json) => json.fold(
      seed,
      Cogen[Boolean].perturb(seed, _),
      Cogen[JsonNumber].perturb(seed, _),
      Cogen[String].perturb(seed, _),
      Cogen[List[Json]].perturb(seed, _),
      Cogen[JsonObject].perturb(seed, _)
    ))

  // TODO: Add in generator for numbers that have an exponent that BigDecimal can't handle.
  val jsonNumberRepGenerator: Gen[JsonNumber] = Gen.oneOf(
    arbitrary[List[Long]].map(ln => JsonBigDecimal(BigDecimal("0" ++ ln.filter(_ >= 0).mkString))),
    arbitrary[Double].map(d => JsonDecimal(d.toString)),
    arbitrary[Long].map(JsonLong(_))
  )

  val jsonNumberGenerator: Gen[JNumber] =
    jsonNumberRepGenerator.map(number => JNumber(number))

  def isValidJSONCharacter(char: Char): Boolean = !char.isControl && char != '\\' && char != '\"'

  val stringGenerator: Gen[String] = arbitrary[String]

  val jsonStringGenerator: Gen[JString] = stringGenerator.map(string => JString(string))

  val jsonBoolGenerator: Gen[JBool] = oneOf(JBool(true), JBool(false))

  val jsonNothingGenerator: Gen[Json] = value(JNull)

  def jsonArrayItemsGenerator(depth: Int = maxJsonStructureDepth): Gen[Seq[Json]] = listOfN(5, jsonValueGenerator(depth - 1))

  def jsonArrayGenerator(depth: Int = maxJsonStructureDepth): Gen[JArray] = jsonArrayItemsGenerator(depth).map{values => JArray(values.toList)}

  def jsonObjectFieldsGenerator(depth: Int = maxJsonStructureDepth): Gen[Seq[(JString, Json)]] = listOfN(5, arbTuple2(Arbitrary(jsonStringGenerator), Arbitrary(jsonValueGenerator(depth - 1))).arbitrary)

  private def arbImmutableMap[T: Arbitrary, U: Arbitrary]: Arbitrary[Map[T, U]] =
    Arbitrary(Gen.listOf(arbTuple2[T, U].arbitrary).map(_.toMap))

  def jsonObjectGenerator(depth: Int = maxJsonStructureDepth): Gen[JObject] = arbImmutableMap(Arbitrary(arbitrary[String]), Arbitrary(jsonValueGenerator(depth - 1))).arbitrary.map{map =>
    JObject(JsonObject.fromIterable(map.toList))
  }

  val nonJsonObjectGenerator = oneOf(jsonNumberGenerator, jsonStringGenerator, jsonBoolGenerator, jsonNothingGenerator, jsonArrayGenerator())

  val jsonObjectOrArrayGenerator = oneOf(jsonObjectGenerator(), jsonArrayGenerator())

  def jsonValueGenerator(depth: Int = maxJsonStructureDepth): Gen[Json] = {
    if (depth > 1) {
      oneOf(jsonNumberGenerator, jsonStringGenerator, jsonBoolGenerator, jsonNothingGenerator, jsonArrayGenerator(depth - 1), jsonObjectGenerator(depth - 1))
    } else {
      oneOf(jsonNumberGenerator, jsonStringGenerator, jsonBoolGenerator, jsonNothingGenerator)
    }
  }

  def objectsOfObjectsGenerator(depth: Int = maxJsonStructureDepth): Gen[Json] = {
    if (depth > 1) {
      listOfN(2, arbTuple2(Arbitrary(arbitrary[String]), Arbitrary(objectsOfObjectsGenerator(depth - 1))).arbitrary).map(fields => JObject(JsonObject.fromIterable(fields)))
    } else {
      oneOf(jsonNumberGenerator, jsonStringGenerator, jsonBoolGenerator, jsonNothingGenerator)
    }
  }

  val arrayOrObjectAndPathGenerator: Gen[(Seq[String], Json, Json)] = objectsOfObjectsGenerator().map{jsonvalue =>
    def buildPath(currentPath: Seq[String], original: Json, jsonValue: Json): (Seq[String], Json, Json) = {
      jsonValue match {
        case jsonObject: JObject => {
          shuffle(jsonObject.o.toMap.toList.collect{case pair@ (innerString: String, innerValue: Json) => pair})
            .headOption
            .map{innerPair =>
              buildPath(currentPath :+ innerPair._1, original, innerPair._2)
            }
            .getOrElse((currentPath, original, jsonObject))
        }
        case other => (currentPath, original, other)
      }
    }
    buildPath(Seq(), jsonvalue, jsonvalue)
  }

  implicit def ArbitraryJString: Arbitrary[JString] = Arbitrary(jsonStringGenerator)

  implicit def ArbitraryJNumber: Arbitrary[JNumber] = Arbitrary(jsonNumberGenerator)

  implicit def ArbitraryJsonNumber: Arbitrary[JsonNumber] =
    Arbitrary(jsonNumberRepGenerator)

  implicit def ArbitraryJArray: Arbitrary[JArray] = Arbitrary(jsonArrayGenerator())

  implicit def ArbitraryJObject: Arbitrary[JObject] = Arbitrary(jsonObjectGenerator())

  implicit def ArbitraryJBool: Arbitrary[JBool] = Arbitrary(jsonBoolGenerator)

  implicit def ArbitraryJson: Arbitrary[Json] = Arbitrary(jsonValueGenerator())

  implicit def ArbitraryJsonObject: Arbitrary[JsonObject] =
    Arbitrary(arbitrary[List[(JsonField, Json)]] map { JsonObject.fromIterable(_) })

  implicit def ArbitraryCursor: Arbitrary[Cursor] = {
    Arbitrary(arbitrary[Json] flatMap (j => {
      val c = +j
      j.arrayOrObject(
        Gen.const(c)
      , _ =>
          for {
            r <- frequency((90, arbitrary[Cursor]), (10, c))
          } yield c.right getOrElse r
      , o =>
          for {
            r <- frequency((90, arbitrary[Cursor]), (10, c))
            q <- frequency((90, if(o.fields.nonEmpty) oneOf(o.fields) else arbitrary[JsonField]), (10, arbitrary[JsonField]))
          } yield c downField q getOrElse r
      )
    }))
  }

  implicit val ArbitraryPrettyParams: Arbitrary[PrettyParams] = Arbitrary(
    for {
      indent <- arbitrary[String]
      lbraceLeft <- arbitrary[String]
      lbraceRight <- arbitrary[String]
      rbraceLeft <- arbitrary[String]
      rbraceRight <- arbitrary[String]
      lbracketLeft <- arbitrary[String]
      lbracketRight <- arbitrary[String]
      rbracketLeft <- arbitrary[String]
      rbracketRight <- arbitrary[String]
      lrbracketsEmpty <- arbitrary[String]
      arrayCommaLeft <- arbitrary[String]
      arrayCommaRight <- arbitrary[String]
      objectCommaLeft <- arbitrary[String]
      objectCommaRight <- arbitrary[String]
      colonLeft <- arbitrary[String]
      colonRight <- arbitrary[String]
      preserveOrder <- arbitrary[Boolean]
      dropNullKeys <- arbitrary[Boolean]
    } yield PrettyParams(
      indent = indent
    , lbraceLeft = lbraceLeft
    , lbraceRight = lbraceRight
    , rbraceLeft = rbraceLeft
    , rbraceRight = rbraceRight
    , lbracketLeft = lbracketLeft
    , lbracketRight = lbracketRight
    , rbracketLeft = rbracketLeft
    , rbracketRight = rbracketRight
    , lrbracketsEmpty = lrbracketsEmpty
    , arrayCommaLeft = arrayCommaLeft
    , arrayCommaRight = arrayCommaRight
    , objectCommaLeft = objectCommaLeft
    , objectCommaRight = objectCommaRight
    , colonLeft = colonLeft
    , colonRight = colonRight
    , preserveOrder = preserveOrder
    , dropNullKeys = dropNullKeys
    )
  )
}
