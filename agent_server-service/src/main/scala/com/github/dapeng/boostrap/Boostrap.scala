package com.github.dapeng.boostrap

import java.util
import java.util.UUID
import java.util.concurrent._

import com.corundumstudio.socketio._
import com.github.dapeng.datasource.ConfigServerSql
import com.github.dapeng.entity.TServiceBuildRecord
import com.github.dapeng.socket.entity._
import com.github.dapeng.socket.enums.EventType
import com.github.dapeng.socket.server.CmdExecutor
import com.github.dapeng.socket.util.IPUtils
import com.github.dapeng.socket.{AgentEvent, HostAgent}
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext

import scala.collection.JavaConverters._

object Boostrap {

  private val LOGGER = LoggerFactory.getLogger(classOf[CmdExecutor])
  private val timer = Executors.newSingleThreadScheduledExecutor

  private var services = new util.ArrayList[DeployRequest](32)

  private var timed = false

  private val gson = new Gson

  private val nodesMap = new ConcurrentHashMap[String, HostAgent]
  private val webClientMap = new ConcurrentHashMap[String, HostAgent]

  private val queue = new LinkedBlockingQueue[Any]

  private val buildCache = new ConcurrentHashMap[String, (Int, ServiceBuildResponse)]

  def main(args: Array[String]): Unit = {
    val buildEnable = System.getenv("build_enable")
    if (null != buildEnable && buildEnable.toBoolean) {
      val ac = new ClassPathXmlApplicationContext("classpath:services.xml");
      ac.start()
    }

    val (host, port) = if (args != null && args.length >= 2) {
      (args(0), Integer.valueOf(args(1)))
    } else {
      val host = System.getenv("socket_server_host")
      val serverPort = System.getenv("socket_server_port")

      (
        if (host == null || host.isEmpty) IPUtils.localIp else host,
        if (serverPort == null || serverPort.isEmpty) Integer.valueOf(6886) else Integer.valueOf(serverPort)
      )
    }

    init(host, port)
  }

