//package com.github.dapeng.datasource
//import ConfigServerDataSource._
//import com.github.dapeng.socket.entity.TServiceBuildRecords
//import wangzx.scala_commons.sql._
//
//object ConfigServerSql {
//
//  def getBuildServiceRecordById(id: Long) = {
//    mysqlData.row[TServiceBuildRecords](sql"select * from t_build_service_records where id = ${id}").get
//  }
//
//  def updateBuildServiceRecordStatus(id: Long, status: Int) = {
//    mysqlData.executeUpdate(sql" update t_build_service_records set status = ${status} where id = ${id} ")
//  }
//}
