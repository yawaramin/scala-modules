trait IntMap {
  type Apply
  type T[_]

  def empty[A]: T[A]
  def get[A](i: Int)(x: T[A]): A
  def insert[A](k: Int, v: A)(x: T[A]): T[A]
}

object IntFunMap extends IntMap {
  case class Apply() extends Exception()
  type T[A] = Int => A

  def empty[A] = i => throw Apply()
  def get[A](i: Int)(x: T[A]) = x(i)
  def insert[A](k: Int, v: A)(x: T[A]) =
    i => if (i == k) v else get(i)(x)
}

