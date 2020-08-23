package moped.internal.macros

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox

import moped.cli._
import moped.json._
import moped.macros.ClassShaper
import moped.macros._

object Macros
class Macros(val c: blackbox.Context) {
  import c.universe._
  def assumeClass[T: c.WeakTypeTag]: Type = {
    val T = weakTypeOf[T]
    if (!T.typeSymbol.isClass || !T.typeSymbol.asClass.isCaseClass)
      c.abort(c.enclosingPosition, s"$T must be a case class")
    T
  }

  def params(T: Type): List[Symbol] = {
    val paramss = T.typeSymbol.asClass.primaryConstructor.asMethod.paramLists
    if (paramss.lengthCompare(1) > 0) {
      c.abort(
        c.enclosingPosition,
        s"${T.typeSymbol} has a curried parameter list, which is not supported."
      )
    }
    paramss.head
  }

  def deriveJsonCodecImpl[T: c.WeakTypeTag](default: Tree): Tree = {
    val T = assumeClass[T]
    q"""
        {
          implicit lazy val classDefinition: _root_.moped.macros.ClassShaper[$T] = _root_.moped.macros.deriveShaper[$T]
          _root_.moped.json.JsonCodec.encoderDecoderJsonCodec[$T](
            classDefinition,
            _root_.moped.macros.deriveEncoder[$T],
            _root_.moped.macros.deriveDecoder[$T]($default)
          )
        }
     """
  }

  def deriveCommandParserImpl[T: c.WeakTypeTag](default: Tree): Tree = {
    val T = assumeClass[T]
    q"_root_.moped.cli.CommandParser.fromCodec(_root_.moped.macros.deriveCodec[$T]($default), $default)"
  }

  def deriveJsonEncoderImpl[T: c.WeakTypeTag]: Tree = {
    val T = assumeClass[T]
    val params = this.params(T)
    val writes = params.map { param =>
      val name = param.name.decodedName.toString
      val select = Select(q"value", param.name)
      val encoder =
        q"_root_.scala.Predef.implicitly[_root_.moped.json.JsonEncoder[${param.info}]]"
      q"_root_.moped.json.JsonMember(_root_.moped.json.JsonString($name), $encoder.encode($select))"
    }
    val result = q"""
       new ${weakTypeOf[JsonEncoder[T]]} {
         override def encode(value: ${weakTypeOf[T]}): _root_.moped.json.JsonObject = {
           new _root_.moped.json.JsonObject(
             _root_.scala.List.apply(..$writes)
           )
         }
       }
     """
    result
  }

  def deriveJsonDecoderImpl[T: c.WeakTypeTag](default: Tree): Tree = {
    val T = assumeClass[T]
    val Tclass = T.typeSymbol.asClass
    val settings = c.inferImplicitValue(weakTypeOf[ClassShaper[T]])
    if (settings == null || settings.isEmpty) {
      c.abort(
        c.enclosingPosition,
        s"Missing implicit for ${weakTypeOf[ClassShaper[T]]}]. " +
          s"Hint, add `implicit val surface: ${weakTypeOf[ClassShaper[T]]}` " +
          s"to the companion ${T.companion.typeSymbol}"
      )
    }
    val paramss = Tclass.primaryConstructor.asMethod.paramLists
    if (paramss.size > 1) {}
    if (paramss.head.isEmpty)
      return q"_root_.moped.json.JsonDecoder.constant($default)"

    val (head :: params) :: Nil = paramss
    def next(param: Symbol): Tree = {
      if (param.info <:< typeOf[Application]) {
        q"_root_.moped.json.ValueResult(context.app)"
      } else {
        val P = param.info.resultType
        val name = param.name.decodedName.toString
        val getter = T.member(param.name)
        val fallback = q"tmp.$getter"
        val next =
          q"_root_.moped.internal.json.DrillIntoJson.getOrElse[$P](conf, $fallback, settings.get($name).get, context)"
        next
      }
    }
    val product = params.foldLeft(next(head)) {
      case (accum, param) => q"$accum.product(${next(param)})"
    }
    val tupleExtract = 1.to(params.length).foldLeft(q"t": Tree) {
      case (accum, _) => q"$accum._1"
    }
    var curr = tupleExtract
    val args = 0.to(params.length).map { _ =>
      val old = curr
      curr = curr match {
        case q"$qual._1" =>
          q"$qual._2"
        case q"$qual._1._2" =>
          q"$qual._2"
        case q"$qual._2" =>
          q"$qual"
        case q"t" => q"t"
      }
      old
    }
    val ctor = q"new $T(..$args)"

    val result = q"""
       new ${weakTypeOf[JsonDecoder[T]]} {
         override def decode(
             context: ${weakTypeOf[DecodingContext]}
         ): ${weakTypeOf[DecodingResult[T]]} = {
           val conf = context.json
           val settings = $settings
           _root_.moped.internal.json.FatalUnknownFieldDecoder.check(settings, context) match {
             case _root_.scala.Some(error) => return _root_.moped.json.ErrorResult(error)
             case _ =>
           }
           val tmp = $default
           $product.map { t =>
             $ctor
           }
         }
       }
     """
    result
  }

