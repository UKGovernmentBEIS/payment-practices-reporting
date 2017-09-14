package webspec

import play.api.mvc.Call

trait EntryPoint {
  def call: Call
}
