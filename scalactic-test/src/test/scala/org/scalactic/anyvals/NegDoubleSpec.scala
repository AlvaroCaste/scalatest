/*
 * Copyright 2001-2014 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalactic.anyvals

import org.scalatest._
import org.scalacheck.Gen._
import org.scalacheck.{Arbitrary, Gen}
import org.scalactic.Equality
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.prop.PropertyChecks
// SKIP-SCALATESTJS,NATIVE-START
import scala.collection.immutable.NumericRange
// SKIP-SCALATESTJS,NATIVE-END
import scala.collection.mutable.WrappedArray
import OptionValues._
import scala.util.{Failure, Success, Try}
import org.scalatest.Inspectors
import org.scalactic.{Good, Bad}
import org.scalactic.{Pass, Fail}

trait NegDoubleSpecSupport {

  val negZDoubleGen: Gen[NegZDouble] =
    for {i <- choose(Double.MinValue, 0.0)} yield NegZDouble.ensuringValid(i)

  implicit val arbNegZDouble: Arbitrary[NegZDouble] = Arbitrary(negZDoubleGen)

  val negDoubleGen: Gen[NegDouble] =
    for {i <- choose(Double.MinValue, -Double.MinPositiveValue)} yield NegDouble.ensuringValid(i)

  implicit val arbNegDouble: Arbitrary[NegDouble] = Arbitrary(negDoubleGen)

  implicit def tryEquality[T]: Equality[Try[T]] = new Equality[Try[T]] {
    override def areEqual(a: Try[T], b: Any): Boolean = a match {
      case Success(double: Double) if double.isNaN =>  // This is because in scala.js x/0 results to NaN not ArithmetricException like in jvm, and we need to make sure Success(NaN) == Success(NaN) is true to pass the test.
        b match {
          case Success(bDouble: Double) if bDouble.isNaN => true
          case _ => false
        }
      case _: Success[_] => a == b
      case Failure(ex) => b match {
        case _: Success[_] => false
        case Failure(otherEx) => ex.getClass == otherEx.getClass && ex.getMessage == otherEx.getMessage
        case _ => false
      }
    }
  }

}

class NegDoubleSpec extends FunSpec with Matchers with PropertyChecks with TypeCheckedTripleEquals with NegDoubleSpecSupport {

  describe("A NegDouble") {
    describe("should offer a from factory method that") {
      it("returns Some[NegDouble] if the passed Double is lesser than 0") {
        NegDouble.from(-50.23).value.value shouldBe -50.23
        NegDouble.from(-100.0).value.value shouldBe -100.0
      }
      it("returns None if the passed Double is NOT lesser than 0") {
        NegDouble.from(0.0) shouldBe None
        NegDouble.from(0.00001) shouldBe None
        NegDouble.from(99.9) shouldBe None
      }
    }
    describe("should offer an ensuringValid factory method that") {
      it("returns NegDouble if the passed Double is lesser than 0") {
        NegDouble.ensuringValid(-50.23).value shouldBe -50.23
        NegDouble.ensuringValid(-100.0).value shouldBe -100.0
        NegDouble.ensuringValid(Double.NegativeInfinity).value shouldBe Double.NegativeInfinity
      }
      it("throws AssertionError if the passed Double is NOT lesser than 0") {
        an [AssertionError] should be thrownBy NegDouble.ensuringValid(0.0)
        an [AssertionError] should be thrownBy NegDouble.ensuringValid(0.00001)
        an [AssertionError] should be thrownBy NegDouble.ensuringValid(99.9)
        an [AssertionError] should be thrownBy NegDouble.ensuringValid(Double.PositiveInfinity)
        an [AssertionError] should be thrownBy NegDouble.ensuringValid(Double.NaN)
      }
    }
    describe("should offer a tryingValid factory method that") {
      import TryValues._
      it("returns a NegDouble wrapped in a Success if the passed NegDouble is lesser than 0") {
        NegDouble.tryingValid(-50.3).success.value.value shouldBe -50.3
        NegDouble.tryingValid(-100.0).success.value.value shouldBe -100.0
      }

      it("returns an AssertionError wrapped in a Failure if the passed Double is NOT lesser than 0") {
        NegDouble.tryingValid(0.0).failure.exception shouldBe an [AssertionError]
        NegDouble.tryingValid(1.0).failure.exception shouldBe an [AssertionError]
        NegDouble.tryingValid(99.9).failure.exception shouldBe an [AssertionError]
      }
    }
    describe("should offer a passOrElse factory method that") {
      it("returns a Pass if the given Double is lesser than 0") {
        NegDouble.passOrElse(-50.0)(i => i) shouldBe Pass
        NegDouble.passOrElse(-100.0)(i => i) shouldBe Pass
      }
      it("returns an error value produced by passing the given Double to the given function if the passed Double is NOT lesser than 0, wrapped in a Fail") {
        NegDouble.passOrElse(0.0)(i => s"$i did not taste good") shouldBe Fail(0.0 + " did not taste good")
        NegDouble.passOrElse(1.1)(i => i) shouldBe Fail(1.1)
        NegDouble.passOrElse(99.0)(i => i + 3.0) shouldBe Fail(102.0)
      }
    }
    describe("should offer a goodOrElse factory method that") {
      it("returns a NegDouble wrapped in a Good if the given Double is lesser than 0") {
        NegDouble.goodOrElse(-50.3)(i => i) shouldBe Good(NegDouble(-50.3))
        NegDouble.goodOrElse(-100.0)(i => i) shouldBe Good(NegDouble(-100.0))
      }
      it("returns an error value produced by passing the given Double to the given function if the passed Double is NOT lesser than 0, wrapped in a Bad") {
        NegDouble.goodOrElse(0.0)(i => s"$i did not taste good") shouldBe Bad(0.0 + " did not taste good")
        NegDouble.goodOrElse(1.1)(i => i) shouldBe Bad(1.1)
        NegDouble.goodOrElse(99.0)(i => i + 3.0) shouldBe Bad(102.0)
      }
    }
    describe("should offer a rightOrElse factory method that") {
      it("returns a NegDouble wrapped in a Right if the given Double is lesser than 0") {
        NegDouble.rightOrElse(-50.3)(i => i) shouldBe Right(NegDouble(-50.3))
        NegDouble.rightOrElse(-100.0)(i => i) shouldBe Right(NegDouble(-100.0))
      }
      it("returns an error value produced by passing the given Double to the given function if the passed Double is NOT lesser than 0, wrapped in a Left") {
        NegDouble.rightOrElse(0.0)(i => s"$i did not taste good") shouldBe Left(0.0 + " did not taste good")
        NegDouble.rightOrElse(1.1)(i => i) shouldBe Left(1.1)
        NegDouble.rightOrElse(99.9)(i => i + 3.0) shouldBe Left(102.9)
      }
    }
    describe("should offer an isValid predicate method that") {
      it("returns true if the passed Double is lesser than 0") {
        NegDouble.isValid(-50.23) shouldBe true
        NegDouble.isValid(-100.0) shouldBe true
        NegDouble.isValid(0.0) shouldBe false
        NegDouble.isValid(-0.0) shouldBe false
        NegDouble.isValid(0.00001) shouldBe false
        NegDouble.isValid(99.9) shouldBe false
      }
    }
    describe("should offer a fromOrElse factory method that") {
      it("returns a NegDouble if the passed Double is lesser than 0") {
        NegDouble.fromOrElse(-50.23, NegDouble(-42.0)).value shouldBe -50.23
        NegDouble.fromOrElse(-100.0, NegDouble(-42.0)).value shouldBe -100.0
      }
      it("returns a given default if the passed Double is NOT lesser than 0") {
        NegDouble.fromOrElse(0.0, NegDouble(-42.0)).value shouldBe -42.0
        NegDouble.fromOrElse(0.00001, NegDouble(-42.0)).value shouldBe -42.0
        NegDouble.fromOrElse(99.9, NegDouble(-42.0)).value shouldBe -42.0
      }
    }
    it("should offer MaxValue and MinValue factory methods") {
      NegDouble.MaxValue shouldEqual NegDouble.from(-Double.MinPositiveValue).get
      NegDouble.MinValue shouldEqual
        NegDouble.from(Double.MinValue).get
    }
    it("should offer a NegativeInfinity factory method") {
      NegDouble.NegativeInfinity shouldEqual NegDouble.ensuringValid(Double.NegativeInfinity)
    }
    it("should not offer a PositiveInfinity factory method") {
      "NegDouble.PositiveInfinity" shouldNot compile
    }
    it("should offer a isNegInfinity method that returns true if the instance is NegativeInfinity") {
      NegDouble.ensuringValid(Double.NegativeInfinity).isNegInfinity shouldBe true
      NegDouble(-1.0).isNegInfinity shouldBe false
    }
    it("should be sortable") {
      val xs = List(NegDouble(-2.2), NegDouble(-4.4), NegDouble(-1.1),
        NegDouble(-3.3))
      xs.sorted shouldEqual List(NegDouble(-4.4), NegDouble(-3.3), NegDouble(-2.2),
        NegDouble(-1.1))
    }

    describe("when created with apply method") {

      it("should compile when -8 is passed in") {
        "NegDouble(-8)" should compile
        NegDouble(-8).value shouldEqual -8.0
        "NegDouble(-8L)" should compile
        NegDouble(-8L).value shouldEqual -8.0
        "NegDouble(-8.0F)" should compile
        NegDouble(-8.0F).value shouldEqual -8.0
        "NegDouble(-8.0)" should compile
        NegDouble(-8.0).value shouldEqual -8.0
      }

      it("should not compile when 0 is passed in") {
        "NegDouble(0)" shouldNot compile
        "NegDouble(0L)" shouldNot compile
        "NegDouble(0.0F)" shouldNot compile
        "NegDouble(0.0)" shouldNot compile
      }

      it("should not compile when 8 is passed in") {
        "NegDouble(8)" shouldNot compile
        "NegDouble(8L)" shouldNot compile
        "NegDouble(8.0F)" shouldNot compile
        "NegDouble(8.0)" shouldNot compile
      }
      it("should not compile when x is passed in") {
        val a: Int = 8
        "NegDouble(a)" shouldNot compile
        val b: Long = 8L
        "NegDouble(b)" shouldNot compile
        val c: Float = 8.0F
        "NegDouble(c)" shouldNot compile
        val d: Double = 8.0
        "NegDouble(d)" shouldNot compile
      }
    }
    describe("when specified as a plain-old Double") {

      def takesNegDouble(pos: NegDouble): Double = pos.value

      it("should compile when -8 is passed in") {
        "takesNegDouble(-8)" should compile
        takesNegDouble(-8) shouldEqual -8.0
        "takesNegDouble(-8L)" should compile
        takesNegDouble(-8L) shouldEqual -8.0
        "takesNegDouble(-8.0F)" should compile
        takesNegDouble(-8.0F) shouldEqual -8.0
        "takesNegDouble(-8.0)" should compile
        takesNegDouble(-8.0) shouldEqual -8.0
      }

      it("should not compile when 0 is passed in") {
        "takesNegDouble(0)" shouldNot compile
        "takesNegDouble(0L)" shouldNot compile
        "takesNegDouble(0.0F)" shouldNot compile
        "takesNegDouble(0.0)" shouldNot compile
      }

      it("should not compile when 8 is passed in") {
        "takesNegDouble(8)" shouldNot compile
        "takesNegDouble(8L)" shouldNot compile
        "takesNegDouble(8.0F)" shouldNot compile
        "takesNegDouble(8.0)" shouldNot compile
      }

      it("should not compile when x is passed in") {
        val x: Int = -8
        "takesNegDouble(x)" shouldNot compile
        val b: Long = -8L
        "takesNegDouble(b)" shouldNot compile
        val c: Float = -8.0F
        "takesNegDouble(c)" shouldNot compile
        val d: Double = -8.0
        "takesNegDouble(d)" shouldNot compile
      }

      it("should offer a unary + method that is consistent with Double") {
        forAll { (p: NegDouble) =>
          (+p).toDouble shouldEqual (+(p.toDouble))
        }
      }

      it("should offer a unary - method that returns PosDouble") {
        forAll { (p: NegDouble) =>
          (-p) shouldEqual (PosDouble.ensuringValid(-(p.toDouble)))
        }
      }
    }

    it("should offer a 'plus' method that takes a NegZDouble and returns a NegDouble") {

      forAll { (negDouble: NegDouble, posZDouble: NegZDouble) =>
        (negDouble plus posZDouble) should === (NegDouble.ensuringValid(negDouble.value + posZDouble.value))
      }

      val examples =
        Table(
          (                "negDouble",                "posZDouble" ),
          (         NegDouble.MinValue,         NegZDouble.MinValue ),
          (         NegDouble.MinValue,         NegZDouble.MaxValue ),
          (         NegDouble.MinValue, NegZDouble.NegativeInfinity ),
          (         NegDouble.MaxValue,         NegZDouble.MinValue ),
          (         NegDouble.MaxValue,         NegZDouble.MaxValue ),
          (         NegDouble.MaxValue, NegZDouble.NegativeInfinity ),
          ( NegDouble.NegativeInfinity,         NegZDouble.MinValue ),
          ( NegDouble.NegativeInfinity,         NegZDouble.MaxValue ),
          ( NegDouble.NegativeInfinity, NegZDouble.NegativeInfinity )
        )

      forAll (examples) { (a, b) =>
        (a plus b).value should be < 0.0
      }

      // Sanity check that implicit widening conversions work too.
      (NegDouble(-1.0) plus NegInt(-2)) should === (NegDouble(-3.0))
    }

    it("should offer overloaded 'sumOf' methods on the companion that take one NegDouble and one or more NegZDoubles and returns a NegDouble") {

      forAll { (negDouble: NegDouble, posZDouble: NegZDouble) =>
        NegDouble.sumOf(negDouble, posZDouble) should === (NegDouble.ensuringValid(negDouble.value + posZDouble.value))
      }
      forAll { (negDouble: NegDouble, posZDoubles: List[NegZDouble]) =>
        whenever(posZDoubles.nonEmpty) {
          NegDouble.sumOf(negDouble, posZDoubles.head, posZDoubles.tail: _*) should === {
            NegDouble.ensuringValid(negDouble.value + posZDoubles.head.value + posZDoubles.tail.map(_.value).sum)
          }
        }
      }

      val posEdgeValues: List[NegDouble] = List(NegDouble.MinValue, NegDouble.MaxValue, NegDouble.NegativeInfinity)
      val posZEdgeValues = List(NegZDouble.MinValue, NegZDouble.MaxValue, NegZDouble.NegativeInfinity)
      // First put each NegDouble edge in front, then follow it with all permutations (orders) of all four NegZDouble edge values.
      Inspectors.forAll (posEdgeValues) { pos =>
        Inspectors.forAll (posZEdgeValues.permutations.toList) { case posZHead :: posZTail =>
          NegDouble.sumOf(pos, posZHead, posZTail: _*) should === {
            NegDouble.ensuringValid(pos.value + posZHead.value + posZTail.map(_.value).sum)
          }
        }
      }

      // Now do each NegDouble edge in front, then follow it with all combinations of 2 NegZEdgeDoubles
      // I get all combos by doing combinations(2) ++ combinations(2).reverse. That seems to do the trick.
      val halfOfThePairs = posZEdgeValues.combinations(2).toList
      val posZPairCombos = halfOfThePairs ++ (halfOfThePairs.reverse)
      Inspectors.forAll (posEdgeValues) { pos =>
        Inspectors.forAll (posZPairCombos) { case posZHead :: posZTail  =>
          NegDouble.sumOf(pos, posZHead, posZTail: _*) should === {
            NegDouble.ensuringValid(pos.value + posZHead.value + posZTail.map(_.value).sum)
          }
        }
      }

      // Now do each NegDouble edge in front, then follow it with all combinations of 3 NegZEdgeDoubles
      // I get all combos by doing combinations(3) ++ combinations(3).reverse. That seems to do the trick.
      val halfOfTheTriples = posZEdgeValues.combinations(3).toList
      val posZTripleCombos = halfOfTheTriples ++ (halfOfTheTriples.reverse)
      Inspectors.forAll (posEdgeValues) { pos =>
        Inspectors.forAll (posZTripleCombos) { case posZHead :: posZTail  =>
          NegDouble.sumOf(pos, posZHead, posZTail: _*) should === {
            NegDouble.ensuringValid(pos.value + posZHead.value + posZTail.map(_.value).sum)
          }
        }
      }
    }

    it("should offer 'min' and 'max' methods that are consistent with Double") {
      forAll { (pdouble1: NegDouble, pdouble2: NegDouble) =>
        pdouble1.max(pdouble2).toDouble shouldEqual pdouble1.toDouble.max(pdouble2.toDouble)
        pdouble1.min(pdouble2).toDouble shouldEqual pdouble1.toDouble.min(pdouble2.toDouble)
      }
    }

    it("should offer an 'isWhole' method that is consistent with Double") {
      forAll { (pdouble: NegDouble) =>
        pdouble.isWhole shouldEqual pdouble.toDouble.isWhole
      }
    }

    it("should offer 'round', 'ceil', and 'floor' methods that are consistent with Double") {
      forAll { (pdouble: NegDouble) =>
        pdouble.round.toDouble shouldEqual pdouble.toDouble.round
        pdouble.ceil.toDouble shouldEqual pdouble.toDouble.ceil
        pdouble.floor.toDouble shouldEqual pdouble.toDouble.floor
      }
    }

    it("should offer 'toRadians' and 'toDegrees' methods that are consistent with Double") {
      forAll { (pdouble: NegDouble) =>
        pdouble.toRadians shouldEqual pdouble.toDouble.toRadians
      }
    }

    // SKIP-SCALATESTJS,NATIVE-START
    it("should offer 'to' and 'until' method that is consistent with Double") {
      def rangeEqual[T](a: NumericRange[T], b: NumericRange[T]): Boolean =
        a.start == b.start && a.end == b.end && a.step == b.step

      forAll { (pdouble: NegDouble, end: Double, step: Double) =>
        rangeEqual(pdouble.until(end).by(1f), pdouble.toDouble.until(end).by(1f)) shouldBe true
        rangeEqual(pdouble.until(end, step), pdouble.toDouble.until(end, step)) shouldBe true
        rangeEqual(pdouble.to(end).by(1f), pdouble.toDouble.to(end).by(1f)) shouldBe true
        rangeEqual(pdouble.to(end, step), pdouble.toDouble.to(end, step)) shouldBe true
      }
    }
    // SKIP-SCALATESTJS,NATIVE-END

    it("should offer widening methods for basic types that are consistent with Double") {
      forAll { (pdouble: NegDouble) =>
        def widen(value: Double): Double = value
        widen(pdouble) shouldEqual widen(pdouble.toDouble)
      }
      forAll { (pdouble: NegDouble) =>
        def widen(value: NegZDouble): NegZDouble = value
        widen(pdouble) shouldEqual widen(NegZDouble.from(pdouble.toDouble).get)
      }
      forAll { (pdouble: NegDouble) =>
        def widen(value: NonZeroDouble): NonZeroDouble = value
        widen(pdouble) shouldEqual widen(NonZeroDouble.from(pdouble.toDouble).get)
      }
    }

    it("should offer an ensuringValid method that takes a Double => Double, throwing AssertionError if the result is invalid") {
      NegDouble(-33.0).ensuringValid(_ + 1.0) shouldEqual NegDouble(-32.0)
      NegDouble(-33.0).ensuringValid(_ => Double.NegativeInfinity) shouldEqual NegDouble.ensuringValid(Double.NegativeInfinity)
      an [AssertionError] should be thrownBy { NegDouble.MaxValue.ensuringValid(_ - NegDouble.MaxValue) }
      an [AssertionError] should be thrownBy { NegDouble.MaxValue.ensuringValid(_ => Double.PositiveInfinity) }
      an [AssertionError] should be thrownBy { NegDouble.MaxValue.ensuringValid(_ => Double.NaN) }
    }
  }
}