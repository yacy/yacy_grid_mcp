#!/usr/bin/env sh
cd "`dirname $0`"
cd ../data
KILLFILE="mcp-8100.kill"
PIDFILE="mcp-8100.pid"

# first method to terminate the process
if [ -f "$KILLFILE" ];
then
   rm $KILLFILE
   echo "termination requested, waiting.."
   # this can take a bit..
   sleep 5
fi

# second method to terminate the process
if [ -f "$PIDFILE" ];
then
   fuser -k $PIDFILE
fi

# check if file does not exist any more which would be a sign that this has terminated
if [ ! -f "$PIDFILE" ];
then
   echo "process terminated"
fi

