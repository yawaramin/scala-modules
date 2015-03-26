import scala.language.higherKinds

object Modules {
  trait Graph[A] {
    type E
    type T

    val empty: T
    def nodes(t: T): Set[A]
    def edges(t: T): Set[E]
    def create(vs: Set[A], es: Set[E]): T
    def insert(v1: A, v2: A)(t: T): T
  }

  trait MapGraph[A] extends Graph[A] {
    type E = (A, A)
    type T = Map[A, Set[A]]

    override val empty: T = Map.empty

    override def nodes(t: T) = t.keySet

    override def edges(t: T) = {
      val pairs = for (v1 <- t.keys; v2 <- t(v1)) yield (v1, v2)
      pairs.toSet
    }

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

  // IMG = IntMapGraph
  val IMG: Graph[Int] = new MapGraph[Int] {}

  trait IntMap[A] {
    type Apply
    type T[_]

    val empty: T[A]
    def get(i: Int)(x: T[A]): A
    def insert(k: Int, v: A)(x: T[A]): T[A]
    def remove(k: Int)(x: T[A]): T[A]
  }

  trait IntFunMap[A] extends IntMap[A] {
    case class Apply() extends Exception()
    type T[A] = Int => A

    override val empty = (i: Int) => throw Apply()

    override def get(i: Int)(x: T[A]) = x(i)

    override def insert(k: Int, v: A)(x: T[A]) =
      (i: Int) => if (i == k) v else get(i)(x)

    override def remove(k: Int)(x: T[A]) =
      (i: Int) => if (i == k) throw Apply() else get(i)(x)
  }

  /*
  Can't create instance of IntFunMap because it's abstract--unless we
  give it an empty body. Then it becomes an anonymous inner class that
  doesn't need to implement anything in its body because all its methods
  are already implemented in the trait.

  ISFM = IntStringFunMap
  */
  val ISFM: IntMap[String] = new IntFunMap[String] {}

  trait Group[A] {
    val empty: A
    def op(t1: A, t2: A): A
    def inverse(t: A): A
  }

  // Group of addition over integers.
  val Z: Group[Int] = new Group[Int] {
    override val empty = 0
    override def op(t1: Int, t2: Int) = t1 + t2
    override def inverse(t: Int) = -t
  }

  def PairG[A](g: Group[A]) =
    new Group[Tuple2[A, A]] {
      override val empty = (g.empty, g.empty)

      override def op(t1: Tuple2[A, A], t2: Tuple2[A, A]) =
        (g.op(t1._1, t2._1), g.op(t2._1, t2._2))

      override def inverse(t: Tuple2[A, A]) =
        (g.inverse(t._1), g.inverse(t._2))
    }

  /*
  Group of addition over pairs of integers.

  IPG = IntPairG
  */
  val IPG: Group[Tuple2[Int, Int]] = PairG(Z)
}

