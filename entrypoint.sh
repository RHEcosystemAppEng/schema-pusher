#!/bin/bash

# TODO: this if for development - remove this when done
export IN_DEV_MODE=true
if [ "$IN_DEV_MODE" = "true" ]; then
  export $(cat .env | xargs)
fi

# supported subject naming strategies
declare -a naming_strategies=("topic", "record", "topic_record")

show_usage() {
  echo ""
  echo "Script for decoding and extracting base64 tar.gz archive containing schema files."
  echo "The schema files will be then pushed to Red Hat's service registry via the attached Java app."
  echo "---------------------------------------------------------------------------------------------"
  echo "$0 Usage: -h/--help"
  echo "$0 Usage: [options]"
  echo ""
  echo "Options:"
  echo "--bootstrap, kafka bootstrap url."
  echo "--registry, service registry url."
  echo "--strategy, subject naming strategy, [${naming_strategies[@]}]."
  echo "--topic (repeatable), topic/s to push the schemas to."
  echo "--content, base64 encoded 'tar.gz' archive containing the schema files."
  echo ""
  echo "Example:"
  echo "$0 --bootstrap https://kafka-bootstrap-url:443 --registry http://service-registry-url:8080 \\"
  echo "--strategy topic_record --topic sometopic --topic anothertopic --topic onemoretopic \\"
  echo "--content \$(base64 -w 0 schema_files.tar.gz)"
  echo ""
  echo "This should result in extracting the tar.gz archive decoded from the content parameter's value,"
  echo "the extracted schema files will be pushed to kafka/registry instance using the specified subject"
  echo "naming strategy."
  echo "Each schema file will be pushed with all the specified topics, for the example above, if the"
  echo "archive contains 2 schema files, then 6 schemas will be pushed, one per each topic specified."
  echo ""
  echo "Please note, multiple topics are only supported with the 'topic_record' naming strategy, the"
  echo "other strategies ('topic' and 'record') will overwrite eachother."
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
      declare $param="$2"
    fi
  fi
  shift
done

# verify named parameters existence
if [ -z "$bootstrap" ] || [ -z "$registry" ] || [ -z "$strategy" ] || [ -z "$content" ]; then
  echo "expected parameter/s missing."
  show_usage
  exit 1
fi

# verify supported strategy
if ! [[ ${naming_strategies[*]} =~ "$strategy" ]]; then
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

# TODO: for development - change back to /app/schema
# destination directory for loading schemas
dest_dir=ttt #/app/schemas
mkdir $dest_dir

# decode the the tar.gz archive and extract it's content
echo $content | base64 --decode - | tar -C $dest_dir -xz

# TODO: for development - change backto /app/sche... instead of target/sche...
# create the java command for executing the program
java_cmd="java -jar target/schema-pusher-jar-with-dependencies.jar -Djava.util.logging.config.file=com/redhat/schema/pusher/logging.properties \
--bootstrap-url=$bootstrap --registry-url=$registry --naming-strategy=$strategy --directory=$dest_dir"

# iterate over the topics list and concatenate the topic to the java command
for topic in "${topics[@]}"
do
  java_cmd+=" --topic=$topic"
done

# execute the java command
eval $java_cmd

# clean up the destination directory
rm -r $dest_dir
