package com.fleckinger.tmps

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TmpsApplication

fun main(args: Array<String>) {
    runApplication<TmpsApplication>(*args)
}
