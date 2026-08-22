package com.prsnl.storage.di;

import com.prsnl.document.repository.NotebookRepository;
import com.prsnl.storage.PageFileStorage;
import com.prsnl.storage.dao.NotebookDao;
import com.prsnl.storage.dao.PageDao;
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
public final class DatabaseModule_ProvideNotebookRepositoryFactory implements Factory<NotebookRepository> {
  private final Provider<NotebookDao> notebookDaoProvider;

  private final Provider<PageDao> pageDaoProvider;

  private final Provider<PageFileStorage> fileStorageProvider;

  public DatabaseModule_ProvideNotebookRepositoryFactory(Provider<NotebookDao> notebookDaoProvider,
      Provider<PageDao> pageDaoProvider, Provider<PageFileStorage> fileStorageProvider) {
    this.notebookDaoProvider = notebookDaoProvider;
    this.pageDaoProvider = pageDaoProvider;
    this.fileStorageProvider = fileStorageProvider;
  }

  @Override
  public NotebookRepository get() {
    return provideNotebookRepository(notebookDaoProvider.get(), pageDaoProvider.get(), fileStorageProvider.get());
  }

  public static DatabaseModule_ProvideNotebookRepositoryFactory create(
      Provider<NotebookDao> notebookDaoProvider, Provider<PageDao> pageDaoProvider,
      Provider<PageFileStorage> fileStorageProvider) {
    return new DatabaseModule_ProvideNotebookRepositoryFactory(notebookDaoProvider, pageDaoProvider, fileStorageProvider);
  }

  public static NotebookRepository provideNotebookRepository(NotebookDao notebookDao,
      PageDao pageDao, PageFileStorage fileStorage) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideNotebookRepository(notebookDao, pageDao, fileStorage));
  }
}
