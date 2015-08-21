#!/bin/bash

kubectl delete service cassandra "$@"

kubectl delete rc cassandra-rc "$@"

