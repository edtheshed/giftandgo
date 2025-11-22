package com.edleshed.giftandgo.ipcheck

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class IpCheckConfig {
    @Bean
    fun ipRestClient(
        builder: RestClient.Builder,
        props: IpCheckProperties,
    ): RestClient =
        builder
            .baseUrl(props.baseUrl)
            .build()
}
