object Common {
  // 2-D coordinate pair
  type Coords = (Int, Int)

  // Pipe-forward (reverse function application like in MLs)
  implicit class Piper[A](val x: A) extends AnyVal {
    def |>[B](f: A => B) = f(x)
  }
}

import Common._

object Inheretic {
  trait Clickable { def click(coords: Coords): Unit }

  trait Draggable extends Clickable {
    def drag(startCoords: Coords, endCoords: Coords): Unit =
      click(startCoords)
  }

  case class GuiIcon(text: String) extends Draggable {
    override def click(coords: Coords) =
      println(s"$text Clicked at (${coords._1}, ${coords._2})")

    override def drag(startCoords: Coords, endCoords: Coords) = {
      super.drag(startCoords, endCoords)
      println(s"$text Dragged to (${endCoords._1}, ${endCoords._2})")
    }
  }

  def dragThenClick(guiIcon: GuiIcon) = {
    guiIcon.drag(0 -> 0, 1 -> 1)
    guiIcon.click(1 -> 1)
  }

  def run = {
    val guiIcon = GuiIcon("Recycle Bin")
    dragThenClick(guiIcon)
  }
}

object Typeclassy {
  trait Clickable[A] { def click(coords: Coords)(a: A): Unit }

  trait Draggable[A] extends Clickable[A] {
    def drag(startCoords: Coords, endCoords: Coords)(a: A): Unit =
      a |> click(startCoords)
  }

  case class GuiIcon(text: String)

  object GuiIcon {
    implicit val clickable: Clickable[GuiIcon] =
      new Clickable[GuiIcon] {
        override def click(coords: Coords)(guiIcon: GuiIcon) =
          println(
            s"${guiIcon.text} Clicked at (${coords._1}, ${coords._2})")
      }

    implicit val draggable: Draggable[GuiIcon] =
      new Draggable[GuiIcon] {
        override def click(coords: Coords)(guiIcon: GuiIcon) =
          guiIcon |> clickable.click(coords)

        override def drag(
          startCoords: Coords, endCoords: Coords)(guiIcon: GuiIcon) = {
          guiIcon |> super.drag(startCoords, endCoords)

          println(
            s"${guiIcon.text} Dragged to (${endCoords._1}, ${endCoords._2})")
        }
      }
  }

  def dragThenClick(
    guiIcon: GuiIcon)(implicit DG: Draggable[GuiIcon]) = {
    guiIcon |> DG.drag(0 -> 0, 1 -> 1)
    guiIcon |> DG.click(1 -> 1)
  }

  def run = {
    val guiIcon = GuiIcon("Recycle Bin")
    dragThenClick(guiIcon)
  }
}

object Modular {
  trait Clickable[A] {
    type T = A
    def click(coords: Coords)(t: T): Unit
  }

  object Clickable {
    def apply[A]: Clickable[A] =
      new Clickable[A] {
        override def click(coords: Coords)(t: T) =
          println(s"Clicked at (${coords._1}, ${coords._2})")
      }
  }

  trait Draggable[A] {
    type T = A
    def drag(startCoords: Coords, endCoords: Coords)(t: T): Unit
  }

  object Draggable {
    def apply[A](CA: Clickable[A]): Draggable[A] =
      new Draggable[A] {
        override def drag(
          startCoords: Coords, endCoords: Coords)(t: T) = {
          t |> CA.click(startCoords)
          println(s"Dragged to (${endCoords._1}, ${endCoords._2})")
        }
      }
  }

  case class GuiIcon(text: String)

  object GuiIcon {
    val clickable = Clickable[GuiIcon]
    val draggable = Draggable(clickable)
  }

  def dragThenClick(guiIcon: GuiIcon) = {
    import GuiIcon._

    guiIcon |> draggable.drag(0 -> 0, 1 -> 1)
    guiIcon |> clickable.click(1 -> 1)
  }

  def run = {
    val guiIcon = GuiIcon("Recycle Bin")
    dragThenClick(guiIcon)
  }
}

