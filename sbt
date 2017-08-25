#!/usr/bin/env bash

java -Xmx1g -Xms512M -XX:+TieredCompilation -XX:ReservedCodeCacheSize=256m -XX:+UseNUMA -XX:+UseParallelGC -XX:+CMSClassUnloadingEnabled -jar bin/sbt-launch.jar "$@"
