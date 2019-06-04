#!/bin/sh -e

export user=vagrant
export script_dir=/vagrant
export history=~/icv-install
export primary_ip=192.168.50.10


echo "Interactive CV"
echo "=============="
echo 

echo "Installing required packages"
sudo pkg install -y nano bash git sbt node npm


# steps to run

run_step=${1:-all}

if [ $run_step = 'all' ]
then
  $script_dir/java8.sh
  $script_dir/build.sh
  $script_dir/neo4j332.sh
  $script_dir/deploy.sh
  $script_dir/finalise.sh
fi


# troubleshooting steps

if [ $run_step = 'java' ]
then
  $script_dir/java8.sh $2
fi

if [ $run_step = 'build' ]
then
  $script_dir/build.sh $2
fi

if [ $run_step = 'neo4j' ]
then
  $script_dir/neo4j332.sh $2
fi

if [ $run_step = 'deploy' ]
then
  $script_dir/deploy.sh $2
fi

if [ $run_step = 'finalise' ]
then
  $script_dir/finalise.sh $2
fi


echo
echo "ALL DONE"
