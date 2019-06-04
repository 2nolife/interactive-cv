## Install Interactive CV on Vagrant FreeBSD ##

```
Command line interface
  $     CLI of a Vagrant box  (FreeBSD)
  mac>  CLI of a host machine (Mac)
```

### Installation steps ###

Install Vagrant and VirtualBox (or any other supported VM provider)

1. copy content of this directory into an empty directory <directory_name>
2. start then stop vagrant to build a new virtual machine (VM)
```
     mac> cd <directory_name>
     mac> vagrant up
     mac> vagrant halt
```
3. open the new VM settings in VirtualBox and copy/paste your NAT MAC address into Vagrant file
```
     config.vm.base_mac = "08002726C4F8"
```
4. start vagrant then ssh into it and run the script
```
     mac> vagrant up
     mac> vagrant ssh
     $ chmod u+x /vagrant/*.sh
     $ /vagrant/install.sh
```

### What it does ###

The script will install everything needed to run the application in VM, apply configuration
and deploy, making the application immediately available. If you shut down vagrant, the
application will start again on boot.

### Try it out ###

Try the following links in Firefox or Chrome browser
  * http://192.168.50.10:8082

Enjoy!

## Troubleshooting ##

The script should produce no errors and there should be no errors in any of the logs

1. Script saves each completed step into ~/icv-install
   On completion it should read in the following order
```
     $ cat ~/icv-install

     java
     build
     neo4j
     deploy
     finalise
```

   If some step is missing then you need to rollback the missing step and all of the steps after it
   See ROLLBACK section at the end of each shell file run by the script and do what it says.
   Then correct the error and re-run the script to continue from the failed step.

2. If you interrupt the running script then it should continue from the point where it stopped.
   But you need to rollback the current step and only then re-run the script (see point 1.)

3. Script produces immense output and stops on a failed step. It should be easy to correct errors
   on abnormal script termination. But if the application is not running in the very end, it may be
   hard to find what failed, then step by step may help (see point 5.)

   To run a single step
```
     $ /vagrant/install.sh <step_name>
```
   E.g. to run just `build` step use `install.sh build` but remember to rollback first.

4. To run the script step by step, use the order from point 1. and check for errors
```
     $ /vagrant/install.sh java
     ...
     $ /vagrant/install.sh finalise
```

5. Vagrant times out while waiting for the machine to boot. First time start up may take a while
   as the VM needs to install OS and apply patches, monitor the progress in VirtualBox.
