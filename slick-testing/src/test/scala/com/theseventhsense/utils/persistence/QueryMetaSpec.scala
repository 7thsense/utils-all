package com.theseventhsense.utils.persistence

import com.theseventhsense.testing.UnitSpec

/**
  * Created by erik on 2/25/16.
  */
class QueryMetaSpec extends UnitSpec {
  "the QueryMeta" should {
    val offset0 = QueryMeta(1, Some(1))
    "be able to construct from an offset" in {
      QueryMeta.fromOffset(0, Some(1)) mustEqual offset0
    }
    "be able to adjust its offset" in {
      offset0.withOffset(0) mustEqual offset0
      offset0.withOffset(1) mustEqual QueryMeta(2, Some(2))
      offset0.withOffset(2) mustEqual QueryMeta(3, Some(3))
    }
  }
}
