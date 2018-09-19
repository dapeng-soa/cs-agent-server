package com.github.dapeng

import com.google.gson.Gson

object GsonTest {

  def main(args: Array[String]): Unit = {
    val objs = new java.util.ArrayList[Obj]()
    objs.add(Obj("test"))
    objs.add(Obj("test1"))

    val gson = new Gson();
    val result = gson.toJson(objs)
    println(result)

    val r1 = gson.fromJson(result, classOf[java.util.ArrayList[Obj]])

    println(r1)
  }


  case class Obj(name: String)
}
