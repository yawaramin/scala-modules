import scala.language.higherKinds

object Modules {
  trait Graph[A] {
    type E
    type T

    def nodes(t: T): Set[A]
    def edges(t: T): Set[E]
    def empty: T
    def create(vs: Set[A], es: Set[E]): T
    def insert(v1: A, v2: A)(t: T): T
  }

  trait MapGraph[A] extends Graph[A] {
    type E = (A, A)
    type T = Map[A, Set[A]]

    override def nodes(t: T) = t.keySet

    override def edges(t: T) = {
      val pairs = for (v1 <- t.keys; v2 <- t(v1)) yield (v1, v2)
      pairs.toSet
    }

    override def empty = Map.empty

    override def create(vs: Set[A], es: Set[E]) = {
      val vertexesEdges =
        for (v <- vs; (v1, v2) <- es if v == v1) yield (v1, v2)
      vertexesEdges.groupBy(_._1).mapValues(_.map(_._2).toSet)
    }

    override def insert(v1: A, v2: A)(t: T) = {
      val otherVertexes = t.get(v1)

      otherVertexes match {
        case Some(vs) => t.updated(v1, vs + v2)
        case None => t + Tuple2(v1, Set(v2))
      }
    }
  }

  val IntMapGraph: Graph[Int] = new MapGraph[Int] {}

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

  /*
  Can't create instance of IntFunMap because it's abstract--unless we
  give it an empty body. Then it becomes an anonymous inner class that
  doesn't need to implement anything in its body because all its methods
  are already implemented in the trait.
  */
  val IntStringFunMap: IntMap[String] = new IntFunMap[String] {}

  trait Group[A] {
    def empty: A
    def op(t1: A, t2: A): A
    def inverse(t: A): A
  }

  // Group of addition over integers.
  val Z: Group[A] = new Group[Int] {
    override def empty = 0
    override def op(t1: A, t2: A) = t1 + t2
    override def inverse(t: A) = -t
  }

  class PairG(val Gr: Group) extends Group[(Gr.T, Gr.T)] {
    override def empty = (Gr.empty, Gr.empty)

    override def op(t1: T, t2: T) =
      (Gr.op(t1._1, t2._1), Gr.op(t1._2, t2._2))

    override def inverse(t: T) = (Gr.inverse(t._1), Gr.inverse(t._2))
  }

  val IntPairG: Group = new PairG(Z)
}

