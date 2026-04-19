package com.eureka.face.infrastructure.tracer

import net.logstash.logback.encoder.LogstashEncoder
import net.logstash.logback.composite.loggingevent.MessageJsonProvider
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider
import net.logstash.logback.composite.loggingevent.TimestampJsonProvider
import net.logstash.logback.composite.loggingevent.ThreadNameJsonProvider
import net.logstash.logback.composite.loggingevent.MdcJsonProvider
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider
import net.logstash.logback.composite.loggingevent.LoggingEventFieldNames
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class TracerConfiguration {
    
    private val logger = LoggerFactory.getLogger(TracerConfiguration::class.java)
    private val logCounter = AtomicLong(0)
    
    fun configure() {
        val loggerContext = (logger as Logger).loggerContext
        
        val rollingAppender = RollingFileAppender<ILoggingEvent>().apply {
            context = loggerContext
            name = "TRACER_APPENDER"
            file = "logs/tracer.log"
            
            val rollingPolicy = SizeAndTimeBasedRollingPolicy<ILoggingEvent>().apply {
                context = loggerContext
                parent = this@apply
                fileNamePattern = "logs/tracer-%d{yyyy-MM-dd}.%i.log.gz"
                maxFileSize = "100MB"
                maxHistory = 30
                totalSizeCap = "3GB"
            }
            this.rollingPolicy = rollingPolicy
            
            val encoder = LayoutWrappingEncoder<ILoggingEvent>().apply {
                context = loggerContext
                encoder = LogstashEncoder().apply {
                    customFields = """{"service":"face-recognition-api"}"""
                }
            }
            this.encoder = encoder
        }
        
        rollingAppender.start()
        
        val rootLogger = loggerContext.loggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.addAppender(rollingAppender)
        
        logger.info("Tracer configuration initialized with gz compression")
    }
    
    fun getLogGroupId(): Long {
        return logCounter.incrementAndGet()
    }
    
    fun log(groupId: Long, message: String, level: String = "INFO") {
        logger.info("[GROUP:$groupId] $message")
    }
}