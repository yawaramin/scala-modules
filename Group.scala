trait Group {
  type T

  val e: T
  def bullet(t1: T, t2: T): T
  def inv(t: T): T
}

object Z extends Group {
  type T = Int

  val e: T = 0
  def bullet(t1: T, t2: T): T = t1 + t2
  def inv(t: T): T = -t
}

class PairG(val gr: Group) extends Group {
  type T = (gr.T, gr.T)

  val e: T = (gr.e, gr. e)

  def bullet(t1: T, t2: T): T =
    (gr.bullet(t1._1, t2._1), gr.bullet(t1._2, t2._2))

  def inv(t: T): T = (gr.inv(t._1), gr.inv(t._2))
}

object IntPairG extends PairG(Z)

