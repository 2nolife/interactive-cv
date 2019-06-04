#!/usr/local/bin/bash -e

echo
echo "Building Application"
echo "--------------------"

if grep -Fxq "build" $history
then
   echo "Seems to be already created"
   echo "Skip!"
else


arg1=${1:-primary}

# build application
cd /usr/local
sudo git clone https://github.com/2nolife/interactive-cv.git
sudo chown -R $user interactive-cv

cd interactive-cv

# change 'master' if you want to use another branch
git checkout master
git pull

# Dependencies.scala
echo "Patching Dependencies.scala"
file=project/Dependencies.scala
sed -i -e 's;"3.5.5" // match Neo4j;"3.3.2" // match Neo4j;g' $file
rm -f $file-e

# overrides.conf
echo "Patching overrides.conf"
file=src/main/resources/overrides.conf
cp $script_dir/overrides.conf $file

# charts.html.ftl
echo "Patching charts.html.ftl"
file=src/main/resources/c4/templates/charts.html.ftl
sed -i -e "s;localhost;$primary_ip;g" $file
rm -f $file-e

# neovis.ftl
echo "Patching neovis.ftl"
file=src/main/resources/c4/templates/neovis.ftl
sed -i -e "s;localhost;$primary_ip;g" $file
rm -f $file-e

echo "Building app"
sbt compile

# config.json
echo "Patching weblet/config.json"
file=weblet/config.json
sed -i -e 's;"port": "8080";"port": "8082";g' $file
rm -f $file-e

echo "Building weblet"
cd weblet
npm install
sudo npm install forever -g


echo "build" >> $history
echo "Done!"


fi


# ROLLBACK (remove 'build' from history file)
#   sudo rm -rf /usr/local/interactive-cv
