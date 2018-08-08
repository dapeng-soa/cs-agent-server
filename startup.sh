oldpid=`cat pid.txt`
echo killing Previous pid: ${oldpid}
kill ${oldpid}
echo runing app
java -jar agent_server.jar >> ./logs/agent_server.log 2>&1 &
echo "$!" > pid.txt
newpid=`cat pid.txt`
echo runing new app pid is : ${newpid}