  def init(hostName: String, port: Int) = {
    val config = new Configuration
    config.setPort(port)
    config.setHostname(hostName)
    config.setAllowCustomRequests(true)
    val subconfig = new SocketConfig()
    subconfig.setReuseAddress(true)
    config.setSocketConfig(subconfig)

    val server = new SocketIOServer(config)

    server.addConnectListener((socketIOClient: SocketIOClient) => LOGGER.info(String.format(socketIOClient.getRemoteAddress + " --> join room %s", socketIOClient.getSessionId)))

    server.addDisconnectListener((socketIOClient: SocketIOClient) => {
      handleDisconnectEvent(socketIOClient, server)
    })
    server.addEventListener(EventType.NODE_REG.name, classOf[String], (client: SocketIOClient, data: String, _) => {
      handleNodeRegEvent(client, data)
    })
    server.addEventListener(EventType.WEB_REG.name, classOf[String], (client: SocketIOClient, data: String, _) => {
      handleWebRegEvent(client, data)
    })

    /*--------------发送给Agent的事件 start----------------------------------*/
    server.addEventListener(EventType.WEB_EVENT.name, classOf[String], (socketIOClient: SocketIOClient, agentEvent: String, _) => {
      handleWebEvent(socketIOClient, server, agentEvent)
    })
    server.addEventListener(EventType.GET_SERVER_INFO.name, classOf[String], (client: SocketIOClient, data: String, _) => {
      handleGetServerInfoEvent(client, server, data)
    })
    server.addEventListener(EventType.GET_YAML_FILE.name, classOf[String], (_, data: String, _) => {
      handleGetYmlFileEvent(server, data)
    })
    server.addEventListener(EventType.BUILD.name, classOf[String], (client: SocketIOClient, data: String, _) => {
      LOGGER.info(" start to build server info...data: " + data)
      handleBuildEvent(client, server, data)
    })
    server.addEventListener(EventType.DEPLOY.name, classOf[String], (_, data: String, _) => {
      handleDeployEvent(server, data)
    })
    server.addEventListener(EventType.STOP.name, classOf[String], (_, data: String, _) => {
      handleStopEvent(server, data)
    })
    server.addEventListener(EventType.RESTART.name, classOf[String], (_, data: String, _) => {
      handleRestartEvent(server, data)
    })
    server.addEventListener(EventType.SYNC_NETWORK.name, classOf[String], (_, data: String, _) => {
      handleSyncNetworkEvent(server, data)
    })
    server.addEventListener(EventType.RM_CONTAINER.name, classOf[String], (_, data: String, _) => {
      handleRmContainerEvent(server, data)
    })
    server.addEventListener(EventType.REMOTE_DEPLOY_RESP.name, classOf[String], (client: SocketIOClient, data: String, _) => {
      handleRemoteDeployRespEvent(client, server, data)
    })
    /*--------------处理web命令行----------------------------------*/
    server.addEventListener(EventType.CMD_EVENT.name, classOf[String], (_, data: String, _) => {
      handleCmdEvent(server, data)
    })
    server.addEventListener(EventType.CMD_EXITED.name, classOf[String], (_, data: String, _) => {
      server.getClient(UUID.fromString(data)).sendEvent(EventType.CMD_EXITED.name, data)
    })
    /*--------------发送给Agent的事件 end----------------------------------*/


    /*-----------发送给web 的事件 start------------------*/
    server.addEventListener(EventType.CMD_RESP.name, classOf[String], (_, data: String, _) => {
      val reply: CmdOutputVo = gson.fromJson(data, classOf[CmdOutputVo])
      server.getClient(UUID.fromString(reply.getSourceClient)).sendEvent(EventType.CMD_RESP.name, reply.getOutput)
    })
    server.addEventListener(EventType.NODE_EVENT.name, classOf[String], (_, agentEvent: String, _) => {
      server.getRoomOperations("web").sendEvent(EventType.NODE_EVENT.name, agentEvent)
    })
    server.addEventListener(EventType.ERROR_EVENT.name, classOf[String], (_, agentEvent: String, _) => {
      server.getRoomOperations("web").sendEvent(EventType.ERROR_EVENT.name, agentEvent)
    })
    server.addEventListener(EventType.GET_REGED_AGENTS.name, classOf[String], (client: SocketIOClient, data: String, ackSender: AckRequest) => {
      server.getRoomOperations("web").sendEvent(EventType.GET_REGED_AGENTS_RESP.name, gson.toJson(nodesMap))
    })
    server.addEventListener(EventType.BUILD_RESP.name, classOf[String], (client: SocketIOClient, data: String, _) => {
      handleBuildResponseEvent(client, server, data)
    })

    server.addEventListener(EventType.GET_BUILD_PROGRESSIVE.name, classOf[String], (client: SocketIOClient, data: String, _) => {
      handleGetBuildProgressiveEvent(client, server, data)
    })
    server.addEventListener(EventType.SYNC_NETWORK_RESP.name, classOf[String], (client: SocketIOClient, data: String, ackRequest: AckRequest) => {
      LOGGER.info(" server received syncNetworkResp cmd" + data)
      server.getRoomOperations("web").sendEvent(EventType.SYNC_NETWORK_RESP.name, data)
    })
    server.addEventListener(EventType.DEPLOY_RESP.name, classOf[String], (client: SocketIOClient, data: String, ackRequest: AckRequest) => {
      LOGGER.info(" server received deployResp cmd" + data)
      server.getRoomOperations("web").sendEvent(EventType.DEPLOY_RESP.name, data)
    })
    server.addEventListener(EventType.STOP_RESP.name, classOf[String], (client: SocketIOClient, data: String, ackRequest: AckRequest) => {
      LOGGER.info(" server received stopResp cmd" + data)
      server.getRoomOperations("web").sendEvent(EventType.STOP_RESP.name, data)
    })
    server.addEventListener(EventType.RM_CONTAINER_RESP.name, classOf[String], (client: SocketIOClient, data: String, ackRequest: AckRequest) => {
      LOGGER.info(" server received rmContainerResp cmd" + data)
      server.getRoomOperations("web").sendEvent(EventType.RM_CONTAINER_RESP.name, data)
    })
    server.addEventListener(EventType.RESTART_RESP.name, classOf[String], (client: SocketIOClient, data: String, ackRequest: AckRequest) => {
      LOGGER.info(" server received restartResp cmd" + data)
      server.getRoomOperations("web").sendEvent(EventType.RESTART_RESP.name, data)
    })
    server.addEventListener(EventType.GET_YAML_FILE_RESP.name, classOf[String], (client: SocketIOClient, data: String, ackRequest: AckRequest) => {
      LOGGER.debug(" server received getYamlFileResp cmd" + data)
      server.getRoomOperations("web").sendEvent(EventType.GET_YAML_FILE_RESP.name, data)
    })
    server.addEventListener(EventType.GET_SERVER_INFO_RESP.name, classOf[String], (_, data: String, _) => {
      handleGetServerInfoResponseEvent(server, data)
    })
    /*-----------发送给web 的事件 End------------------*/

    server.start()
    LOGGER.info("websocket server started at " + port)

    val ex = new CmdExecutor(queue, server)
    LOGGER.info("CmdExecutor Thread started")

    new Thread(ex).start()
    try
      Thread.sleep(Integer.MAX_VALUE)
    catch {
      case e: Exception =>
        LOGGER.info(" Failed to sleep.." + e.getMessage)
    }

    server.stop()

  }

