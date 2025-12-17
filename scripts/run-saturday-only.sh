#!/bin/bash

# Script to run IAPD process only on Saturdays
# This script checks the current day of the week and only executes if it's Saturday

# Get the current day of the week (1=Monday, 6=Saturday, 7=Sunday)
DAY_OF_WEEK=$(date +%u)

# Check if today is Saturday (6)
if [ "$DAY_OF_WEEK" -eq 6 ]; then
    echo "Today is Saturday. Running IAPD process..."
    cd /Users/thomasstockdale/Work/IAPD && /usr/bin/java -jar iapd.jar --verbose --url-rate 4 --download-rate 10 --force-restart  
else
    echo "Today is not Saturday (day $DAY_OF_WEEK). Skipping IAPD process."
    exit 0
fi
