package com.github.dapeng.datasource

import com.github.dapeng.datasource.ConfigServerDataSource._
import com.github.dapeng.entity.TServiceBuildRecord
import com.github.dapeng.socket.entity.TServiceBuildRecords
import wangzx.scala_commons.sql._


object ConfigServerSql {

  def getBuildServiceRecordById(id: Long) = {
    mysqlData.row[TServiceBuildRecord](sql"select * from t_build_service_records where id = ${id}")
  }

  def updateBuildServiceRecordStatus(id: Long, status: Int) = {
    mysqlData.executeUpdate(sql" update t_build_service_records set status = ${status} where id = ${id} ")
  }

  def updateBuildServiceRecordContent(id: Long, content: String) = {
    mysqlData.executeUpdate(sql" update t_build_service_records set buildLog = ${content} where id = ${id} ")
  }
}
