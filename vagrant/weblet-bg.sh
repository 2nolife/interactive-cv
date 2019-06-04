#!/bin/sh -e

# Run webled in the background
#
# stop:
#   forever list
#   forever stop #process
#
# output:
#   tail -f ~/.forever/weblet.log

cd /usr/local/interactive-cv/weblet
forever start -l weblet.log --append weblet.js