  private def handleCmdEvent(server: SocketIOServer, data: String): Unit = {
    val request: CmdRequest = gson.fromJson(data, classOf[CmdRequest])
    nodesMap.values.forEach((agent: HostAgent) => {
      if (request.getIp == agent.getIp) {
        val targetAgent = server.getClient(UUID.fromString(agent.getSessionId))
        if (targetAgent != null) targetAgent.sendEvent(EventType.CMD_EVENT.name, data)
      }
    })
  }

  private def handleRestartEvent(server: SocketIOServer, data: String) = {
    val request = gson.fromJson(data, classOf[DeployRequest])
    LOGGER.info(" server received stop cmd" + data)
    nodesMap.values.forEach((agent: HostAgent) => {
      if (request.getIp == agent.getIp) {
        val targetAgent = server.getClient(UUID.fromString(agent.getSessionId))
        if (targetAgent != null) targetAgent.sendEvent(EventType.RESTART.name, data)
      }
    })
  }

  private def handleStopEvent(server: SocketIOServer, data: String) = {
    val request = gson.fromJson(data, classOf[DeployRequest])
    LOGGER.info(" server received stop cmd" + data)
    nodesMap.values.forEach((agent: HostAgent) => {
      if (request.getIp == agent.getIp) {
        val targetAgent = server.getClient(UUID.fromString(agent.getSessionId))
        if (targetAgent != null) targetAgent.sendEvent(EventType.STOP.name, data)
      }
    })
  }

  private def handleRmContainerEvent(server: SocketIOServer, data: String) = {
    val request = gson.fromJson(data, classOf[DeployRequest])
    LOGGER.info(" server received rmContainer cmd" + data)
    nodesMap.values.forEach((agent: HostAgent) => {
      if (request.getIp == agent.getIp) {
        val targetAgent = server.getClient(UUID.fromString(agent.getSessionId))
        if (targetAgent != null) targetAgent.sendEvent(EventType.RM_CONTAINER.name, data)
      }
    })
  }

