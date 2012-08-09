# Tomcat Build Pack for WAR Deployments to Heroku

This buildpack is a demonstration of how you can use the buildpack architecture to model the classic separation in the Java world of web application and web application server. If you are interested in how this works, you should start by reading about [buildpacks](https://devcenter.heroku.com/articles/buildpacks).

This buildpack will do the following:

1. It expects the application root to be the root of an exploded WAR file. I.e. it expects to find `WEB-INF/web.xml` in the root.
1. It sets up a standard Tomcat distribution in the `tomcat` folder
1. It copies the original application files to `tomcat/webapps/ROOT`. Again in exploded format.
1. It copies in a default Procfile that starts tomcat using the custom `run_tomcat.sh` script. This script sets Java system properties for HTTP port and for JDBC connection configuration if the `DATABASE_URL` environment variable is present during runtime.
1. It configures a JNDI data source on `jdbc/default` that maps to the database pointed to by `DATABASE_URL`. The data source name and parameters can be customized with environment variables.
1. It configures Tomcat to not listen on any other ports than $PORT
1. It configures Tomcat to only log to standard out.

## Usage

Because this build pack expects you to build your application locally, it is most convenient to deploy using [Anvil](https://github.com/ddollar/anvil) instead of git. Install the [anvil plugin](https://github.com/ddollar/heroku-anvil) for the heroku CLI:

```
$ heroku plugins:install https://github.com/ddollar/heroku-anvil
```

Build your application using your preferred build tool. The Tomcat build pack only cares about the WAR file structure. It does not try to build your application for you. If you are using maven, it usually goes like this:

```
$ mvn package
```

if you are using Ant, it usually goes like this:

```
$ ant war
```

(all depending on the specifics of your ant project). [Check out this simple web app](https://github.com/jesperfj/webapp-with-jndi) for a simple maven based example.

This build pack expects you to push the exploded WAR file structure using Anvil. This is more efficient because Anvil will only upload files that have changes since your last deployment.

Before you deploy, you must create a new Heroku app. (Anvil doesn't do that for you):

```
$ heroku create
Creating stormy-reef-2997... done, stack is cedar
http://stormy-reef-2997.herokuapp.com/ | git@heroku.com:stormy-reef-2997.git
Git remote heroku added
```

To use database connectivity, provision a developer database:

```
$ heroku addons:add heroku-postgresql:dev -a stormy-reef-2997
Adding heroku-postgresql:dev to stormy-reef-2997... done, (free)
Attached as HEROKU_POSTGRESQL_ONYX
Database has been created and is available
Use `heroku addons:docs heroku-postgresql:dev` to view documentation
```

Promote this to be the default database:

```
$ heroku pg:promote onyx -a stormy-reef-2997
Promoting HEROKU_POSTGRESQL_ONYX to DATABASE_URL... done
```

(replace `onyx` with the name given to your database when it was provisioned).

Now you're ready to deploy with Anvil:

```
$ heroku build target/my-webapp -r -b http://github.com/jesperfj/buildpack-tomcat.git -a stormy-reef-2997
Generating app manifest... done
Uploading new files... done, 1 files needed
Launching build process... done 
Fetching app... done 
Fetching buildpack... done 
Detecting buildpack... done, Java Web App 
Fetching cache... done 
Compiling app... 
  Assembling Tomcat App.
    Build dir = /tmp/compile_cbV5y
    Cache dir = /tmp/cache_UsmOd
    Buildpack dir = /tmp/buildpack_EadSv
Writing .profile... done 
Writing .profile.d/buildpack.sh... done 
Putting cache... done 
Creating slug... done 
Uploading slug... done 
Success, slug is https://api.anvilworks.org/slugs/c48b4fa1-dfe3-11e1-a010-99ef3259988d.tgz 
Releasing to stormy-reef-2997... done, v5
```

In this example, the WAR directory is at `target/my-webapp`. Replace this argument with the path to your exploded WAR directory. This command does a few things:

* Uploads all files in `target/my-webapp` to the Anvil build service. It only uploads deltas. If a file is already known to Anvil, it will not need to be uploaded.
* Runs the `compile` script in this build pack which sets up Tomcat to serve the uploaded web apps
* Packages up the result as an immutable release artifact called a "slug".
* Tells Heroku to deploy this slug to the `stormy-reef-2997` application.

For this example, [this simple web app](https://github.com/jesperfj/webapp-with-jndi) was used. We can test that it works by hitting the `/hello` path:

```
$ curl http://stormy-reef-2997.herokuapp.com/hello
Hello World
```

and we can check the logs to verify that the Servlet initialized the database connection correctly:

```
$ heroku logs -a stormy-reef-2997
...
2012-08-06T16:28:48+00:00 heroku[web.1]: Starting process with command `tomcat/bin/run_tomcat.sh`
2012-08-06T16:28:48+00:00 app[web.1]: Detected DATABASE_URL. Configuring Java System properties: jdbc.url=jdbc:postgresql://ec2-23-21-91-29.compute-1.amazonaws.com:5432/dabgnq65c6eoc7, jdbc.user=vujhadtflgvkjx, jdbc.password=********
2012-08-06T16:28:49+00:00 app[web.1]: Aug 6, 2012 4:28:49 PM org.apache.catalina.core.AprLifecycleListener init
2012-08-06T16:28:49+00:00 app[web.1]: INFO: The APR based Apache Tomcat Native library which allows optimal performance in production environments was not found on the java.library.path: /usr/lib/jvm/java-6-openjdk/jre/lib/amd64/server:/usr/lib/jvm/java-6-openjdk/jre/lib/amd64:/usr/lib/jvm/java-6-openjdk/jre/../lib/amd64:/usr/java/packages/lib/amd64:/usr/lib/jni:/lib:/usr/lib
2012-08-06T16:28:50+00:00 app[web.1]: Aug 6, 2012 4:28:50 PM org.apache.coyote.AbstractProtocol init
2012-08-06T16:28:50+00:00 app[web.1]: INFO: Initializing ProtocolHandler ["http-bio-52213"]
2012-08-06T16:28:50+00:00 app[web.1]: Aug 6, 2012 4:28:50 PM org.apache.catalina.startup.Catalina load
2012-08-06T16:28:50+00:00 app[web.1]: INFO: Initialization processed in 1564 ms
2012-08-06T16:28:50+00:00 app[web.1]: Aug 6, 2012 4:28:50 PM org.apache.catalina.core.StandardService startInternal
2012-08-06T16:28:50+00:00 app[web.1]: INFO: Starting service Catalina
2012-08-06T16:28:50+00:00 app[web.1]: Aug 6, 2012 4:28:50 PM org.apache.catalina.core.StandardEngine startInternal
2012-08-06T16:28:50+00:00 app[web.1]: INFO: Starting Servlet Engine: Apache Tomcat/7.0.29
2012-08-06T16:28:50+00:00 app[web.1]: Aug 6, 2012 4:28:50 PM org.apache.catalina.startup.HostConfig deployDescriptor
2012-08-06T16:28:50+00:00 app[web.1]: INFO: Deploying configuration descriptor /app/tomcat/conf/Catalina/localhost/ROOT.xml
2012-08-06T16:28:51+00:00 heroku[web.1]: State changed from starting to up
2012-08-06T16:28:51+00:00 app[web.1]: Aug 6, 2012 4:28:51 PM org.apache.coyote.AbstractProtocol start
2012-08-06T16:28:51+00:00 app[web.1]: INFO: Starting ProtocolHandler ["http-bio-52213"]
2012-08-06T16:28:51+00:00 app[web.1]: Aug 6, 2012 4:28:51 PM org.apache.catalina.startup.Catalina start
2012-08-06T16:28:51+00:00 app[web.1]: INFO: Server startup in 1027 ms
2012-08-06T16:29:45+00:00 heroku[router]: GET stormy-reef-2997.herokuapp.com/ dyno=web.1 queue=0 wait=0ms service=700ms status=200 bytes=52
2012-08-06T16:29:45+00:00 heroku[router]: GET stormy-reef-2997.herokuapp.com/favicon.ico dyno=web.1 queue=0 wait=0ms service=6ms status=404 bytes=988
2012-08-06T16:29:50+00:00 app[web.1]: Aug 6, 2012 4:29:50 PM org.apache.catalina.core.ApplicationContext log
2012-08-06T16:29:50+00:00 app[web.1]: INFO: SQL Connection Test: true
2012-08-06T16:29:50+00:00 heroku[router]: GET stormy-reef-2997.herokuapp.com/hello dyno=web.1 queue=0 wait=0ms service=127ms status=200 bytes=12
```

The 2nd line indicates that connection parameters were properly detected and parsed. The second to last line indicates that the servlet was able to hit the database during initialization.

# Customize Data Source Settings

You can customize the following data source settings:

* JNDI name. Default is jdbc/default. You can set it to jdbc/whatever.
* Max Active: 20
* Max Idle: 10
* Max Wait: -1 (disabled)

To change their default value, set new values of one or more of these paramters using the config variable approach.
