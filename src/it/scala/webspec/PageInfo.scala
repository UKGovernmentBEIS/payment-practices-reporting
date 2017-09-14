package webspec

trait PageInfo {

  /**
    * @return an name that identifies the page in testing. It does not need to
    *         correspond to any name or text for the page itself.
    */
  def url: String = ""

  /**
    * @return the title that appears on the page itself
    */
  def title: String
}
