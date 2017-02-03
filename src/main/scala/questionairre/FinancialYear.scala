package questionairre

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait FinancialYear extends EnumEntry with Lowercase

object FinancialYear extends Enum[FinancialYear] with PlayJsonEnum[FinancialYear] {
  override def values = findValues

  case object Unknown extends FinancialYear

  case object First extends FinancialYear

  case object Second extends FinancialYear

  case object ThirdOrLater extends FinancialYear

}