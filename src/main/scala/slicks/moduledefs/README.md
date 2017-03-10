## Module Defs

These classes make use of the `slickgen` library as a convenient way of
creating the skeleton of the slick modules from case classes. Each of
the objects here extends `ModuleDefinition`, which is a convenience trait
that contains a `main` method. When run it inspects the `ModuleSpec` provide
by the object and writes code for the slick module to `stdout`.

It's a bit clunky, and the generated code doesn't quite match what we need
(because I'm using slick postgresql extensions) but save a lot of manual
typing of boilerplate to get a new table set up.
