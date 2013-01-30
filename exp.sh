#!/bin/sh
STARTTIME=$SECONDS
TIMESTEPS="1000"
SEEDS=30

# Only set if not already set
[ -z $LOGDIR ] && LOGDIR="./logs/current"

[ -z $ScenarioName ] && echo "ScenarioName not set by parent" && exit

PREFIX="$LOGDIR/$ScenarioName-$TIMESTEPS"
EXTRA_ARGS=$*

if [ ! -d "$LOGDIR" ]; then
    echo "Error: Logdir $LOGDIR does not exist"
    exit
fi

# Iterate over numbers, also use as seeds
for SEED in `seq 1 $SEEDS`
do
    OUTPUT_FILE="$PREFIX-$SEED.csv"

    # If file exists and is greater than 0 size
    if [ -s $OUTPUT_FILE ]; then
	echo "File exists: $OUTPUT_FILE. Skipping..."
	continue
    fi

    SUMMARY_FILE="$PREFIX-$SEED-summary.txt"
    STDOUT_FILE="/dev/null" #"$PREFIX-$SEED-stdout.txt"
    ARGS="--no-gui -t $TIMESTEPS -o $OUTPUT_FILE --summaryfile $SUMMARY_FILE --seed $SEED"
    echo "Running with args: $ARGS $EXTRA_ARGS"

    ./run.sh $ARGS" "$EXTRA_ARGS > $STDOUT_FILE
done

echo "$SEEDS seeds done in "$(($SECONDS-$STARTTIME))" seconds"