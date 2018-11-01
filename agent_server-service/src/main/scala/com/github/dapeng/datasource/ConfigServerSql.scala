package com.github.dapeng.datasource

import com.github.dapeng.datasource.ConfigServerDataSource._
import com.github.dapeng.entity.TServiceBuildRecord
import wangzx.scala_commons.sql._


object ConfigServerSql {

  def getBuildServiceRecordById(id: Long) = {
    mysqlData.row[TServiceBuildRecord](sql"select * from t_service_build_records where id = ${id}")
  }

  def updateBuildServiceRecord(id: Long, status: Int, content: String) = {
    mysqlData.executeUpdate(sql" update t_service_build_records set status = ${status},  build_log = ${content} where id = ${id} ")
  }

//  def updateBuildServiceRecordContent(id: Long, content: String) = {
//    mysqlData.executeUpdate(sql" update t_service_build_records set build_log = ${content} where id = ${id} ")
//  }
}
