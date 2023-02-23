package utils

import eu.timepit.refined.api.Refined

object RefinedUtils {

  implicit class RefinedIntOps[T](val refined: Int Refined T) extends AnyVal {

    def arrayIndex: Int = refined.value - 1
  }
}