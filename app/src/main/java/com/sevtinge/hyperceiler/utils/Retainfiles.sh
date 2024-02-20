########### kill #######################################
pid=$(pgrep -f "XX" | grep -v $$)
if [[ $pid == "" ]]; then
    pids=""
    pid=$(ps -A -o PID,ARGS=CMD | grep "XX" | grep -v "grep")
    for i in $pid; do
        if [[ $(echo $i | grep '[0-9]' 2>/dev/null) != "" ]]; then
            if [[ $pids == "" ]]; then
                pids=$i
            else
                pids="$pids $i"
            fi
        fi
    done
fi
if [[ $pids != "" ]]; then
    pid=$pids
fi
if [[ $pid != "" ]]; then
    #    pkill -l 15 -f "XX"
    #    if [[ $? != 0 ]]; then
    #        killall -s 15 "XX" &>/dev/null
    #        if [[ $? != 0 ]]; then
    for i in $pid; do
        kill -s 15 $i &>/dev/null
    done
    #        fi
    #    fi
else
    echo "No Find Pid!"
fi
########### kill #######################################

pid=$(ps -A -o PID,ARGS=CMD | grep "XX" | grep -v "grep")
if [[ $pid == "" ]]; then
    echo "No Find Pid!"
else
    ps -A -o PID,ARGS=CMD | grep "XX" | grep -v "grep"
fi
