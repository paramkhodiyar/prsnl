package com.prsnl.storage.di;

import android.content.Context;
import com.prsnl.storage.PrsnlDatabase;
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
public final class DatabaseModule_ProvidePrsnlDatabaseFactory implements Factory<PrsnlDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvidePrsnlDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PrsnlDatabase get() {
    return providePrsnlDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvidePrsnlDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvidePrsnlDatabaseFactory(contextProvider);
  }

  public static PrsnlDatabase providePrsnlDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePrsnlDatabase(context));
  }
}
