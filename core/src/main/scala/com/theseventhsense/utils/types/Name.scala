package com.theseventhsense.utils.types

case class Name(name: String) extends AnyVal

object Name {
  import octopus.dsl._

  private val allowedSymbols: Set[Char] = Set('~', '!', '@', '#', '$', '%', '^', '&',
    '*', '(', ')', '-', '_', '+', '=', ',', '.', ':', ';', '\'', '"')

  implicit val userIdValidator: Validator[Name] = Validator[Name]
    .rule(_.name.length <= 80, "mus be <= 80 characters")
    .rule(_.name.length >= 3, "must be >= 3 characters")
    .rule(
      _.name.forall(
        c => c.isLetterOrDigit || c.isWhitespace || allowedSymbols.contains(c)
      ),
      "must contain only letters, digits or whitespace"
    )

}
