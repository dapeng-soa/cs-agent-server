package com.github.dapeng.datasource

import javax.annotation.Resource
import javax.sql.DataSource


object ConfigServerDataSource {
  var mysqlData: DataSource = _
}

class ConfigServerDataSource {

  @Resource(name = "configServer_dataSource")
  def setMysqlData(mysqlData: DataSource): Unit = {
    ConfigServerDataSource.mysqlData = mysqlData
  }
}
