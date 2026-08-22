package com.prsnl.storage.di;

import com.prsnl.document.repository.FolderRepository;
import com.prsnl.storage.dao.FolderDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class DatabaseModule_ProvideFolderRepositoryFactory implements Factory<FolderRepository> {
  private final Provider<FolderDao> folderDaoProvider;

  public DatabaseModule_ProvideFolderRepositoryFactory(Provider<FolderDao> folderDaoProvider) {
    this.folderDaoProvider = folderDaoProvider;
  }

  @Override
  public FolderRepository get() {
    return provideFolderRepository(folderDaoProvider.get());
  }

  public static DatabaseModule_ProvideFolderRepositoryFactory create(
      Provider<FolderDao> folderDaoProvider) {
    return new DatabaseModule_ProvideFolderRepositoryFactory(folderDaoProvider);
  }

  public static FolderRepository provideFolderRepository(FolderDao folderDao) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideFolderRepository(folderDao));
  }
}
