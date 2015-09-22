// A functional convenience

object Piper {
  implicit class PiperClass[A](val x: A) extends AnyVal {
    def |>[B](f: A => B) = f(x)
  }
}

import Piper._

// 2-D coordinate pair

object Coords { type T = (Int, Int) }

// Inheritance

/*
trait Clickable {
  def click(coords: Coords.T): Unit
}

trait Draggable extends Clickable {
  def drag(startCoords: Coords.T, endCoords: Coords.T): Unit =
    click(startCoords)
}

trait Loggable {
  def log(msg: String): Unit
}

class GuiIcon(text: String)
  extends Clickable with Draggable with Loggable {
  override def click(coords: Coords.T) = {
    // ...
    log("Clicked")
  }

  override def drag(startCoords: Coords.T, endCoords: Coords.T) = {
    super.drag(startCoords, endCoords)

    // ...
    log("Dragged")
  }

  override def log(msg: String) = {
    print(text)
    print(": ")
    println(msg)
  }
}
*/

// Typeclasses

trait Clickable[A] {
  def click(coords: Coords.T)(a: A): Unit
}

trait Draggable[A] extends Clickable[A] {
  def drag(startCoords: Coords.T, endCoords: Coords.T)(a: A): Unit =
    a |> click(startCoords)
}

trait Loggable[A] {
  def log(msg: String)(a: A): Unit
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
      override def click(coords: Coords.T)(guiIcon: GuiIcon) = {
        // ...
        guiIcon |> loggableGuiIcon.log("Clicked")
      }
    }

  implicit val draggableGuiIcon: Draggable[GuiIcon] =
    new Draggable[GuiIcon] {
      override def click(coords: Coords.T)(guiIcon: GuiIcon) =
        guiIcon |> clickableGuiIcon.click(coords)

      override def drag(
        startCoords: Coords.T, endCoords: Coords.T)(guiIcon: GuiIcon) = {
        guiIcon |> super.drag(startCoords, endCoords)

        // ...
        guiIcon |> loggableGuiIcon.log("Dragged")
      }
    }
}

object TypeclassesTest {
  def dragThenClick(
    guiIcon: GuiIcon)(implicit DG: Draggable[GuiIcon]) = {
    guiIcon |> DG.drag(0 -> 0, 1 -> 1)
    guiIcon |> DG.click(1 -> 1)
  }
}

