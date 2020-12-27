#!/bin/bash

#RUN_SERVER = $(sleep 1 | echo "1" | make run_storageserver)
NR_SERVERS=$(sed -n '/nr_servers/p' config.toml | cut -c 14-)

for i in "$NR_SERVERS"
do
    echo $i
done