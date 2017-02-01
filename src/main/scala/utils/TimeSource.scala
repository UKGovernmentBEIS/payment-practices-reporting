package utils

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[SystemTimeSource])
trait TimeSource {
  def currentTimeMillis(): Long
}

class SystemTimeSource extends TimeSource {
  override def currentTimeMillis() = System.currentTimeMillis()
}