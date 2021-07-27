package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.collection.Map

//see also MapSerializerTest for tests that also pass with Scala3
class MapScala2SerializerTest extends SerializerTest {
  def module = DefaultScalaModule

  "MapSerializer" should "correctly serialize type information" in {
    val wrapper = new {
      val map = Map.apply[String, MapValueBase]("Double" -> MapValueDouble(1.0), "String" -> MapValueString("word"))
      //val map = ImmutableMap.of[String, MapValueBase]("Double", MapValueDouble(1.0), "String", MapValueString("word"))
    }
    serialize(wrapper) should be ("""{"map":{"Double":{"type":"MapValueDouble","value":1.0},"String":{"type":"MapValueString","value":"word"}}}""")
  }
  it should "suppress None when WRITE_NULL_MAP_VALUES is active" in {
    val wrapper = new {
      val map = Map("key" -> None)
    }
    val m = newMapper.copy()
    m.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
    val v = m.writeValueAsString(wrapper)
    v shouldBe """{"map":{}}"""
  }
}
