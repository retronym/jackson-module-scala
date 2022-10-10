package tools.jackson.module.scala.deser

import tools.jackson.core.JsonParser
import tools.jackson.databind.`type`.MapLikeType
import tools.jackson.databind.deser.Deserializers
import tools.jackson.databind.deser.std.{ContainerDeserializerBase, StdValueInstantiator}
import tools.jackson.databind.jsontype.TypeDeserializer
import tools.jackson.databind._
import tools.jackson.databind.deser.jdk.MapDeserializer
import tools.jackson.module.scala.{DefaultScalaModule, IteratorModule}

import java.util
import scala.collection.JavaConverters._
import scala.collection.immutable.IntMap

/**
 * Adds support for deserializing Scala [[scala.collection.immutable.IntMap]]s. Scala IntMaps can already be
 * serialized using [[IteratorModule]] or [[DefaultScalaModule]].
 *
 * @since 2.14.0
 */
private[deser] object IntMapDeserializerResolver extends Deserializers.Base {

  private val intMapClass = classOf[IntMap[_]]

  override def findMapLikeDeserializer(theType: MapLikeType,
                                       config: DeserializationConfig,
                                       beanDesc: BeanDescription,
                                       keyDeserializer: KeyDeserializer,
                                       elementTypeDeserializer: TypeDeserializer,
                                       elementDeserializer: ValueDeserializer[_]): ValueDeserializer[_] = {
    if (!intMapClass.isAssignableFrom(theType.getRawClass)) None.orNull
    else {
      val mapDeserializer = new MapDeserializer(theType, new IntMapInstantiator(config, theType), keyDeserializer,
        elementDeserializer.asInstanceOf[ValueDeserializer[AnyRef]], elementTypeDeserializer)
      new IntMapDeserializer(theType, mapDeserializer)
    }
  }

  override def hasDeserializerFor(config: DeserializationConfig, valueType: Class[_]): Boolean = {
    intMapClass.isAssignableFrom(valueType)
  }

  private class IntMapDeserializer[V](mapType: MapLikeType, containerDeserializer: MapDeserializer)
    extends ContainerDeserializerBase[IntMap[V]](mapType) {

    override def getContentType: JavaType = containerDeserializer.getContentType

    override def getContentDeserializer: ValueDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

    override def createContextual(ctxt: DeserializationContext, property: BeanProperty): ValueDeserializer[_] = {
      val newDelegate = containerDeserializer.createContextual(ctxt, property).asInstanceOf[MapDeserializer]
      new IntMapDeserializer(mapType, newDelegate)
    }

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext): IntMap[V] = {
      containerDeserializer.deserialize(jp, ctxt) match {
        case wrapper: BuilderWrapper => wrapper.asIntMap()
      }
    }

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext, intoValue: IntMap[V]): IntMap[V] = {
      val newMap = deserialize(jp, ctxt)
      if (newMap.isEmpty) {
        intoValue
      } else {
        intoValue ++ newMap
      }
    }

    override def getEmptyValue(ctxt: DeserializationContext): Object = IntMap.empty[V]
  }

  private class IntMapInstantiator(config: DeserializationConfig, mapType: MapLikeType) extends StdValueInstantiator(config, mapType) {
    override def canCreateUsingDefault = true
    override def createUsingDefault(ctxt: DeserializationContext) = new BuilderWrapper
  }

  private class BuilderWrapper extends util.AbstractMap[Object, Object] {
    var baseMap = IntMap[Object]()

    override def put(k: Object, v: Object): Object = {
      k match {
        case n: Number => baseMap += (n.intValue() -> v)
        case s: String => baseMap += (s.toInt -> v)
        case _ => {
          val typeName = Option(k) match {
            case Some(n) => n.getClass.getName
            case _ => "null"
          }
          throw new IllegalArgumentException(s"IntMap does not support keys of type $typeName")
        }
      }
      v
    }

    // Used by the deserializer when using readerForUpdating
    override def get(key: Object): Object = key match {
      case n: Number => baseMap.get(n.intValue()).orNull
      case s: String => baseMap.get(s.toInt).orNull
      case _ => None.orNull
    }

    // Isn't used by the deserializer
    override def entrySet(): java.util.Set[java.util.Map.Entry[Object, Object]] =
      baseMap.asJava.entrySet().asInstanceOf[java.util.Set[java.util.Map.Entry[Object, Object]]]

    def asIntMap[V](): IntMap[V] = baseMap.asInstanceOf[IntMap[V]]
  }
}
