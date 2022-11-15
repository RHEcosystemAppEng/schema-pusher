#!/bin/bash

# supported subject naming strategies
declare -a naming_strategies=("topic" "record" "topic_record")

show_usage() {
  echo ""
  echo "Tool for decoding base64 schema files and producing Kafka messages for the specified topics."
  echo "The schema files will be pushed to Red Hat's service registry via the attached Java application."
  echo "------------------------------------------------------------------------------------------------"
  echo "Usage: -h/--help"
  echo "Usage: [options]"
  echo ""
  echo "Options:"
  echo "--bootstrap, (mandatory) kafka bootstrap url."
  echo "--registry, (mandatory) service registry url."
  echo "--strategy, (optional) subject naming strategy, [${naming_strategies[*]}] (default: topic_record)."
  echo "--topic (mandatory), topic/s to push the schemas to (repeatable in correlation with schema)."
  echo "--schema, (mandatory) base64 encoded schema file (repeatable in correlation with topic)."
  echo "--propkey, (optional) a string key to set for the producer (repeatable in correlation with propvalue)."
  echo "--propvalue, (optional) a string value to set for the producer (repeatable in correlation with propkey)."
  echo "--truststore, (optional) base64 encoded pkcs12 truststore for identifying the bootstrap (inclusive with truststorePassword)."
  echo "--truststorePassword (optional) password for accessing the pkcs12 truststore (inclusive with truststore)."
  echo "--keystore, (optional) base64 encoded pkcs12 keystore for identifying to the bootstrap (inclusive with keystorePassword)."
  echo "--keystorePassword (optional) password for accessing the pkcs12 keystore (inclusive with keystore)."
  echo ""
  echo "Example:"
  echo "--bootstrap https://kafka-bootstrap-url:443 --registry http://service-registry-url:8080 \\"
  echo "--strategy topic_record \\"
  echo "--topic sometopic --schema \$(base64 -w 0 my-schema.avsc) \\"
  echo "--topic someothertopic --schema \$(base64 -w 0 my-other-schema.avsc) \\"
  echo "--propkey basic.auth.credentials.source --propvalue USER_INFO \\"
  echo "--propkey schema.registry.basic.auth.user.info --propvalue registry-user:changeme \\"
  echo "--truststore \$(base64 -w 0 kafka_cluster_ca.p12) \\"
  echo "--truststorePassword secretTruststorePassword \\"
  echo "--keystore \$(base64 -w 0 kafka_user_ca.p12) \\"
  echo "--keystorePassword secretKeystorePassword"
  echo ""
  echo "This should result in each schema file being produced to its respective topic using the specified naming strategy."
  echo ""
}

if [[ ($1 == "--help") || $1 == "-h" ]]; then
  show_usage
  exit 0
fi

# create a lists for aggregating options
declare -a topics=() # aggregate topics
declare -a schemas=() # aggregate schemas
declare -a propkeys=() # aggregate consumer custom property keys
declare -a propvalues=() # aggregate consumer custom property values

# iterate over arguments and create named parameters
while [ $# -gt 0 ]; do
  if [[ $1 == *"--"* ]]; then
    param="${1/--/}"
    if [ "$param" = "topic" ]; then
      # if argument is topic, add to list
      topics+=("$2")
    elif [ "$param" = "schema" ]; then
      # if argument is schema, add to list
      schemas+=("$2")
    elif [ "$param" = "propkey" ]; then
      # if argument is propkey, add to list
      propkeys+=("$2")
    elif [ "$param" = "propvalue" ]; then
      # if argument is propvalue, add to list
      propvalues+=("$2")
    else
      # else declare it
      declare "$param"="$2"
    fi
  fi
  shift
done

# default named parameters
strategy=${strategy:-topic_record}

# verify bootstrap
if [ -z "$bootstrap" ]; then
  echo "the bootstrap parameter is mandatory."
  show_usage
  exit 1
fi

# verify registry
if [ -z "$registry" ]; then
  echo "the registry parameter is mandatory."
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
if [ -z "$topics" ]; then
  echo "at least one topic is required"
  show_usage
  exit 1
fi

# verify minimum of 1 schema
if [ -z "$schemas" ]; then
  echo "at least one schema is required"
  show_usage
  exit 1
fi

# verify topics and schema counts are correlated
if [ "${#topics[@]}" -ne "${#schemas[@]}" ]; then
  echo "topics and schemas specified doesn't correlate"
  show_usage
  exit 1
fi

# verify property keys and values counts are correlated
if [ "${#propkeys[@]}" -ne "${#propvalues[@]}" ]; then
  echo "property keys and values specified doesn't correlate"
  show_usage
  exit 1
fi

# create directory structure
dest_dir=tmp_schemas
certs_dir=certs
mkdir $dest_dir
mkdir $certs_dir

# create the java command for executing the program
java_cmd="java -jar /app/schema-pusher-jar-with-dependencies.jar \
--bootstrap-url=$bootstrap --registry-url=$registry --naming-strategy=$strategy"

# iterate over the topics and schemas lists and create the arguments for the app
for i in "${!topics[@]}"; do
  # decode the schema based on the current index and save file based on topic name
  echo "${schemas[$i]}" | base64 --decode - > $dest_dir/"${topics[$i]}".avsc
  # incorporate topic and schema file into the java command
  java_cmd+=" --topic=${topics[$i]} --schema-path=$dest_dir/${topics[$i]}.avsc"
done

for i in "${!propkeys[@]}"; do
  java_cmd+=" --property-key=${propkeys[$i]} --property-value=${propvalues[$i]}"
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
exit $ret_code
