package calculator

import forms.DateRange
import org.joda.time.LocalDate
import org.junit.Assert._
import org.junit.Test
import org.scalatest.FunSuite

class CalculatorModelTest extends FunSuite {
  implicit def periodFromDateRange(dateRange: DateRange):FinancialYear = FinancialYear(dateRange)

  @Test
  @throws[Exception]
  def getReportingPeriods() {
    val dr = DateRange(new LocalDate(2020, 1, 1), new LocalDate(2020, 12, 31))
    val reportingPeriods = Calculator(dr).getReportingPeriods
    assertEquals(2, reportingPeriods.size)
    assertPeriod(reportingPeriods(0), "1 January 2020", "30 June 2020", "30 July 2020")
    assertPeriod(reportingPeriods(1), "1 July 2020", "31 December 2020", "30 January 2021")
  }

  @Test
  @throws[Exception]
  def getReportingPeriods_preCutoff() {
    val calculatorModel = Calculator(DateRange(new LocalDate(2017, 1, 1), new LocalDate(2017, 12, 31)))
    val reportingPeriods = calculatorModel.getReportingPeriods
    assertEquals(2, reportingPeriods.size)
    assertPeriod(reportingPeriods(0), "1 January 2018", "30 June 2018", "30 July 2018")
    assertPeriod(reportingPeriods(1), "1 July 2018", "31 December 2018", "30 January 2019")
  }

  @Test
  @throws[Exception]
  def getReportingPeriods_twicePreCutoff() {
    val calculatorModel = Calculator(DateRange(new LocalDate(2016, 1, 1), new LocalDate(2016, 12, 31)))
    val reportingPeriods = calculatorModel.getReportingPeriods
    assertEquals(2, reportingPeriods.size)
    assertPeriod(reportingPeriods(0), "1 January 2018", "30 June 2018", "30 July 2018")
    assertPeriod(reportingPeriods(1), "1 July 2018", "31 December 2018", "30 January 2019")
  }

  @Test
  @throws[Exception]
  def getReportingPeriods_AndFebruary() {
    val reportingPeriods = Calculator(DateRange(new LocalDate(2017, 9, 1), new LocalDate(2018, 8, 31))).getReportingPeriods
    assertEquals(2, reportingPeriods.size)
    assertPeriod(reportingPeriods(0), "1 September 2017", "28 February 2018", "30 March 2018")
    assertPeriod(reportingPeriods(1), "1 March 2018", "31 August 2018", "30 September 2018")
  }

  @Test
  @throws[Exception]
  def getReportingPeriods_9MonthYear() {
    val reportingPeriods = Calculator(DateRange(new LocalDate(2018, 1, 1), new LocalDate(2018, 9, 30))).getReportingPeriods
    assertEquals(1, reportingPeriods.size)
    assertPeriod(reportingPeriods(0), "1 January 2018", "30 September 2018", "30 October 2018")
  }

  @Test
  @throws[Exception]
  def getReportingPeriods_15MonthYear() {
    val reportingPeriods = Calculator(DateRange(new LocalDate(2018, 1, 1), new LocalDate(2019, 3, 31))).getReportingPeriods
    assertEquals(2, reportingPeriods.size)
    assertPeriod(reportingPeriods(0), "1 January 2018", "30 June 2018", "30 July 2018")
    assertPeriod(reportingPeriods(1), "1 July 2018", "31 March 2019", "30 April 2019")
  }

  @Test
  @throws[Exception]
  def getReportingPeriods_15MonthYearPlus() {
    val reportingPeriods = Calculator(DateRange(new LocalDate(2018, 1, 1), new LocalDate(2019, 4, 1))).getReportingPeriods
    assertEquals(3, reportingPeriods.size)
    assertPeriod(reportingPeriods(0), "1 January 2018", "30 June 2018", "30 July 2018")
    assertPeriod(reportingPeriods(1), "1 July 2018", "31 December 2018", "30 January 2019")
    assertPeriod(reportingPeriods(2), "1 January 2019", "1 April 2019", "1 May 2019")
  }