  private def handleDeployEvent(server: SocketIOServer, data: String) = {
    val vo = gson.fromJson(data, classOf[DeployVo])
    LOGGER.info(" server received deploy cmd" + data)
    nodesMap.values.forEach((agent: HostAgent) => {
      if (vo.getIp == agent.getIp) {
        val targetAgent = server.getClient(UUID.fromString(agent.getSessionId))
        if (targetAgent != null) targetAgent.sendEvent(EventType.DEPLOY.name, data)
      }
    })
  }

  private def handleSyncNetworkEvent(server: SocketIOServer, data: String) = {
    val vo = gson.fromJson(data, classOf[SyncNetworkVo])
    LOGGER.info(" server received syncNetwork cmd" + data)
    nodesMap.values.forEach((agent: HostAgent) => {
      vo.getHosts.forEach((host: String) => {
        if (host == agent.getIp) {
          val targetAgent = server.getClient(UUID.fromString(agent.getSessionId))
          if (targetAgent != null) targetAgent.sendEvent(EventType.SYNC_NETWORK.name, data)
        }
      })
    })
  }

  private def handleRemoteDeployRespEvent(client: SocketIOClient, server: SocketIOServer, data: String) = {
    LOGGER.info(" server received remoteDeployResp cmd" + data)
    if (data.contains("[SOURCE_HOST]")) {
      val res = data.split(":::")
      val log = res(0)
      val sourceIp = res(2)
      val responseTuple = buildCache.get(sourceIp)
      val response = responseTuple._2
      response.getContent.append(log + "\r\n")
    }
    if (data.contains("[REMOTE_DEPLOY_END]")) {
      val res = data.split(":")
      val deployStatus = res(1).toInt
      val buildId = res(2).toLong
      val sourceIp = res(3)

      // 根据来源ip查找构建缓存,将缓存的任务状态修改，删除缓存
      val responseTuple = buildCache.get(sourceIp)
      val response = responseTuple._2
      response.getContent.append(data + "\r\n")
      ConfigServerSql.updateBuildServiceRecord(buildId, if (deployStatus == 0) 2 else 3, response.getContent.toString)
      buildCache.remove(sourceIp)
    }

  }

  private def handleBuildResponseEvent(client: SocketIOClient, server: SocketIOServer, data: String) = {
    LOGGER.info(" server received buildResp cmd" + data)
    val agent = nodesMap.get(client.getSessionId.toString)
    val responseTuple = buildCache.get(agent.getIp)
    val response = responseTuple._2
    response.getContent.append(data + "\r\n")

    server.getRoomOperations("web").sendEvent(EventType.BUILD_RESP.name, data)

    // 远程部署事件发布start
    if (data.contains("[REMOTE_DEPLOY]")) {
      LOGGER.info(s"[REMOTE_DEPLOY INFO]=> $data")
      val info = data.split(":::")
      val buildId = info(1).toInt
      val sourceIp = info(2)
      val deployHost = info(3)
      val serviceName = info(4)
      val agents: List[HostAgent] = nodesMap.values().asScala.filter(a => deployHost == a.getIp).toList
      if (agents.isEmpty) {
        val responseTuple = buildCache.get(sourceIp)
        val response = responseTuple._2
        response.getContent.append(s"\\033[31m [$serviceName]remote deploy fail,deployHost[$deployHost] not activity\\033[0m")
        ConfigServerSql.updateBuildServiceRecord(buildId, 3, response.getContent.toString)
        buildCache.remove(sourceIp)
      } else {
        agents.foreach(x => {
          val targetAgent = server.getClient(UUID.fromString(x.getSessionId))
          if (targetAgent != null) {
            targetAgent.sendEvent(EventType.REMOTE_DEPLOY.name, data)
          } else {
            val responseTuple = buildCache.get(sourceIp)
            val response = responseTuple._2
            response.getContent.append(s"\\033[31m [$serviceName]remote deploy fail,deployHost[$deployHost] not activity\\033[0m")
            ConfigServerSql.updateBuildServiceRecord(buildId, 3, response.getContent.toString)
            buildCache.remove(sourceIp)
          }
        })
      }
    }
    //远程部署事件end

    //TODO: if build done , update TServiceBuildRecord
    if (data.contains("BUILD_END")) { //fixme 1. updateRecord 消除魔法数字

      val buildStatus = data.split(":")(1).toInt
      LOGGER.info(s" service has built done.. buildId: ${response.getId}, status: ${buildStatus}, responseSize: ${response.getContent.toString.length}, counter: ${responseTuple._1}")
      if (responseTuple._1 + 1 == response.getBuildServiceSize) {

        ConfigServerSql.updateBuildServiceRecord(response.getId, if (buildStatus == 0) 2 else 3, response.getContent.toString)
        //2. clearBuildCache
        buildCache.remove(agent.getIp)
      } else {

        buildCache.put(agent.getIp, (responseTuple._1 + 1, response))
      }

    }
    server.getRoomOperations("web").sendEvent(EventType.BUILD_RESP.name, data)
  }

