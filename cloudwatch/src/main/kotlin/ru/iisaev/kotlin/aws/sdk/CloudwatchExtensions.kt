package ru.iisaev.kotlin.aws.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.future.await
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest
import software.amazon.awssdk.services.cloudwatch.model.MetricDataResult
import java.util.concurrent.ConcurrentHashMap


private val clientByRegion by lazy { ConcurrentHashMap<Region, CloudWatchAsyncClient>() }
fun SdkAsyncHttpClient.cloudwatch(region: Region) = clientByRegion.computeIfAbsent(region) {
    CloudWatchAsyncClient.builder().httpClient(this).region(it).build()
}

@ExperimentalCoroutinesApi
suspend fun CloudWatchAsyncClient.getMetricDataAsync(rq: (GetMetricDataRequest.Builder) -> Unit): Flow<MetricDataResult> = flow {
    var nextToken: String? = null
    do {
        val result = getMetricData() { it.nextToken(nextToken).maxDatapoints(5000).applyMutation(rq) }.await()
        result.metricDataResults().forEach { emit(it) }
        nextToken = result.nextToken()
    } while (nextToken != null)
}.flowOn(Dispatchers.IO)