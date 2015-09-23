object Common {
  // 2-D coordinate pair
  type Coords = (Int, Int)

  // A functional convenience
  implicit class Piper[A](val x: A) extends AnyVal {
    def |>[B](f: A => B) = f(x)
  }
}

import Common._

object Inheretic {
  trait Loggable { def log(msg: String): Unit }
  trait Clickable { def click(coords: Coords): Unit }

  trait Draggable extends Clickable {
    def drag(startCoords: Coords, endCoords: Coords): Unit =
      click(startCoords)
  }

  class GuiIcon(text: String)
    extends Clickable with Draggable with Loggable {
    override def log(msg: String) = {
      print(text)
      print(": ")
      println(msg)
    }

    override def click(coords: Coords) = {
      // ...
      log("Clicked")
    }

    override def drag(startCoords: Coords, endCoords: Coords) = {
      super.drag(startCoords, endCoords)

      // ...
      log("Dragged")
    }
  }

  def dragThenClick(guiIcon: GuiIcon) = {
    guiIcon.drag(0 -> 0, 1 -> 1)
    guiIcon.click(1 -> 1)
  }
}

object Typeclassy {
  trait Loggable[A] { def log(msg: String)(a: A): Unit }
  trait Clickable[A] { def click(coords: Coords)(a: A): Unit }

  trait Draggable[A] extends Clickable[A] {
    def drag(startCoords: Coords, endCoords: Coords)(a: A): Unit =
      a |> click(startCoords)
  }

  case class GuiIcon(text: String)

  object GuiIcon {
    implicit val loggableGuiIcon: Loggable[GuiIcon] =
      new Loggable[GuiIcon] {
        override def log(msg: String)(guiIcon: GuiIcon) = {
          print(guiIcon.text)
          print(": ")
          println(msg)
        }
      }

    implicit val clickableGuiIcon: Clickable[GuiIcon] =
      new Clickable[GuiIcon] {
        override def click(coords: Coords)(guiIcon: GuiIcon) = {
          // ...
          guiIcon |> loggableGuiIcon.log("Clicked")
        }
      }

    implicit val draggableGuiIcon: Draggable[GuiIcon] =
      new Draggable[GuiIcon] {
        override def click(coords: Coords)(guiIcon: GuiIcon) =
          guiIcon |> clickableGuiIcon.click(coords)

        override def drag(
          startCoords: Coords, endCoords: Coords)(guiIcon: GuiIcon) = {
          guiIcon |> super.drag(startCoords, endCoords)

          // ...
          guiIcon |> loggableGuiIcon.log("Dragged")
        }
      }
  }

  def dragThenClick(
    guiIcon: GuiIcon)(implicit DG: Draggable[GuiIcon]) = {
    guiIcon |> DG.drag(0 -> 0, 1 -> 1)
    guiIcon |> DG.click(1 -> 1)
  }
}

object Modular {
  trait Loggable[A] {
    type T = A
    def log(msg: String)(t: T): Unit
  }

  trait Clickable[A] {
    type T = A
    def click(coords: Coords)(t: T): Unit
  }

  object Clickable {
    def Mk[A](LA: Loggable[A]): Clickable[A] =
      new Clickable[A] {
        override def click(coords: Coords)(t: T) = {
          // ...
          t |> LA.log("Clicked")
        }
      }
  }

  trait Draggable[A] {
    type T = A
    def drag(startCoords: Coords, endCoords: Coords)(t: T): Unit
  }

  object Draggable {
    def Mk[A](LA: Loggable[A], CA: Clickable[A]): Draggable[A] =
      new Draggable[A] {
        override def drag(
          startCoords: Coords, endCoords: Coords)(t: T) = {
          t |> CA.click(startCoords)

          // ...
          t |> LA.log("Dragged")
        }
      }
  }

  trait GuiAble[A] {
    type T = A
    def dragThenClick(t: T): Unit
  }

  object GuiAble {
    def Mk[A](CA: Clickable[A], DA: Draggable[A]): GuiAble[A] =
      new GuiAble[A] {
        override def dragThenClick(t: T) = {
          t |> DA.drag(0 -> 0, 1 -> 1)
          t |> CA.click(1 -> 1)
        }
      }
  }

  case class GuiIcon(text: String)

  object GuiIcon {
    val loggable: Loggable[GuiIcon] =
      new Loggable[GuiIcon] {
        override def log(msg: String)(t: T) = {
          print(t.text)
          print(": ")
          println(msg)
        }
      }

    val clickable = Clickable.Mk(loggable)
    val draggable = Draggable.Mk(loggable, clickable)
    val guiable = GuiAble.Mk(clickable, draggable)
  }
}

