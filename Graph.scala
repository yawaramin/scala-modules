trait Graph {
  type V
  type E
  type T

  val nodes: T => Set[V]
  val edges: T => Set[E]
  val empty: T
  val create: (Set[V], Set[E]) => T
  val insert: (V, V) => T => T
}

trait MapGraph extends Graph {
  type E = (V, V)
  type T = Map[V, Set[V]]

  val nodes: T => Set[V] = t => t.keySet

  val edges: T => Set[E] = t => {
    val pairs = for (v1 <- t.keys; v2 <- t(v1)) yield (v1, v2)
    pairs.toSet
  }

  val empty: T = Map.empty

  val create: (Set[V], Set[E]) => T = (vs, es) => {
    val vertexesEdges =
      for (v <- vs; (v1, v2) <- es if v == v1) yield (v1, v2)
    vertexesEdges.groupBy(_._1).mapValues(_.map(_._2).toSet)
  }

  val insert: (V, V) => T => T = (v1, v2) => t => {
    val otherVertexes = t.get(v1)

    otherVertexes match {
      case Some(vs) => t.updated(v1, vs + v2)
      case None => t + Tuple2(v1, Set(v2))
    }
  }
}

object IMG extends MapGraph { type V = Int }

object Main {
  val g =
    IMG.insert(
      2, 3
    )(
      IMG.insert(1, 3)(IMG.create(Set(1, 2), Set((1, 2))))
    )
}

