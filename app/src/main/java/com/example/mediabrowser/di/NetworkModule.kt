package com.example.mediabrowser.di

import com.example.mediabrowser.BuildConfig
import com.example.mediabrowser.data.remote.ArchiveApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        logging: HttpLoggingInterceptor,
        hostProvider: com.example.mediabrowser.data.remote.ApiHostProvider
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                var request = chain.request()

                // Multi-site support: Retrofit is built against Rule34's API host;
                // when another booru is selected in Settings, rewrite the host on
                // DAPI calls (index.php). Scoped to the api.rule34.xxx host so
                // image/CDN requests are never touched.
                val site = hostProvider.currentSite()
                if (!site.isDefault && request.url.host == "api.rule34.xxx") {
                    val base = site.apiBaseUrl.toHttpUrlOrNull()
                    if (base != null) {
                        request = request.newBuilder()
                            .url(
                                request.url.newBuilder()
                                    .scheme(base.scheme)
                                    .host(base.host)
                                    .port(base.port)
                                    .build()
                            )
                            .build()
                    }
                }

                // Per-site credentials: override any api_key/user_id query params
                // with the pair stored for the CURRENT site, so the caller doesn't
                // have to know which site is active. Empty → strip the param so we
                // don't send Site A's key to Site B.
                run {
                    val cred = hostProvider.currentCredential()
                    val url = request.url
                    if (url.queryParameter("api_key") != null || url.queryParameter("user_id") != null || !cred.isEmpty) {
                        val builder = url.newBuilder()
                            .removeAllQueryParameters("api_key")
                            .removeAllQueryParameters("user_id")
                        if (cred.key.isNotBlank()) builder.addQueryParameter("api_key", cred.key)
                        if (cred.user.isNotBlank()) builder.addQueryParameter("user_id", cred.user)
                        request = request.newBuilder().url(builder.build()).build()
                    }
                }

                val original = request
                // rule34.xxx sits behind Cloudflare, which challenges/blocks requests
                // that don't look like a real browser. Sending a clean browser-style
                // User-Agent (no custom app token) plus standard Accept headers makes
                // the request pass without needing a VPN to change egress IP.
                //
                // NOTE: API credentials are NOT sent as headers — rule34 expects them
                // as `api_key` and `user_id` query params, which ArchiveApi already
                // attaches. The previous Authorization/X-API-Key headers did nothing
                // useful and could themselves trip Cloudflare's WAF.
                val requestBuilder = original.newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                    )
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "en-US,en;q=0.9")

                chain.proceed(requestBuilder.build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.rule34.xxx/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideArchiveApi(retrofit: Retrofit): ArchiveApi =
        retrofit.create(ArchiveApi::class.java)
}