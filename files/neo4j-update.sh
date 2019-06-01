#!/bin/bash -e

# Example:
#   set NEO4J_HOME=/usr/local/neo4j-community-3.5.5
#   set ICV_HOME=/usr/local/interactive-cv
#   sudo -E ./neo4j-update.sh

echo "Updating Neo4j database with fresh graphs"

if [ -z "$NEO4J_HOME" ]
then
  echo "NEO4J_HOME not set (Neo4j database home)"
  exit
fi

if [ -z "$ICV_HOME" ]
then
  echo "ICV_HOME not set (project home)"
  exit
fi

wait_5s () {
  for n in 5 4 3 2 1
  do
    echo -ne "$n"\\r
    sleep 1
  done
}

echo "Neo4j   home: $NEO4J_HOME"
echo "Project home: $ICV_HOME"
wait_5s

echo "Stopping ..."
$NEO4J_HOME/bin/neo4j stop
wait_5s

echo "Creating backup"
target_dir=$NEO4J_HOME/data/databases/graph.db
backup_dir=$NEO4J_HOME/data/databases/graph.db.backup-`date '+%Y%m%d-%H%M%S'`
mv $target_dir $backup_dir

echo "Updating data"
source_dir=$ICV_HOME/target/output/graph.db
mv $source_dir $target_dir

echo "Starting ..."
$NEO4J_HOME/bin/neo4j start
wait_5s

echo "Done"
