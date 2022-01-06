# Schema Pusher

<!-- markdownlint-disable -->
<a href="https://quay.io/repository/ecosystem-appeng/schema-pusher">
    <img src="https://raw.githubusercontent.com/RHEcosystemAppEng/schema_pusher/main/images/docker_run_flow.png" width="800" height="350" alt="">
</a>
<!-- markdownlint-restore -->

[Red Hat's Service Registry][10], part of [Red Hat's Integration][11] services portfolio,</br>
is a *Kafka* based schema database.</br>
Used for sharing and reusing data structures between developers and services.</br>
An ideal solution for a complex microservices-based environment.</br>

And if you're in for the *full* [OpenShift][12] experience,</br>
[AMQ][13] which is also part of [Red Hat's Integration][11] services portfolio,</br>
got your underlying *Kafka* deployment covered with [AMQ Streams][14].

This [containerized application][15] helps you easily migrate your various data structures, schemas to [Red Hat's Service Registry][10].</br>
Simply create a *tar.gz* archive with all of your schema files (subfolders are ok),</br>
run the container, and get back to work.</br>
:grin:

## Run

```shell
docker run --network host --rm -it \
quay.io/ecosystem-appeng/schema-pusher:latest \
--bootstrap https://<kafka-bootstrap-route-url-goes-here>:443 \
--registry http://<service-registry-route-url-goes-here> \
--strategy topic_record \
--topic topic1 --topic anothertopic1 --topic onemoretopic1 \
--content $(base64 -w 0 schema_files.tar.gz)
```

> Note, for `AMQ Streams`, the bootstrap route is probably a secured one.</br>
> Also note, when producing messages to multiple topics, only the *topic_record* strategy is allowed.

For help:

```shell
docker run --rm -it quay.io/ecosystem-appeng/schema-pusher:latest --help
```

prints:

```text
Script for decoding and extracting base64 tar.gz archive containing schema files.
The schema files will be then pushed to Red Hat's service registry via the attached Java app.
---------------------------------------------------------------------------------------------
/app/entrypoint.sh Usage: -h/--help
/app/entrypoint.sh Usage: [options]

Options:
--bootstrap, kafka bootstrap url.
--registry, service registry url.
--strategy, subject naming strategy, [topic, record, topic_record].
--topic (repeatable), topic/s to push the schemas to.
--content, base64 encoded 'tar.gz' archive containing the schema files.

Example:
/app/entrypoint.sh --bootstrap https://kafka-bootstrap-url:443 --registry http://service-registry-url:8080 \
--strategy topic_record --topic sometopic --topic anothertopic --topic onemoretopic \
--content $(base64 -w 0 schema_files.tar.gz)

This should result in extracting the tar.gz archive decoded from the content parameter's value,
the extracted schema files will be pushed to the kafka and registry instances using the specified subject
naming strategy.
Each schema file will be pushed to all the specified topics, for the example above, if the
the archive contains 2 schema files, then 6 schemas will be pushed, one per topic specified.

Please note, multiple topics are only supported with the 'topic_record' naming strategy, the
other strategies ('topic' and 'record') will result in messages overwriting each other.
```

### Example usage

The following is a *base64* representation of a *tar.gz* archive containing
[these testing resources](../src/test/resources/com/redhat/schema/pusher/avro/schemas).</br>
Using the following value with the example above will pick up three schema files, *test_schema.avsc*,
*test_schema_more.avro*, and *subfolder/test_schema_too.json*.</br>
Each schema will be produced for each topic since the example above specifies three topics, and we have three schemas,
hence, when using the specified (mandatory) *topic_record* strategy, a total of nine messages will be produced.

```text
H4sIAAAAAAAAA+2Y24rbMBCGfZ2nML4OtnwU9AEKvWgpdKEXpQTHVjZebCtI8pay5N0ryTmVdSo1GC1L5rsxlkce0D//jBPOqkgQLiJGOB1YRXhU0U7e1dtSRLzakq6MdgPfEhaVz4welnjk2YMkGOfqGuMcXV6PeHEWF2mK4jxJPZSkOMaen/9HjpsZuCiZ73uCdoRtmqtxpufvFH6r/mrTarwJy2de/SOHErgosqv6p3Ex6p8UUvpY6o8RzjwfuTiAO9f/ZeH7QV92hO/KigQf/EDKH47yhwd5R/lDJf9hiQdLtU/83uktjFSU1eOaepdae5D10fSP33T8+GjTkLbm8uEPead4OUXr6/L8Ri6Y3Bzsl68im/oyrqbDuiXBXob9XOwXb32a74+b/c+H9Ya2tVw25rDu/7r9y7gkx6iA/u+CGfS/mAQrQWn4xGn/Vw5T/8/ksD/1/zzx5EDABfR/J7jp/w+UGkdA2VMh03yxngSHDZ8mB8JEeF2T+qPKP/X6ex0gc3z/rTrKiC6P6Rxm/ydn/8tvQSQHgfwMBP87wI3/P8sCMTYAVUX27lfRttZXsa+cv6a0JWV/v9bXzOJ/daQt4fxKDuPvv6w4/f7PMqT8j3AC/neBo/l/KBBjDzhWkn0fOO6Y7gV37W0bZvH/L0b7x1Cd/WQOs//jC//nyv9pWoD/XeDG/99VgagmYGwAupTs3a/Dwfq3M4v/d7TpRXg1h9H/aX7+/1/5P8GZXAL/O8CN/7+qAjF6X5eRvfd1OHgfAAAAAAAAAADAjj85mSqvACgAAA==
```

### Other run options

The docker image wraps a *Shell* script invoking a *Java* application.</br>
You can invoke [the shell script](../src/main/shell/entrypoint.sh) directly:

```shell
./src/main/shell/entrypoint.sh \
--bootstrap https://<kafka-bootstrap-route-url-goes-here>:443 \
--registry http://<service-registry-route-url-goes-here> \
--strategy topic_record \
--topic topic1 --topic anothertopic1 --topic onemoretopic1 \
--content $(base64 -w 0 schema_files.tar.gz)
```

You can also run the *Java* application directly,</br>
just keep in mind that the app takes a directory of schema files and not an encoded base64 value,</br>
the *Shell* script is in charge of decoding and extracting the archive prior to invoking the app.</br>

> Note that this form of execution requires an application build.

```shell
java -jar target/schema-pusher-jar-with-dependencies.jar \
-b=https://<kafka-bootstrap-route-url-goes-here>:443 \
-r=http://<service-registry-route-url-goes-here> \
-t=topic1 -t=anothertopic1 -t=onemoretopic1 \
-d src/test/resources/com/redhat/schema/pusher/avro/schemas/
```

> Note that for the *Java* run, the *topic_record* strategy is the default one used if none is specified.

For help:

```shell
java -jar target/schema-pusher-jar-with-dependencies.jar --help
```

prints:

```text
Usage: avro_push [-hV] -b=<kafkaBootstrap> -d=<directory> [-n=<namingStrategy>]
                 -r=<serviceRegistry> (-t=<topic>)...
Push AVRO schemas to Red Hat's Service Registry
  -b, --bootstrap-url=<kafkaBootstrap>
                        The url for Kafka's bootstrap server.
  -d, --directory=<directory>
                        The path of the directory containing the schema files.
  -h, --help            Show this help message and exit.
  -n, --naming-strategy=<namingStrategy>
                        The subject naming strategy.
  -r, --registry-url=<serviceRegistry>
                        The url for Red Hat's service registry.
  -t, --topic=<topic>   The topic to produce the message too, repeatable.
  -V, --version         Print version information and exit.
```

## Supported schema types

At the time of writing this, this application supports [Apache AVRO][16].</br>
Supported file types are `JSON`, `AVSC`, `AVRO`, other types won't get picked up by the application.

## Development

This application is constructed of three layers:

- A *Java Application*, a *CLI* application in charge of consuming schema files from a directory and producing Kafka
  messages and service registry requests for each schema file.</br>
  Code pointers:
  - [src/main/java/com/redhat/schema/pusher/PushCli.java](../src/main/java/com/redhat/schema/pusher/PushCli.java) is the
    CLI abstraction, and [src/main/java/com/redhat/schema/pusher/avro/AvroPushCli.java](../src/main/java/com/redhat/schema/pusher/avro/AvroPushCli.java)
    it the implementation in charge of picking up the arguments and preparing them for the producer invocations.
  - [src/main/java/com/redhat/schema/pusher/SchemaPusher.java](../src/main/java/com/redhat/schema/pusher/SchemaPusher.java)
    is the contract, and
    [src/main/java/com/redhat/schema/pusher/avro/AvroSchemaPusher.java](../src/main/java/com/redhat/schema/pusher/avro/AvroSchemaPusher.java)
    is the implementation in charge of pushing schema messages via the producer.
  - [src/main/java/com/redhat/schema/pusher/MainApp.java](../src/main/java/com/redhat/schema/pusher/MainApp.java) is
    the main application starting point, instantiating the di context and loading the command line.
  - [src/main/java/com/redhat/schema/pusher/MainApp.java](../src/main/java/com/redhat/schema/pusher/MainApp.java) is
    the starting point of the application.
- A [Shell Script](../src/main/shell/entrypoint.sh) is in charge of decoding the archive, extracting it, and invoking
  the *Java App* with the extracted content.
- A [Dockerfile](../src/main/docker/Dockerfile) instruction set in charge of containerizing the *Shell Script* and the *Java App*.

### Usefull build commands

| Command                 | Description                               |
| ----------------------- | ----------------------------------------- |
| `mvn package`           | tests and builds the java application.    |
| `mvn verify`            | tests, build, verifies the code coverage. |
| `mvn k8s:build`         | builds the docker image.                  |
| `mvn help:all-profiles` | will display the existing profiles.       |

### CI

| Workflow                         | Trigger                                            | Description                                       |
| -------------------------------- | -------------------------------------------------- | ------------------------------------------------- |
| [Build](workflows/build.yml)     | manually - pull requests - push to the main branch | will build the project including the docker image |
| [Release](workflows/release.yml) | push semver tags to the main branch                | will create a release for the triggering tag      |

### Release process

- Decide the desired version, i.e. 1.2.3.
- Set the desired release version, add, commit, and tag it:

  ```shell
  mvn versions:set -DnewVersion=1.2.3
  git add pom.xml
  git commit -m "build: set release version to 1.2.3"
  git tag -m "new version title" 1.2.3
  ```

- Build the project and push it to the image registry:

  ```shell
  mvn package k8s:build k8s:push
  ```

- Set the next development version iteration, add, commit, tag, and push it:

  ```shell
  mvn versions:set -DnextSnapshot
  git add pom.xml
  git commit -m "build: set a new snapshot version [skip ci]"
  git push --follow-tags
  ```

The pushed tag will trigger the [release.yml workflow](workflows/release.yml) and automatically create a
*GitHub Release* for the *1.2.3* tag with the title *new version title*.

## Useful links

- [What is a service registry blog post][23]
- [The schema-pusher Docker image][15]
- [Installing the Red Hat Integration operator on OpenShift guide][17]
- [Installing and deploying Service Registry on OpenShift guide][18]
- [Service Registry user guide][19]
- [Using AMQ Streams on OpenShift][20]
- [Red Hat AMQ][21]
- [Red Hat Integration][22]

<!-- links -->
[10]: https://www.redhat.com/en/technologies/cloud-computing/openshift/openshift-service-registry
[11]: https://www.redhat.com/en/products/integration
[12]: https://www.redhat.com/en/technologies/cloud-computing/openshift
[13]: https://www.redhat.com/en/technologies/jboss-middleware/amq
[14]: https://www.redhat.com/en/resources/amq-streams-datasheet
[15]: https://quay.io/repository/ecosystem-appeng/schema-pusher
[16]: https://avro.apache.org/
[17]: https://access.redhat.com/documentation/en-us/red_hat_integration/2021.q3/html/installing_the_red_hat_integration_operator_on_openshift/index
[18]: https://access.redhat.com/documentation/en-us/red_hat_integration/2021.q3/html/installing_and_deploying_service_registry_on_openshift/index
[19]: https://access.redhat.com/documentation/en-us/red_hat_integration/2021.q3/html/service_registry_user_guide/index
[20]: https://access.redhat.com/documentation/en-us/red_hat_amq/2021.q3/html/using_amq_streams_on_openshift/index
[21]: https://access.redhat.com/products/red-hat-amq/
[22]: https://access.redhat.com/products/red-hat-integration
[23]: https://www.redhat.com/en/topics/integration/what-is-a-service-registry
