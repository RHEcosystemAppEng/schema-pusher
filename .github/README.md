# Schema Pusher

<!-- markdownlint-disable -->
<!-- editorconfig-checker-disable -->
<a href="https://quay.io/repository/ecosystem-appeng/schema-pusher">
    <img src="https://raw.githubusercontent.com/RHEcosystemAppEng/schema-pusher/main/images/docker_run_flow.png" width="700" height="300" alt="">
</a>
<!-- editorconfig-checker-enable -->
<!-- markdownlint-restore -->

[Red Hat's Service Registry][10], part of [Red Hat's Integration][11] services portfolio,</br>
is a *Kafka* based schema database.</br>
Used for sharing and reusing data structures between developers and services.</br>
An ideal solution for a complex microservices-based environment.</br>

And if you're in for the *full* [OpenShift][12] experience,</br>
[AMQ][13] which is also part of [Red Hat's Integration][11] services portfolio,</br>
got your underlying *Kafka* deployment covered with [AMQ Streams][14].

This [containerized application][15] helps you easily migrate your various data structures,</br>
schemas to [Red Hat's Service Registry][10].</br>
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
--content $(base64 -w 0 schema_files.tar.gz) \
--certificate $(base64 -w 0 kafka_bootstrap_ca.crt)
```

> Note, when producing messages to multiple topics, only the *topic_record* strategy is allowed.</br>
> </br>
> Also Note, for *AMQ Streams*, the bootstrap route is probably a secured one,</br>
> if you use a self-signed certificate, you can use the optional *--certificate* parameter to pass a it as *base64*
> encoded.</br>
> You can get the certificate from you deployment's scerets, look for the `*cluster-ca-cert` secret:
>
> ```shell
> oc extract secret/my-kafka-cluster-ca-cert --keys=ca.crt --to=- > kafka_bootstrap_ca.crt
> ```

## Get help

For help:

```shell
docker run --rm -it quay.io/ecosystem-appeng/schema-pusher:latest --help
```

prints:

```text
Tool for decoding and extracting base64 tar.gz archive containing schema files.
The schema files will be pushed to Red Hat's service registry via the attached Java application.
------------------------------------------------------------------------------------------------
Usage: -h/--help
Usage: [options]

Options:
--bootstrap, (mandatory) kafka bootstrap url.
--registry, (mandatory) service registry url.
--strategy, (optional) subject naming strategy, [topic, record, topic_record] (default: topic_record).
--topic (mandatory), topic/s to push the schemas to (repeatable).
--content, (mandatory) base64 encoded 'tar.gz' archive containing the schema files.
--certificate, (optional) base64 encoded certificate for using with the bootstrap.

Example:
--bootstrap https://kafka-bootstrap-url:443 --registry http://service-registry-url:8080 \
--strategy topic_record --topic sometopic --topic anothertopic --topic onemoretopic \
--content $(base64 -w 0 schema_files.tar.gz)
--certificate $(base64 -w 0 kafka_bootstrap_ca.cert)

This should result in extracting the tar.gz archive decoded from the content parameter's value,
the extracted schema files will be pushed to kafka/registry instance using the specified subject
naming strategy.
Each schema file will be pushed to all the specified topics, for the example above, if the
archive contains 2 schema files, then 6 schemas will be pushed, one per each topic specified.

Please note, multiple topics are only supported with the 'topic_record' naming strategy, the
other strategies ('topic' and 'record') will result in messages overwriting eachother.
```

## Example usage

The following is a *base64* representation of a *tar.gz* archive containing [these testing resources][50].</br>
<!-- editorconfig-checker-disable-max-line-length -->
```text
H4sIAAAAAAAAA+2Y24rbMBCGfZ2nML4OtnwU9AEKvWgpdKEXpQTHVjZebCtI8pay5N0ryTmVdSo1GC1L5rsxlkce0D//jBPOqkgQLiJGOB1YRXhU0U7e1dtSRLzakq6MdgPfEhaVz4welnjk2YMkGOfqGuMcXV6PeHEWF2mK4jxJPZSkOMaen/9HjpsZuCiZ73uCdoRtmqtxpufvFH6r/mrTarwJy2de/SOHErgosqv6p3Ex6p8UUvpY6o8RzjwfuTiAO9f/ZeH7QV92hO/KigQf/EDKH47yhwd5R/lDJf9hiQdLtU/83uktjFSU1eOaepdae5D10fSP33T8+GjTkLbm8uEPead4OUXr6/L8Ri6Y3Bzsl68im/oyrqbDuiXBXob9XOwXb32a74+b/c+H9Ya2tVw25rDu/7r9y7gkx6iA/u+CGfS/mAQrQWn4xGn/Vw5T/8/ksD/1/zzx5EDABfR/J7jp/w+UGkdA2VMh03yxngSHDZ8mB8JEeF2T+qPKP/X6ex0gc3z/rTrKiC6P6Rxm/ydn/8tvQSQHgfwMBP87wI3/P8sCMTYAVUX27lfRttZXsa+cv6a0JWV/v9bXzOJ/daQt4fxKDuPvv6w4/f7PMqT8j3AC/neBo/l/KBBjDzhWkn0fOO6Y7gV37W0bZvH/L0b7x1Cd/WQOs//jC//nyv9pWoD/XeDG/99VgagmYGwAupTs3a/Dwfq3M4v/d7TpRXg1h9H/aX7+/1/5P8GZXAL/O8CN/7+qAjF6X5eRvfd1OHgfAAAAAAAAAADAjj85mSqvACgAAA==
```
<!-- editorconfig-checker-enable-max-line-length -->
Using this value with [the example above](#run) will pick up three schema files:

- [test_schema.avsc][51]
- [test_schema_more.avro][52]
- [subfolder/test_schema_too.json][53]

Each schema will be produced per each topic.</br>
Since the example above specifies three topics, and we have three schemas,</br>
hence, with the *topic_record* strategy, a total of nine messages will be produced.

## Supported schema types

At the time of writing this, this application supports [Apache AVRO][16].</br>
Supported file types are `JSON`, `AVSC`, `AVRO`, other types won't get picked up by the application.

## Development

### Application walkthrough

This application is constructed of three layers:

- A *Java Application*, a *CLI* application in charge of consuming schema files from a directory and producing Kafka
  messages and service registry requests for each schema file.</br>
  Code pointers:
  - [com/redhat/schema/pusher/PushCli.java][55] is the
    CLI abstraction, and [com/redhat/schema/pusher/avro/AvroPushCli.java][56]
    it the implementation in charge of picking up the arguments and preparing them for the producer invocations.
  - [com/redhat/schema/pusher/SchemaPusher.java][57]
    is the contract, and
    [com/redhat/schema/pusher/avro/AvroSchemaPusher.java][58]
    is the implementation in charge of pushing schema messages via the producer.
  - [com/redhat/schema/pusher/MainApp.java][59] is
    the main application starting point, instantiating the di context and loading the command line.
- A [Shell script][54] is in charge of decoding, extracting, and invoking the *Java* application with the extracted
  content.
- A [Dockerfile][61] instruction set in charge of containerizing the *Shell* script and the *Java* application.

### Invoking the Java App directly

You can invoke the *Java* application directly,</br>
keep in mind that the app takes a directory of schema files and not an encoded base64 value,</br>
as well as a jks path and literal password instead of an encoded certificate.</br>
The *Shell* script is the component in charge of decoding and extracting the archive,</br>
as well as creating the truststore prior to invoking the *Java* app.</br>

> Note that this form of execution requires an application build.

```shell
java -jar target/schema-pusher-jar-with-dependencies.jar \
-b=https://<kafka-bootstrap-route-url-goes-here>:443 \
-r=http://<service-registry-route-url-goes-here> \
-t=topic1 -t=anothertopic1 -t=onemoretopic1 \
-d=src/test/resources/com/redhat/schema/pusher/avro/schemas/ \
-j=./truststore.jks \
-p=password
```

> Note that the *topic_record* strategy is the default one used if none is specified.

For help:

```shell
java -jar target/schema-pusher-jar-with-dependencies.jar --help
```

prints:

```text
Usage: <main class> [-hV] -b=<kafkaBootstrap> -d=<directory>
                    [-n=<namingStrategy>] -r=<serviceRegistry> (-t=<topic>)...
                    [-j=<truststoreJksPath> -p=<truststorePassword>]
Push schemas to Red Hat's Service Registry
  -b, --bootstrap-url=<kafkaBootstrap>
                        The url for Kafka's bootstrap server.
  -d, --directory=<directory>
                        The path of the directory containing the schema files.
  -h, --help            Show this help message and exit.
  -j, --truststore-jks-path=<truststoreJksPath>
                        The path for the truststore jks file for use with the
                          Kafka producer
  -n, --naming-strategy=<namingStrategy>
                        The subject naming strategy.
  -p, --truststore-password=<truststorePassword>
                        The password for the truststore jks file for use with
                          the Kafka producer
  -r, --registry-url=<serviceRegistry>
                        The url for Red Hat's service registry.
  -t, --topic=<topic>   The topic to produce the message too, repeatable.
  -V, --version         Print version information and exit.
```

### Usefull build commands

| Command                 | Description                               |
| ----------------------- | ----------------------------------------- |
| `mvn package`           | tests and builds the java application.    |
| `mvn verify`            | tests, build, verifies the code coverage. |
| `mvn k8s:build`         | builds the docker image.                  |
| `mvn k8s:push`          | push the docker image to [Quay.io][15].   |
| `mvn help:all-profiles` | will display the existing profiles.       |

> The *dev* profile will turn off enforcing and coverage checks.</br>
> The *cov* profile will create a *Jacoco* html coverage report.

### Linting locally

```shell
docker run --rm -e RUN_LOCAL=true -e IGNORE_GITIGNORED_FILES=true -e VALIDATE_BASH=true \
-e VALIDATE_DOCKERFILE_HADOLINT=true -e VALIDATE_EDITORCONFIG=true -e VALIDATE_GITHUB_ACTIONS=true \
-e VALIDATE_JAVA=true -e VALIDATE_JSON=true -e VALIDATE_MARKDOWN=true  -e VALIDATE_XML=true \
-e VALIDATE_YAML=true -v ${PWD}:/tmp/lint ghcr.io/github/super-linter:slim-v4
```

> Note: not linting *Java* with this.

### CI

| Workflow           | Trigger                             | Description                                    |
| ------------------ | ----------------------------------- | ---------------------------------------------- |
| [Pull Request][62] | pull requests                       | lint and test the project                      |
| [Release][63]      | push semver tags to the main branch | create a github release for the triggering tag |
| [Stage][64]        | manually : push to the main branch  | build the project including the docker image   |

### Release process

- Decide the desired version, i.e. *1.2.3* and version title, i.e. *new version title*.
- Set the desired release version, add, commit, and tag it (do not use *skip ci* for this commit):

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

- Set the next development version iteration, add, commit, and push it:

  ```shell
  mvn versions:set -DnextSnapshot
  git add pom.xml
  git commit -m "build: set a new snapshot version [skip ci]"
  git push --follow-tags
  ```

The pushed tag will trigger the [release.yml workflow][63],</br>
this will automatically create a *GitHub Release* for the *1.2.3* tag with the title *new version title*.

## Useful links

- [What is a service registry blog post][23]
- [The schema-pusher Docker image][15]
- [Installing the Red Hat Integration operator on OpenShift guide][17]
- [Installing and deploying Service Registry on OpenShift guide][18]
- [Service Registry user guide][19]
- [Using AMQ Streams on OpenShift][20]
- [Red Hat AMQ][21]
- [Red Hat Integration][22]

<!-- editorconfig-checker-disable-max-line-length -->
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

<!-- relative paths -->
[50]: ../src/test/resources/com/redhat/schema/pusher/avro/schemas
[51]: ../src/test/resources/com/redhat/schema/pusher/avro/schemas/test_schema.avsc
[52]: ../src/test/resources/com/redhat/schema/pusher/avro/schemas/test_schema_more.avro
[53]: ../src/test/resources/com/redhat/schema/pusher/avro/schemas/subfolder/test_schema_too.json
[54]: ../src/main/shell/entrypoint.sh
[55]: ../src/main/java/com/redhat/schema/pusher/PushCli.java
[56]: ../src/main/java/com/redhat/schema/pusher/avro/AvroPushCli.java
[57]: ../src/main/java/com/redhat/schema/pusher/SchemaPusher.java
[58]: ../src/main/java/com/redhat/schema/pusher/avro/AvroSchemaPusher.java
[59]: ../src/main/java/com/redhat/schema/pusher/MainApp.java
[60]: ../src/main/java/com/redhat/schema/pusher/MainApp.java
[61]: ../src/main/docker/Dockerfile
[62]: workflows/pr.yml
[63]: workflows/release.yml
[64]: workflows/stage.yml
