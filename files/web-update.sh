#!/bin/bash -e

# Example:
#   set ICV_HOME=/usr/local/interactive-cv
#   sudo -E ./web-update.sh

echo "Updating weblet with fresh files"

if [ -z "$ICV_HOME" ]
then
  echo "ICV_HOME not set (project home)"
  exit
fi

echo "Project home: $ICV_HOME"

echo "Updating files"
target_dir=$ICV_HOME/weblet/public
source_dir=$ICV_HOME/target/output/c3
mkdir -p $target_dir
rm -r $target_dir
mv $source_dir $target_dir

echo "Done"
