package com.prsnl.storage.di;

import com.prsnl.storage.PrsnlDatabase;
import com.prsnl.storage.dao.FolderDao;
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
public final class DatabaseModule_ProvideFolderDaoFactory implements Factory<FolderDao> {
  private final Provider<PrsnlDatabase> databaseProvider;

  public DatabaseModule_ProvideFolderDaoFactory(Provider<PrsnlDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public FolderDao get() {
    return provideFolderDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideFolderDaoFactory create(
      Provider<PrsnlDatabase> databaseProvider) {
    return new DatabaseModule_ProvideFolderDaoFactory(databaseProvider);
  }

  public static FolderDao provideFolderDao(PrsnlDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideFolderDao(database));
  }
}
