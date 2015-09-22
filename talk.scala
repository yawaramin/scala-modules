object Common {
  // 2-D coordinate pair
  type Coords = (Int, Int)

  // A functional convenience
  implicit class Piper[A](val x: A) extends AnyVal {
    def |>[B](f: A => B) = f(x)
  }
}

import Common._

// Inheritance

/*
trait Clickable {
  def click(coords: Coords): Unit
}

trait Draggable extends Clickable {
  def drag(startCoords: Coords, endCoords: Coords): Unit =
    click(startCoords)
}

trait Loggable {
  def log(msg: String): Unit
}

class GuiIcon(text: String)
  extends Clickable with Draggable with Loggable {
  override def click(coords: Coords) = {
    // ...
    log("Clicked")
  }

  override def drag(startCoords: Coords, endCoords: Coords) = {
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
  def click(coords: Coords)(a: A): Unit
}

trait Draggable[A] extends Clickable[A] {
  def drag(startCoords: Coords, endCoords: Coords)(a: A): Unit =
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

object TypeclassesTest {
  def dragThenClick(
    guiIcon: GuiIcon)(implicit DG: Draggable[GuiIcon]) = {
    guiIcon |> DG.drag(0 -> 0, 1 -> 1)
    guiIcon |> DG.click(1 -> 1)
  }
}

