package webspec

import cats.data.Kleisli
import cats.instances.either._
import cats.syntax.either._
import com.gargoylesoftware.htmlunit.html.HtmlElement
import org.scalactic.TripleEquals._

final case class Step[T1, T2](private val k: Kleisli[ErrorOr, T1, T2]) {
  val run: (T1) => ErrorOr[T2] = k.run

  def andThen[T3](step: Step[T2, T3]): Step[T1, T3] = Step[T1, T3](k andThen step.k)

  def should[T3]: (Step[T2, T3]) => Step[T1, T3] = andThen

  def where[T3](side: SideStep[T2, T3]): Step[T2, T2] = Step[T2, T2]((v2: T2) => side.run(v2).map(_ => v2))
}

object Step {
  def apply[T1, T2](f: T1 => ErrorOr[T2]): Step[T1, T2] = Step(Kleisli(f))
}

final case class SideStep[T1, T2](private val k: Kleisli[ErrorOr, T1, (T1, T2)]) {
  val run: T1 => ErrorOr[(T1, T2)] = k.run

  def having(check: Step[T2, T2]): SideStep[T1, T2] = SideStep {
    k.flatMapF {
      case (v1, v2) => check.run(v2).map(v3 => (v1, v3))
    }
  }

  def and(step: SideStep[T1, T2]): SideStep[T1, T2] = SideStep {
    k.flatMapF {
      case (v1, _) => step.k.run(v1)
    }
  }
}

object SideStep {
  def apply[T1, T2](f: T1 => ErrorOr[(T1, T2)]): SideStep[T1, T2] = new SideStep[T1, T2](Kleisli(f))
}

/**
  * An `OptionalSideStep` represents a branch from the main flow into steps associated
  * with an optional value derived from the main flow value. E.g. if the main flow is dealing
  * with an HtmlPage then finding a page element will result in a side flow that
  * can perform tests on the element.
  *
  * @tparam T1 - the type of the value on the main flow
  * @tparam T2 - the type of the value on the side flow.
  */
final case class OptionalSideStep[T1, T2 <: HtmlElement](private val k: Kleisli[ErrorOr, T1, (T1, Option[T2])]) {
  /**
    * If the side flow element is present then the step will be applied to it, otherwise
    * it is bypassed.
    */
  def andThen[T3](step: Step[T2, T3]): Step[T1, T1] = Step[T1, T1] {
    k.flatMapF {
      case (v1, Some(v2)) => step.run(v2).map(_ => v1)
      case (v1, None)     => Right(v1)
    }
  }

  /**
    * Confirm that the optional value exists by converting to a SideStep[T1,T2]
    * with either the main and side flow values, or an error
    */
  def exists: SideStep[T1, T2] = SideStep {
    k.flatMapF {
      case (_, None)      => Left(SpecError("does not exist"))
      case (v1, Some(v2)) => Right((v1, v2))
    }
  }

  def should[T3](step: SideStep[T2, T3]): SideStep[T1, T2] = SideStep {
    k.flatMapF {
      case (_, None)      => Left(SpecError("does not exist"))
      case (v1, Some(v2)) => step.run(v2).map(_ => (v1, v2))
    }
  }

  def is(text: String): SideStep[T1, T2] = SideStep[T1, T2] {
    k.flatMapF {
      case (_, None)      => Left(SpecError("does not exist"))
      case (v1, Some(v2)) =>
        if (v2.getTextContent.trim === text) Right((v1, v2))
        else Left(SpecError(s"Expected value '$text' but was '$v2'"))
    }
  }

}

object OptionalSideStep {
  def apply[T1, T2 <: HtmlElement](f: T1 => ErrorOr[(T1, Option[T2])]): OptionalSideStep[T1, T2] = new OptionalSideStep[T1, T2](Kleisli(f))
}

