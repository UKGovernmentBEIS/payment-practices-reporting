# Running the application in a standalone mode

The script `quickstart` will bootstrap the application from clean into a full running application
in a completely standalone setup. Mock versions of components to handle company search,
authentication and GDS notify will be wired in, and the application will use an in-memory
database instance, which will not be persisted between runs.

The first time you run this on a clean machine, the build tool, `sbt`, will download all the
dependency libraries and compile the application code. This can take a few minutes, but the
dependencies are cached and compilation is incremental, so subsequent runs will be a lot faster
to start up.

Play framework will be run in development mode. This means that if you make changes to project files,
scala, templates, sass, etc, then these will get recompiled when you refresh the page in your browser.

Once it is running, point your browser at http://localhost:9000

Use CTRL-D to shut down the application.