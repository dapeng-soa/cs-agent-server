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
  //web_client
  val WEB_EVENT = new EventType(4, "webEvent")
  //返回服务器时间结果的事件
  val GET_SERVER_TIME_RESP = new EventType(5, "getServerTimeResp")
  //请求服务器时间事件
  val GET_SERVER_TIME = new EventType(6, "getServerTime")
  //部署事件
  val DEPLOY = new EventType(7, "deploy")
  val DEPLOY_RESP = new EventType(8, "deployResp")
  //停止服务事件
  val STOP = new EventType(9, "stop")
  val STOP_RESP = new EventType(10, "stopResp")
  val RESTART = new EventType(11, "restart")
  val RESTART_RESP = new EventType(12, "restartResp")
  //获取yamlFile事件
  val GET_YAML_FILE = new EventType(13, "getYamlFile")
  //获取yamlFile resp
  val GET_YAML_FILE_RESP = new EventType(14, "getYamlFileResp")
  val GET_SERVICE_STATUS = new EventType(15, "getServiceStatus")
  val GET_SERVICE_STATUS_RESP = new EventType(16, "getServiceStatusResp")
  val ERROR_EVENT = new EventType(99, "errorEvent")

  def unknown(id: Int) = new EventType(id, id + "")

  def valueOf(id: Int): EventType = id match {
    case 1 => NODE_REG
    case 2 => NODE_EVENT
    case 3 => WEB_REG
    case 4 => WEB_EVENT
    case 5 => GET_SERVER_TIME_RESP
    case 6 => GET_SERVER_TIME
    case 7 => DEPLOY
    case 8 => DEPLOY_RESP
    case 9 => STOP
    case 10 => STOP_RESP
    case 11 => RESTART
    case 12 => RESTART_RESP
    case 13 => GET_YAML_FILE
    case 14 => GET_YAML_FILE_RESP
    case 15 => GET_SERVICE_STATUS
    case 16 => GET_SERVICE_STATUS_RESP
    case 99 => ERROR_EVENT
    case _ => unknown(id)
  }

  def apply(v: Int) = valueOf(v)

  def unapply(v: EventType): Option[Int] = Some(v.id)

  implicit object Accessor extends DbEnumJdbcValueAccessor[EventType](valueOf)

}

