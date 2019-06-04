#!/bin/sh -e

# Generate files and update UI and Neo4j

NEO4J_DB=/var/db/neo4j/databases
ICV_HOME=/usr/local/interactive-cv

echo "Building CVs"
cd $ICV_HOME
sbt "run src/test/resources/samples"


echo "Updating Neo4j database with fresh graphs"
sudo service neo4j stop

# create backup
target_dir=$NEO4J_DB/graph.db
backup_dir=$NEO4J_DB/graph.db.backup-`date '+%Y%m%d-%H%M%S'`
sudo mv $target_dir $backup_dir

# update data
source_dir=$ICV_HOME/target/output/graph.db
sudo mv $source_dir $target_dir

sudo service neo4j start


echo "Updating weblet with fresh files"

target_dir=$ICV_HOME/weblet/public
source_dir=$ICV_HOME/target/output/c3
mkdir -p $target_dir
rm -r $target_dir
mv $source_dir $target_dir