  private def handleGetBuildProgressiveEvent(client: SocketIOClient, server: SocketIOServer, data: String): Unit = {
    LOGGER.info(" server received getBuildProgressive cmd" + data)
    /**
      * 日志的返回逻辑
      * 1.传递记录id，如果状态是已完成的则直接查询数据库将其全量返回
      * 1.1如果传递的id所在的记录状态为初始化或者构建中则返回缓存中的服务构建日志
      * 2.时间参数除去id之外还需要传递当前控制台的字符数，初始传递0券量获取
      */
    val vo: ProgressiveVo = gson.fromJson(data, classOf[ProgressiveVo])
    val maybeRecord: Option[TServiceBuildRecord] = ConfigServerSql.getBuildServiceRecordById(vo.getId)
    val respose = maybeRecord match {
      case Some(x) => {
        // 构建中则从内存中获取,但需要判断start字段
        val logs = if (x.status == 0) {
          "waiting for build"
        } else if (x.status == 1) {
          LOGGER.info(s" current progress buildCache. ${buildCache.asScala.keys}")
          val responseTuple = buildCache.get(x.agentHost)
          val response = responseTuple._2
          // 再次确认一下存不存在，不行旧丢弃掉
          if (response.getId == vo.getId) {
            response.getContent.substring(vo.getStart)
          } else {
            "notBuildCacheFound buildRecord"
          }
        } else {
          // 构建完成直接返回数据库全量的log
          // fixme 是否这类情况告知不要轮训
          x.buildLog.substring(vo.getStart)
        }
        val record = new TServiceBuildRecords()
        record.setAgentHost(maybeRecord.get.agentHost)
        record.setBuildLog(logs)
        record.setBuildService(maybeRecord.get.buildService)
        record.setCreatedAt(maybeRecord.get.createdAt)
        record.setCreatedBy(maybeRecord.get.createdBy)
        record.setId(maybeRecord.get.id)
        record.setStatus(maybeRecord.get.status)
        record.setTaskId(maybeRecord.get.taskId)
        record.setUpdatedAt(maybeRecord.get.updatedAt)
        record
      }

      case _ =>
        val record = new TServiceBuildRecords()
        record.setBuildLog("")
        record
    }

    server.getClient(client.getSessionId).sendEvent(EventType.GET_BUILD_PROGRESSIVE_RESP.name, gson.toJson(respose))
  }

  private def handleGetYmlFileEvent(server: SocketIOServer, data: String) = {
    LOGGER.info(" server received getYamlFile cmd" + data)
    val request = gson.fromJson(data, classOf[DeployRequest])
    nodesMap.values.forEach((agent: HostAgent) => {
      if (request.getIp == agent.getIp) {
        val targetAgent = server.getClient(UUID.fromString(agent.getSessionId))
        if (targetAgent != null) targetAgent.sendEvent(EventType.GET_YAML_FILE.name, data)
      }
    })
  }

