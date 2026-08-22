package com.prsnl.storage.di;

import android.content.Context;
import com.prsnl.storage.PageFileStorage;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DatabaseModule_ProvidePageFileStorageFactory implements Factory<PageFileStorage> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvidePageFileStorageFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PageFileStorage get() {
    return providePageFileStorage(contextProvider.get());
  }

  public static DatabaseModule_ProvidePageFileStorageFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvidePageFileStorageFactory(contextProvider);
  }

  public static PageFileStorage providePageFileStorage(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePageFileStorage(context));
  }
}
