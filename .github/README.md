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

This [containerized application][15] helps you easily migrate your various data structures schemas to
[Red Hat's Service Registry][10].</br>
Simply invoke with a list of topics and schemas, and get back to work.</br>
:grin:

## Run

```shell
docker run --network host --rm -it \
quay.io/ecosystem-appeng/schema-pusher:latest \
--bootstrap https://<kafka-bootstrap-route-url-goes-here>:443 \
--registry http://<service-registry-route-url-goes-here> \
--strategy topic_record \
--topic sometopic --schema $(base64 -w 0 my_schema.avsc) \
--topic someothertopic --schema $(base64 -w 0 my_other_schema.avsc) \
--truststore $(base64 -w 0 kafka_cluster_ca.p12) \
--truststorePassword $(cat kafka_cluster_ca.password) \
--keystore $(base64 -w 0 kafka_user.p12) \
--keystorePassword $(cat kafka_user.password)
```

> If you use a self-signed certificate for your *kafka* deployment, you can use the optional *--truststore*
> and *truststorePassword* parameters to pass the *pkcs12 truststore* file and related password of the *kakfa
> cluster*.</br>
> If your *kafka* deployment require authentication, you can use the optional *--keystore* and *--keystorePassword*
> to pass the the *pkcs12 keystore* file and related password of the *kakfa user*.</br>
> </br>
> You can follow [this][24] and grab the relevant *pkcs12* and password,
> replace *USER_SECRET_NAME* with the user secret name,
> repalce *CLUSTER_CA_SECRET_NAME* with the cluster ca name (usally ends with *cluster-ca-cert*):
>
> ```shell
> oc get secret CLUSTER_CA_SECRET_NAME -o jsonpath='{.data.ca\.p12}' | base64 -d > kafka_cluster_ca.p12
> oc get secret CLUSTER_CA_SECRET_NAME -o jsonpath='{.data.ca\.password}' | base64 -d > kafka_cluster_ca.password
> oc get secret USER_SECRET_NAME -o jsonpath='{.data.user\.p12}' | base64 -d > kafka_user.p12
> oc get secret USER_SECRET_NAME -o jsonpath='{.data.user\.password}' | base64 -d > kakfa_user.password
> ```
>

## Get help

For help:

```shell
docker run --rm -it quay.io/ecosystem-appeng/schema-pusher:latest --help
```

prints:
<!-- editorconfig-checker-disable-max-line-length -->
```text
Tool for decoding base64 schema files and procude kafka messages for the specified topics.
The schema files will be pushed to Red Hat's service registry via the attached Java application.
------------------------------------------------------------------------------------------------
Usage: -h/--help
Usage: [options]

Options:
--bootstrap, (mandatory) kafka bootstrap url.
--registry, (mandatory) service registry url.
--strategy, (optional) subject naming strategy, [topic record topic_record] (default: topic_record).
--topic (mandatory), topic/s to push the schemas to (repeatable in correlation with schema).
--schema, (mandatory) base64 encoded schema file (repeatable in correlation with topic).
--truststore, (optional) base64 encoded pkcs12 truststore for identifying the bootstrap (inclusive with truststorePassword).
--truststorePassword (optional) password for accessing the pkcs12 truststore (inclusive with truststore).
--keystore, (optional) base64 encoded pkcs12 keystore for identifying to the bootstrap (inclusive with keystorePassword).
--keystorePassword (optional) password for accessing the pkcs12 keystore (inclusive with keystore).

Example:
--bootstrap https://kafka-bootstrap-url:443 --registry http://service-registry-url:8080 \
--strategy topic_record \
--topic sometopic --schema $(base64 -w 0 my-schema.avsc) \
--topic someothertopic --schema $(base64 -w 0 my-other-schema.avsc) \
--truststore $(base64 -w 0 kafka_cluster_ca.p12) \
--truststorePassword secretTruststorePassword \
--keystore $(base64 -w 0 kafka_user_ca.p12) \
--keystorePassword secretKeystorePassword

This should result in each schema file being produced to its respective topic using the specified naming strategy.
```
<!-- editorconfig-checker-enable-max-line-length -->

## Schema types

Currently, we support [AVRO Schemas][16].

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
as well as the actual path of the pkcs12 files and literal passwords.</br>
The *Shell* script is the component in charge of decoding and preparing the arguments for invoking the *Java* app.

> Note that this form of execution requires an application build.

```shell
java -jar target/schema-pusher-jar-with-dependencies.jar \
-b=https://<kafka-bootstrap-route-url-goes-here>:443 \
-r=http://<service-registry-route-url-goes-here> \
-t=topic1 -t=anothertopic1 -t=onemoretopic1 \
--schema-path=src/test/resources/com/redhat/schema/pusher/avro/schemas/test_schema2.avsc,
--topic=anothertopic1,
--schema-path=src/test/resources/com/redhat/schema/pusher/avro/schemas/test_schema1.avsc,
--tf=certs/ca.p12 \
--tp=secretClusterCaPKCS12password \
--kf=certs/user.p12 \
--kp=secretUserPKCS12password
```

> Note that the *topic_record* strategy is the default one used if none is specified.

For help:

```shell
java -jar target/schema-pusher-jar-with-dependencies.jar --help
```

prints:

```text
Usage: <main class> [-hV] -b=<kafkaBootstrap> [-n=<namingStrategy>]
                    -r=<serviceRegistry> (-t=<topic> -s=<schemaPath>)...
                    [--tf=<truststoreFile> --tp=<truststorePassword>]
                    [--kf=<keystoreFile> --kp=<keystorePassword>]
Push schemas to Red Hat's Service Registry
  -b, --bootstrap-url=<kafkaBootstrap>
                        The url for Kafka's bootstrap server.
  -h, --help            Show this help message and exit.
      --kf, --keystore-file=<keystoreFile>
                        The path for the keystore pkcs12 file for use with the
                          Kafka producer
      --kp, --keystore-password=<keystorePassword>
                        The password for the keystore pkcs12 file for use with
                          the Kafka producer
  -n, --naming-strategy=<namingStrategy>
                        The subject naming strategy.
  -r, --registry-url=<serviceRegistry>
                        The url for Red Hat's service registry.
  -s, --schema-path=<schemaPath>
                        The schema path for the topic, correlated with a topic.
  -t, --topic=<topic>   The desired topic for the schema, correlated with a
                          schema path.
      --tf, --truststore-file=<truststoreFile>
                        The path for the truststore pkcs12 file for use with
                          the Kafka producer
      --tp, --truststore-password=<truststorePassword>
                        The password for the truststore pkcs12 file for use
                          with the Kafka producer
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

## Linting locally

```shell
docker run --rm -e RUN_LOCAL=true -e IGNORE_GITIGNORED_FILES=true -e VALIDATE_BASH=true \
-e VALIDATE_DOCKERFILE_HADOLINT=true -e VALIDATE_EDITORCONFIG=true -e VALIDATE_GITHUB_ACTIONS=true \
-e VALIDATE_JAVA=true -e VALIDATE_JSON=true -e VALIDATE_MARKDOWN=true  -e VALIDATE_XML=true \
-e VALIDATE_YAML=true -v ${PWD}:/tmp/lint ghcr.io/github/super-linter:slim-v4
```

### CI

| Workflow           | Trigger                             | Description                                    |
| ------------------ | ----------------------------------- | ---------------------------------------------- |
| [Pull Request][62] | pull requests                       | lint and test the project                      |
| [Release][63]      | push semver tags to the main branch | create a github release for the triggering tag |
| [Stage][64]        | manually : push to the main branch  | build the project including the docker image   |

### Release process

- Decide the desired version, i.e. *1.2.3* and version title, i.e. *new version title*.
  > Tip: the following command will predict the next `semver` based on git tags and conventional commits:
  >
  > ```shell
  > docker run --rm -it -v $PWD:/usr/share/repo tomerfi/version-bumper:latest | cut -d ' ' -f 1 | xargs
  > ```

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

- Set the next development version iteration, add, commit, and push:

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
[24]: https://access.redhat.com/documentation/en-us/red_hat_amq/2021.q3/html-single/deploying_and_upgrading_amq_streams_on_openshift/index#setup-external-clients-str

<!-- relative paths -->
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
