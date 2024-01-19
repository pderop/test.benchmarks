#!/usr/bin/env bash

SERVER_HOST=$1
SERVER_PORT=$2
SIMULATIONS=$3
DURATION=$4
APP_NAME=$5
PROTOCOL=$6
STEPS=$7
INCREMENT=$8
BENCHDIR=${9:-/tmp/bench}

echo "Running simulations: ${SIMULATIONS} for application $APP_NAME, steps=$STEPS, increment=$INCREMENT"
java -version
df -h .
mkdir -p $BENCHDIR/results

# Create a tmp file that will be used to create the json file)
tmp=$(mktemp)

# Create a tmp file used to collect gatling output logs
gatling_output=$(mktemp)

# Declare a variable that will point to the gatling forked process
gatling_pid=

# Remove tmp files and kill gatling child process when the script exits
function cleanup() {
  rm -f "$tmp";
  rm -f "$gatling_output";
  if [ -n "$gatling_pid" ] && [ "$gatling_pid" != "0" ] && kill -0 $gatling_pid 2> /dev/null; then
    echo "sending SIGTERM to Gatling process $gatling_pid"
    kill -SIGTERM $gatling_pid 2>/dev/null;
    sleep 1
    if kill -0 $gatling_pid 2> /dev/null; then
      echo "sending SIGINT to Gatling process $gatling_pid"
      kill -SIGINT $gatling_pid 2>/dev/null;
    fi
    sleep 1
    if kill -0 $gatling_pid 2> /dev/null; then
        echo "sending SIGKILL to Gatling process $gatling_pid"
        kill -9 $gatling_pid 2>/dev/null;
    fi
  fi
}
trap cleanup EXIT

# Server is expected to have started the app, but check it's available. If protocol is H2, then use https scheme, else use http scheme.
options=(
  --silent
  --fail
  --location
  --retry 30
  --retry-connrefused
  --retry-delay 6
  --retry-max-time 300
)
scheme=http
if [ "$PROTOCOL" == "H1S" ] || [ "$PROTOCOL" == "H2" ]; then
    options+=(-k)
    scheme=https
fi

curl $scheme://$SERVER_HOST:$SERVER_PORT/text "${options[@]}"
echo "Server responded to ping message, starting simulations."

# initialize our custom json output (we will fill it for each simulation results, see below)
echo "[]" > $BENCHDIR/results/gh-benchmark.json

# Now, start gatling for this app
for simulation in $(echo ${SIMULATIONS} | tr ";" "\n"); do
  JOPTS="-DHOST=${SERVER_HOST} -DPORT=${SERVER_PORT} -DDURATION=${DURATION} -DPROTOCOL=${PROTOCOL} -DSTEPS=${STEPS} -DINCREMENT=${INCREMENT}"
  name="$APP_NAME-$simulation"

  # fork gatling, because sometimes, it may hangs after displaying a log like
  # "Coordinated shutdown phase [actor-system-terminate] timed out after 10000 milliseconds"
  # So, fork gatling, wait for "mean requests/sec", and kill gatling
  > $gatling_output
  # Start the gatling in the background and capture its PID
  java ${JOPTS} -jar gatling-*-all.jar "$name" $simulation 2>&1 > $gatling_output &
  gatling_pid=$!

  # Wait for gatling output result
  tail -f $gatling_output | while read LINE ; do
    echo $LINE

    # Stop the tail command if we detect end of the test
    if echo "$LINE" | grep -q "Reports generated" ; then
      pkill -P $$ tail
      break
    fi

    # Abort on any KO responses
#    if echo "$LINE" | grep -q "KO="; then
#      ko_value=$(echo $LINE | grep -o 'KO=[^[:space:]]*' | awk -F= '{print $2}')
#      if [ -n $ko_value ] && [ "$ko_value" != "0" ] && [ "$ko_value" != "-" ]; then
#        pkill -P $$ tail
#        break
#        echo "Warning: some requests failed: $LINE"
#      fi
#    fi
  done

  # Abort in case we have some KO
  if grep -qE 'KO=[1-9][0-9]*' $gatling_output; then
    echo "Some requests failed."
    exit 1
  fi

  # Look for the final result
  mean_reqs_sec=$(grep "mean requests/sec" $gatling_output)
  if [ "$mean_reqs_sec" == "" ]; then
    echo "Result not found from gatling output"
    exit 1
  fi

  # Fail if some KO responses are detected
  unset mean
  ko_value=$(echo $mean_reqs_sec | grep -o 'KO=[^[:space:]]*' | awk -F= '{print $2}')
  if [ "$ko_value" != "-" ]; then
    echo "Some requests failed: $mean_reqs_sec"
    exit 1
  else
    # Extract the mean requests per seconds
    mean=$(echo $mean_reqs_sec | grep "mean requests/sec" | awk '{print $4}')
  fi

  unit="mean requests/sec"
  value="$mean"

  echo "Result mean reqs/sec=$mean"

  # Add to the github-action json data the current result for the current simulation
  cat $BENCHDIR/results/gh-benchmark.json | jq --argjson name '"'"$name"'"' --argjson unit '"'"$unit"'"' --argjson value "$value" '. += [{
                      "name": $name,
                      "unit": $unit,
                      "value": $value
                 }]' > $tmp
  cp $tmp $BENCHDIR/results/gh-benchmark.json
  simulation_lower=$(echo "$simulation" | tr '[:upper:]' '[:lower:]')
  rm -f test-reports/$simulation_lower*/simulation.log
  mkdir -p $BENCHDIR/results/bench/$APP_NAME/$simulation
  mv test-reports/$simulation_lower*/* $BENCHDIR/results/bench/$APP_NAME/$simulation/
  rm -rf test-reports

  # Kill gatling, in case it hangs (sometimes, it may hang if akka Coordinated shutdown times out)
  cleanup
done

cd $BENCHDIR/results/
tar zcf bench-${APP_NAME}.tgz bench gh-benchmark.json

# all done, copy results into bucket
gsutil cp $BENCHDIR/results/bench-${APP_NAME}.tgz gs://${BUCKET}/results/

echo "Gatling simulation done."
exit 0

