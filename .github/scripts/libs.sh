#!/bin/bash
##!/opt/homebrew/bin/bash

set -e

#
# Get a commit info key.
# arg1 (input): commit info string, like "pderop;test;main;pderop;2023-02-25T07:44:15Z;11bbc56fa63e44edffa2e47d52234a98f6030778"
# output: the commit info prefix ("pderop;test;main")
#
_get_commit_info_key() {
  commit_info="$1"
  echo $commit_info | cut -d ";" -f 1-3
}

#
# args1 (input): history file
# args2 (output): previous commits map
#
_get_previous_commits() {
  [[ $# -ne 2 ]] && echo "::warning: Invalid number of arguments for function _get_previous_commits" && return 1

  history_file=$1
  local -n prev_commits_ref="$2"

  if [ -f $history_file ]; then
    for info in $(cat $history_file); do
      project=$(echo $info | cut -f1 -d ';')
      repo=$(echo $info | cut -f2 -d ';')
      branch=$(echo $info | cut -f3 -d ';')
      commit=$(echo $info | cut -f4 -d ';')

      prev_commits_ref+=("$project;$repo;$branch;$commit")
    done
  fi
}

#
# arg1 (input): repos (like "pierre;test;main pierre;gcp_benchmarks;reactor-netty-1.0.x"")
# arg2 (output): commits_map
#
_get_current_commits() {
  [[ $# -ne 2 ]] && echo "::warning: Invalid number of arguments for function _get_current_commits" && return 1

  repos="$1"
  local -n curr_commits_ref="$2"

  for info in $repos; do
    project=$(echo $info | cut -f1 -d ';')
    repo=$(echo $info | cut -f2 -d ';')
    branch=$(echo $info | cut -f3 -d ';')
    commit=$(git ls-remote "https://github.com/$project/$repo/" | grep -w "refs/heads/$branch\$" | cut -f1)
    curr_commits_ref+=("$project;$repo;$branch;$commit")
  done
}

#
# arg1 (input): list of repos (like "pderop;test;main pderop;gcp-benchmarks;reactor-netty-1.0.x")
# arg2 (input): history file
# arg3 (input): output file where we will write results
check_for_new_commits() {
  [[ $# -ne 3 ]] && echo "::warning: Invalid number of arguments for function check_for_new_commits" && return 1
  repos="$1"
  history_file=$2
  output=$3

  declare -a curr_commits
  declare -a prev_commits

  # get current commits from dependencies
  _get_current_commits "$repos" curr_commits

  echo "COMMITS=${curr_commits[@]}" >>$output

  # if we don't have any history of previous commits, consider we have some changes
  if [ ! -f $history_file ]; then
    echo "::notice:: no previous commits found in history file, will trigger a new benchmark"
    echo "CHANGED=true" >>$output
  else
    # get the diff between the previous commits and the current commits, and if there is one diff,
    # then output diff urls for each new commits
    _get_previous_commits $history_file prev_commits

    new_commits=$(comm -13 <(echo "${prev_commits[*]}" | tr ' ' '\n' | sort -u) <(echo "${curr_commits[*]}" | tr ' ' '\n' | sort -u))

    if [ "$new_commits" != "" ]; then
      declare -a new_commit_diff_urls
      echo "CHANGED=true" >>$output

      for new_commit_info in $new_commits; do
        key=$(_get_commit_info_key "$new_commit_info")
        cat $history_file
        old_commit_info=$(grep $key $history_file)

        if [ -n "$old_commit_info" ]; then
          project=$(echo $old_commit_info | cut -f1 -d ';')
          repo=$(echo $old_commit_info | cut -f2 -d ';')
          old_branch=$(echo $old_commit_info | cut -f3 -d ';')
          commit=$(echo $old_commit_info | cut -f4 -d ';')

          new_commit=$(echo $new_commit_info | cut -f4 -d ';')

          new_commit_diff_url="https://github.com/$project/$repo/compare/$commit..$new_commit"
          echo "::notice:: New commit since last benchmark: $new_commit_diff_url"
          new_commit_diff_urls+=("$new_commit_diff_url")
        else
          echo "::warning:: new commit not found from history: $key"
        fi
      done

      echo "COMMITS_DIFF=${new_commit_diff_urls[*]}" >>$output
    else
      echo "CHANGED=false" >>$output
      echo "::notice:: No changes detected since previous benchmark."
    fi
  fi
}

#
# arg1 (input): history file
# arg2 (input): string for list of commits info (space separated), like "pderop;test;main;pderop;15048a0f321bd04a2dccce88b0bad94a1280a903 pderop;gcp-benchmarks;reactor-netty-1.0.x;aa84ae4c87193fc3318bfc5c0eecd9f45590deed")
#
update_benchmark_history() {
  [[ $# -ne 2 ]] && echo "::warning: Invalid number of arguments for function update_benchmark_history" && return 1
  history_file=$1
  commits_info="$2"
  >$history_file
  for commit_info in $commits_info; do
    echo $commit_info >>$history_file
  done
}

#
# arg1 (input): data.js file
# arg2 (input): benchmark name in json file
# arg3 (input): job url
# arg4 (input): job id
# arg5 (input): list of commit diff urls (space separated)
#
transform_benchmark_json_data() {
  [[ $# -ne 5 ]] && echo "::warning: Invalid number of arguments for function transform_benchmark_json_data" && return 1

  json_file=$1
  name=$2
  job_url=$3
  job_id=$4
  commit_diff_urls="$5"

  message="No new commit since last benchmark."
  if [ "$commit_diff_urls" != "" ]; then
    message="New commits since last benchmark:\n"
    for url in $commit_diff_urls; do
      message="$message\n\t$url"
    done
  fi

  cat $json_file | sed 's/window.BENCHMARK_DATA = //g' | jq --argjson job_id '"'$job_id'"' --argjson message '"'"$message"'"' --argjson job_url '"'"$job_url"'"' \
    '.entries."'"$name"'"[-1].commit.id=$job_id | .entries."'"$name"'"[-1].commit.message=$message | .entries."'"$name"'"[-1].commit.url=$job_url' \
    >${json_file}.tmp
  echo -n "window.BENCHMARK_DATA = " >$json_file
  cat ${json_file}.tmp >>$json_file
  rm -f ${json_file}.tmp
}

build-results-readme() {
  [[ $# -ne 1 ]] && echo "::warning: Invalid number of arguments for function readme" && return 1
  [[ ! -d $1 ]] && echo "directory $1 does not exist" && return 1
  benchdir=$1

  # detect available simulations
  simulations=$(find $benchdir/* -mindepth 1 -maxdepth 1 -exec basename {} \; | sort | uniq)

  echo "# Servers shootout board"
  echo "## Results"

  # get list of apps
  apps=$(ls $benchdir)

  # display first table line
  echo
  echo -n "| Application "
  for sim in $simulations; do
    echo -n " | $sim"
  done
  echo " |"

  # display second table line
  echo -n "| --- "
  for sim in $(echo $simulations | tr ";" "\n"); do
    echo -n " | :---:"
  done
  echo " |"

  # display results
  for app in $apps; do
    echo -n "| $app"
    for sim in $(echo $simulations | tr ";" "\n"); do
      if [ -f $benchdir/$app/$sim/index.html ]; then
        if [ "$sim" != "Trends" ]; then
          # extract result from json data
          result=$(cat $benchdir/$app/Trends/data.js | \
            sed 's/window.BENCHMARK_DATA = //g' | \
            jq --arg app "$app" --arg sim "$sim" '.entries."Trends for \($app)" | .[-1].benches[] | select(.name == "\($app)-\($sim)").value')
          echo -n " | [**$result**]($benchdir/$app/$sim/index.html)"
        else
          echo -n " | [**result**]($benchdir/$app/$sim/index.html)"
        fi
      else
        echo -n " | n/a"
      fi
    done
    echo " |"
  done

  echo
  set +x
  cat <<EOF
## Scenario

Each benchmark case starts with no traffic and does the following:

- increase the concurrency by 100 users (1 users = 1 connection) in 1 seconds
- hold that concurrency level for 1 seconds
- go to first step, unless the maximum concurrency of 1000 users is reached

## Benchmark cases
- PlainText: frontend responds a "text/plain" response body
- Echo: gatling sends a "text/plain" request body and the frontend echoes that in the response
- JsonGet: frontend responds with a JSON payload
- JsonPost: gatling sends a JSON payload, frontend deserializes it and replies with a JSON payload
- HtmlGet: front renders an HTML view with a templating engine
- Remote: frontend forwards gatling request to the backend

EOF
}
