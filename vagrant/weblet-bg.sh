#!/bin/sh -e

# Run weblet in the background
#
# stop:
#   forever stopall
#
# output:
#   tail -f ~/.forever/weblet.log

cd /usr/local/interactive-cv/weblet
forever start -l weblet.log --append weblet.js
