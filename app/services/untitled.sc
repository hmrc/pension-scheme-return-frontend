import scala.math.Integral.Implicits.infixIntegralOps

def indexToCsvKey(index: Int): String = {
  val aToZ: List[Char] = ('a' to 'z').toList.map(_.toUpper)
  if (index == 0) aToZ.head.toString
  else {
    val (quotient, remainder) = index /% (aToZ.size)
    if (quotient == 0) aToZ(remainder).toString
    else indexToCsvKey(quotient - 1) + indexToCsvKey(remainder)
  }
}

require(indexToCsvKey(0) == "A")
require(indexToCsvKey(1) == "B")
require(indexToCsvKey(13) == "N")
require(indexToCsvKey(25) == "Z")
require(indexToCsvKey(26) == "AA")
require(indexToCsvKey(27) == "AB")
require(indexToCsvKey(39) == "AN")
require(indexToCsvKey(49) == "AX")
require(indexToCsvKey(50) == "AY")
require(indexToCsvKey(51) == "AZ")
require(indexToCsvKey(52) == "BA")
require(indexToCsvKey(77) == "BZ")
require(indexToCsvKey(103) == "CZ")
require(indexToCsvKey(129) == "DZ")
require(indexToCsvKey(207) == "GZ")
require(indexToCsvKey(441) == "PZ")
require(indexToCsvKey(675) == "YZ")
require(indexToCsvKey(701) == "ZZ")
require(indexToCsvKey(702) == "AAA")
require(indexToCsvKey(1376) == "AZY")
require(indexToCsvKey(1377) == "AZZ")
require(indexToCsvKey(1378) == "BAA")