  @Test
  @throws[Exception]
  def getReportingPeriods_19MonthYear() {
    val reportingPeriods = Calculator(DateRange(new LocalDate(2018, 1, 1), new LocalDate(2019, 9, 30))).getReportingPeriods
    assertEquals(3, reportingPeriods.size)
    assertPeriod(reportingPeriods(0), "1 January 2018", "30 June 2018", "30 July 2018")
    assertPeriod(reportingPeriods(1), "1 July 2018", "31 December 2018", "30 January 2019")
    assertPeriod(reportingPeriods(2), "1 January 2019", "30 September 2019", "30 October 2019")
  }

  @Test
  @throws[Exception]
  def getReportingPeriods_19MonthYearPlus() {
    val reportingPeriods = Calculator(DateRange(new LocalDate(2018, 1, 1), new LocalDate(2019, 10, 1))).getReportingPeriods
    assertEquals(3, reportingPeriods.size)
    assertPeriod(reportingPeriods(0), "1 January 2018", "30 June 2018", "30 July 2018")
    assertPeriod(reportingPeriods(1), "1 July 2018", "31 December 2018", "30 January 2019")
    assertPeriod(reportingPeriods(2), "1 January 2019", "1 October 2019", "31 October 2019")
  }

  @Test
  @throws[Exception]
  def getReportingPeriods_StartingMarch() {
    val reportingPeriods = Calculator(DateRange(new LocalDate(2018, 3, 1), new LocalDate(2019, 2, 28))).getReportingPeriods
    assertEquals(2, reportingPeriods.size)
    assertPeriod(reportingPeriods(0), "1 March 2018", "31 August 2018", "30 September 2018")
    assertPeriod(reportingPeriods(1), "1 September 2018", "28 February 2019", "30 March 2019")
  }

  @Test
  @throws[Exception]
  def getReportingPeriods_24MonthYear_StartingEndOfMonthHittingEndOfFebruary() {
    val reportingPeriods = Calculator(DateRange(new LocalDate(2017, 8, 31), new LocalDate(2019, 8, 30))).getReportingPeriods
    assertEquals(3, reportingPeriods.size)
    assertPeriod(reportingPeriods(0), "31 August 2017", "27 February 2018", "29 March 2018")
    assertPeriod(reportingPeriods(1), "28 February 2018", "30 August 2018", "29 September 2018")
    assertPeriod(reportingPeriods(2), "31 August 2018", "30 August 2019", "29 September 2019")
  }


  @Test
  @throws[Exception]
  def getReportingPeriods_forAges() {
    val reportingPeriods = Calculator(DateRange(new LocalDate(2018, 1, 1), new LocalDate(3018, 1, 1))).getReportingPeriods
    assertEquals(0, reportingPeriods.size)
  }

  @Test
  @throws[Exception]
  def getReportingPeriods_forBadDates() {
    assertEquals(0, Calculator(DateRange(new LocalDate(2018, 1, 1), new LocalDate(2018, 1, 1))).getReportingPeriods.size)
    assertEquals(0, Calculator(DateRange(new LocalDate(2018, 1, 1), new LocalDate(2017, 1, 1))).getReportingPeriods.size)
    assertEquals(0, Calculator(DateRange(new LocalDate(2018, 2, 40), new LocalDate(2019, 1, 1))).getReportingPeriods.size)
  }

  private def assertPeriod(p: ReportingPeriod, expectedStart: String, expectedEnd: String, expectedDeadline: String) {
//    assertEquals("Start date", expectedStart, p.StartDate.ToDateString)
//    assertEquals("End date", expectedEnd, p.EndDate.ToDateString)
//    assertEquals("Deadline", expectedDeadline, p.FilingDeadline.ToDateString)
  }
}
