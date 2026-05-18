package ai.aligned.di

import android.content.Context
import ai.aligned.data.AlignedDb
import ai.aligned.net.AlignedApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideApi(): AlignedApi = AlignedApi()

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AlignedDb = AlignedDb.open(ctx)
}
