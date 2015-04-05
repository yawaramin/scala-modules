object Modules {
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

  // IMG = IntMapGraph
  val IMG: Graph[Int] = new MapGraph[Int] {}

  trait IntMap[A] {
    type NotFound
    type T

    val empty: T
    def get(i: Int)(x: T): A
    def insert(k: Int, v: A)(x: T): T
    def remove(k: Int)(x: T): T
  }

  trait IntFunMap[A] extends IntMap[A] {
    case class NotFound() extends Exception()
    type T = Int => A

    override val empty = (i: Int) => throw NotFound()

    override def get(i: Int)(x: T) = x(i)

    override def insert(k: Int, v: A)(x: T) =
      (i: Int) => if (i == k) v else get(i)(x)

    override def remove(k: Int)(x: T) =
      (i: Int) => if (i == k) empty(i) else get(i)(x)
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
    type T = A

    val empty: T
    def op(t1: T, t2: T): T
    def inverse(t: T): T
  }

  trait Summary[A] {
    type T = A

    def sum(xs: Seq[T]): T
    def sumNonEmpty(xs: Seq[T]): Option[T]
    def sumDifference(xs: Seq[T], ys: Seq[T]): T
  }

  // Group of integers under addition.
  val Z: Group[Int] = new Group[Int] {
    override val empty = 0
    override def op(t1: T, t2: T) = t1 + t2
    override def inverse(t: T) = -t
  }

  // Group of reals under addition.
  val R: Group[Double] = new Group[Double] {
    override val empty = 0.0
    override def op(t1: T, t2: T) = t1 + t2
    override def inverse(t: T) = -t
  }

  // Group of rationals under addition.
  val Rational: Group[(Int, Int)] =
    new Group[(Int, Int)] {
      /*
      gcd and reduce will never be seen by users of this module because
      it's been upcast immediately on declaration (to Group) and its
      real type is never captured.
      */
      def gcd(x: Int, y: Int): Int =
        if (x == y) x
        else if (x < y) gcd(x, y - x)
        else gcd(y, x)

      def reduce(t: T) = {
        val u = if (t._2 < 0) (t._1 * -1, t._2 * -1) else t

        if (u._1 == 0) t
        else {
          val d = gcd(u._1 |> Math.abs, u._2 |> Math.abs)
          if (d == u._2) (u._1 / d, 1) else (u._1 / d, u._2 / d)
        }
      }

      override val empty = (0, 1)

      override def op(t1: T, t2: T) =
        (t1._1 * t2._2 + t2._1 * t1._2, t1._2 * t2._2) |> reduce

      override def inverse(t: T) = (-t._1, t._2) |> reduce
    }

  /*
  Functor from input groups to a group of pairs of elements of the input
  groups, under element-wise operation on elements of the input groups.
  */
  def PairG[A1, A2](G1: Group[A1], G2: Group[A2]): Group[(A1, A2)] =
    new Group[(A1, A2)] {
      override val empty = (G1.empty, G2.empty)

      override def op(t1: T, t2: T) =
        (G1.op(t1._1, t2._1), G2.op(t1._2, t2._2))

      override def inverse(t: T) = (G1.inverse(t._1), G2.inverse(t._2))
    }

  /*
  Group of pairs of integers under element-wise addition.

  IPG = IntPairG
  */
  val IPG = PairG(Z, Z)

  /*
  Functor from input group to some summary functions for the same type
  as the group's type.

  Exercise in translating typeclass style Scala into ML-style modular
  Scala. Example functions originally from
  http://aakashns.github.io/better-type-class.html.
  */
  def GroupSummary[A](G: Group[A]): Summary[A] =
    new Summary[A] {
      override def sum(xs: Seq[T]) = xs.foldLeft(G.empty)(G.op)

      override def sumNonEmpty(xs: Seq[T]) =
        if (xs.isEmpty) None else xs |> sum |> Some.apply

      override def sumDifference(xs: Seq[T], ys: Seq[T]) =
        G.op(xs |> sum, ys |> sum |> G.inverse)
    }

  // Summary functions for group of integers.
  val ZGS = GroupSummary(Z)

  // Summary functions for group of reals.
  val RGS = GroupSummary(R)

  // Summary functions for pair of ints under element-wise addition.
  val IPGS = GroupSummary(IPG)

  // Summary functions for rationals under addition.
  val RaGS = GroupSummary(Rational)

  trait Ordered[A] {
    type T = A

    def compare(t1: T, t2: T): Int
  }

  val IntOrdered: Ordered[Int] = new Ordered[Int] {
    override def compare(t1: T, t2: T) = t1 - t2
  }

  trait MySet[A] {
    type E = A
    type T

    val empty: T
    def insert(e: E)(t: T): T
    def member(e: E)(t: T): Boolean
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
}

