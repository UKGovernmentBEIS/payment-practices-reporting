import de.heikoseeberger.sbtheader.license.GPLv3

enablePlugins(AutomateHeaderPlugin)

licenses += ("GPLv3", url("https://opensource.org/licenses/GPL-3.0"))

headers := Map("scala" -> GPLv3("2016", "Department for Business, Energy and Industrial Strategy"))
