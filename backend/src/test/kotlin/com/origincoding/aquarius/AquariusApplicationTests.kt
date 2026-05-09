package com.origincoding.aquarius

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.modulith.core.ApplicationModules

@SpringBootTest
class AquariusApplicationTests {

    @Test
    fun `text context loads and module is verified`() {
        ApplicationModules.of(AquariusApplication::class.java).verify()
    }

}
