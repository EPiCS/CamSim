#!/bin/sh
function getCommStr { 
    if [ "$1" == "0" ]; then 
	    echo "broadcast"
    elif [ "$1" == "1" ]; then 
	    echo "smooth"
    elif [ "$1" == "2" ]; then
	    echo "step"
    else
	    echo "error"
    fi
}

for ScenarioFile in `find ./scenarios -name "scenario[4-6]*.xml" | grep -v hist`
do 
    export ScenarioName=`basename $ScenarioFile .xml`
    for Comm in 0 1 2
    do 
	    CommStr=`getCommStr $Comm`
	    export LOGDIR="./logs/current/$ScenarioName-$CommStr"
	    if [ ! -d $LOGDIR ]; then 
	        mkdir $LOGDIR
	    fi
	    ./exp.sh --comm $Comm $ScenarioFile
    done
done