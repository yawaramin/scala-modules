# Exploring ML-Style Modular Programming in Scala

I recently watched a [talk by Martin
Odersky](https://www.youtube.com/watch?v=P8jrvyxHodU) in which he boils
Scala down to what he considers to be the essential parts of the
language. In it he remarks that Scala is designed to be a modular
programming language and its modular abstractions are greatly inspired
by modular programming in ML (SML). I found this intriguing, because
I regard SML-style modular programming as a great way to organise
software when doing programming-in-the-large. If Prof. Odersky's
assertion about modular progamming in Scala is correct, SML-style
modules might be a great fit with Scala.

As is usually the case, I went searching for what others have had to say
on the matter. I found a great
[post](http://io.pellucid.com/blog/scalas-modular-roots) by @dwhjames
showing several attempts and approaches at encoding ML-style modules in
Scala, and tries out several syntactic 'styles' of Scala to see which
one might 'feel' better in use. James starts with Odersky's central
premise, which I interpret as the following key points:

  - Scala object = ML module

  - Scala trait = ML signature

  - Scala class = ML functor

He then goes on to explore actually implementing modules using the
driving example of a `set` data structure from Okasaki's Purely
Functional Data Structures (appropriate, as Okasaki famously uses SML in
the code examples for his data structures).

I also found a very thoughtful [answer and
discussion](http://stackoverflow.com/q/23006951/20371) to the same
question, posed on Stack Overflow. The answer and discussion here are
just a gold mine of information about Scala's theoretical issues with
handling ML-style modules. The key points I took away from them:

  - ML functor = Scala function. This is slightly more elegant than the
    cumbersome declaration of a new class; but it does require a little
    finesse to avoid typechecker traps.

  - Scala's type relationships are _nominal_ as opposed to ML's
    _structural_ relationships. In practice this means we will need to
    declare and define all our types and traits before trying to set up
    relationships between them using functors. This is admittedly a
    limitation, but perhaps one worth living with given the code
    organisation and maintainability benefits we gain in exchange.

Finally, I found a [fantastic talk](https://youtu.be/oJOYVDwSE3Q) by
@chrisamaphone on ML modules, their use, and the type theory behind
them. I'm still mulling all the new ideas over, but so far I've managed
to take away the following main points:

  - SML functors are _generative_ functors, so called because they
    _generate_ new modules as return values for each time they're
    called. This is true even if they're called with the exact same
    arguments each time. Similar to Haskell's `newtype A = Int` and
    `newtype B = Int` creating two completely distinct types `A` and `B`
    despite being literally the same type.

  - SML functors are actually modelled using formal logic techniques
    which I won't pretend to understand--just appreciate.

  - It's possible and sometimes even desirable to get different results
    from two modules which have been created by the same functor. This
    despite the general idea behind functors being to abstract
    implementation away from interface, implying that all
    implementations should return the same results (otherwise the
    interface 'breaks its promise').

## A Basic Module

As a sort of review, let's look at a simple SML module and beside it a
Scala port. This example is adapted from a fantastic paper on the
[Essentials of Standard ML
Modules](http://www.itu.dk/courses/FDP/E2004/Tofte-1996-Essentials_of_SML_Modules.pdf)
(PDF), which explains SML modules from the ground up. It implements a
_finite map_ from integers to values of some arbitrary type. In other
words, a vector. On a side note, the interesting thing about this data
structure is that it's implemented completely using function
composition.

```
structure IntFn =              | object IntFn {
  struct                       |   case class NotFound() extends Exception()
    exception NotFound         |   type T[A] = Int => A
    type 'a t = int -> 'a      |
                               |   def empty[A]: T[A] =
    fun empty i =              |     (i: Int) => throw NotFound()
      raise NotFound           |
                               |   def get[A](i: Int)(x: T[A]) = x(i)
    fun get i x = x i          |
                               |   def insert[A](k: Int, v: A)(x: T[A]) =
    fun insert (k, v) x i =    |     (i: Int) => if (i == k) v else get(i)(x)
      if i = k then v else x i | }
  end;                         |
```

With this implementation, we can do things like:

    scala> (IntFn.insert(1, "a") _ andThen IntFn.get(1) _) { IntFn.empty }
    res7: String = a

A few points to take away from this:

  - Scala's generic methods will require type parameters, which is
    really clunky, unless you manage to define the type parameter
    elsewhere, as we will see later.

  - Scala is somewhat denser than SML for the equivalent functionality.
    To me, this is mostly a result of Scala's weaker type inference that
    forces us to specify a lot more.

  - Scala doesn't really optimise for using curried functions, so if we
    want to use that style function composition using `andThen` and
    partial application is the [most
    idiomatic](http://stackoverflow.com/a/20574722/20371) way to do it.
    In OCaml or F# we could use the 'forward-pipe' operator (`|>`). We
    can define a forward-pipe operator in Scala (and people have); but
    the implementation isn't something you want running in production.

## The Module Signature

The next step in the evolution of a module is usually to extract its
signature:

```
signature INTMAP =                       | import scala.language.higherKinds
  sig                                    |
    exception NotFound                   | trait IntMap[A] {
    type 'a t                            |   type NotFound
                                         |   type T[_]
    val empty: 'a t                      |
    val get: int -> 'a t -> 'a           |   val empty: T[A]
    val insert: int * 'a -> 'a t -> 'a t |   def get(i: Int)(x: T[A]): A
  end;                                   |   def insert(k: Int, v: A)(x: T[A]): T[A]
                                         | }
```

Now we start making some trade-offs in Scala. Some points to take away:

  - We're able to clean out the type parameters from all the methods,
    but we're now passing in the type parameter into the trait. And,
    because the type `T` is abstract (we don't know in the trait how
    implementors will define it), we have to say that it accepts any
    unknown type as a parameter, and so we have to now enable
    higher-kinded types.

  - We express `empty` as a `val` instead of as a `def` because we want
    it to return the exact same thing each time; so no need for a
    function call to do that. We couldn't do this with the object
    version before because `val`s can't accept type parameters
    (this is a hard fact of Scala syntax). This also forces us to move
    out the type parameter to the trait, so that it's in scope by the
    time we start declaring `empty`.

After that, the next step is to express the module's implementation in
terms of the signature:

```
structure IntFn :> INTMAP =    | trait IntFn[A] extends IntMap[A] {
  struct                       |   case class NotFound() extends Exception()
    exception NotFound         |   type T[A] = Int => A
    type 'a t = int -> 'a      |
                               |   override val empty =
    fun empty i =              |     (i: Int) => throw NotFound()
      raise NotFound           |
                               |   override def get(i: Int)(x: T[A]) = x(i)
    fun get i x = x i          |
                               |   override def insert(k: Int, v: A)(x: T[A]) =
    fun insert (k, v) x i =    |     (i: Int) =>
      if i = k                 |       if (i == k) v else get(i)(x)
        then v                 | }
        else get i x           |
  end;                         |
```

Can you tell the crucial difference between the original SML and the
Scala translation? Hint: it's right at the first word of the first line
of each definition.

We express the SML module directly as a concrete structure, while we
express the Scala module as an abstract trait that takes a type
parameter. Remember how we mentioned earlier that in the Scala version
we needed to pass in a type parameter? I mentioned some basic reasons
earlier, but there are better reasons that that we'll explore later. For
now, I'll mention that we want Scala modules to be as flexible as
possible, and that leads to design decisions that inevitably lead us to
passing in type parameters.

Well, as a consequence of these decisions, we also aren't able to
directly create a module (i.e., a Scala object) that can work on any
given type. Scala objects can't take type parameters; they can only be
instantiated with concrete types (well, unless you use existential
types, which is advanced type hackery that I want to avoid).

So, we have to define something which _can_ take a type parameter; and
the choices are a trait or a class (if we're defining something at the
toplevel, that is). I went with a trait partly to minimise the number of
different concepts I use, and partly to emphasise the abstract nature of
this 'module template', if you will.

Now, we can instantiate _concrete_ Scala modules (objects) with a type
parameter of our choosing, and within some scope (not at the toplevel):

```scala
object MyCode {
  val IntStrFn: IntMap[String] = new IntFn[String] {}

  (IntStrFn.insert(1, "a") _ andThen IntStrFn.get(1) _ andThen print) {
    IntStrFn.empty
  }
}
```

Notice how:

  - We constrain the `IntStrFn` module to only expose the `IntMap`
    interface, just as we constrained the SML `IntFn` module to only
    expose the `INTMAP` signature using the constraint operator `:>`.

  - The Scala implementation ends up using two traits for two levels of
    abstraction (the module interface and the implementation using a
    representation of composed functions), which is somewhat sensible.

  - We use `override` extensively in the `IntFn` trait to enable
    C++-style virtual method dispatch for when we're holding several
    different `IntMap` instances with different concrete implementations
    and we want to operate on all of them uniformly and have them just
    do the right thing automatically.

  - We implement the `IntStrFn` module as actually an anonymous class
    that extends the `IntFn` trait and passes in the concrete type as
    the parameter. The class has an empty body because it extends a
    trait which defines all its methods and values already.

TODO: mention Prof. Dan Grossman's excellent course using SML and his
explanations of data type abstraction:
http://courses.cs.washington.edu/courses/cse341/13sp/unit4notes.pdf

TODO: mention push-pull of needing to be able to pass in concrete values
into functions that are typed as taking abstract types; and of needing
to be able to create abstract type aliases that refer to trait type
parameters.

