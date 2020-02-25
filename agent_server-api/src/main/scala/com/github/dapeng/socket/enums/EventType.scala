package com.github.dapeng.socket.enums

import wangzx.scala_commons.sql.{DbEnum, DbEnumJdbcValueAccessor}

class EventType private(val id: Int, val name: String) extends DbEnum {
  override def toString(): String = "(" + id + "," + name + ")"

  override def equals(obj: Any): Boolean = {
    if (obj == null) false
    else if (obj.isInstanceOf[EventType]) obj.asInstanceOf[EventType].id == this.id
    else false
  }

  override def hashCode(): Int = this.id
}

object EventType {
  //agent_client 节点注册
  /**
    *
    */
  val NODE_REG = new EventType(1, "nodeReg")
  //agent_client 事件返回结果, 适用于不需要处理的返回
  val NODE_EVENT = new EventType(2, "nodeEvent")
  //web_client 节点注册
  val WEB_REG = new EventType(3, "webReg")
  //web_client 节点注销
  val WEB_LEAVE = new EventType(4, "webLeave")
  //web_client
  val WEB_EVENT = new EventType(5, "webEvent")
  //返回服务器时间/状态结果的事件
  val GET_SERVER_INFO_RESP = new EventType(6, "getServerInfoResp")
  //请求服务器时间/状态事件
  val GET_SERVER_INFO = new EventType(7, "getServerInfo")
  //部署事件
  val DEPLOY = new EventType(8, "deploy")
  val DEPLOY_RESP = new EventType(9, "deployResp")
  //停止服务事件
  val STOP = new EventType(10, "stop")
  val STOP_RESP = new EventType(11, "stopResp")
  val RESTART = new EventType(12, "restart")
  val RESTART_RESP = new EventType(13, "restartResp")
  //获取yamlFile事件
  val GET_YAML_FILE = new EventType(14, "getYamlFile")
  //获取yamlFile resp
  val GET_YAML_FILE_RESP = new EventType(15, "getYamlFileResp")

  val BUILD = new EventType(16, "build")

  val GET_REGED_AGENTS = new EventType(17, "getRegedAgents")

  val GET_REGED_AGENTS_RESP = new EventType(18, "getRegedAgentsResp")

  val BUILD_RESP = new EventType(19, "buildResp")

  val BUILDING = new EventType(20, "building")

  val BUILDING_LIST = new EventType(21, "buildingList")

  val GET_BUILD_PROGRESSIVE = new EventType(22, "getBuildProgressive")

  val GET_BUILD_PROGRESSIVE_RESP = new EventType(23, "getBuildProgressiveResp")

  val SYNC_NETWORK = new EventType(24, "syncNetwork")

  val SYNC_NETWORK_RESP = new EventType(25, "syncNetworkResp")

  val RM_CONTAINER = new EventType(26, "rmContainer")

  val RM_CONTAINER_RESP = new EventType(27, "rmContainerResp")

  val REMOTE_DEPLOY = new EventType(28, "remoteDeploy")

  val REMOTE_DEPLOY_RESP = new EventType(29, "remoteDeployResp")

  val CMD_EVENT = new EventType(30, "cmd")

  val CMD_RESP = new EventType(31, "cmdResult")

  val CMD_EXITED = new EventType(32, "cmdExited")

  val ERROR_EVENT = new EventType(99, "errorEvent")

  def unknown(id: Int) = new EventType(id, id + "")

  def unknown(label: String) = new EventType(999, label)

  def valueOf(id: Int): EventType = id match {
    case 1 => NODE_REG
    case 2 => NODE_EVENT
    case 3 => WEB_REG
    case 4 => WEB_LEAVE
    case 5 => WEB_EVENT
    case 6 => GET_SERVER_INFO_RESP
    case 7 => GET_SERVER_INFO
    case 8 => DEPLOY
    case 9 => DEPLOY_RESP
    case 10 => STOP
    case 11 => STOP_RESP
    case 12 => RESTART
    case 13 => RESTART_RESP
    case 14 => GET_YAML_FILE
    case 15 => GET_YAML_FILE_RESP
    case 16 => BUILD
    case 17 => GET_REGED_AGENTS
    case 18 => GET_REGED_AGENTS_RESP
    case 19 => BUILD_RESP
    case 20 => BUILDING
    case 21 => BUILDING_LIST
    case 22 => GET_BUILD_PROGRESSIVE
    case 23 => GET_BUILD_PROGRESSIVE_RESP
    case 24 => SYNC_NETWORK
    case 25 => SYNC_NETWORK_RESP
    case 26 => RM_CONTAINER
    case 27 => RM_CONTAINER_RESP
    case 28 => REMOTE_DEPLOY
    case 29 => REMOTE_DEPLOY_RESP
    case 30 => CMD_EVENT
    case 31 => CMD_RESP
    case 32 => CMD_EXITED
    case 99 => ERROR_EVENT
    case _ => unknown(id)
  }

  def findByLabel(label: String): EventType = label match {
    case "nodeReg" => NODE_REG
    case "nodeEvent" => NODE_EVENT
    case "webReg" => WEB_REG
    case "webLeave" => WEB_LEAVE
    case "webEvent" => WEB_EVENT
    case "getServerInfoResp" => GET_SERVER_INFO_RESP
    case "getServerInfo" => GET_SERVER_INFO
    case "deploy" => DEPLOY
    case "deployResp" => DEPLOY_RESP
    case "stop" => STOP
    case "stopResp" => STOP_RESP
    case "restart" => RESTART
    case "restartResp" => RESTART_RESP
    case "getYamlFile" => GET_YAML_FILE
    case "getYamlFileResp" => GET_YAML_FILE_RESP
    case "build" => BUILD
    case "getRegedAgents" => GET_REGED_AGENTS
    case "getRegedAgentsResp" => GET_REGED_AGENTS_RESP
    case "buildResp" => BUILD_RESP
    case "building" => BUILDING
    case "buildingList" => BUILDING_LIST
    case "getBuildProgressive" => GET_BUILD_PROGRESSIVE
    case "getBuildProgressiveResp" => GET_BUILD_PROGRESSIVE_RESP
    case "syncNetwork" => SYNC_NETWORK
    case "syncNetworkResp" => SYNC_NETWORK_RESP
    case "rmContainer" => RM_CONTAINER
    case "rmContainerResp" => RM_CONTAINER_RESP
    case "remoteDeploy" => REMOTE_DEPLOY
    case "remoteDeployResp" => REMOTE_DEPLOY_RESP
    case "cmd" => CMD_EVENT
    case "cmdResult" => CMD_RESP
    case "cmdExit" => CMD_EXITED
    case "errorEvent" => ERROR_EVENT
    case _ => unknown(label)
  }


  def apply(v: String) = findByLabel(v)

  def unapply(v: EventType): Option[Int] = Some(v.id)

  implicit object Accessor extends DbEnumJdbcValueAccessor[EventType](valueOf)

}

