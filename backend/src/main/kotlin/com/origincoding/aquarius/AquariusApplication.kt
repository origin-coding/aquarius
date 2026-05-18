package com.origincoding.aquarius

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.modulith.Modulithic

@ConfigurationPropertiesScan("com.origincoding.aquarius")
@Modulithic
@SpringBootApplication
class AquariusApplication

fun main(args: Array<String>) {
	runApplication<AquariusApplication>(*args)
}
