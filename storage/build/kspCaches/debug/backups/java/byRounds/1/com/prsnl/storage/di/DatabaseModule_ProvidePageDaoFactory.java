package com.prsnl.storage.di;

import com.prsnl.storage.PrsnlDatabase;
import com.prsnl.storage.dao.PageDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class DatabaseModule_ProvidePageDaoFactory implements Factory<PageDao> {
  private final Provider<PrsnlDatabase> databaseProvider;

  public DatabaseModule_ProvidePageDaoFactory(Provider<PrsnlDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PageDao get() {
    return providePageDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvidePageDaoFactory create(
      Provider<PrsnlDatabase> databaseProvider) {
    return new DatabaseModule_ProvidePageDaoFactory(databaseProvider);
  }

  public static PageDao providePageDao(PrsnlDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePageDao(database));
  }
}
