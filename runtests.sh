#!/bin/bash
SBT_OPTS="-Xmx4g -XX:+UseParallelGC -Xss8m" sbt clean compile coverage it/test test coverageReport