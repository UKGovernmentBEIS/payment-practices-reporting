import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

package object questionnaire {

  sealed trait YesNo extends EnumEntry with Lowercase

  object YesNo extends Enum[YesNo] with PlayJsonEnum[YesNo] {
    override def values = findValues

    case object Yes extends YesNo

    case object No extends YesNo

  }

  sealed trait FinancialYear extends EnumEntry with Lowercase

  object FinancialYear extends Enum[FinancialYear] with PlayJsonEnum[FinancialYear] {
    override def values = findValues

    case object First extends FinancialYear

    case object Second extends FinancialYear

    case object ThirdOrLater extends FinancialYear

  }

}
