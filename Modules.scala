import scala.language.higherKinds

implicit class Piper[A](val x: A) extends AnyVal {
  def |>[B](f: A => B) = f(x)
}

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

trait IntMap[A] {
  type NotFound
  type T[_]

  val empty: T[A]
  def get(i: Int)(x: T[A]): A
  def insert(k: Int, v: A)(x: T[A]): T[A]
  def remove(k: Int)(x: T[A]): T[A]
}

trait IntFunMap[A] extends IntMap[A] {
  case class NotFound() extends Exception()
  type T[A] = Int => A

  override val empty = (i: Int) => throw NotFound()

  override def get(i: Int)(x: T[A]) = x(i)

  override def insert(k: Int, v: A)(x: T[A]) =
    (i: Int) => if (i == k) v else get(i)(x)

  override def remove(k: Int)(x: T[A]) =
    (i: Int) => if (i == k) empty(i) else get(i)(x)
}

trait Group[A] {
  type T = A

  val empty: T
  def op(t1: T, t2: T): T
  def inverse(t: T): T
}

trait Ordered[A] {
  type T = A

  def compare(t1: T, t2: T): Int
}

trait MySet[A] {
  type E = A
  type T

  val empty: T
  def insert(e: E)(t: T): T
  def member(e: E)(t: T): Boolean
}

object Modules {
  // IMG = IntMapGraph
  val IMG: Graph[Int] = new MapGraph[Int] {}

  /*
  Can't create instance of IntFunMap because it's abstract--unless we
  give it an empty body. Then it becomes an anonymous inner class that
  doesn't need to implement anything in its body because all its methods
  are already implemented in the trait.

  ISFM = IntStringFunMap
  */
  val ISFM: IntMap[String] = new IntFunMap[String] {}

  // Group of integers under addition.
  val Z: Group[Int] = new Group[Int] {
    override val empty = 0
    override def op(t1: T, t2: T) = t1 + t2
    override def inverse(t: T) = -t
  }

  /*
  Functor from input group to a group of pairs of elements of the input
  group, under element-wise operation on elements of the input group.
  */
  def PairG[A](G: Group[A]): Group[(A, A)] =
    new Group[(A, A)] {
      override val empty = (G.empty, G.empty)

      override def op(t1: T, t2: T) =
        (G.op(t1._1, t2._1), G.op(t1._2, t2._2))

      override def inverse(t: T) =
        (G.inverse(t._1), G.inverse(t._2))
    }

  /*
  Group of pairs of integers under element-wise addition.

  IPG = IntPairG
  */
  val IPG = PairG(Z)

  val IntOrdered: Ordered[Int] = new Ordered[Int] {
    override def compare(t1: T, t2: T) = t1 - t2
  }

  def UnbalancedSet[A](O: Ordered[A]): MySet[A] =
    new MySet[A] {
      sealed trait T
      case object Leaf extends T
      case class Branch(left: T, e: E, right: T) extends T

      override val empty = Leaf

      override def insert(e: E)(t: T) =
        t match {
          case Leaf => Branch(Leaf, e, Leaf)
          case Branch(l, x, r) =>
            val comp = O.compare(e, x)

            if (comp < 0) Branch(insert(e)(l), x, r)
            else if (comp > 0) Branch(l, x, insert(e)(r))
            else t
        }

      override def member(e: E)(t: T) =
        t match {
          case Leaf => false
          case Branch(l, x, r) =>
            val comp = O.compare(e, x)

            if (comp < 0) member(e)(l)
            else if (comp > 0) member(e)(r)
            else true
        }
    }

  // UIS = UnbalancedIntSet
  val UIS = UnbalancedSet(IntOrdered)

  /* Slay the compiler:
  val UIS = UnbalancedSet(IntOrdered)
  */
}