  private def handleGetServerInfoResponseEvent(server: SocketIOServer, data: String) = {
    LOGGER.debug(" received getServerInfoResp cmd..." + data)
    val tempData = data.split(":")
    val socketId = tempData(0)
    val ip = tempData(1)
    val serviceName = tempData(2)
    val status = tempData(3).toBoolean
    val time = tempData(4).toLong
    val tag = tempData(5)
    val info = new ServerInfo
    info.setSocketId(socketId)
    info.setIp(ip)
    info.setServiceName(serviceName)
    info.setTime(time)
    info.setStatus(status)
    info.setTag(tag)
    // 单个返回
    server.getRoomOperations("web").sendEvent(EventType.GET_SERVER_INFO_RESP.name, gson.toJson(info))
  }

  private def handleGetServerInfoEvent(client: SocketIOClient, server: SocketIOServer, data: String) = {
    LOGGER.debug("server received serverInf cmd....." + data)
    val requests: util.ArrayList[DeployRequest] = gson.fromJson(data, new TypeToken[util.List[DeployRequest]]() {}.getType)
    // 如有修改应当拷贝一份,定时器需要更新查询的数据
    // fixme 将不同客户端发送的服务都问询一遍,需要去重复，拿并集
    services = requests
    // 定时发送所有的服务状态检查，但需要做状态判断，只能启动一次定时器
    if (!timed) {
      timer.scheduleAtFixedRate(() => {
        LOGGER.debug(":::timing send getServiceInfo runing")
        sendGetServiceInfo(nodesMap, server)
      }, 0, 10000, TimeUnit.MILLISECONDS)
      timed = true
    }
    else LOGGER.debug(":::warn getServiceInfo  is Timing ,skip")
    // 当再次发起调用需要即时发送检查
    sendGetServiceInfo(nodesMap, server)
  }

  private def handleWebEvent(client: SocketIOClient, server: SocketIOServer, agentEvent: String) = {
    LOGGER.info(" agentEvent: " + agentEvent)
    val agentEventObj = gson.fromJson(agentEvent, classOf[AgentEvent])
    agentEventObj.getClientSessionIds.asScala.foreach(sessionId => {
      val client = server.getClient(UUID.fromString(sessionId))
      if (client != null) client.sendEvent(EventType.WEB_EVENT.name, agentEvent)
      else LOGGER.error(" Failed to get socketClient......")
    })
  }

  private def handleWebRegEvent(client: SocketIOClient, data: String) = {
    client.joinRoom("web")
    LOGGER.info("web Reg..." + client.getSessionId)
    val s = client.getRemoteAddress.toString
    val remoteIp = s.replaceFirst("/", "").substring(0, s.lastIndexOf(":") - 1)
    val name = data.split(":")(0)
    val ip = data.split(":")(1)
    webClientMap.put(client.getSessionId.toString, new HostAgent(name, remoteIp, client.getSessionId.toString))
  }


  private def handleNodeRegEvent(client: SocketIOClient, data: String) = {
    client.joinRoom("nodes")
    LOGGER.info("nodes Reg")
    val name = data.split(":")(0)
    val ip = data.split(":")(1)
    nodesMap.put(client.getSessionId.toString, new HostAgent(name, ip, client.getSessionId.toString))
  }


  private def handleDisconnectEvent(socketIOClient: SocketIOClient, server: SocketIOServer): Unit = {
    if (nodesMap.containsKey(socketIOClient.getSessionId.toString)) {
      socketIOClient.leaveRoom("nodes")
      nodesMap.remove(socketIOClient.getSessionId.toString)
      LOGGER.info(String.format("leave room  nodes %s", socketIOClient.getSessionId))
    }
    if (webClientMap.containsKey(socketIOClient.getSessionId.toString)) {
      socketIOClient.leaveRoom("web")
      LOGGER.info(String.format("leave room web  %s", socketIOClient.getSessionId))
      webClientMap.remove(socketIOClient.getSessionId.toString)
      // web 离开通知所有agent客户端
      nodesMap.values.forEach((agent: HostAgent) => {
        val targetAgent = server.getClient(UUID.fromString(agent.getSessionId))
        if (targetAgent != null) targetAgent.sendEvent(EventType.WEB_LEAVE.name, socketIOClient.getSessionId.toString)
      })
    }
  }


