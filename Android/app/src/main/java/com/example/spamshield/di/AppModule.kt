package com.example.spamshield.di

import android.content.Context
import androidx.room.Room
import com.example.spamshield.AuthInterceptor
import com.example.spamshield.api.SpamShieldApi
import com.example.spamshield.api.TokenAuthenticator
import com.example.spamshield.data.local.MessageDao
import com.example.spamshield.data.local.SpamShieldDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .authenticator(TokenAuthenticator(context))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://impartial-perception-production-44ad.up.railway.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

    @Provides
    @Singleton
    fun provideSpamShieldApi(retrofit: Retrofit): SpamShieldApi =
        retrofit.create(SpamShieldApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpamShieldDatabase =
        Room.databaseBuilder(context, SpamShieldDatabase::class.java, "spamshield_db").build()

    @Provides
    fun provideMessageDao(db: SpamShieldDatabase): MessageDao = db.messageDao()
}
