trait IntMapSig {
  type Apply
  type T[_]

  def e[A](i: Int): T[A]
  def app[A](f: T[A]): T[A]
  def extend[A](a: Int, b: A)(f: => T[A])(i: Int): A
}

object IntMap extends IntMapSig {
  case class Apply() extends Exception()
  type T[A] = Int => A

  def e[A](i: Int) = throw Apply()
  def app[A](f: T[A]) = f

  def extend[A](a: Int, b: A)(f: => T[A])(i: Int) =
    if (i == a) b else app(f)(i)
}

