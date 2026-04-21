package com.eureka.face.infrastructure.tracer

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class TracerConfiguration {
    private val logger = LoggerFactory.getLogger(TracerConfiguration::class.java)
    private val logCounter = AtomicLong(0)

    fun getLogGroupId(): Long = logCounter.incrementAndGet()

    fun log(groupId: Long, message: String, level: String = "INFO") {
        when (level.uppercase()) {
            "ERROR" -> logger.error("[GROUP:$groupId] $message")
            "WARN" -> logger.warn("[GROUP:$groupId] $message")
            "DEBUG" -> logger.debug("[GROUP:$groupId] $message")
            else -> logger.info("[GROUP:$groupId] $message")
        }
    }
}