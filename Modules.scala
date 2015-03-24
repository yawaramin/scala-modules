import scala.language.higherKinds

object Modules {
  trait Graph {
    type V
    type E
    type T

    def nodes(t: T): Set[V]
    def edges(t: T): Set[E]
    def empty: T
    def create(vs: Set[V], es: Set[E]): T
    def insert(v1: V, v2: V)(t: T): T
  }

  trait MapGraph extends Graph {
    type E = (V, V)
    type T = Map[V, Set[V]]

    override def nodes(t: T) = t.keySet

    override def edges(t: T) = {
      val pairs = for (v1 <- t.keys; v2 <- t(v1)) yield (v1, v2)
      pairs.toSet
    }

    override def empty = Map.empty

    override def create(vs: Set[V], es: Set[E]) = {
      val vertexesEdges =
        for (v <- vs; (v1, v2) <- es if v == v1) yield (v1, v2)
      vertexesEdges.groupBy(_._1).mapValues(_.map(_._2).toSet)
    }

    override def insert(v1: V, v2: V)(t: T) = {
      val otherVertexes = t.get(v1)

      otherVertexes match {
        case Some(vs) => t.updated(v1, vs + v2)
        case None => t + Tuple2(v1, Set(v2))
      }
    }
  }

  val IntMapGraph: Graph = new MapGraph { type V = Int }

  trait IntMap[A] {
    type Apply
    type T[_]

    def empty: T[A]
    def get(i: Int)(x: T[A]): A
    def insert(k: Int, v: A)(x: T[A]): T[A]
  }

  trait IntFunMap[A] extends IntMap[A] {
    case class Apply() extends Exception()
    type T[A] = Int => A

    override def empty = (i: Int) => throw Apply()
    override def get(i: Int)(x: T[A]) = x(i)
    override def insert(k: Int, v: A)(x: T[A]) =
      (i: Int) => if (i == k) v else get(i)(x)
  }

  val IntStringFunMap: IntMap[String] = new IntFunMap[String] {}
  //val IntStringFunMap = new IntFunMap { type A = String }

  trait Group {
    type T

    def empty: T
    def op(t1: T, t2: T): T
    def inverse(t: T): T
  }

  // Group of integers over addition.
  val Z: Group = new Group {
    type T = Int

    override def empty = 0
    override def op(t1: T, t2: T) = t1 + t2
    override def inverse(t: T) = -t
  }

  class PairG(val Gr: Group) extends Group {
    type T = (Gr.T, Gr.T)

    override def empty = (Gr.empty, Gr.empty)

    override def op(t1: T, t2: T) =
      (Gr.op(t1._1, t2._1), Gr.op(t1._2, t2._2))

    override def inverse(t: T) = (Gr.inverse(t._1), Gr.inverse(t._2))
  }

  val IntPairG: Group = new PairG(Z)
}

