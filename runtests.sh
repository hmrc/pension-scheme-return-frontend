#!/bin/bash
SBT_OPTS="-Xmx4g -XX:+UseParallelGC" sbt clean compile coverage it/test test coverageReport