#!/bin/bash

# supported subject naming strategies
declare -a naming_strategies=("topic" "record" "topic_record")

show_usage() {
  echo ""
  echo "Tool for decoding and extracting base64 tar.gz archive containing schema files."
  echo "The schema files will be pushed to Red Hat's service registry via the attached Java application."
  echo "------------------------------------------------------------------------------------------------"
  echo "Usage: -h/--help"
  echo "Usage: [options]"
  echo ""
  echo "Options:"
  echo "--bootstrap, (mandatory) kafka bootstrap url."
  echo "--registry, (mandatory) service registry url."
  echo "--strategy, (optional) subject naming strategy, [${naming_strategies[*]}] (default: topic_record)."
  echo "--topic (mandatory), topic/s to push the schemas to (repeatable)."
  echo "--content, (mandatory) base64 encoded 'tar.gz' archive containing the schema files."
  echo "--truststore, (optional) base64 encoded pkcs12 truststore for identifying the bootstrap (inclusive with truststorePassword)."
  echo "--truststorePassword (optional) password for accessing the pkcs12 truststore (inclusive with truststore)."
  echo "--keystore, (optional) base64 encoded pkcs12 keystore for identifying to the bootstrap (inclusive with keystorePassword)."
  echo "--keystorePassword (optional) password for accessing the pkcs12 keystore (inclusive with keystore)."
  echo ""
  echo "Example:"
  echo "--bootstrap https://kafka-bootstrap-url:443 --registry http://service-registry-url:8080 \\"
  echo "--strategy topic_record --topic sometopic --topic anothertopic --topic onemoretopic \\"
  echo "--content \$(base64 -w 0 schema_files.tar.gz) \\"
  echo "--truststore \$(base64 -w 0 kafka_cluster_ca.p12) \\"
  echo "--truststorePassword secretTruststorePassword \\"
  echo "--keystore \$(base64 -w 0 kafka_user_ca.p12) \\"
  echo "--keystorePassword secretKeystorePassword"
  echo ""
  echo "This should result in extracting the tar.gz archive decoded from the content parameter's value,"
  echo "the extracted schema files will be pushed to kafka/registry instance using the specified subject"
  echo "naming strategy."
  echo "Each schema file will be pushed to all the specified topics, for the example above, if the"
  echo "archive contains 2 schema files, then 6 schemas will be pushed, one per each topic specified."
  echo ""
  echo "Please note, multiple topics are only supported with the 'topic_record' naming strategy, the"
  echo "other strategies ('topic' and 'record') will result in messages overwriting eachother."
  echo ""
}

if [[ ($1 == "--help") || $1 == "-h" ]]; then
  show_usage
  exit 0
fi

# create a list for aggregating topics
declare -a topics=()

# iterate over arguments and create named parameters
while [ $# -gt 0 ]; do
  if [[ $1 == *"--"* ]]; then
    param="${1/--/}"
    if [ "$param" = "topic" ]; then
      # if argument is topic, add to list
      topics+=("$2")
    else
      # else declare it
      declare "$param"="$2"
    fi
  fi
  shift
done

# default named parameters
strategy=${strategy:-topic_record}

# verify mandatory named parameters existence
if [ -z "$bootstrap" ] || [ -z "$registry" ] || [ -z "$content" ]; then
  echo "expected parameter/s missing."
  show_usage
  exit 1
fi

# verify supported strategy
if ! [[ ${naming_strategies[*]} =~ $strategy ]]; then
  echo "unknown subject naming strategy $strategy."
  show_usage
  exit 1
fi

# verify minimum of 1 topic
if [ "${#topics[@]}" -lt 1 ]; then
  echo "at least one topic is required"
  show_usage
  exit 1
fi

# verify multiple topics are only used with topic_record strategy
if [ "${#topics[@]}" -gt 1 ] && [[ "$strategy" != "topic_record" ]]; then
  echo "using the '$strategy' strategy with multiple topics may result in messages being overwritten."
  show_usage
  exit 1
fi

# create directory structure
dest_dir=tmp_schemas
certs_dir=certs
mkdir $dest_dir
mkdir $certs_dir

# decode the the tar.gz archive and extract it's content
echo "$content" | base64 --decode - | tar -C $dest_dir -xz

# create the java command for executing the program
java_cmd="java -jar /app/schema-pusher-jar-with-dependencies.jar \
--bootstrap-url=$bootstrap --registry-url=$registry --naming-strategy=$strategy --directory=$dest_dir"

# iterate over the topics list and concatenate the topic to the java command
for topic in "${topics[@]}"
do
  java_cmd+=" --topic=$topic"
done

# if provided truststore and truststore password
if [[ -v truststore ]] && [[ -v truststorePassword ]]; then
  # decode truststore to p12 type file
  echo "$truststore" | base64 --decode - > $certs_dir/truststore.p12
  # append the related arguments to the java command
  java_cmd+=" --truststore-file=$certs_dir/truststore.p12 --truststore-password=$truststorePassword"
fi

# if provided keystore and keystore password
if [[ -v keystore ]] && [[ -v keystorePassword ]]; then
  # decode keystore to p12 type file
  echo "$keystore" | base64 --decode - > $certs_dir/keystore.p12
  # append the related arguments to the java command
  java_cmd+=" --keystore-file=$certs_dir/keystore.p12 --keystore-password=$keystorePassword"
fi

# execute the java command
ret_code=$(eval "$java_cmd")

# clean up the destination directory
rm -r $dest_dir

# exit with the java app's return code
# shellcheck disable=SC2086
exit $ret_code