  def deriveShaperImpl[T: c.WeakTypeTag]: Tree = {
    val T = weakTypeOf[T]
    if (!T.typeSymbol.isClass || !T.typeSymbol.asClass.isCaseClass)
      c.abort(c.enclosingPosition, s"$T must be a case class")
    val Tclass = T.typeSymbol.asClass
    val ctor = Tclass.primaryConstructor.asMethod
    val argss = ctor.paramLists.map { params =>
      val fields = params.map { param =>
        val paramTpe = param.info.resultType
        val baseAnnots = param.annotations.collect {
          case annot if annot.tree.tpe <:< typeOf[StaticAnnotation] =>
            annot.tree
        }
        val isMap = paramTpe <:< typeOf[Map[_, _]]
        val isConf = paramTpe <:< typeOf[JsonElement]
        val isIterable = paramTpe <:< typeOf[Iterable[_]] && !isMap
        val repeated =
          if (isIterable) {
            q"new _root_.moped.annotations.Repeated" :: Nil
          } else {
            Nil
          }
        val dynamic =
          if (isMap || isConf) {
            q"new _root_.moped.annotations.Dynamic" :: Nil
          } else {
            Nil
          }
        val flag =
          if (paramTpe <:< typeOf[Boolean]) {
            q"new _root_.moped.annotations.Flag" :: Nil
          } else {
            Nil
          }
        val hidden =
          if (paramTpe <:< typeOf[Application]) {
            q"new _root_.moped.annotations.Hidden" :: Nil
          } else {
            Nil
          }

        val completerType = c.internal.typeRef(
          NoPrefix,
          weakTypeOf[Completer[_]].typeSymbol,
          paramTpe :: Nil
        )
        val completerInferred = c.inferImplicitValue(completerType)

        val tabCompletePath =
          if (completerInferred == null || completerInferred.isEmpty) {
            Nil
          } else {
            q"new _root_.moped.annotations.TabCompleter($completerInferred)" :: Nil
          }

        val finalAnnots =
          repeated ::: dynamic ::: flag ::: hidden ::: tabCompletePath ::: baseAnnots
        val fieldsParamTpe = c.internal.typeRef(
          NoPrefix,
          weakTypeOf[ClassShaper[_]].typeSymbol,
          paramTpe :: Nil
        )
        val underlyingInferred = c.inferImplicitValue(fieldsParamTpe)
        val underlying =
          if (underlyingInferred == null || underlyingInferred.isEmpty) {
            q"_root_.scala.None"
          } else {
            q"_root_.scala.Some.apply($underlyingInferred)"
          }
        val tprint = c.internal.typeRef(
          NoPrefix,
          weakTypeOf[pprint.TPrint[_]].typeSymbol,
          paramTpe :: Nil
        )
        val tpeString = c.inferImplicitValue(tprint)

        val field = q"""new ${weakTypeOf[ParameterShape]}(
           ${param.name.decodedName.toString},
           $tpeString.render,
           _root_.scala.List.apply(..$finalAnnots),
           $underlying
         )"""
        field
      }
      val args = q"_root_.scala.List.apply(..$fields)"
      args
    }
    val args = q"_root_.scala.List.apply(..$argss)"
    val classAnnotations = Tclass.annotations.collect {
      case annot if annot.tree.tpe <:< typeOf[StaticAnnotation] =>
        annot.tree
    }
    val result =
      q"""_root_.moped.macros.ClassShaper.apply[${weakTypeOf[T]}](
            new ${weakTypeOf[ClassShape]}(
              ${T.typeSymbol.name.decodedName.toString()},
              ${T.typeSymbol.fullName},
              $args,
              _root_.scala.List.apply(..$classAnnotations)
            )
          )"""
    c.untypecheck(result)
  }

}
