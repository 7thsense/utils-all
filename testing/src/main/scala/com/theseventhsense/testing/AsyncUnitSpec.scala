package com.theseventhsense.testing

import cats.scalatest.EitherValues
import com.theseventhsense.utils.logging.Logging
import org.scalatest._
import org.scalatest.mockito.MockitoSugar

trait AsyncUnitSpec
    extends AsyncWordSpecLike
    with Logging
    with MockitoSugar
    with MustMatchers
    with MonadTransformerValues
    with FutureValues
    with EitherValues

//with OptionValues with Inside with Inspectors
