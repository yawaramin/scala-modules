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

With this implementation, you can do things like (it helps to read from
right to left, or in other words, we really need a pipe-forward operator
in Scala):

    scala> IntFn.get(1)(IntFn.insert(1, "a")(IntFn.empty))
    res7: String = a

A few points to take away from this:

  - Scala's generic methods will require type parameters, which is
    really clunky, unless you manage to define the type parameter
    elsewhere, as we will see later.

  - Scala is a _lot_ denser than SML for the equivalent functionality.
    One could charitably say that Scala optimises for something other
    than curried methods.

  - Surprisingly, though, Scala is on par with SML in terms of line
    count. Admittedly this isn't a very strong point.

## The Module Signature

The next step in the evolution of a module is usually to extract its
signature and re-implement it in terms of its signature:

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

    Cleaning out the method signatures isn't the only reason we want to
    move the type parameter `A` out to the trait, however. I'll fully
    explain this later, but for now notice that any object that
    ultimately implements this trait will have a concrete type passed in
    to it and will in turn pass in that concrete type to the member
    values and methods under the alias of `A`.

  - We express `empty` as a `val` instead of as a `def` because we want
    it to return the exact same thing each time; so no need for a
    function call to do that. We couldn't do this with the object
    version before because `val`s can't accept type parameters
    (this is a hard fact of Scala syntax).

