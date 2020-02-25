package com.github.dapeng.entity

import java.sql.Timestamp

case class TServiceBuildRecord(
   id : Long,
   agentHost: String ,
   buildService: String,
   taskId: Long,
   status: Int,
   buildLog: String,
   createdAt: Timestamp,
   createdBy: Int,
   updatedAt: Timestamp
)
