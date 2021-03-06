/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.json

/** JSON configuration */
sealed trait JsonConfiguration {
  /** Compile-time options for the JSON macros */
  type Opts <: Json.MacroOptions

  /** Naming strategy */
  def naming: JsonNaming
}

object JsonConfiguration {
  type Aux[O <: Json.MacroOptions] = JsonConfiguration { type Opts = O }

  private final class Impl[O <: Json.MacroOptions](
    val naming: JsonNaming = JsonNaming.Identity
  ) extends JsonConfiguration {
    type Opts = O
  }

  /**
   * @tparam O the options for the JSON macros
   * @param naming the naming strategy
   */
  def apply[O <: Json.MacroOptions](
    naming: JsonNaming = JsonNaming.Identity
  ): JsonConfiguration.Aux[O] = new Impl(naming)

  /** Default configuration instance */
  implicit def default[Opts <: Json.MacroOptions] = apply[Opts]()
}

/**
 * Naming strategy, to map each class property to the corresponding column.
 */
trait JsonNaming extends (String => String) {
  /**
   * Returns the column name for the class property.
   *
   * @param property the name of the case class property
   */
  def apply(property: String): String
}

/** Naming companion */
object JsonNaming {

  /**
   * For each class property, use the name
   * as is for its column (e.g. fooBar -> fooBar).
   */
  object Identity extends JsonNaming {
    def apply(property: String): String = property
    override val toString = "Identity"
  }

  /**
   * For each class property, use the snake case equivalent
   * to name its column (e.g. fooBar -> foo_bar).
   */
  object SnakeCase extends JsonNaming {
    def apply(property: String): String = {
      val length = property.length
      val result = new StringBuilder(length * 2)
      var resultLength = 0
      var wasPrevTranslated = false
      for (i <- 0 until length) {
        var c = property.charAt(i)
        if (i > 0 || i != '_') {
          if (Character.isUpperCase(c)) {
            // append a underscore if the previous result wasn't translated
            if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
              result.append('_')
              resultLength += 1
            }
            c = Character.toLowerCase(c)
            wasPrevTranslated = true
          } else {
            wasPrevTranslated = false
          }
          result.append(c)
          resultLength += 1
        }
      }

      // builds the final string
      result.toString()
    }

    override val toString = "SnakeCase"
  }

  /** Naming using a custom transformation function. */
  def apply(transformation: String => String): JsonNaming = new JsonNaming {
    def apply(property: String): String = transformation(property)
  }
}