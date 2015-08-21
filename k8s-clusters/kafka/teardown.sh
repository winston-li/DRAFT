#!/bin/bash

kubectl delete service kafka "$@"

kubectl delete rc kafka-rc "$@"

