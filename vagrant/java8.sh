#!/usr/local/bin/bash -e

echo
echo "Installing Java 8"
echo "-----------------"

if grep -Fxq "java" $history
then
   echo "Seems to be already installed"
   echo "Skip!"
else


sudo pkg install -y openjdk8
cd /usr/local
sudo ln -s openjdk8 java

# post install note
# ======================================================================
# This OpenJDK implementation requires fdescfs(5) mounted on /dev/fd and
# procfs(5) mounted on /proc.
# If you have not done it yet, please do the following:
# 	mount -t fdescfs fdesc /dev/fd
# 	mount -t procfs proc /proc
# To make it permanent, you need the following lines in /etc/fstab:
# 	fdesc  /dev/fd  fdescfs  rw  0  0
# 	proc   /proc    procfs   rw  0  0
# ======================================================================

echo "Mounting openjdk8 partitions"
sudo mount -t fdescfs fdesc /dev/fd
sudo mount -t procfs proc /proc

# fstab
echo "Patching /etc/fstab"
file=/etc/fstab
echo "" | sudo tee -a $file
echo "# openjdk8 partitions" | sudo tee -a $file
echo "fdesc  /dev/fd  fdescfs  rw  0  0" | sudo tee -a $file
echo "proc   /proc    procfs   rw  0  0" | sudo tee -a $file
echo "" | sudo tee -a $file
sudo rm -f $file-e


echo "java" >> $history
echo "Done!"


fi


# ROLLBACK (remove 'java' from history file)
#   sudo pkg remove -y openjdk8
#   revert /etc/fstab
