# wunderboss-artemis

This allows WunderBoss-based systems
([Immutant](http://immutant.org/), [TorqueBox](http://torquebox.org/))
to use [ActiveMQ Artemis](http://activemq.apache.org/artemis/) instead
of [HornetQ](http://hornetq.org/) for messaging.

## What is Artemis?

Artemis is the next generation of ActiveMQ, and is based on a merging
of the ActiveMQ and HornetQ code bases.

## Usage

In theory, you should be able to exclude
`org.projectodd.wunderboss/wunderboss-messaging-hornetq` from your
dependencies, and depend directly on
`org.projectodd.wunderboss/wunderboss-artemis`. In reality, it may be
a tetch more complicated.

### With Immutant

First, you'll need to depend on a recent
[incremental build](http://immutant.org/builds/2x/) (#585 or newer),
then make a few adjustments to your `:dependencies`:

```clojure
:dependencies [[org.immutant/messaging "2.x.incremental.585"
                :exclusions [org.projectodd.wunderboss/wunderboss-messaging-hornetq]]
               [org.projectodd.wunderboss/wunderboss-artemis "0.1.0"]]
```

Note that this will only currently work outside of WildFly, since
Artemis is not yet available in the container, and this project
doesn't yet provide the proper bindings to access it even if it was
available.

### With TorqueBox

TBD - it will likely require a gem, since the
`wunderboss-messaging-hornetq` is embedded within the
`torquebox-messaging` gem.

### Overriding the default configuration

If you need to customize the configuration, you just need to provide a
`broker.xml` on the classpath. You can use
[our default one](https://github.com/projectodd/wunderboss-artemis/blob/master/src/main/resources/default-broker.xml)
as a base, and refer to the
[Artemis docs](http://activemq.apache.org/artemis/docs.html) for help.

## License

wunderboss-artemis is licensed under the Apache License, v2. See
[LICENSE](LICENSE) for details.
