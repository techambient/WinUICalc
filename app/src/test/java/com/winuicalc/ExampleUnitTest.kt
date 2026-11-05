package com.winuicalc

import com.winuicalc.ui.screens.WINCALC_URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun targetUrl_isSecureAndValid() {
    assertEquals("https://wincalc-lilac.vercel.app/", WINCALC_URL)
    assertTrue(WINCALC_URL.startsWith("https://"))
  }
}
