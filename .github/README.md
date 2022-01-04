# Schema Pusher

A containerized Java application for pushing schemas to Red Hat's Service Registry.

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

**Important notes**:

- For `AMQ Streams`, the bootstrap route is probably a secured one.
- When producing messages to multiple topics, only the *topic_record* strategy is allowed.
- Schema files of type `JSON`, `AVSC`, `AVRO` are supported, other files won't get picked up by the application.
- The files extracted from the decoded *tar.gz* archive, will be scanned recursively.

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

**Example usage**:

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

The docker image basically wraps a shell script invoking a java app.</br>
You can invoke [the shell script](../src/main/shell/entrypoint.sh) directly:

```shell
./src/main/shell/entrypoint.sh \
--bootstrap https://<kafka-bootstrap-route-url-goes-here>:443 \
--registry http://<service-registry-route-url-goes-here> \
--strategy topic_record \
--topic topic1 --topic anothertopic1 --topic onemoretopic1 \
--content $(base64 -w 0 schema_files.tar.gz)
```

You can also run the *java application* directly,</br>
just keep in mind that the app takes a directory of schema files and not an encoded base64 value,</br>
the *shell script* is in charge of decoding and extracting the archive prior to invoking the app.</br>

> Note that this form of execution requires an application build.

```shell
java -jar target/schema-pusher-jar-with-dependencies.jar \
-b=https://<kafka-bootstrap-route-url-goes-here>:443 \
-r=http://<service-registry-route-url-goes-here> \
-t=topic1 -t=anothertopic1 -t=onemoretopic1 \
-d src/test/resources/com/redhat/schema/pusher/avro/schemas/
```

> Note the for the java app, the *topic_record* strategy is the default one used if none specified.

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

### Release process

- Decide the desired version, i.e. 1.2.3.
- Set the desired release version, add, commit, and tag it:

  ```shell
  mvn versions:set -DnewVersion 1.2.3
  git add pom.xml
  git commit -m "build: set release version to 1.2.3 [skip ci]"
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
  git commit -m "build: set a new snapshot version"
  git push --follow-tags
  ```