  private def sendGetServiceInfo(nodesMap: util.Map[String, HostAgent], server: SocketIOServer): Unit = {
    LOGGER.debug("::: request services[" + services.size + "]" + services)
    services.forEach((request: DeployRequest) => {
      nodesMap.values.forEach((agent: HostAgent) => {
        if (request.getIp == agent.getIp) {
          val targetAgent = server.getClient(UUID.fromString(agent.getSessionId))
          if (targetAgent != null) targetAgent.sendEvent(EventType.GET_SERVER_INFO.name, gson.toJson(request))
        }
      })
    })
  }


  /**
    * 1. 判断当前有没有对应构建任务
    *      1.1. 有则过滤，返回提示
    *      1.2  否则添加构建任务  key => serverIp:buildServerName ; value => consoleOutput
    *      1.3  插入一条构建状态
    * 2. 实时更新内存输出 (根据agent的ip 判断属于哪个agent的构建内容)
    * 3. 构建完成，把构建内容入库， 以支持查看构建历史
    * 4. 清掉当前构建内存数据
    *
    * @param client
    * @param server
    * @param data
    */
  private def handleBuildEvent(client: SocketIOClient, server: SocketIOServer, data: String): Unit = {
    LOGGER.info(s" received build event, data: $data")
    LOGGER.info(s" current buildCache....${buildCache.asScala.keys}")
    val buildVo = gson.fromJson(data, classOf[BuildVo])

    if (!buildCache.isEmpty && buildCache.contains(buildVo.getAgentHost)) client.sendEvent(EventType.BUILDING.name, "服务正在构建中, 请稍等........")
    else {
      val sb = new StringBuilder(64)
      sb.append(buildVo.getAgentHost)
      val response = toServiceBuildResponse(buildVo)
      buildCache.put(sb.toString, (0, response))
      //fixme, 消除魔法数字
      ConfigServerSql.updateBuildServiceRecord(buildVo.getId, 1, "")
      val buildServerIp = buildVo.getAgentHost
      if (buildServerIp == null || buildServerIp.isEmpty) {
        server.getClient(client.getSessionId).sendEvent(EventType.ERROR_EVENT.name, "构建服务器的IP不能为空")
      } else {
        nodesMap.values.stream.filter((i: HostAgent) => i.getIp.equals(buildServerIp)).forEach((agent: HostAgent) => {
          val agentClient = server.getClient(UUID.fromString(agent.getSessionId))
          if (agentClient != null) {
            agentClient.sendEvent(EventType.BUILD.name, gson.toJson(buildVo))
            LOGGER.info(s"send event to:${agent.getIp}")
          } else {
            LOGGER.error(s"not found clientAgent:${agent.getIp}")
            server.getClient(client.getSessionId).sendEvent(EventType.ERROR_EVENT.name, "找不到对应clientAgent: " + agent.getIp)
          }
        })
      }
    }
  }

  private def toServiceBuildResponse(buildVo: BuildVo) = {
    val response = new ServiceBuildResponse
    response.setAgentHost(buildVo.getAgentHost)
    response.setId(buildVo.getId)
    response.setBuildService(buildVo.getBuildService)
    response.setContent(new java.lang.StringBuilder())
    response.setStatus(1)
    response.setTaskId(buildVo.getTaskId)
    response.setBuildServiceSize(buildVo.getBuildServices.size())
    response
  }


}
